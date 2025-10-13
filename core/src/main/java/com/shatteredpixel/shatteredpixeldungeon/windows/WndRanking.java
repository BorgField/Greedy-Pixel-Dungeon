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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.Rankings;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BadgesGrid;
import com.shatteredpixel.shatteredpixeldungeon.ui.BadgesList;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.DeviceCompat;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class WndRanking extends WndTabbed {
	
	private static final int WIDTH			= 115;
	private static final int HEIGHT			= 144;
	
	private static WndRanking INSTANCE;
	
	private String gameID;
	private Rankings.Record record;
	
	public WndRanking( final Rankings.Record rec ) {
		
		super();
		resize( WIDTH, HEIGHT );

		if (INSTANCE != null){
			INSTANCE.hide();
		}
		INSTANCE = this;

		this.gameID = rec.gameID;
		this.record = rec;

		try {
			Badges.loadGlobal();
			Rankings.INSTANCE.loadGameData( rec );
			createControls();
		} catch ( Exception e ) {
			Game.reportException( new RuntimeException("Rankings Display Failed!",e));
			Dungeon.hero = null;
			createControls();
		}
	}
	
	@Override
	public void destroy() {
		super.destroy();
		if (INSTANCE == this){
			INSTANCE = null;
		}
	}
	
	private void createControls() {

		if (Dungeon.hero != null) {
			Icons[] icons =
					{Icons.RANKINGS, Icons.TALENT, Icons.BACKPACK_LRG, Icons.BADGES, Icons.CHALLENGE_COLOR};
			Group[] pages =
					{new StatsTab(), new TalentsTab(), new ItemsTab(), new BadgesTab(), null};

			if (Dungeon.challenges != 0) pages[4] = new ChallengesTab();

			for (int i = 0; i < pages.length; i++) {

				if (pages[i] == null) {
					break;
				}

				add(pages[i]);

				Tab tab = new RankingTab(icons[i], pages[i]);
				add(tab);
			}

			layoutTabs();

			select(0);
		} else {
			StatsTab tab = new StatsTab();
			add(tab);

		}
	}

	private class RankingTab extends IconTab {
		
		private Group page;
		
		public RankingTab( Icons icon, Group page ) {
			super( Icons.get(icon) );
			this.page = page;
		}
		
		@Override
		protected void select( boolean value ) {
			super.select( value );
			if (page != null) {
				page.visible = page.active = selected;
			}
		}
	}
	
	private class StatsTab extends Group {

		private int GAP	= 4;
		
		public StatsTab() {
			super();
			
			String heroClass = record.heroClass.name();
			if (Dungeon.hero != null){
				heroClass = Dungeon.hero.className();
			}
			
			IconTitle title = new IconTitle();
			title.icon( HeroSprite.avatar( record.heroClass, record.armorTier ) );
			title.label( Messages.get(this, "title", record.herolevel, heroClass ).toUpperCase( Locale.ENGLISH ) );
			title.color(Window.TITLE_COLOR);
			title.setRect( 0, 0, WIDTH, 0 );
			add( title );

			if (Dungeon.hero != null && Dungeon.seed != -1){
				GAP--;
			}
			
			float pos = title.bottom() + 1;

			RenderedTextBlock date = PixelScene.renderTextBlock(record.date, 7);
			date.hardlight(0xCCCCCC);
			date.setPos(0, pos);
			add(date);

			RenderedTextBlock version = PixelScene.renderTextBlock(record.version, 7);
			version.hardlight(0xCCCCCC);
			version.setPos(WIDTH-version.width(), pos);
			add(version);

			pos = date.bottom()+5;

			NumberFormat num = NumberFormat.getInstance(Messages.locale());

			if (Dungeon.hero == null){
				pos = statSlot( this, Messages.get(this, "score"), num.format( record.score ), pos );
				pos += GAP;

				Image errorIcon = Icons.WARNING.get();
				errorIcon.y = pos;
				add(errorIcon);

				RenderedTextBlock errorText = PixelScene.renderTextBlock(Messages.get(WndRanking.class, "error"), 6);
				errorText.maxWidth((int)(WIDTH-errorIcon.width()-GAP));
				errorText.setPos(errorIcon.width()+GAP, pos + (errorIcon.height()-errorText.height())/2);
				add(errorText);

			} else {

				pos = statSlot(this, Messages.get(this, "score"), num.format(Statistics.totalScore), pos);

				IconButton scoreInfo = new IconButton(Icons.get(Icons.INFO)) {
					@Override
					protected void onClick() {
						super.onClick();
						ShatteredPixelDungeon.scene().addToFront(new WndScoreBreakdown());
					}
				};
				scoreInfo.setSize(16, 16);
				scoreInfo.setPos(WIDTH - scoreInfo.width(), pos - 10 - GAP);
				add(scoreInfo);

				pos += GAP;

				int strBonus = Dungeon.hero.STR() - Dungeon.hero.STR;
				if (strBonus > 0)
					pos = statSlot(this, Messages.get(this, "str"), Dungeon.hero.STR + " + " + strBonus, pos);
				else if (strBonus < 0)
					pos = statSlot(this, Messages.get(this, "str"), Dungeon.hero.STR + " - " + -strBonus, pos);
				else
					pos = statSlot(this, Messages.get(this, "str"), Integer.toString(Dungeon.hero.STR), pos);
				pos = statSlot(this, Messages.get(this, "duration"), num.format((int) Statistics.duration), pos);
				if (Statistics.highestAscent == 0) {
					pos = statSlot(this, Messages.get(this, "depth"), num.format(Statistics.deepestFloor), pos);
				} else {
					pos = statSlot(this, Messages.get(this, "ascent"), num.format(Statistics.highestAscent), pos);
				}
				if (Dungeon.seed != -1) {
					if (Dungeon.daily) {
						if (Dungeon.dailyReplay) {
							pos = statSlot(this, Messages.get(this, "replay_for"), "_" + Dungeon.customSeedText + "_", pos);
						} else {
							pos = statSlot(this, Messages.get(this, "daily_for"), "_" + Dungeon.customSeedText + "_", pos);
						}
					} else if (!Dungeon.customSeedText.isEmpty()) {
						pos = statSlot(this, Messages.get(this, "custom_seed"), "_" + Dungeon.customSeedText + "_", pos);
					} else {
						pos = statSlot(this, Messages.get(this, "seed"), DungeonSeed.convertToCode(Dungeon.seed), pos);
					}
				} else {
					pos += GAP + 5;
				}

				pos += GAP;

				pos = statSlot(this, Messages.get(this, "enemies"), num.format(Statistics.enemiesSlain), pos);
				pos = statSlot(this, Messages.get(this, "gold"), num.format(Statistics.goldCollected), pos);
				pos = statSlot(this, Messages.get(this, "food"), num.format(Statistics.foodEaten), pos);
				pos = statSlot(this, Messages.get(this, "alchemy"), num.format(Statistics.itemsCrafted), pos);
			}

			int buttontop = HEIGHT - 16;

			if (Dungeon.hero != null && Dungeon.seed != -1 && !Dungeon.daily &&
					(DeviceCompat.isDebug() || Badges.isUnlocked(Badges.Badge.VICTORY))){
				final Image icon = Icons.get(Icons.SEED);
				RedButton btnSeed = new RedButton(Messages.get(this, "copy_seed")){
					@Override
					protected void onClick() {
						super.onClick();
						ShatteredPixelDungeon.scene().addToFront(new WndOptions(new Image(icon),
								Messages.get(WndRanking.StatsTab.this, "copy_seed"),
								Messages.get(WndRanking.StatsTab.this, "copy_seed_desc"),
								Messages.get(WndRanking.StatsTab.this, "copy_seed_copy"),
								Messages.get(WndRanking.StatsTab.this, "copy_seed_cancel")){
							@Override
							protected void onSelect(int index) {
								super.onSelect(index);
								if (index == 0){
									SPDSettings.customSeed(DungeonSeed.convertToCode(Dungeon.seed));
									icon.hardlight(1f, 1.5f, 0.67f);
								}
							}
						});
					}
				};
				if (DungeonSeed.convertFromText(SPDSettings.customSeed()) == Dungeon.seed){
					icon.hardlight(1f, 1.5f, 0.67f);
				}
				btnSeed.icon(icon);
				btnSeed.setRect(0, buttontop, 115, 16);
				add(btnSeed);
			}

		}
		
		private float statSlot( Group parent, String label, String value, float pos ) {
			
			RenderedTextBlock txt = PixelScene.renderTextBlock( label, 7 );
			txt.setPos(0, pos);
			parent.add( txt );
			
			txt = PixelScene.renderTextBlock( value, 7 );
			txt.setPos(WIDTH * 0.6f, pos);
			PixelScene.align(txt);
			parent.add( txt );
			
			return pos + GAP + txt.height();
		}
	}

	private class TalentsTab extends Group{

		public TalentsTab(){
			super();

			camera = WndRanking.this.camera;

			int tiers = 1;
			if (Dungeon.hero.lvl >= 6) tiers++;
			if (Dungeon.hero.lvl >= 12 && Dungeon.hero.subClass != HeroSubClass.NONE) tiers++;
			if (Dungeon.hero.lvl >= 20 && Dungeon.hero.armorAbility != null) tiers++;
			while (Dungeon.hero.talents.size() > tiers){
				Dungeon.hero.talents.remove(Dungeon.hero.talents.size()-1);
			}

			TalentsPane p = new TalentsPane(TalentButton.Mode.INFO);
			add(p);
			p.setPos(0, 0);
			p.setSize(WIDTH, HEIGHT);
			p.setPos(0, 0);

		}

	}

	private class ItemsTab extends Group {
		private float pos;
		private final ScrollPane list;
		private final ArrayList<ItemButton> itemButtons = new ArrayList<>();
		private final ArrayList<QuickSlotButton> quickSlotButtons = new ArrayList<>();
		private int exHeight = 0;

		public ItemsTab() {
			super();

			camera = WndRanking.this.camera;
			list = new ScrollPane(new Component());
			add(list);

			updateList();
		}

		private void updateList() {
			// 清除旧的按钮
			clearButtons();

			Belongings stuff = Dungeon.hero.belongings;
			exHeight = (stuff.weapon3 != null || stuff.weapon4 != null) ? 42 : 0;

			// 设置滚动面板
			list.content().clear();
			list.content().setSize(WIDTH, HEIGHT + 28 + exHeight);
			list.setRect(0, 0, WIDTH, HEIGHT);

			// 添加装备物品
			addEquipmentItems();

			// 添加分隔线
			addSeparator();

			// 添加快捷栏按钮
			addQuickSlotButtons();
		}

		private void clearButtons() {
			// 清除物品按钮
			for (ItemButton button : itemButtons) {
				list.content().remove(button);
			}
			itemButtons.clear();

			// 清除快捷栏按钮
			for (QuickSlotButton button : quickSlotButtons) {
				list.content().remove(button);
			}
			quickSlotButtons.clear();
		}

		private void addEquipmentItems() {
			Belongings stuff = Dungeon.hero.belongings;
			addItemIfNotNull(stuff.weapon);
			addItemIfNotNull(stuff.weapon2);
			addItemIfNotNull(stuff.armor);
			addItemIfNotNull(stuff.artifact);
			addItemIfNotNull(stuff.misc);
			addItemIfNotNull(stuff.ring);
			addItemIfNotNull(stuff.weapon3);
			addItemIfNotNull(stuff.weapon4);
		}

		private void addItemIfNotNull(Item item) {
			if (item != null) {
				addItem(item);
			}
		}

		private void addSeparator() {
			ColorBlock sep = new ColorBlock(1, 1, 0xFF000000);
			sep.size(WIDTH, 1);
			sep.y = 127 + exHeight;
			list.content().add(sep);
		}

		private void addQuickSlotButtons() {
			float pos = 0;
			float slotWidth = (WIDTH - 5) / 6f;

			// 添加第一行快捷栏按钮
			addQuickSlotRow(0, 6, 130 + exHeight, pos, slotWidth);

			// 添加第二行快捷栏按钮
			addQuickSlotRow(6, QuickSlot.SIZE, 151 + exHeight, 0, slotWidth);
		}

		private void addQuickSlotRow(int start, int end, float y, float startPos, float slotWidth) {
			float pos = startPos;
			for (int i = start; i < end; i++) {
				Item item = Dungeon.quickslot.getItem(i);
				if (item != null) {
					QuickSlotButton slot = new QuickSlotButton(item);
					slot.setRect(pos, y, slotWidth, 20);
					PixelScene.align(slot);
					list.content().add(slot);
					quickSlotButtons.add(slot);
					pos += slotWidth + 1;
				}
			}
		}

		private void addItem(Item item) {
			ItemButton slot = new ItemButton(item);
			slot.setRect(0, pos, WIDTH, 20);
			list.content().add(slot);
			itemButtons.add(slot);
			pos += slot.height() + 1;
		}
	}


	private class BadgesTab extends Group {
		
		public BadgesTab() {
			super();
			
			camera = WndRanking.this.camera;

			Component badges;
			if (Badges.filterReplacedBadges(false).size() <= 8){
				badges = new BadgesList(false);
			} else {
				badges = new BadgesGrid(false);
			}
			add(badges);
			badges.setSize( WIDTH, HEIGHT );
		}
	}

	private class ChallengesTab extends Group{
		private float pos;
		private final ScrollPane challenges;
		private static final int GAP = 1;
		private static final int BTN_HEIGHT = 16;

		public ChallengesTab() {
			super();

			camera = WndRanking.this.camera;

			// 创建一个组件作为滚动内容
			Component content = new Component();
			challenges = new ScrollPane(content);
			add(challenges);

			updateList();
		}

		private void updateList() {
			// 设置滚动面板
			challenges.content().clear();

			// 计算总高度
			float totalHeight = 30;
			for (int i=0; i < Challenges.NAME_IDS.length; i++) {
				totalHeight += BTN_HEIGHT; // 每个挑战项的高度
				if (i > 0) {
					totalHeight += GAP; // 间隔
				}
			}

			// 设置内容尺寸，确保可以滚动
			challenges.content().setSize(WIDTH, totalHeight);
			challenges.setRect(0, 0, WIDTH, HEIGHT);

			float pos = 0;
			boolean isCustom = false;

			for (int i=0; i < Challenges.NAME_IDS.length; i++) {
				final String challenge = Challenges.NAME_IDS[i];

				// 添加传统挑战标题
				if(i == 0) {
					RenderedTextBlock block = PixelScene.renderTextBlock(9);
					block.text(Messages.get(Challenges.class, "traditional"));
					block.hardlight(Window.TITLE_COLOR);
					block.setPos((WIDTH - block.width()) / 2, pos + 1);
					PixelScene.align(block);
					challenges.content().add(block);
					pos += block.height() + 5;
				}

				// 添加扩展挑战标题
				if(Challenges.NAME_IDS[i].equals("test_mode")) {
					RenderedTextBlock block = PixelScene.renderTextBlock(9);
					block.text(Messages.get(Challenges.class, "expansion"));
					block.hardlight(Window.TITLE_COLOR);
					block.setPos((WIDTH - block.width()) / 2, pos + 4);
					PixelScene.align(block);
					challenges.content().add(block);
					pos += block.height() + 8;
					isCustom = true;
				}

				CheckBox cb = new CheckBox( Messages.titleCase(Messages.get(Challenges.class, challenge)) );
				cb.checked( (Dungeon.challenges & Challenges.MASKS[i]) != 0 );
				cb.active = false;

				if (i > 0) {
					pos += GAP;
				}
				cb.setRect( 0, pos, WIDTH-16, BTN_HEIGHT );

				challenges.content().add( cb );

				IconButton info = new IconButton(Icons.get(Icons.INFO)){
					@Override
					protected void onClick() {
						super.onClick();
						ShatteredPixelDungeon.scene().add(
								new WndMessage(Messages.get(Challenges.class, challenge+"_desc"))
						);
					}
				};
				info.setRect(cb.right(), pos, 16, BTN_HEIGHT);
				challenges.content().add(info);

				pos = cb.bottom();
			}
		}
	}

	private class ItemButton extends Button {

		public static final int WIDTH   = 20;
		public static final int HEIGHT	= 20;
		
		private Item item;
		
		private ItemSlot slot;
		private ColorBlock bg;
		private RenderedTextBlock name;
		
		public ItemButton( Item item ) {
			
			super();

			this.item = item;
			
			slot.item( item );
			if (item.cursed && item.cursedKnown) {
				bg.ra = +0.3f;
				bg.ga = -0.15f;
				bg.ba = -0.15f;
			} else if (!item.isIdentified()) {
				if ((item instanceof EquipableItem || item instanceof Wand) && item.cursedKnown){
					bg.ba = +0.3f;
					bg.ra = -0.1f;
				} else {
					bg.ra = +0.35f;
					bg.ba = +0.35f;
				}
			}
		}
		
		@Override
		protected void createChildren() {
			
			bg = new ColorBlock( 20, HEIGHT, 0x9953564D );
			add( bg );
			
			slot = new ItemSlot();
			add( slot );
			
			name = PixelScene.renderTextBlock( 7 );
			add( name );
			
			super.createChildren();
		}
		
		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;
			
			slot.setRect( x, y, WIDTH, HEIGHT );
			PixelScene.align(slot);
			
			name.maxWidth((int)(width - slot.width() - 2));
			name.text(Messages.titleCase(item.name()));
			name.setPos(
					slot.right()+2,
					y + (height - name.height()) / 2
			);
			PixelScene.align(name);
			
			super.layout();
		}
		
		@Override
		protected void onPointerDown() {
			bg.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.7f, 0.7f, 1.2f );
		}
		
		protected void onPointerUp() {
			bg.brightness( 1.0f );
		}
		
		@Override
		protected void onClick() {
			Game.scene().add( new WndInfoItem( item ) );
		}
	}

	private class QuickSlotButton extends ItemSlot{

		private Item item;
		private ColorBlock bg;

		QuickSlotButton(Item item){
			super(item);
			this.item = item;

			if (item.cursed && item.cursedKnown) {
				bg.ra = +0.2f;
				bg.ga = -0.1f;
			} else if (!item.isIdentified()) {
				bg.ra = 0.1f;
				bg.ba = 0.1f;
			}
		}

		@Override
		protected void createChildren() {
			bg = new ColorBlock( 1, 1, 0x9953564D );
			add( bg );

			super.createChildren();
		}

		@Override
		protected void layout() {
			bg.x = x;
			bg.y = y;

			bg.size( width(), height() );

			super.layout();
		}

		@Override
		protected void onPointerDown() {
			bg.brightness( 1.5f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.7f, 0.7f, 1.2f );
		}

		protected void onPointerUp() {
			bg.brightness( 1.0f );
		}

		@Override
		protected void onClick() {
			Game.scene().add(new WndInfoItem(item));
		}
	}
}
