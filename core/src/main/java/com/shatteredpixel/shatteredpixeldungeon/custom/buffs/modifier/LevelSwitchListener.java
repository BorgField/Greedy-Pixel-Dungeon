package com.shatteredpixel.shatteredpixeldungeon.custom.buffs.modifier;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.custom.buffs.GameTracker;

public class LevelSwitchListener {
    public static void onLevelSwitch(){
        GameTracker gmt = Dungeon.hero.buff(GameTracker.class);
        if(gmt != null){
            gmt.onNewLevel();
        }
    }
}
