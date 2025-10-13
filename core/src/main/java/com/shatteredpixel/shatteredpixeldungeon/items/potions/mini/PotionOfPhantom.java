package com.shatteredpixel.shatteredpixeldungeon.items.potions.mini;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

public class PotionOfPhantom extends MiniPotion {
    //明目试剂
    {
        icon = ItemSpriteSheet.Icons.POTION_PHANTOM;

    }

    @Override
    public void apply(Hero hero) {
        identify();
        Buff.affect(hero, PotionOfPhantom.PhantomMini.class).set(1);
    }

    public static class PhantomMini extends Buff {

        {
            type = buffType.POSITIVE;
        }

        private int levelsLeft;
        public static final int VISION_BONUS = 2;


        public void set(int levels) {
            levelsLeft = levels;
        }

        @Override
        public float visionModifier() {
            return VISION_BONUS;
        }

        @Override
        public boolean attachTo( Char target ) {
            if (super.attachTo( target )) {
                // 当buff附加时，触发一次视野更新
                if (target instanceof Hero && Dungeon.level != null) {
                    Dungeon.observe();
                }
                return true;
            }
            return false;
        }

        @Override
        public void detach() {
            super.detach();
            // 当buff移除时，也触发一次视野更新
            if (target instanceof Hero && Dungeon.level != null) {
                Dungeon.observe();
            }
        }

        @Override
        public int icon() {
            return BuffIndicator.MINIPOTION;
        }

        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.6f, 0.94f, 1.0f);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString(levelsLeft);
        }

        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }

        public void onLevelUp(){
            levelsLeft --;
            if (levelsLeft <= 0){
                detach();
            }
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", levelsLeft);
        }

        private static final String LEVELS_LEFT = "levelsLeft";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(LEVELS_LEFT, levelsLeft);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            levelsLeft = bundle.getInt(LEVELS_LEFT);
        }
    }
}
