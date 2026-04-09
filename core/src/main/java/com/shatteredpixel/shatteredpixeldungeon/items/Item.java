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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.TippedDart;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Item implements Bundlable {

	protected static final String TXT_TO_STRING_LVL		= "%s %+d";
	protected static final String TXT_TO_STRING_X		= "%s x%d";
	
	protected static final float TIME_TO_THROW		= 1.0f;
	protected static final float TIME_TO_PICK_UP	= 1.0f;
	protected static final float TIME_TO_DROP		= 1.0f;
	
	public static final String AC_DROP		= "DROP";
	public static final String AC_THROW		= "THROW";
	
	protected String defaultAction;
	public boolean usesTargeting;

	//TODO should these be private and accessed through methods?
	public int image = 0;
	public int icon = -1; //used as an identifier for items with randomized images
	
	public boolean stackable = false;
	protected int quantity = 1;
	public boolean dropsDownHeap = false;
	
	private int level = 0;

	public boolean levelKnown = false;
	
	public boolean cursed;
	public boolean cursedKnown;
	
	// Unique items persist through revival
	public boolean unique = false;

	// These items are preserved even if the hero's inventory is lost via unblessed ankh
	// this is largely set by the resurrection window, items can override this to always be kept
	public boolean keptThoughLostInvent = false;

	// whether an item can be included in heroes remains
	public boolean bones = false;

	public int customNoteID = -1;
	
	// 动态项目属性Tag系统(懒加载)
	private Map<String, ItemTag> tags;
	
	public static final Comparator<Item> itemComparator = new Comparator<Item>() {
		@Override
		public int compare( Item lhs, Item rhs ) {
			return Generator.Category.order( lhs ) - Generator.Category.order( rhs );
		}
	};
	
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = new ArrayList<>();
		actions.add( AC_DROP );
		actions.add( AC_THROW );
		return actions;
	}

	public String actionName(String action, Hero hero){
		return Messages.get(this, "ac_" + action);
	}

	public final boolean doPickUp( Hero hero ) {
		return doPickUp( hero, hero.pos );
	}

	public boolean doPickUp(Hero hero, int pos) {
		if (collect( hero.belongings.backpack )) {
			
			GameScene.pickUp( this, pos );
			Sample.INSTANCE.play( Assets.Sounds.ITEM );
			hero.spendAndNext( TIME_TO_PICK_UP );
			return true;
			
		} else {
			return false;
		}
	}
	
	public void doDrop( Hero hero ) {
		hero.spendAndNext(TIME_TO_DROP);
		int pos = hero.pos;
		Dungeon.level.drop(detachAll(hero.belongings.backpack), pos).sprite.drop(pos);
	}

	//resets an item's properties, to ensure consistency between runs
	public void reset(){
		keptThoughLostInvent = false;
	}

	public boolean keptThroughLostInventory(){
		return keptThoughLostInvent;
	}

	public void doThrow( Hero hero ) {
		GameScene.selectCell(thrower);
	}
	
	public void execute( Hero hero, String action ) {

		GameScene.cancel();
		curUser = hero;
		curItem = this;
		
		if (action.equals( AC_DROP )) {
			
			if (hero.belongings.backpack.contains(this) || isEquipped(hero)) {
				doDrop(hero);
			}
			
		} else if (action.equals( AC_THROW )) {
			
			if (hero.belongings.backpack.contains(this) || isEquipped(hero)) {
				doThrow(hero);
			}
			
		}
	}

	//can be overridden if default action is variable
	public String defaultAction(){
		return defaultAction;
	}
	
	public void execute( Hero hero ) {
		String action = defaultAction();
		if (action != null) {
			execute(hero, defaultAction());
		}
	}
	
	protected void onThrow( int cell ) {
		Heap heap = Dungeon.level.drop( this, cell );
		if (!heap.isEmpty()) {
			heap.sprite.drop( cell );
		}
	}
	
	//takes two items and merges them (if possible)
	public Item merge( Item other ){
		if (isSimilar( other )){
			quantity += other.quantity;
			other.quantity = 0;
		}
		return this;
	}
	
	public boolean collect( Bag container ) {

		if (quantity <= 0){
			return true;
		}

		ArrayList<Item> items = container.items;

		if (items.contains( this )) {
			return true;
		}

		for (Item item:items) {
			if (item instanceof Bag && ((Bag)item).canHold( this )) {
				if (collect( (Bag)item )){
					return true;
				}
			}
		}

		if (!container.canHold(this)){
			return false;
		}
		
		if (stackable) {
			for (Item item:items) {
				if (isSimilar( item )) {
					item.merge( this );
					item.updateQuickslot();
					if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
						Badges.validateItemLevelAquired( this );
						Talent.onItemCollected(Dungeon.hero, item);
						if (isIdentified()) {
							Catalog.setSeen(getClass());
							Statistics.itemTypesDiscovered.add(getClass());
						}
					}
					if (TippedDart.lostDarts > 0){
						Dart d = new Dart();
						d.quantity(TippedDart.lostDarts);
						TippedDart.lostDarts = 0;
						if (!d.collect()){
							//have to handle this in an actor as we can't manipulate the heap during pickup
							Actor.add(new Actor() {
								{ actPriority = VFX_PRIO; }
								@Override
								protected boolean act() {
									Dungeon.level.drop(d, Dungeon.hero.pos).sprite.drop();
									Actor.remove(this);
									return true;
								}
							});
						}
					}
					return true;
				}
			}
		}

		if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
			Badges.validateItemLevelAquired( this );
			Talent.onItemCollected( Dungeon.hero, this );
			if (isIdentified()){
				Catalog.setSeen(getClass());
				Statistics.itemTypesDiscovered.add(getClass());
			}
		}

		items.add( this );
		Dungeon.quickslot.replacePlaceholder(this);
		Collections.sort( items, itemComparator );
		updateQuickslot();
		return true;

	}
	
	public final boolean collect() {
		return collect( Dungeon.hero.belongings.backpack );
	}
	
	//returns a new item if the split was sucessful and there are now 2 items, otherwise null
	public Item split( int amount ){
		if (amount <= 0 || amount >= quantity()) {
			return null;
		} else {
			//pssh, who needs copy constructors?
			Item split = Reflection.newInstance(getClass());
			
			if (split == null){
				return null;
			}
			
			Bundle copy = new Bundle();
			this.storeInBundle(copy);
			split.restoreFromBundle(copy);
			split.quantity(amount);
			quantity -= amount;
			
			return split;
		}
	}

	public Item duplicate(){
		Item dupe = Reflection.newInstance(getClass());
		if (dupe == null){
			return null;
		}
		Bundle copy = new Bundle();
		this.storeInBundle(copy);
		dupe.restoreFromBundle(copy);
		return dupe;
	}
	
	public final Item detach( Bag container ) {
		
		if (quantity <= 0) {
			
			return null;
			
		} else
		if (quantity == 1) {

			if (stackable){
				Dungeon.quickslot.convertToPlaceholder(this);
			}

			return detachAll( container );
			
		} else {
			
			
			Item detached = split(1);
			updateQuickslot();
			if (detached != null) detached.onDetach( );
			return detached;
			
		}
	}
	
	public final Item detachAll( Bag container ) {
		Dungeon.quickslot.clearItem( this );

		for (Item item : container.items) {
			if (item == this) {
				container.items.remove(this);
				item.onDetach();
				container.grabItems(); //try to put more items into the bag as it now has free space
				updateQuickslot();
				return this;
			} else if (item instanceof Bag) {
				Bag bag = (Bag)item;
				if (bag.contains( this )) {
					return detachAll( bag );
				}
			}
		}

		updateQuickslot();
		return this;
	}
	
	public boolean isSimilar( Item item ) {
		return getClass() == item.getClass();
	}

	protected void onDetach(){}

	//returns the true level of the item, ignoring all modifiers aside from upgrades
	public final int trueLevel(){
		return level;
	}

	//returns the persistant level of the item, only affected by modifiers which are persistent (e.g. curse infusion)
	public int level(){
		return level;
	}
	
	//returns the level of the item, after it may have been modified by temporary boosts/reductions
	//note that not all item properties should care about buffs/debuffs! (e.g. str requirement)
	public int buffedLvl(){
		//only the hero can be affected by Degradation
		if (Dungeon.hero != null && Dungeon.hero.buff( Degrade.class ) != null
			&& (isEquipped( Dungeon.hero ) || Dungeon.hero.belongings.contains( this ))) {
			return Degrade.reduceLevel(level());
		} else {
			return level();
		}
	}

	public void level( int value ){
		level = value;

		updateQuickslot();
	}
	
	public Item upgrade() {
		
		this.level++;

		updateQuickslot();
		
		return this;
	}
	
	final public Item upgrade( int n ) {
		for (int i=0; i < n; i++) {
			upgrade();
		}
		
		return this;
	}
	
	public Item degrade() {
		
		this.level--;
		
		return this;
	}
	
	final public Item degrade( int n ) {
		for (int i=0; i < n; i++) {
			degrade();
		}
		
		return this;
	}
	
	public int visiblyUpgraded() {
		return levelKnown ? level() : 0;
	}

	public int buffedVisiblyUpgraded() {
		return levelKnown ? buffedLvl() : 0;
	}
	
	public boolean visiblyCursed() {
		return cursed && cursedKnown;
	}
	
	public boolean isUpgradable() {
        return !banUpgraded;
    }

	public boolean isBanUpgraded() { return banUpgraded;}
	public void setBanUpgraded(boolean value) { banUpgraded = value;}
	private boolean banUpgraded = false;
	
	public boolean isIdentified() {
		return levelKnown && cursedKnown;
	}
	
	public boolean isEquipped( Hero hero ) {
		return false;
	}

	public final Item identify(){
		return identify(true);
	}

	public Item identify( boolean byHero ) {

		if (byHero && Dungeon.hero != null && Dungeon.hero.isAlive()){
			Catalog.setSeen(getClass());
			Statistics.itemTypesDiscovered.add(getClass());
		}

		levelKnown = true;
		cursedKnown = true;
		Item.updateQuickslot();
		
		return this;
	}
	
	public void onHeroGainExp( float levelPercent, Hero hero ){
		//do nothing by default
	}
	
	public static void evoke( Hero hero ) {
		hero.sprite.emitter().burst( Speck.factory( Speck.EVOKE ), 5 );
	}

	public String title() {

		String name = name();

		if (visiblyUpgraded() != 0)
			name = Messages.format( TXT_TO_STRING_LVL, name, visiblyUpgraded()  );

		if (quantity > 1)
			name = Messages.format( TXT_TO_STRING_X, name, quantity );

		return name;

	}
	
	public String name() {
		return trueName();
	}
	
	public final String trueName() {
		return Messages.get(this, "name");
	}
	
	public int image() {
		return image;
	}
	
	public ItemSprite.Glowing glowing() {
		return null;
	}

	public Emitter emitter() { return null; }
	
	public String info() {

		if (Dungeon.hero != null) {
			Notes.CustomRecord note = Notes.findCustomRecord(customNoteID);
			if (note != null) {
				//we swap underscore(0x5F) with low macron(0x2CD) here to avoid highlighting in the item window
				return Messages.get(this, "custom_note", note.title().replace('_', 'ˍ')) + "\n\n" + desc();
			} else {
				note = Notes.findCustomRecord(getClass());
				if (note != null) {
					//we swap underscore(0x5F) with low macron(0x2CD) here to avoid highlighting in the item window
					return Messages.get(this, "custom_note_type", note.title().replace('_', 'ˍ')) + "\n\n" + desc();
				}
			}
		}

		String info = desc();
		
		// Append visible tags to item description
		List<ItemTag> visibleTags = getVisibleTags();
		if (!visibleTags.isEmpty()) {
			info += "\n\n";
			for (ItemTag tag : visibleTags) {
				info += "Ⅶ #"+ getTagDescription(tag) + "Ⅶ ";
			}
		}
		
		return info;
	}
	
	public String desc() {
		return Messages.get(this, "desc")
				+(isBanUpgraded() ? "\n\n" + Messages.get(Scroll.class, "copyBan_desc") : "");
	}

	protected String getTagDescription(ItemTag tag) {
		// Use full key path directly without class prefix
		String tagKey = "items.tag." + tag.key();
		String desc = Messages.get((Class)null, tagKey);
		
		// If no specific description exists, use a default format
		if (desc == null || (!desc.isEmpty() && desc.startsWith("!"))) {
			desc = Messages.get((Class)null, "items.tag.default", tag.key());
			if (tag.level() > 0) {
				desc += " (" + tag.level() + ")";
			}
		}
		
		return desc;
	}
	
	public int quantity() {
		return quantity;
	}
	
	public Item quantity( int value ) {
		quantity = value;
		return this;
	}

	//item's value in gold coins
	public int value() {
		return 0;
	}

	//item's value in energy crystals
	public int energyVal() {
		return 0;
	}
	
	public Item virtual(){
		Item item = Reflection.newInstance(getClass());
		if (item == null) return null;
		
		item.quantity = 0;
		item.level = level;
		return item;
	}
	
	public Item random() {
		return this;
	}
	
	public String status() {
		return quantity != 1 ? Integer.toString( quantity ) : null;
	}

	public static void updateQuickslot() {
		GameScene.updateItemDisplays = true;
	}
	
	private static final String QUANTITY		= "quantity";
	private static final String LEVEL			= "level";
	private static final String LEVEL_KNOWN		= "levelKnown";
	private static final String CURSED			= "cursed";
	private static final String CURSED_KNOWN	= "cursedKnown";
	private static final String QUICKSLOT		= "quickslotpos";
	private static final String KEPT_LOST       = "kept_lost";
	private static final String CUSTOM_NOTE_ID = "custom_note_id";
	private static final String BAN_UPGRADED   = "ban_upgraded";
	private static final String TAGS           = "tags";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put( QUANTITY, quantity );
		bundle.put( LEVEL, level );
		bundle.put( LEVEL_KNOWN, levelKnown );
		bundle.put( CURSED, cursed );
		bundle.put( CURSED_KNOWN, cursedKnown );
		if (Dungeon.quickslot.contains(this)) {
			bundle.put( QUICKSLOT, Dungeon.quickslot.getSlot(this) );
		}
		bundle.put( KEPT_LOST, keptThoughLostInvent );
		if (customNoteID != -1)     bundle.put(CUSTOM_NOTE_ID, customNoteID);
		bundle.put(BAN_UPGRADED, banUpgraded);
		if (tags != null && !tags.isEmpty()) {
			bundle.put(TAGS, new ArrayList<>(tags.values()));
		}
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		quantity	= bundle.getInt( QUANTITY );
		levelKnown	= bundle.getBoolean( LEVEL_KNOWN );
		cursedKnown	= bundle.getBoolean( CURSED_KNOWN );
		
		int level = bundle.getInt( LEVEL );
		if (level > 0) {
			upgrade( level );
		} else if (level < 0) {
			degrade( -level );
		}
		
		cursed	= bundle.getBoolean( CURSED );

		//only want to populate slot on first load.
		if (Dungeon.hero == null) {
			if (bundle.contains(QUICKSLOT)) {
				Dungeon.quickslot.setSlot(bundle.getInt(QUICKSLOT), this);
			}
		}

		keptThoughLostInvent = bundle.getBoolean( KEPT_LOST );
		if (bundle.contains(CUSTOM_NOTE_ID))    customNoteID = bundle.getInt(CUSTOM_NOTE_ID);
		banUpgraded = bundle.getBoolean(BAN_UPGRADED);
		
		// 恢复标签
		if (bundle.contains(TAGS)) {
			Collection<? extends Bundlable> restoredTags = bundle.getCollection(TAGS);
			if (restoredTags != null) {
				for (Bundlable tag : restoredTags) {
					if (tag instanceof ItemTag) {
						addTag((ItemTag) tag);
					}
				}
			}
		}
	}

	public int targetingPos( Hero user, int dst ){
		return throwPos( user, dst );
	}

	public int throwPos( Hero user, int dst){
		return new Ballistica( user.pos, dst, Ballistica.PROJECTILE ).collisionPos;
	}

	public void throwSound(){
		Sample.INSTANCE.play(Assets.Sounds.MISS, 0.6f, 0.6f, 1.5f);
	}
	
	public void cast( final Hero user, final int dst ) {
		
		final int cell = throwPos( user, dst );
		user.sprite.zap( cell );
		user.busy();

		throwSound();

		Char enemy = Actor.findChar( cell );
		QuickSlotButton.target(enemy);
		
		final float delay = castDelay(user, dst);

		if (enemy != null) {
			((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
					reset(user.sprite,
							enemy.sprite,
							this,
							new Callback() {
						@Override
						public void call() {
							curUser = user;
							Item i = Item.this.detach(user.belongings.backpack);
							if (i != null) i.onThrow(cell);
							if (curUser.hasTalent(Talent.IMPROVISED_PROJECTILES)
									&& !(Item.this instanceof MissileWeapon)
									&& curUser.buff(Talent.ImprovisedProjectileCooldown.class) == null){
								if (enemy != null && enemy.alignment != curUser.alignment){
									Sample.INSTANCE.play(Assets.Sounds.HIT);
									Buff.affect(enemy, Blindness.class, 1f + curUser.pointsInTalent(Talent.IMPROVISED_PROJECTILES));
									Buff.affect(curUser, Talent.ImprovisedProjectileCooldown.class, 50f);
								}
							}
							if (user.buff(Talent.LethalMomentumTracker.class) != null){
								user.buff(Talent.LethalMomentumTracker.class).detach();
								user.next();
							} else {
								user.spendAndNext(delay);
							}
						}
					});
		} else {
			((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).
					reset(user.sprite,
							cell,
							this,
							new Callback() {
						@Override
						public void call() {
							curUser = user;
							Item i = Item.this.detach(user.belongings.backpack);
							user.spend(delay);
							if (i != null) i.onThrow(cell);
							user.next();
						}
					});
		}
	}
	
	public float castDelay( Char user, int dst ){
		return TIME_TO_THROW;
	}
	
	protected static Hero curUser = null;
	protected static Item curItem = null;
	public void setCurrent( Hero hero ){
		curUser = hero;
		curItem = this;
	}

	protected static CellSelector.Listener thrower = new CellSelector.Listener() {
		@Override
		public void onSelect( Integer target ) {
			if (target != null) {
				curItem.cast( curUser, target );
			}
		}
		@Override
		public String prompt() {
			return Messages.get(Item.class, "prompt");
		}
	};

	// ==================== 标签系统 ====================

	/**
	 * 获取标签映射(懒加载)
	 * @return 标签映射实例
	 */
	private Map<String, ItemTag> getTagsMap() {
		if (tags == null) {
			tags = new HashMap<>();
		}
		return tags;
	}
	
	/**
	 * 给物品添加标签。如果存在相同键的标签，则会被替换。
	 * @param tag 要添加的标签(可以是预定义标签或新实例)
	 */
	public void addTag(ItemTag tag) {
		if (tag == null || tag.key() == null) return;
		// 复制标签以避免修改原始预定义实例
		ItemTag tagCopy = tag.copy();
		getTagsMap().put(tagCopy.key(), tagCopy);
		onTagAdded(tagCopy);
		updateQuickslot();
	}

	/**
	 * 批量添加多个标签(优化版,只触发一次UI更新)
	 * @param tags 要添加的标签数组
	 */
	public void addTags(ItemTag... tags) {
		if (tags == null || tags.length == 0) return;
		boolean anyAdded = false;
		for (ItemTag tag : tags) {
			if (tag != null && tag.key() != null) {
				// 复制标签以避免修改原始预定义实例
				ItemTag tagCopy = tag.copy();
				getTagsMap().put(tagCopy.key(), tagCopy);
				onTagAdded(tagCopy);
				anyAdded = true;
			}
		}
		// 只在有标签被添加时才更新UI
		if (anyAdded) {
			updateQuickslot();
		}
	}

	/**
	 * 通过键从物品中移除标签。
	 * @param key 要移除的标签的键
	 * @return 如果标签被移除返回 true，如果不存在返回 false
	 */
	public boolean removeTag(String key) {
		if (tags == null) return false;
		ItemTag removed = tags.remove(key);
		if (removed != null) {
			onTagRemoved(removed);
			updateQuickslot();
		}
		return removed != null;
	}

	/**
	 * 检查物品是否有特定标签。
	 * @param key 要检查的标签的键
	 * @return 如果标签存在返回 true
	 */
	public boolean hasTag(String key) {
		return tags != null && tags.containsKey(key);
	}

	/**
	 * 检查物品是否有任意一个指定标签
	 * @param keys 要检查的标签键数组
	 * @return 如果存在任意一个标签返回 true
	 */
	public boolean hasAnyTag(String... keys) {
		if (tags == null) return false;
		for (String key : keys) {
			if (tags.containsKey(key)) return true;
		}
		return false;
	}

	/**
	 * 检查物品是否同时拥有所有指定标签
	 * @param keys 要检查的标签键数组
	 * @return 如果拥有所有标签返回 true
	 */
	public boolean hasAllTags(String... keys) {
		if (tags == null) return false;
		for (String key : keys) {
			if (!tags.containsKey(key)) return false;
		}
		return true;
	}

	/**
	 * 通过键获取特定标签。
	 * @param key 标签的键
	 * @return 该标签，如果不存在返回 null
	 */
	public ItemTag getTag(String key) {
		return tags != null ? tags.get(key) : null;
	}

	/**
	 * 获取标签的附加数据(便捷方法)
	 * @param key 标签的键
	 * @return 标签的附加数据，如果标签不存在返回 null
	 */
	public Bundle getTagData(String key) {
		ItemTag tag = getTag(key);
		return tag != null ? tag.data() : null;
	}
		
	/**
	 * 设置标签的附加数据(便捷方法)
	 * @param key 标签的键
	 * @param data 要设置的附加数据
	 */
	public void setTagData(String key, Bundle data) {
		if (!hasTag(key)) {
			addTag(new ItemTag(key));
		}
		ItemTag tag = getTag(key);
		if (tag != null) {
			tag.setData(data);
		}
	}

	/**
	 * 获取物品的所有可见标签，按优先级排序。
	 * @return 可见标签列表
	 */
	public List<ItemTag> getVisibleTags() {
		List<ItemTag> visible = new ArrayList<>();
		if (tags != null) {
			for (ItemTag tag : tags.values()) {
				if (tag.isVisible()) {
					visible.add(tag);
				}
			}
			Collections.sort(visible, ItemTag.orderByPriority);
		}
		return visible;
	}

	/**
	 * 获取物品的所有标签(包括可见和隐藏标签)。
	 * @return 所有标签映射的副本
	 */
	public Map<String, ItemTag> getAllTags() {
		return tags != null ? new HashMap<>(tags) : new HashMap<>();
	}

	/**
	 * 获取特定标签的等级。
	 * @param key 标签的键
	 * @return 标签等级，如果标签不存在返回 0
	 */
	public int getTagLevel(String key) {
		ItemTag tag = getTag(key);
		return tag != null ? tag.level() : 0;
	}

	/**
	 * 设置标签的等级。如果标签不存在则创建。
	 * @param key 标签的键
	 * @param level 新的等级
	 */
	public void setTagLevel(String key, int level) {
		if (tags != null && tags.containsKey(key)) {
			tags.get(key).level(level);
		} else {
			addTag(new ItemTag(key, ItemTag.Type.VISIBLE, level));
		}
	}

	/**
	 * 检查物品是否有可见的标签。
	 * @param key 标签的键
	 * @return 如果标签存在且可见返回 true
	 */
	public boolean hasVisibleTag(String key) {
		ItemTag tag = getTag(key);
		return tag != null && tag.isVisible();
	}
		
	/**
	 * 切换标签的可见性(显示/隐藏)。
	 * @param key 标签的键
	 * @param visible true 设为可见，false 设为隐藏
	 */
	public void setTagVisibility(String key, boolean visible) {
		ItemTag tag = getTag(key);
		if (tag != null) {
			tag.setType(visible ? ItemTag.Type.VISIBLE : ItemTag.Type.HIDDEN);
		}
	}

	/**
	 * 清空物品的所有标签。
	 */
	public void clearTags() {
		if (tags != null) {
			tags.clear();
		}
		updateQuickslot();
	}

	/**
	 * 当添加标签时自动调用。重写此方法以实现标签效果。
	 * @param tag 被添加的标签
	 */
	protected void onTagAdded(ItemTag tag) {
		// 子类可以重写此方法来处理特定标签效果
	}

	/**
	 * 当移除标签时自动调用。重写此方法以清理标签效果。
	 * @param tag 被移除的标签
	 */
	protected void onTagRemoved(ItemTag tag) {
		// 基础实现: 处理通用状态标签的移除
		// 子类可以重写此方法来清理特定标签的效果
	}

	// ==================== 标签系统 ====================
}
