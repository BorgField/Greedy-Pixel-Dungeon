package com.shatteredpixel.shatteredpixeldungeon.items.potions.mini;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

import java.util.Arrays;

public class PotionOfVenom extends MiniPotion {
    //毒雾试剂
    {
        icon = ItemSpriteSheet.Icons.POTION_VENOM;

    }

    @Override
    public void shatter(int cell) {
        splash(cell);
        if (Dungeon.level.heroFOV[cell]) {
            identify();

            Sample.INSTANCE.play(Assets.Sounds.SHATTER);
            Sample.INSTANCE.play(Assets.Sounds.GAS);
        }

        // 创建并添加毒雾气体
        GameScene.add( Blob.seed( cell, 500, LimitedToxicGas.class ) );
    }

    public static class LimitedToxicGas extends ToxicGas {

        private static final int SPREAD_RANGE = 3; // 扩散范围为3x3
        private int originCell; // 记录初始位置

        public LimitedToxicGas() {
            super();
        }

        @Override
        public void seed(Level level, int cell, int amount) {
            super.seed(level, cell, amount);
            originCell = cell; // 记录初始位置
        }

        @Override
        protected void evolve() {
            // 先执行父类的扩散逻辑
            super.evolve();

            // 然后限制扩散范围
            int originX = originCell % Dungeon.level.width();
            int originY = originCell / Dungeon.level.width();

            // 清空超出范围的格子
            for (int i = 0; i < cur.length; i++) {
                int x = i % Dungeon.level.width();
                int y = i / Dungeon.level.width();

                // 计算到原点的距离
                int distance = Math.max(Math.abs(x - originX), Math.abs(y - originY));

                // 如果超出3x3范围，清除该格子的气体
                if (distance > 1) { // 1表示3x3范围（包括中心点）
                    off[i] = 0;
                }
            }

            // 更新体积
            volume = 0;
            for (int i = 0; i < off.length; i++) {
                volume += off[i];
            }

            // 更新扩散区域
            setupArea();
        }
    }


}
