package com.shatteredpixel.shatteredpixeldungeon.items.potions.mini;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.WheelChair;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TendonHookSickle;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;

public class PotionOfPrance extends MiniPotion {
    //腾跃试剂
    {
        icon = ItemSpriteSheet.Icons.POTION_PRANCE;

    }

    @Override
    public void apply(Hero hero) {
        identify();
        PranceMini buff = Buff.affect(hero, PotionOfPrance.PranceMini.class);
        buff.setCount(2);
        buff.triggerAction();
    }

    public static class PranceMini extends Buff implements ActionIndicator.Action {

        {
            type = buffType.POSITIVE;
        }

        private int count = 0;

        public void setCount(int sct){
            count += sct;
        }

        public int getCount(){
            return count;
        }

        public void lossCount(int ct){
            count = Math.max(0, count - ct);
        }

        @Override
        public int icon() {
            return BuffIndicator.MINIPOTION;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0f, 0.726f, 0.94f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(count);
        }

        @Override
        public void detach() {
            super.detach();
            ActionIndicator.clearAction(this);
        }


        @Override
        public boolean act() {
            if (count <= 0) {
                detach();
                return true;
            }
            spend(TICK);
            return true;
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", count);
        }

        @Override
        public String actionName() {
            return Messages.get(this, "name");
        }

        @Override
        public int actionIcon() {
            return HeroIcon.COMBO;
        }

        // 触发动作
        public void triggerAction() {
            if (count > 0) {
                ActionIndicator.setAction(this);
                BuffIndicator.refreshHero();
            }
        }


        @Override
        public Visual secondaryVisual() {
            BitmapText txt = new BitmapText(PixelScene.pixelFont);
            txt.text(Integer.toString(count));
            txt.hardlight(0f, 0.726f, 0.94f);
            txt.measure();
            return txt;
        }

        @Override
        public int indicatorColor() {
            return 0x3ac3f0;
        }

        @Override
        public void doAction() {
            GameScene.selectCell(caster);
        }

        public CellSelector.Listener caster = new CellSelector.Listener(){

            public void onSelect(Integer target) {
                if (target != null && (Dungeon.level.visited[target] || Dungeon.level.mapped[target])){

                    PathFinder.buildDistanceMap(target, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
                    if (!(Dungeon.level instanceof MiningLevel) && PathFinder.distance[hero.pos] == Integer.MAX_VALUE){
                        GLog.w( Messages.get(EtherealChains.class, "cant_reach") );
                        return;
                    }

                    int distance = Dungeon.level.distance(hero.pos, target);
                    // 检查距离是否超过2
                    if (distance > 2) {
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
                    triggerAction();
                    count--; // 使用次数减1
                    hero.sprite.jump(hero.pos, cell, new Callback() {
                        @Override
                        public void call() {
                            hero.move(dest);
                            Dungeon.level.occupyCell(hero);
                            Dungeon.observe();
                            GameScene.updateFog();

                            WandOfBlastWave.BlastWave.blast(dest);
//                            PixelScene.shake(2, 0.5f);

//                            Buff.prolong( hero, Stamina.class, (3));
                            Invisibility.dispel();
                            hero.spendAndNext(Actor.TICK);
                        }
                    });
                }
            }

            @Override
            public String prompt() {
                return Messages.get( PotionOfPrance.class, "prompt");
            }
        };

        private static final String PRANCE_COUNT	= "prance_count";

        @Override
        public void storeInBundle( Bundle bundle ) {
            super.storeInBundle( bundle );
            bundle.put( PRANCE_COUNT, count );
        }
        @Override
        public void restoreFromBundle( Bundle bundle ) {
            super.restoreFromBundle(bundle);
            count = bundle.getInt( PRANCE_COUNT );
            if (count != 0) ActionIndicator.setAction(this);
        }
    }
}

