package com.shatteredpixel.shatteredpixeldungeon.custom.testmode;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CrystalGuardianSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class TestRat extends Mob {

    {
        spriteClass = CrystalGuardianSprite.class;

        HP = HT = 200;
        defenseSkill = 0;

        EXP = 0;
        maxLvl = -2;

        state = PASSIVE;

        properties.add(Property.IMMOVABLE);
        properties.add(Property.STATIC);
    }

    private boolean recovering = false;

    public boolean recovering(){
        return recovering;
    }

    @Override
    protected boolean act() {
        if (recovering){
            HP = Math.min(HT, HP + 10);
            if (Dungeon.level.heroFOV[pos]) {
                sprite.showStatusWithIcon(CharSprite.POSITIVE, "10", FloatingText.HEALING);
            }
            if (HP == HT){
                recovering = false;
                state = PASSIVE;
            }
            spend(TICK);
            return true;
        }
        return super.act();
    }

    @Override
    public int damageRoll() {
        return 0;
    }

    @Override
    public int attackSkill( Char target ) {
        return 0;
    }

    @Override
    public int defenseSkill(Char enemy) {
        if (recovering) return 0;
        else            return super.defenseSkill(enemy);
    }

    @Override
    public boolean reset() {
        return true;
    }

    @Override
    public int defenseProc(Char enemy, int damage) {
        if (recovering){
            sprite.showStatusWithIcon(CharSprite.NEGATIVE, Integer.toString(damage), FloatingText.PHYS_DMG_NO_BLOCK);
            HP = Math.max(1, HP-damage);
            damage = -1;
        }
        return super.defenseProc(enemy, damage);
    }


    @Override
    public boolean isAlive() {
        if (HP <= 0){
            HP = 1;
            for (Buff b : buffs()){
                if (!(b instanceof Doom)) {
                    b.detach();
                }
            }
            if (!recovering) {
                recovering = true;
                Bestiary.setSeen(getClass());
                Bestiary.countEncounter(getClass());
            }
        }
        return super.isAlive();
    }

    public TestRat(){
        super();
        switch (Random.Int(3)){
            case 0: default:
                spriteClass = CrystalGuardianSprite.Blue.class;
                break;
            case 1:
                spriteClass = CrystalGuardianSprite.Green.class;
                break;
            case 2:
                spriteClass = CrystalGuardianSprite.Red.class;
                break;
        }
    }

    @Override
    public float spawningWeight() {
        return 0;
    }

    public static final String RECOVERING = "recovering";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(RECOVERING, recovering);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        recovering = bundle.getBoolean(RECOVERING);
    }
}
