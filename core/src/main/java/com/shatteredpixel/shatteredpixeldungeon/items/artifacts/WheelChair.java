package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TendonHookSickle;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WheelChair extends Artifact {
    public static final String AC_JUMP       = "JUMP";

    {
        image = ItemSpriteSheet.ROUND_SHIELD;

        levelCap = 10;
        exp = 0;

        charge = 3;

        defaultAction = AC_JUMP;
        usesTargeting = true;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions( hero );
        if (isEquipped(hero) && charge > 0 && !cursed && hero.buff(MagicImmune.class) == null) {
            actions.add(AC_JUMP);
        }
        return actions;
    }

    public int targetingPos( Hero user, int dst ){
        return dst;
    }

    @Override
    public void execute(Hero hero, String action) {

        super.execute(hero, action);

        if (hero.buff(MagicImmune.class) != null) return;

        if (action.equals(AC_JUMP)){

            curUser = hero;

            if (!isEquipped( hero )) {
                GLog.i( Messages.get(Artifact.class, "need_to_equip") );
                usesTargeting = false;

            } else if (charge < 1) {
                GLog.i( Messages.get(this, "no_charge") );
                usesTargeting = false;

            } else if (cursed) {
                GLog.w( Messages.get(this, "cursed") );
                usesTargeting = false;

            } else {
                usesTargeting = true;
                GameScene.selectCell(caster);
            }

        }
    }

    @Override
    public void resetForTrinity(int visibleLevel) {
        super.resetForTrinity(visibleLevel);
        charge = Math.min( 3 + level(), 10); //sets charge to soft cap
    }

    public CellSelector.Listener caster = new CellSelector.Listener(){

        @Override
        public void onSelect(Integer target) {
            if (target != null && (Dungeon.level.visited[target] || Dungeon.level.mapped[target])){

                PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
                if (!(Dungeon.level instanceof MiningLevel) && PathFinder.distance[curUser.pos] == Integer.MAX_VALUE){
                    GLog.w( Messages.get(EtherealChains.class, "cant_reach") );
                    return;
                }

                int distance = Dungeon.level.distance(curUser.pos, target);
                // 检查距离是否超过6
                if (distance > 2+0.2*level()) {
                    GLog.w(Messages.get(TendonHookSickle.class, "out_of_range"));
                    return;
                }

                final Ballistica chain = new Ballistica(hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);

                int cell = chain.collisionPos;

                int backTrace = chain.dist-1;
                while (Actor.findChar( cell ) != null && cell != hero.pos) {
                    cell = chain.path.get(backTrace);
                    backTrace--;
                }

                final int dest = cell;
                hero.busy();
                charge--;
                hero.sprite.jump(hero.pos, cell, new Callback() {
                    @Override
                    public void call() {
                        hero.move(dest);
                        Dungeon.level.occupyCell(hero);
                        Dungeon.observe();
                        GameScene.updateFog();

                        WandOfBlastWave.BlastWave.blast(dest);
                        PixelScene.shake(2, 0.5f);

                        Buff.prolong( hero, Haste.class, (10+level()));
                        Invisibility.dispel();
                        hero.spendAndNext(Actor.TICK);
                    }
                });


            }

        }

        @Override
        public String prompt() {
            int maxRange = 2;
            if (level() == 5) {
                maxRange += 1;
            } else if (level() == 10) {
                maxRange += 2;
            }
            return Messages.get( WheelChair.class, "prompt" , maxRange);
        }
    };

    @Override
    protected Artifact.ArtifactBuff passiveBuff() {
        return new wheelRecharge();
    }

    @Override
    public void charge(Hero target, float amount) {
        if (cursed || target.buff(MagicImmune.class) != null) return;
        int chargeTarget = 5+(level()*2);
        if (charge < chargeTarget*2){
            partialCharge += 0.5f*amount;
            while (partialCharge >= 1){
                partialCharge--;
                charge++;
                updateQuickslot();
            }
        }
    }

    @Override
    public String desc() {
        String desc = super.desc();

        if (isEquipped( hero )){
            desc += "\n\n";
            if (cursed)
                desc += Messages.get(this, "desc_cursed");
            else
                desc += Messages.get(this, "desc_equipped", exp, (1000+level()*100));
        }
        return desc;
    }
    private boolean movedLastTurn = true;

    public class wheelRecharge extends Artifact.ArtifactBuff {
        @Override
        public boolean act() {
            int chargeTarget = Math.min( 3 + level(), 10);
            if (charge < chargeTarget
                    && !cursed
                    && target.buff(MagicImmune.class) == null
                    && Regeneration.regenOn()) {
                //gains a charge in 40 - 2*missingCharge turns
                //80-charge*2-level
                float chargeGain = (1 / (80f- level()- charge*2f));
                chargeGain *= RingOfEnergy.artifactChargeMultiplier(target);
                partialCharge += chargeGain;
            } else if (cursed && Random.Int(100) == 0){
                Buff.prolong( target, Cripple.class, 10f);
            }

            while (partialCharge >= 1) {
                partialCharge --;
                charge ++;
            }
            // 重置移动标志
            movedLastTurn = false;

            updateQuickslot();
            spend( TICK );
            return true;
        }

        public void gainStack(){
            if (cursed || target.buff(MagicImmune.class) != null) return;
            movedLastTurn = true;
//            postpone(target.cooldown()+(1/target.speed()));
            exp = exp +1;

            if (exp > 1000+level()*100 && level() < levelCap){
                exp -= 1000+level()*100;
                GLog.p( Messages.get(WheelChair.class, "levelup") );
//                Catalog.countUses(WheelChair.class, 2);
                upgrade();
                updateQuickslot();
            }

            BuffIndicator.refreshHero();
        }
    }


}