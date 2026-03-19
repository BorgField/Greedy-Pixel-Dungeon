package com.shatteredpixel.shatteredpixeldungeon.actors.hero;


import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class MultiWielding {
    private final Hero hero;
    private int currentAttackIndex = 0;
    private final KindOfWeapon[] weapons = new KindOfWeapon[4];

    public MultiWielding(Hero hero) {
        this.hero = hero;
        updateWeapons();
    }

    // 更新武器数组
    public void updateWeapons() {
        weapons[0] = hero.belongings.weapon;
        weapons[1] = hero.belongings.weapon2;
        weapons[2] = hero.belongings.weapon3;
        weapons[3] = hero.belongings.weapon4;
    }

    public boolean weaponNotNull() {
        return weapons[0] != null
                || weapons[1] != null
                || weapons[2] != null
                || weapons[3] != null;
    }

    public KindOfWeapon currentWeapon() {
        if (currentAttackIndex >= 0 && currentAttackIndex < weapons.length) {
            return weapons[currentAttackIndex];
        }
        return null;
    }

    public int weaponProc(Char attacker, Char defender, int damage) {
        KindOfWeapon wep = currentWeapon();
        if (wep == null) return damage;

        // 远程武器不参与双持，直接返回原值
        if (wep instanceof MissileWeapon || wep instanceof SpiritBow) {
            return wep.proc(attacker, defender, damage);
        }

        return calculateWeaponProc(attacker, defender, damage, wep);
    }

    // 统一处理武器proc计算
    private int calculateWeaponProc(Char attacker, Char defender, int damage, KindOfWeapon wep) {
        int weaponIndex = getCurrentWeaponIndex();
        float groupMultiplier = calculateGroupMultiplier(weaponIndex);

        int baseProcDamage = wep.proc(attacker, defender, damage);
        if (baseProcDamage == damage) {
            return baseProcDamage;
        }

        int extraProcDamage = baseProcDamage - damage;
        int adjustedExtraDamage = Math.round(extraProcDamage * groupMultiplier);
        return damage + adjustedExtraDamage;
    }

    private int getCurrentWeaponIndex() {
        return currentAttackIndex;
    }

    private float calculateGroupMultiplier(int weaponIndex) {
        if (weaponIndex < 2) {
            return calculateGroupMultiplier(weaponIndex, 0, 1);
        } else {
            return calculateGroupMultiplier(weaponIndex, 2, 3);
        }
    }

    // 通用的组倍率计算方法
    private float calculateGroupMultiplier(int weaponIndex, int firstWeaponIndex, int secondWeaponIndex) {
        boolean hasFirstWeapon = weapons[firstWeaponIndex] != null;
        boolean hasSecondWeapon = weapons[secondWeaponIndex] != null;
        boolean isDualWielding = hasFirstWeapon && hasSecondWeapon;
    
        if (weaponIndex == firstWeaponIndex) {
            // 主武器：双持时 75%，单独时 100%
            return isDualWielding ? 0.75f : 1.0f;
        } else if (weaponIndex == secondWeaponIndex) {
            // 副武器：双持时 50%，单独时 100%
            return isDualWielding ? 0.5f : 1.0f;
        }

        return 1.0f;
    }

    public boolean isAttack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
        updateWeapons(); // 确保武器是最新的

        List<KindOfWeapon> availableWeapons = new ArrayList<>();
        for (KindOfWeapon w : weapons) {
            if (w != null && w.canReach(hero, enemy.pos)) {
                availableWeapons.add(w);
            }
        }

        // 如果没有武器，使用空手攻击
        if (availableWeapons.isEmpty()) {
            boolean hit = hero.attack(enemy, dmgMulti, dmgBonus, accMulti);
            hero.spend(hero.attackDelay());
            return hit;
        }

        float totalDelay = 0f;
        for (KindOfWeapon w : availableWeapons) {
            totalDelay += w.delayFactor(hero);
        }
        float averageDelay = totalDelay / availableWeapons.size();

        // 执行分组攻击
        boolean anyHit = executeGroupedAttacks(enemy, dmgMulti, dmgBonus, accMulti, availableWeapons);

        hero.spend(averageDelay);
        return anyHit;
    }

    private boolean executeGroupedAttacks(Char enemy, float dmgMulti, float dmgBonus, float accMulti,
                                          List<KindOfWeapon> availableWeapons) {
        boolean anyHit = false;
        KindOfWeapon originalAbilityWeapon = hero.belongings.abilityWeapon;

        // 第一组攻击
        boolean group1Hit = executeGroupAttack(enemy, dmgMulti, dmgBonus, accMulti, availableWeapons,
                0, 1);
        anyHit = anyHit || group1Hit;

        if (!enemy.isAlive()) return anyHit;

        // 第二组攻击
        boolean group2Hit = executeGroupAttack(enemy, dmgMulti, 0, accMulti, availableWeapons,
                2, 3);
        anyHit = anyHit || group2Hit;

        hero.belongings.abilityWeapon = originalAbilityWeapon;

        return anyHit;
    }

    private boolean executeGroupAttack(
            Char enemy, float dmgMulti, float dmgBonus, float accMulti,
            List<KindOfWeapon> availableWeapons, int weaponIndex1, int weaponIndex2) {

        boolean anyHit = false;

        // 执行第一个武器的攻击
        anyHit = executeWeaponAttack(
                enemy, dmgMulti, dmgBonus, accMulti, availableWeapons,
                weaponIndex1, weaponIndex2, true) || anyHit;
        
        // 如果敌人还活着，执行第二个武器的攻击
        if (enemy.isAlive()) {
            anyHit = executeWeaponAttack(
                    enemy, dmgMulti, 0, accMulti, availableWeapons,
                    weaponIndex2, weaponIndex1, false) || anyHit;
        }
        
        return anyHit;
    }
    
    // 通用的武器攻击执行方法
    private boolean executeWeaponAttack(Char enemy, float dmgMulti, float dmgBonus, float accMulti,
                                       List<KindOfWeapon> availableWeapons,
                                       int weaponIndex, int otherWeaponIndex, boolean isFirstWeapon) {

        KindOfWeapon weapon = weapons[weaponIndex];
        KindOfWeapon otherWeapon = weapons[otherWeaponIndex];
        
        boolean hasWeapon = weapon != null && availableWeapons.contains(weapon);
        boolean hasOtherWeapon = otherWeapon != null && availableWeapons.contains(otherWeapon);
        
        if (!hasWeapon) {
            return false;
        }

        // 远程武器不参与双持，单独处理
        if (weapon instanceof MissileWeapon || weapon instanceof SpiritBow) {
            hero.belongings.abilityWeapon = weapon;
            return hero.attack(enemy, dmgMulti, dmgBonus, accMulti);
        }

        hero.belongings.abilityWeapon = weapon;
        float currentDmgMulti = dmgMulti;
        
        // 计算是否为双持状态
        boolean isDualWielding = hasWeapon && hasOtherWeapon;
        
        // 主武器：双持时 75%，单独时 100%
        // 副武器：双持时 50%，单独时 100%
        if (isDualWielding) {
            currentDmgMulti *= isFirstWeapon ? 0.75f : 0.5f;
        }
        
        // 第二组攻击不享受伤害加成
        float currentDmgBonus = isFirstWeapon ? dmgBonus : 0;

        return hero.attack(enemy, currentDmgMulti, currentDmgBonus, accMulti);
    }

    // 防御计算 - 累加所有武器的防御加成
    public int weaponDefenseFactor(Char owner) {
        int defenceFactor = 0;
        for (KindOfWeapon w : weapons) {
            if (w != null) {
                defenceFactor += w.defenseFactor(owner);
            }
        }
        return defenceFactor;
    }
}