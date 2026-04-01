/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.events.HeroLevelUpEvent;
import com.shatteredpixel.shatteredpixeldungeon.events.SubscribeEvent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
public class LeatherArmor extends Armor {

	{
		image = ItemSpriteSheet.ARMOR_LEATHER;
	}
	
	public LeatherArmor() {
		super( 2 );
	}

	@SubscribeEvent(event = HeroLevelUpEvent.class, priority = 0)
	public static void onHeroLevelUp(HeroLevelUpEvent event) {
		Hero hero = event.getHero();
		if (hero != null && hero.isAlive()) {
			if (hero.belongings.armor() instanceof LeatherArmor) {
				GLog.p("\n你的皮甲：恭喜你升级了！");
			}
		}
	}


}
