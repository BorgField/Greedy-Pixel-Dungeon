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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Belongings implements Iterable<Item> {

	private Hero owner;

	public static class Backpack extends Bag {
		{
			image = ItemSpriteSheet.BACKPACK;
		}
		public int capacity(){
			int cap = super.capacity();
			for (Item item : items){
				if (item instanceof Bag){
					cap++;
				}
			}
			if (Dungeon.hero != null && Dungeon.hero.belongings.secondWep != null){
				//secondary weapons still occupy an inv. slot
				cap--;
			}
			return cap;
		}
	}

	public Backpack backpack;

	public Belongings( Hero owner ) {
		this.owner = owner;

		backpack = new Backpack();
		backpack.owner = owner;
	}

	public KindOfWeapon weapon = null;
	public KindOfWeapon weapon2 = null;
	public KindOfWeapon weapon3 = null;
	public KindOfWeapon weapon4 = null;
	public Armor armor = null;
	public Artifact artifact = null;
	public KindofMisc misc = null;
	public Ring ring = null;

	//used when thrown weapons temporary become the current weapon
	public KindOfWeapon thrownWeapon = null;

	//used to ensure that the duelist always uses the weapon she's using the ability of
	public KindOfWeapon abilityWeapon = null;

	//used by the champion subclass
	public KindOfWeapon secondWep = null;

	//*** these accessor methods are so that worn items can be affected by various effects/debuffs
	// we still want to access the raw equipped items in cases where effects should be ignored though,
	// such as when equipping something, showing an interface, or dealing with items from a dead hero

	//normally the primary equipped weapon, but can also be a thrown weapon or an ability's weapon
	public KindOfWeapon attackingWeapon(){
		if (thrownWeapon != null) return thrownWeapon;
		if (abilityWeapon != null) return abilityWeapon;
		KindOfWeapon mainWep = weapon();
		if (mainWep != null) return mainWep;
		// 如果主武器槽位为空，检查其他武器槽位
		if (weapon2() != null) return weapon2();
		if (weapon3() != null) return weapon3();
		if (weapon4() != null) return weapon4();
		return null;
	}

	//we cache whether belongings are lost to avoid lots of calls to hero.buff(LostInventory.class)
	private boolean lostInvent;
	public void lostInventory( boolean val ){
		lostInvent = val;
	}

	public boolean lostInventory(){
		return lostInvent;
	}

	public KindOfWeapon weapon(){
		if (!lostInventory() || (weapon != null && weapon.keptThroughLostInventory())){
			return weapon;
		} else {
			return null;
		}
	}

	public KindOfWeapon weapon2(){
		if (!lostInventory() || (weapon2 != null && weapon2.keptThroughLostInventory())){
			return weapon2;
		} else {
			return null;
		}
	}

	public KindOfWeapon weapon3(){
		if (!lostInventory() || (weapon3 != null && weapon3.keptThroughLostInventory())){
			return weapon3;
		} else {
			return null;
		}
	}

	public KindOfWeapon weapon4(){
		if (!lostInventory() || (weapon4 != null && weapon4.keptThroughLostInventory())){
			return weapon4;
		} else {
			return null;
		}
	}

	public Armor armor(){
		if (!lostInventory() || (armor != null && armor.keptThroughLostInventory())){
			return armor;
		} else {
			return null;
		}
	}

	public Artifact artifact(){
		if (!lostInventory() || (artifact != null && artifact.keptThroughLostInventory())){
			return artifact;
		} else {
			return null;
		}
	}

	public KindofMisc misc(){
		if (!lostInventory() || (misc != null && misc.keptThroughLostInventory())){
			return misc;
		} else {
			return null;
		}
	}

	public Ring ring(){
		if (!lostInventory() || (ring != null && ring.keptThroughLostInventory())){
			return ring;
		} else {
			return null;
		}
	}

	public KindOfWeapon secondWep(){
		if (!lostInventory() || (secondWep != null && secondWep.keptThroughLostInventory())){
			return secondWep;
		} else {
			return null;
		}
	}

	// ***

	private static final String WEAPON = "weapon";
	private static final String WEAPON2		= "weapon2";
	private static final String WEAPON3		= "weapon3";
	private static final String WEAPON4		= "weapon4";
	private static final String ARMOR		= "armor";
	private static final String ARTIFACT   = "artifact";
	private static final String MISC       = "misc";
	private static final String RING       = "ring";

	private static final String SECOND_WEP = "second_wep";

	public void storeInBundle( Bundle bundle ) {

		backpack.storeInBundle( bundle );

		bundle.put(WEAPON, weapon);
		bundle.put( WEAPON2, weapon2 );
		bundle.put( WEAPON3, weapon3 );
		bundle.put( WEAPON4, weapon4 );
		bundle.put( ARMOR, armor );
		bundle.put( ARTIFACT, artifact );
		bundle.put( MISC, misc );
		bundle.put( RING, ring );
		bundle.put( SECOND_WEP, secondWep );
	}

	public void restoreFromBundle( Bundle bundle ) {

		backpack.clear();
		backpack.restoreFromBundle( bundle );

		weapon = (KindOfWeapon) bundle.get(WEAPON);
		if (weapon() != null)       weapon().activate(owner);

		weapon2 = (KindOfWeapon) bundle.get(WEAPON2);
		if (weapon2() != null)      weapon2().activate(owner);

		weapon3 = (KindOfWeapon) bundle.get(WEAPON3);
		if (weapon3() != null)      weapon3().activate(owner);

		weapon4 = (KindOfWeapon) bundle.get(WEAPON4);
		if (weapon4() != null)      weapon4().activate(owner);

		armor = (Armor)bundle.get( ARMOR );
		if (armor() != null)        armor().activate( owner );

		artifact = (Artifact) bundle.get(ARTIFACT);
		if (artifact() != null)     artifact().activate(owner);

		misc = (KindofMisc) bundle.get(MISC);
		if (misc() != null)         misc().activate( owner );

		ring = (Ring) bundle.get(RING);
		if (ring() != null)         ring().activate( owner );

		secondWep = (KindOfWeapon) bundle.get(SECOND_WEP);
		if (secondWep() != null)    secondWep().activate(owner);
	}

	public List<KindOfWeapon> getWeapons() {
		return Arrays.asList(weapon, weapon2, weapon3, weapon4);
	}

	public static void preview( GamesInProgress.Info info, Bundle bundle ) {
		if (bundle.contains( ARMOR )){
			Armor armor = ((Armor)bundle.get( ARMOR ));
			if (armor instanceof ClassArmor){
				info.armorTier = 6;
			} else {
				info.armorTier = armor.tier;
			}
		} else {
			info.armorTier = 0;
		}
	}

	//ignores lost inventory debuff
	public ArrayList<Bag> getBags(){
		ArrayList<Bag> result = new ArrayList<>();

		result.add(backpack);

		for (Item i : this){
			if (i instanceof Bag){
				result.add((Bag)i);
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public<T extends Item> T getItem( Class<T> itemClass ) {

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (itemClass.isInstance( item )) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return (T) item;
				}
			}
		}

		return null;
	}

	public<T extends Item> ArrayList<T> getAllItems( Class<T> itemClass ) {
		ArrayList<T> result = new ArrayList<>();

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (itemClass.isInstance( item )) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					result.add((T) item);
				}
			}
		}

		return result;
	}

	public boolean contains( Item contains ){

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (contains == item) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return true;
				}
			}
		}

		return false;
	}

	public Item getSimilar( Item similar ){

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (similar != item && similar.isSimilar(item)) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					return item;
				}
			}
		}

		return null;
	}

	public ArrayList<Item> getAllSimilar( Item similar ){
		ArrayList<Item> result = new ArrayList<>();

		boolean lostInvent = lostInventory();

		for (Item item : this) {
			if (item != similar && similar.isSimilar(item)) {
				if (!lostInvent || item.keptThroughLostInventory()) {
					result.add(item);
				}
			}
		}

		return result;
	}

	//triggers when a run ends, so ignores lost inventory effects
	public void identify() {
		for (Item item : this) {
			item.identify(false);
		}
	}

	public void observe() {
		if (weapon() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && weapon() instanceof Weapon){
				((Weapon) weapon()).setIDReady();
			} else {
				weapon().identify();
				Badges.validateItemLevelAquired(weapon());
			}
		}
		if (weapon2() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && weapon2() instanceof Weapon){
				((Weapon) weapon2()).setIDReady();
			} else {
				weapon2().identify();
				Badges.validateItemLevelAquired(weapon2());
				}
		}
		if (weapon3() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && weapon3() instanceof Weapon){
				((Weapon) weapon3()).setIDReady();
			} else {
				weapon3().identify();
				Badges.validateItemLevelAquired(weapon3());
			}
		}
		if (weapon4() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && weapon4() instanceof Weapon){
				((Weapon) weapon4()).setIDReady();
			} else {
				weapon4().identify();
				Badges.validateItemLevelAquired(weapon4());
			}
		}
		if (secondWep() != null){
			if (ShardOfOblivion.passiveIDDisabled() && secondWep() instanceof Weapon){
				((Weapon) secondWep()).setIDReady();
			} else {
				secondWep().identify();
				Badges.validateItemLevelAquired(secondWep());
			}
		}
		if (armor() != null) {
			if (ShardOfOblivion.passiveIDDisabled()){
				armor().setIDReady();
			} else {
				armor().identify();
				Badges.validateItemLevelAquired(armor());
			}
		}
		if (artifact() != null) {
			//oblivion shard does not prevent artifact IDing
			artifact().identify();
			Badges.validateItemLevelAquired(artifact());
		}
		if (misc() != null) {
			if (ShardOfOblivion.passiveIDDisabled() && misc() instanceof Ring){
				((Ring) misc()).setIDReady();
			} else {
				misc().identify();
				Badges.validateItemLevelAquired(misc());
			}
		}
		if (ring() != null) {
			if (ShardOfOblivion.passiveIDDisabled()){
				ring().setIDReady();
			} else {
				ring().identify();
				Badges.validateItemLevelAquired(ring());
			}
		}
		if (ShardOfOblivion.passiveIDDisabled()){
			GLog.p(Messages.get(ShardOfOblivion.class, "identify_ready_worn"));
		}
		for (Item item : backpack) {
			if (item instanceof EquipableItem || item instanceof Wand) {
				item.cursedKnown = true;
			}
		}
		Item.updateQuickslot();
	}

	public void uncurseEquipped() {
		ScrollOfRemoveCurse.uncurse(owner,
				armor(), weapon(), weapon2(), weapon3(), weapon4(), // 新增武器槽
				artifact(), misc(), ring(), secondWep()
		);
	}

	public Item randomUnequipped() {
		if (owner.buff(LostInventory.class) != null) return null;

		return Random.element( backpack.items );
	}

	public int charge( float charge ) {

		int count = 0;

		for (Wand.Charger charger : owner.buffs(Wand.Charger.class)){
			charger.gainCharge(charge);
			count++;
		}

		return count;
	}

	@Override
	public Iterator<Item> iterator() {
		return new ItemIterator();
	}

	private class ItemIterator implements Iterator<Item> {

		private int index = 0;

		private Iterator<Item> backpackIterator = backpack.iterator();

		private Item[] equipped = {weapon, weapon2, weapon3, weapon4, armor, artifact, misc, ring, secondWep};
		private int backpackIndex = equipped.length;

		@Override
		public boolean hasNext() {

			for (int i=index; i < backpackIndex; i++) {
				if (equipped[i] != null) {
					return true;
				}
			}

			return backpackIterator.hasNext();
		}

		@Override
		public Item next() {

			while (index < backpackIndex) {
				Item item = equipped[index++];
				if (item != null) {
					return item;
				}
			}

			return backpackIterator.next();
		}

		@Override
		public void remove() {
			if (index < backpackIndex) {
				switch (index - 1) { // 调整索引位置
					case 0: equipped[0] = weapon = null; break;
					case 1: equipped[1] = weapon2 = null; break;
					case 2: equipped[2] = weapon3 = null; break;
					case 3: equipped[3] = weapon4 = null; break;
					case 4: equipped[4] = armor = null; break;
					case 5: equipped[5] = artifact = null; break;
					case 6: equipped[6] = misc = null; break;
					case 7: equipped[7] = ring = null; break;
					case 8: equipped[8] = secondWep = null; break;
				}
			} else {
				backpackIterator.remove();
			}
		}
	}
}
