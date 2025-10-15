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

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.custom.buffs.GameTracker;
import com.shatteredpixel.shatteredpixeldungeon.custom.dict.Dict;
import com.shatteredpixel.shatteredpixeldungeon.custom.dict.HeroStat;
import com.shatteredpixel.shatteredpixeldungeon.custom.messages.M;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.TalentsPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.DungeonSeed;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class WndHero extends WndTabbed {

	private static final int WIDTH		= 120;
	private static final int HEIGHT		= 136;

	private StatsTab stats;
	private TalentsTab talents;
	private BuffsTab buffs;

	public static int lastIdx = 0;

	public WndHero() {

		super();

		resize( WIDTH, HEIGHT );

		stats = new StatsTab();
		add( stats );

		talents = new TalentsTab();
		add(talents);
		talents.setRect(0, 0, WIDTH, HEIGHT);

		buffs = new BuffsTab();
		add( buffs );
		buffs.setRect(0, 0, WIDTH, HEIGHT);
		buffs.setupList();

		add( new IconTab( Icons.get(Icons.RANKINGS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) {
					lastIdx = 0;
					if (!stats.visible) {
						stats.initialize();
					}
				}
				stats.visible = stats.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.TALENT) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 1;
				if (selected) StatusPane.talentBlink = 0;
				talents.visible = talents.active = selected;
			}
		} );
		add( new IconTab( Icons.get(Icons.BUFFS) ) {
			protected void select( boolean value ) {
				super.select( value );
				if (selected) lastIdx = 2;
				buffs.visible = buffs.active = selected;
			}
		} );

		layoutTabs();

		talents.setRect(0, 0, WIDTH, HEIGHT);
		talents.pane.scrollTo(0, talents.pane.content().height() - talents.pane.height());
		talents.layout();

		select( lastIdx );
	}

	@Override
	public boolean onSignal(KeyEvent event) {
		if (event.pressed && KeyBindings.getActionForKey( event ) == SPDAction.HERO_INFO) {
			onBackPressed();
			return true;
		} else {
			return super.onSignal(event);
		}
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		talents.layout();
		buffs.layout();
	}

	private class StatsTab extends Group {

		private static final int GAP = 4;
		private float pos;
		private final boolean useIconLayout = SPDSettings.graphicStats(); // 控制使用哪种布局

		public StatsTab() {
			initialize();
		}

		public void initialize() {
			for (Gizmo g : members) {
				if (g != null) g.destroy();
			}
			clear();

			Hero hero = Dungeon.hero;

			// 共同的标题部分
			IconTitle title = new IconTitle();
			title.icon(HeroSprite.avatar(hero));
			if (hero.name().equals(hero.className()))
				title.label(Messages.get(this, "title", hero.lvl, hero.className()).toUpperCase(Locale.ENGLISH));
			else
				title.label((hero.name() + "\n" + Messages.get(this, "title", hero.lvl, hero.className())).toUpperCase(Locale.ENGLISH));
			title.color(Window.TITLE_COLOR);
			title.setRect(0, 0, WIDTH - 16, 0);
			add(title);

			IconButton infoButton = new IconButton(Icons.get(Icons.INFO)) {
				@Override
				protected void onClick() {
					super.onClick();
					if (ShatteredPixelDungeon.scene() instanceof GameScene) {
						GameScene.show(new WndHeroInfo(hero.heroClass));
					} else {
						ShatteredPixelDungeon.scene().addToFront(new WndHeroInfo(hero.heroClass));
					}
				}

				@Override
				protected String hoverText() {
					return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
				}
			};
			infoButton.setRect(title.right(), 0, 16, 16);
			add(infoButton);

			IconButton itemButton = new IconButton(Icons.get(Icons.MAGNIFY)){
				@Override
				protected void onClick() {
					super.onClick();
					GameScene.show(new StatsTab.WndTreasureGenerated());
				}

				@Override
				protected String hoverText() {
					return Messages.titleCase(Messages.get(HeroStat.class, "item_enter"));
				}

			};
			itemButton.setRect(title.right() + 1, itemButton.height() + 16, 16, 16);
			if (Dungeon.isChallenged(Challenges.TEST_MODE)) add(itemButton);

			pos = title.bottom() + GAP;

			if (useIconLayout) {
				// 使用第二种布局（图标布局）
				initializeIconLayout();
			} else {
				// 使用第一种布局（原始布局）
				initializeOriginalLayout();
			}
		}

		private void initializeOriginalLayout() {
			Hero hero = Dungeon.hero;

			int strBonus = hero.STR() - hero.STR;
			if (strBonus > 0)           statSlot(Messages.get(this, "str"), hero.STR + " + " + strBonus);
			else if (strBonus < 0)      statSlot(Messages.get(this, "str"), hero.STR + " - " + -strBonus);
			else                        statSlot(Messages.get(this, "str"), hero.STR());
			if (hero.shielding() > 0)   statSlot(Messages.get(this, "health"), hero.HP + "+" + hero.shielding() + "/" + hero.HT);
			else                        statSlot(Messages.get(this, "health"), (hero.HP) + "/" + hero.HT);
			statSlot(Messages.get(this, "exp"), hero.exp + "/" + hero.maxExp());

			pos += GAP;

			statSlot(Messages.get(this, "gold"), Statistics.goldCollected);
			statSlot(Messages.get(this, "depth"), Statistics.deepestFloor);
			if (Dungeon.daily) {
				if (!Dungeon.dailyReplay) {
					statSlot(Messages.get(this, "daily_for"), "_" + Dungeon.customSeedText + "_");
				} else {
					statSlot(Messages.get(this, "replay_for"), "_" + Dungeon.customSeedText + "_");
				}
			} else if (!Dungeon.customSeedText.isEmpty()) {
				statSlot(Messages.get(this, "custom_seed"), "_" + Dungeon.customSeedText + "_");
			} else {
				statSlot(Messages.get(this, "dungeon_seed"), DungeonSeed.convertToCode(Dungeon.seed));
			}

			pos += GAP;

			Hunger hunger = Dungeon.hero.buff(Hunger.class);
			String hunger_str = "null";
			if (hunger != null) {
				hunger_str = hunger.hunger() + "/" + Hunger.STARVING;
			}
			statSlot(M.L(Challenges.class, "hunger"), hunger_str);
			statSlot(M.L(Challenges.class, "turns"), String.format(Locale.ROOT, "%.2f", Statistics.turnsPassed));
			int t_all_sec = Math.round(Statistics.real_seconds);
			int t_hour = t_all_sec / 3600;
			int t_minute = (t_all_sec - t_hour * 3600) / 60;
			int t_second = t_all_sec - t_hour * 3600 - t_minute * 60;
			statSlot(M.L(Challenges.class, "playtime"), String.format(Locale.ROOT, "%dd %dm %ds", t_hour, t_minute, t_second));

//			if (Dungeon.isChallenged(Challenges.TEST_MODE)) {
//				pos += 2;
//				RedButton buttonItem = new RedButton(M.L(Challenges.class, "item_enter"), 8) {
//					@Override
//					protected void onClick() {
//						super.onClick();
//						GameScene.show(new WndTreasureGenerated());
//					}
//				};
//				add(buttonItem);
//				buttonItem.setRect(2, pos, WIDTH - 4, 16);
//				buttonItem.active = true;
//				buttonItem.alpha(1.0f);
//				pos += buttonItem.height();
//			}
		}

		private void initializeIconLayout() {
			Hero hero = Dungeon.hero;

			// 属性统计部分（图标+数值布局）
			int strBonus = hero.STR() - hero.STR;
			String strValue = hero.STR + (strBonus != 0 ? " " + (strBonus > 0 ? "+" : "-") + Math.abs(strBonus) : "");
			statSlot(Icons.ABILITY_STR, Messages.get(this, "str"), strValue, 0, true);

			String healthValue = hero.HP + (hero.shielding() > 0 ? "+" + hero.shielding() : "") + "/" + hero.HT;
			statSlot(Icons.ABILITY_HP, Messages.get(this, "health"), healthValue, 0, true);

			Hunger hunger = Dungeon.hero.buff(Hunger.class);
			int satiety = 450 - hunger.hunger();
			String hungerValue = (hunger != null) ?
					satiety + "/" + (int)Hunger.STARVING : "null";
			statSlot(Icons.ABILITY_HUNGER, M.L(Dict.HeroStat.class, "hunger"), hungerValue, 0, false);
			statSlot(Icons.ABILITY_EXP, Messages.get(this, "exp"), hero.exp + "/" + hero.maxExp(), WIDTH / 2f, true);

			pos += 3;

			statSlot(Icons.ABILITY_GOLD, Messages.get(this, "gold"), Statistics.goldCollected, false);
			statSlot(Icons.ABILITY_DEPTH, Messages.get(this, "depth"), String.valueOf(Statistics.deepestFloor), WIDTH / 2f, true);

			int t_all_sec = Math.round(Statistics.real_seconds);
			int t_hour = t_all_sec / 3600;
			int t_minute = (t_all_sec % 3600) / 60;
			int t_second = t_all_sec % 60;
			String playtime = String.format(Locale.ENGLISH, "%dh %dm %ds", t_hour, t_minute, t_second);
			statSlot(Icons.ABILITY_TIMES, M.L(Dict.HeroStat.class, "playtime"), playtime, 0, false);

			statSlot(Icons.ABILITY_TURNS, M.L(Dict.HeroStat.class, "turns"),
					String.format(Locale.ENGLISH, "%.2f", Statistics.turnsPassed), WIDTH / 2f, true);

			pos += 3;

			// 种子信息
			if (Dungeon.daily) {
				String seedText = "_" + Dungeon.customSeedText + "_";
				if (!Dungeon.dailyReplay) {
					statSlot(Icons.ABILITY_SEED, Messages.get(this, "daily_for"), seedText, 0, true);
				} else {
					statSlot(Icons.ABILITY_SEED, Messages.get(this, "replay_for"), seedText, 0, true);
				}
			} else if (!Dungeon.customSeedText.isEmpty()) {
				String seedText = "_" + Dungeon.customSeedText + "_";
				statSlot(Icons.ABILITY_SEED, Messages.get(this, "custom_seed"), seedText, 0, true);
			} else {
				statSlot(Icons.ABILITY_SEED, Messages.get(this, "dungeon_seed"), DungeonSeed.convertToCode(Dungeon.seed), 0, true);
			}

//			if (Dungeon.isChallenged(Challenges.TEST_MODE)) {
//				pos -= 3;
//				RedButton buttonItem = new RedButton(M.L(Challenges.class, "item_enter"), 8) {
//					@Override
//					protected void onClick() {
//						super.onClick();
//						GameScene.show(new WndTreasureGenerated());
//					}
//				};
//				add(buttonItem);
//				buttonItem.setRect(2, pos, WIDTH - 4, 16);
//				buttonItem.active = true;
//				buttonItem.alpha(1.0f);
//				pos += buttonItem.height();
//			}
		}

		// 第一种文本布局的statSlot方法
		private void statSlot(String label, String value) {
			RenderedTextBlock txt = PixelScene.renderTextBlock(label, 8);
			txt.setPos(0, pos);
			add(txt);

			txt = PixelScene.renderTextBlock(value, 8);
			txt.setPos(WIDTH * 0.55f, pos);
			PixelScene.align(txt);
			add(txt);

			pos += GAP + txt.height();
		}

		private void statSlot(String label, int value) {
			statSlot(label, Integer.toString(value));
		}

		// 第二种图形化布局的statSlot方法
		private void statSlot(Icons icon, String tooltip, String value, float xStart, boolean enter) {
			// 图标按钮
			IconButton iconBtn = new IconButton(Icons.get(icon)) {
				@Override
				protected String hoverText() {
					return tooltip;
				}
			};
			iconBtn.setSize(12, 12);
			iconBtn.setPos(3 + xStart, pos + (PixelScene.renderTextBlock(value, 8).height() - 12) / 2);
			add(iconBtn);

			// 数值文本
			RenderedTextBlock txt = PixelScene.renderTextBlock(value, 8);
			txt.setPos(WIDTH * 0.145f + xStart, pos + (6 - txt.height()));
			PixelScene.align(txt);
			add(txt);

			// 更新位置
			if (enter) {
				pos += Math.max(iconBtn.height(), txt.height()) + GAP;
			}
		}

		private void statSlot(Icons icon, String tooltip, int value, boolean enter) {
			statSlot(icon, tooltip, Integer.toString(value), 0, enter);
		}

		public float height() {
			return pos;
		}

		private class WndTreasureGenerated extends Window{
			private static final int WIDTH = 120;
			private static final int HEIGHT = 144;

			public WndTreasureGenerated(){
				super();
				resize(WIDTH, HEIGHT);
				ScrollPane pane = new ScrollPane(new Component());
				Component content = pane.content();
				this.add(pane);
				pane.setRect(0,0,WIDTH, HEIGHT);

				GameTracker gmt = hero.buff(GameTracker.class);
				if(gmt != null){
					String allInfo = gmt.itemInfo();
					String[] result = allInfo.split("\n");
					float pos = 2;
					for(String info: result){
						if(info.contains("dungeon_depth")){
							pos += 4;
							// 处理深度文本，使用默认值以防本地化键不存在
							String depthTextStr = info.replace("dungeon_depth: ", "Depth: ");
							try {
								depthTextStr = info.replace("dungeon_depth: ", M.L(HeroStat.class, "item_wnd_depth"));
							} catch (Exception e) {
								// 如果本地化失败，使用默认值
							}
							RenderedTextBlock depthText = PixelScene.renderTextBlock(depthTextStr, 8);
							depthText.maxWidth(WIDTH);
							depthText.hardlight(0xFFFF00);
							content.add(depthText);
							depthText.setPos(0, pos);
							pos += 8;
						}else{
							pos += 1;
							// 处理物品文本，使用默认值以防本地化键不存在
							String processedInfo = info;
							try {
								processedInfo = info.replace("MIMIC_HOLD", M.L(HeroStat.class, "item_wnd_mimic"));
							} catch (Exception e) {
								processedInfo = info.replace("MIMIC_HOLD", "Mimic Hold");
							}

							try {
								processedInfo = processedInfo.replace("QUEST_REWARD", M.L(HeroStat.class, "item_wnd_reward"));
							} catch (Exception e) {
								processedInfo = processedInfo.replace("QUEST_REWARD", "Quest Reward");
							}

							try {
								processedInfo = processedInfo.replace("CURSED", M.L(HeroStat.class, "item_wnd_cursed"));
							} catch (Exception e) {
								processedInfo = processedInfo.replace("CURSED", "Cursed");
							}
							RenderedTextBlock itemText = PixelScene.renderTextBlock(processedInfo, 6);
							itemText.maxWidth(WIDTH);
							content.add(itemText);
							itemText.setPos(0, pos);
							pos += 6;
							String level = Pattern.compile("[^0-9]").matcher(processedInfo).replaceAll("").trim();
							try{
								int lvl = Integer.parseInt(level);
								if(lvl == 1){
									itemText.hardlight(0x57FAFF);
								}else if(lvl == 2){
									itemText.hardlight(0xA000A0);
								}else if(lvl == 3){
									itemText.hardlight(0xFFB700);
								}else if(lvl >= 4) {
									itemText.hardlight(Window.Pink_COLOR);
								}
							}catch (Exception e){

							}


						}
					}
					content.setSize(WIDTH, pos + 2);
				}
				pane.scrollTo(0, 0);
			}
		}
	}

	public class TalentsTab extends Component {

		TalentsPane pane;

		@Override
		protected void createChildren() {
			super.createChildren();
			pane = new TalentsPane(TalentButton.Mode.UPGRADE);
			add(pane);
		}

		@Override
		protected void layout() {
			super.layout();
			pane.setRect(x, y, width, height);
		}

	}

	private class BuffsTab extends Component {

		private static final int GAP = 2;

		private float pos;
		private ScrollPane buffList;
		private ArrayList<BuffSlot> slots = new ArrayList<>();

		@Override
		protected void createChildren() {

			super.createChildren();

			buffList = new ScrollPane( new Component() ){
				@Override
				public void onClick( float x, float y ) {
					int size = slots.size();
					for (int i=0; i < size; i++) {
						if (slots.get( i ).onClick( x, y )) {
							break;
						}
					}
				}
			};
			add(buffList);
		}

		@Override
		protected void layout() {
			super.layout();
			buffList.setRect(0, 0, width, height);
		}

		private void setupList() {
			Component content = buffList.content();
			for (Buff buff : Dungeon.hero.buffs()) {
				if (buff.icon() != BuffIndicator.NONE) {
					BuffSlot slot = new BuffSlot(buff);
					slot.setRect(0, pos, WIDTH, slot.icon.height());
					content.add(slot);
					slots.add(slot);
					pos += GAP + slot.height();
				}
			}
			content.setSize(buffList.width(), pos);
			buffList.setSize(buffList.width(), buffList.height());
		}

		private class BuffSlot extends Component {

			private Buff buff;

			Image icon;
			RenderedTextBlock txt;

			public BuffSlot( Buff buff ){
				super();
				this.buff = buff;

				icon = new BuffIcon(buff, true);
				icon.y = this.y;
				add( icon );

				txt = PixelScene.renderTextBlock( Messages.titleCase(buff.name()), 8 );
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
				add( txt );

			}

			@Override
			protected void layout() {
				super.layout();
				icon.y = this.y;
				txt.maxWidth((int)(width - icon.width()));
				txt.setPos(
						icon.width + GAP,
						this.y + (icon.height - txt.height()) / 2
				);
				PixelScene.align(txt);
			}

			protected boolean onClick ( float x, float y ) {
				if (inside( x, y )) {
					GameScene.show(new WndInfoBuff(buff));
					return true;
				} else {
					return false;
				}
			}
		}
	}
}