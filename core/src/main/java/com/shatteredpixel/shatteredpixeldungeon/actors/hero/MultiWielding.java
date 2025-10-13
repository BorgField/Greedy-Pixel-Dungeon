package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.watabou.utils.QuietCallable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiWielding {
    private final Hero hero;
    private int currentAttackIndex = 0;
    private final KindOfWeapon[] weapons = new KindOfWeapon[4];
    private final boolean[] canAttack = new boolean[4];

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

    public KindOfWeapon currentWeapon() {
        if (currentAttackIndex >= 0 && currentAttackIndex < weapons.length) {
            return weapons[currentAttackIndex];
        }
        return null;
    }

    public void nextWeapon() {
        int startIndex = currentAttackIndex;
        do {
            currentAttackIndex = (currentAttackIndex + 1) % 4;
        } while (weapons[currentAttackIndex] == null && currentAttackIndex != startIndex);
    }

    public boolean weaponCanAttack(Char owner, Char enemy) {
        if (enemy == null || !Actor.chars().contains(enemy)) {
            Arrays.fill(canAttack, false);
            return false;
        }

        updateWeapons(); // 确保武器是最新的

        for (int i = 0; i < 4; i++) {
            KindOfWeapon wep = weapons[i];
            canAttack[i] = wep != null && wep.canReach(owner, enemy.pos);
        }
        return canAttack[0] || canAttack[1] || canAttack[2] || canAttack[3];
    }

    public int weaponDamageRoll(Char owner) {
        KindOfWeapon wep = currentWeapon();
        if (wep == null) return 0;

        int baseDamage = wep.damageRoll(owner);
        int weaponIndex = getCurrentWeaponIndex();

        float groupMultiplier = calculateGroupMultiplier(weaponIndex);
        return Math.round(baseDamage * groupMultiplier);
    }

    public int weaponProc(Char attacker, Char defender, int damage) {
        KindOfWeapon wep = currentWeapon();
        if (wep == null) return damage;

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
            return calculateGroup1Multiplier(weaponIndex);
        } else {
            return calculateGroup2Multiplier(weaponIndex);
        }
    }

    private float calculateGroup1Multiplier(int weaponIndex) {
        boolean hasWeapon0 = weapons[0] != null;
        boolean hasWeapon1 = weapons[1] != null;

        if (weaponIndex == 0) {
            // 武器0：双持时75%，单独时100%
            return (hasWeapon0 && hasWeapon1) ? 0.75f : 1.0f;
        } else if (weaponIndex == 1) {
            // 武器1：双持时50%，单独时100%
            return (hasWeapon0 && hasWeapon1) ? 0.5f : 1.0f;
        }

        return 1.0f;
    }

    private float calculateGroup2Multiplier(int weaponIndex) {
        boolean hasWeapon2 = weapons[2] != null;
        boolean hasWeapon3 = weapons[3] != null;

        if (weaponIndex == 2) {
            // 武器2：双持时75%，单独时100%
            return (hasWeapon2 && hasWeapon3) ? 0.75f : 1.0f;
        } else if (weaponIndex == 3) {
            // 武器3：双持时50%，单独时100%
            return (hasWeapon2 && hasWeapon3) ? 0.5f : 1.0f;
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

        if (availableWeapons.isEmpty()) {
            return false;
        }

        float totalDelay = 0f;
        for (KindOfWeapon w : availableWeapons) {
            totalDelay += w.delayFactor(hero);
        }
        float averageDelay = totalDelay / availableWeapons.size();

        boolean anyHit = false;
        KindOfWeapon originalAbilityWeapon = hero.belongings.abilityWeapon;

        anyHit = executeGroupedAttacks(enemy, dmgMulti, dmgBonus, accMulti, availableWeapons, originalAbilityWeapon);

        hero.belongings.abilityWeapon = originalAbilityWeapon;
        hero.spend(averageDelay);

        return anyHit;
    }

    private boolean executeGroupedAttacks(Char enemy, float dmgMulti, float dmgBonus, float accMulti,
                                          List<KindOfWeapon> availableWeapons, KindOfWeapon originalAbilityWeapon) {
        boolean anyHit = false;

        // 第一组攻击
        boolean group1Hit = executeGroupAttack(enemy, dmgMulti, dmgBonus, accMulti, availableWeapons,
                originalAbilityWeapon, 0, 1);
        anyHit = anyHit || group1Hit;

        if (!enemy.isAlive()) return anyHit;

        // 第二组攻击
        boolean group2Hit = executeGroupAttack(enemy, dmgMulti, 0, accMulti, availableWeapons,
                originalAbilityWeapon, 2, 3);
        anyHit = anyHit || group2Hit;

        return anyHit;
    }

    private boolean executeGroupAttack(Char enemy, float dmgMulti, float dmgBonus, float accMulti,
                                       List<KindOfWeapon> availableWeapons, KindOfWeapon originalAbilityWeapon,
                                       int weaponIndex1, int weaponIndex2) {
        boolean anyHit = false;

        KindOfWeapon weapon1 = weaponIndex1 < weapons.length ? weapons[weaponIndex1] : null;
        KindOfWeapon weapon2 = weaponIndex2 < weapons.length ? weapons[weaponIndex2] : null;

        boolean hasWeapon1 = weapon1 != null && availableWeapons.contains(weapon1);
        boolean hasWeapon2 = weapon2 != null && availableWeapons.contains(weapon2);

        // 计算是否为双持状态
        boolean isDualWielding = hasWeapon1 && hasWeapon2;

        if (hasWeapon1) {
            hero.belongings.abilityWeapon = weapon1;
            float currentDmgMulti = dmgMulti;

            // 武器0/2：双持时75%，单独时100%
            if (isDualWielding) {
                currentDmgMulti *= 0.75f;
            }

            boolean hit = hero.attack(enemy, currentDmgMulti, dmgBonus, accMulti);
            anyHit = anyHit || hit;
            if (!enemy.isAlive()) return anyHit;
        }

        if (hasWeapon2) {
            hero.belongings.abilityWeapon = weapon2;
            float currentDmgMulti = dmgMulti;

            // 武器1/3：双持时50%，单独时100%
            if (isDualWielding) {
                currentDmgMulti *= 0.5f;
            }

            // 第二组攻击不享受伤害加成
            float currentDmgBonus = (weaponIndex2 < 2) ? dmgBonus : 0;

            boolean hit = hero.attack(enemy, currentDmgMulti, currentDmgBonus, accMulti);
            anyHit = anyHit || hit;
            if (!enemy.isAlive()) return anyHit;
        }

        return anyHit;
    }
}