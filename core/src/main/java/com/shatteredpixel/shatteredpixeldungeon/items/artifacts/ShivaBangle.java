package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import static com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class ShivaBangle extends Artifact {

    {
        image = ItemSpriteSheet.GAUNTLETS;
        defaultAction = AC_FANATICAL;
        levelCap = 5;
        exp = 0;

        charge = 0;
        partialCharge = 0;
        chargeCap = 100;
    }

    //攻击后获取2%充能，闪避时获取4%充能，消耗60-5*lv%充能使用施加战狂姿态lv1，如果处于空手状态，改为施加战狂姿态lv2，
    // 如果已经处于战狂姿态充能足够，再次使用时 战狂姿态时间刷新且lv+1；
    // 战狂姿态lv1：视为徒手攻击，触发被动属性增加效果
    // 战狂姿态lv2：被动效果*2，攻击距离+1
    // 战狂姿态lv3：被动效果*4，攻击距离+1
    //获取升级所需的经验值
    public int expToNextLevel() {
        if (level() >= levelCap) {
            return Integer.MAX_VALUE;
        }
        // 确保返回值至少为10，避免level为0时返回0导致无限升级
        return Math.max(10, 20 * level());
    }

    //获取经验
    public void gainExp(int amount) {
        if (level() >= levelCap) {
            return;
        }
        exp += amount;
        while (exp >= expToNextLevel() && level() < levelCap) {
            exp -= expToNextLevel();
            upgrade();
            GLog.p("level_up!"+ level());
        }
        updateQuickslot();
    }

    public static final String AC_OUTFIT = "OUTFIT";
    public static final String AC_FANATICAL= "FANATICAL";

    @Override
    public ArrayList<String> actions( Hero hero ) {
        ArrayList<String> actions = super.actions( hero );
        if (isIdentified() && !cursed){
            actions.add(AC_OUTFIT);
            actions.add(AC_FANATICAL);
        }

        return actions;
    }


    @Override
    public void execute( Hero hero, String action ) {

        super.execute(hero, action);

        if (action.equals(AC_OUTFIT)){
            GameScene.show( new ShivaBangle.WndShivaWeapon(this) );
        }
        if (action.equals(AC_FANATICAL)){
            // 计算消耗的充能：60-5*lv%
            int chargeCost = 60 - 5 * level();

            if (charge < chargeCost) {
                GLog.w(Messages.get(this, "no_charge"));
                return;
            }

            Berserker berserker = hero.buff(Berserker.class);
            boolean isUnarmed = RingOfForce.fightingUnarmed(hero);

            if (berserker == null) {
                // 第一次使用战狂姿态
                charge -= chargeCost;
                if (isUnarmed) {
                    Buff.affect(hero, Berserker.class).setLevel(2); // 空手状态直接lv2

                } else {
                    Buff.affect(hero, Berserker.class).setLevel(1); // 非空手状态lv1

                }
            } else {
                // 已经处于战狂姿态
                if (charge >= chargeCost) {
                    charge -= chargeCost;
                    // 刷新时间并升级
                    berserker.setCooldown();
                    int newLevel = berserker.getLvl() + 1;
                    if (newLevel > 3) newLevel = 3; // 最高lv3
                    berserker.setLevel(newLevel);
                    hero.spendAndNext(Actor.TICK);
                    GLog.p(Messages.get(this, "berserker_upgrade", newLevel));
                } else {
                    GLog.w(Messages.get(this, "no_charge_upgrade"));
                }
            }

            updateQuickslot();
        }
    }

    @Override
    public boolean doEquip(Hero hero) {
        if (super.doEquip(hero)) {
            // 在这里添加装备时的触发逻辑
            GLog.w(Messages.get(this, "equip_message"));
            activate(hero);

            // 确保weapon3和weapon4槽位可用
            if (hero.belongings.weapon3 != null) {
                hero.belongings.weapon3.activate(hero);
            }
            if (hero.belongings.weapon4 != null) {
                hero.belongings.weapon4.activate(hero);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void activate(Char target) {
        super.activate(target);
        // 在这里添加激活时的逻辑

        if (target instanceof Hero) {
            Hero hero = (Hero) target;
            // 确保weapon3和weapon4槽位可用
            if (hero.belongings.weapon3 != null) {
                hero.belongings.weapon3.activate(hero);
            }
            if (hero.belongings.weapon4 != null) {
                hero.belongings.weapon4.activate(hero);
            }
        }
    }

    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (super.doUnequip(hero, collect, single)) {
            // 卸下时，将weapon3和weapon4的武器放回背包或掉落
            if (hero.belongings.weapon3 != null) {
                if (!hero.belongings.weapon3.doPickUp(hero)) {
                    Dungeon.level.drop(hero.belongings.weapon3, hero.pos);
                }
                hero.belongings.weapon3 = null;
            }
            if (hero.belongings.weapon4 != null) {
                if (!hero.belongings.weapon4.doPickUp(hero)) {
                    Dungeon.level.drop(hero.belongings.weapon4, hero.pos);
                }
                hero.belongings.weapon4 = null;
            }
            return true;
        } else {
            return false;
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
                desc += Messages.get(this, "desc_equipped");
        }
        return desc;
    }

    @Override
    public String status() {
        return super.status();
    }

    @Override
    public int value() {
        int price = 300;
        if (level() > 0)
            price += 50*visiblyUpgraded();
        if (cursed && cursedKnown) {
            price /= 2;
        }
        if (price < 1) {
            price = 1;
        }
        return price;
    }

    @Override
    public void charge(Hero target, float amount) {
        if (cursed || target.buff(MagicImmune.class) != null) return;
        if (charge < chargeCap){
            partialCharge += amount;
            while (partialCharge >= 1f){
                charge++;
                partialCharge--;
            }
            if (charge >= chargeCap) {
                charge = chargeCap;
                partialCharge = 0;
            }
            updateQuickslot();
        }
    }

    @Override
    protected ArtifactBuff passiveBuff() {
        return new MultiArmBlows();
    }

    public class MultiArmBlows extends ArtifactBuff {

        //攻击时获取经验和充能
        public void onAttack() {
            if (target instanceof Hero) {
                gainExp(10);
                charge((Hero) target, 2f);
            }
        }

        //闪避时获取充能
        public void onEvade() {
            if (target instanceof Hero) {
                // 闪避时获取4%充能
                charge((Hero) target, 4f);
            }
        }

        // 提取公共方法：获取战狂倍率
        private float getMultiplier() {
            if (target instanceof Hero) {
                Hero hero = (Hero) target;
                Berserker berserker = hero.buff(Berserker.class);
                if (berserker != null) {
                    return berserker.getPassiveMultiplier();
                }
            }
            return 1f;
        }

        // 提取公共方法：检查是否应该应用加成（空手或有战狂 buff）
        private boolean shouldBonus() {
            if (!(target instanceof Hero)) {
                return false;
            }
            Hero hero = (Hero) target;
            return isUnarmed() || hero.buff(Berserker.class) != null;
        }

        public int damageBonus(int damage) {
            if (shouldBonus()) {
                int lvl = level();
                float multiplier = getMultiplier();
                // 伤害增加：当前等级到 3*当前等级，乘以战狂姿态倍率
                return damage + Math.round(Random.NormalIntRange(lvl, 2 + 3*lvl) * multiplier);
            }
            return damage;
        }

        // 减少攻击延迟（空手或战狂时）
        public float attackDelayBonus(float delay) {
            if (shouldBonus()) {
                // 攻击间隔减少：0.05 + 0.04 * 等级) 满级25%
                float reduction = 0.05f + 0.04f * level();
                float multiplier = getMultiplier();

                // 确保延迟不会低于原始值的 10%
                float delayMultiplier = Math.max(0.1f, 1f - reduction* multiplier);

                return delay * delayMultiplier;
            }
            return delay;
        }

        // 增加闪避（空手或战狂时）
        public int evasionBonus() {
            if (shouldBonus()) {
                float multiplier = getMultiplier();
                // 增加 2*当前等级的闪避，乘以战狂姿态倍率
                return Math.round(2 * level() * multiplier);
            }
            return 0;
        }

        //判断是否为空手状态 - 基于RingOfForce.fightingUnarmed方法
        private boolean isUnarmed() {
            if (target instanceof Hero) {
                Hero hero = (Hero) target;
                // 使用RingOfForce的fightingUnarmed方法判断徒手状态
                return RingOfForce.fightingUnarmed(hero);
            }
            return false;
        }

        //获取攻击距离加成
//        public int getAttackRangeBonus() {
//            if (target instanceof Hero) {
//                Hero hero = (Hero) target;
//                Berserker berserker = hero.buff(Berserker.class);
//                if (berserker != null) {
//                    return berserker.getAttackRangeBonus();
//                }
//            }
//            return 0;
//        }

        @Override
        public boolean attachTo(Char target) {
            if (super.attachTo(target)) {
                // 装备时立即应用被动效果
                if (target instanceof Hero) {
                    Hero hero = (Hero) target;
                    // 更新角色的属性
                    hero.updateHT(true);
                }
                return true;
            }
            return false;
        }

        @Override
        public void detach() {
            super.detach();
        }

        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }
    }

    public static class Berserker extends Buff {
        {
            type = buffType.POSITIVE;
            announced = true;
        }

        private int cooldown = 0;
        private static final int COOLDOWN_MAX = 25;

        public void setCooldown() {
            cooldown = -1;
        }

        @Override
        public int icon() { return BuffIndicator.MINIPOTION; }

        @Override
        public void tintIcon(Image icon) {
            switch(lvl) {
                case 2:
                    icon.hardlight(1f, 0.5f, 0f);
                    break;
                case 3:
                    icon.hardlight(1f, 0f, 0f);
                    break;
                case 1:
                default:
                    icon.hardlight(0f, 0.85f, 0.5f);
            }
        }

        @Override
        public float iconFadePercent() { return GameMath.gate(0, cooldown / (float)COOLDOWN_MAX, 1); }

        @Override
        public String desc() {
            switch(lvl) {
                case 2:
                    return Messages.get(this, "desc_lv2", cooldown);
                case 3:
                    return Messages.get(this, "desc_lv3", cooldown);
                case 1:
                default:
                    return Messages.get(this, "desc_lv1", cooldown);
            }
        }

        @Override
        public boolean act() {
            if (cooldown < COOLDOWN_MAX){
                cooldown++;
                spend( TICK );
            }else if (COOLDOWN_MAX == cooldown){
                detach();
            }
            return true;
        }

        @Override
        public void detach() {
            super.detach();
        }

        public int lvl = 1; // 战狂姿态等级，1-3

        public void setLevel(int level) {
            this.lvl = Math.max(1, Math.min(3, level));
        }

        public int getLvl() {
            return lvl;
        }

        // 获取被动效果倍率
        public float getPassiveMultiplier() {
            switch(lvl) {
                case 2:
                    return 2f;
                case 3:
                    return 4f;
                default:
                    return 1f;
            }
        }

        // 获取攻击距离加成
//        public int getAttackRangeBonus() {
//            if (lvl >= 2) {
//                return lvl - 1; // lv2+1, lv3+2
//            }
//            return 0;
//        }

        private static final String COOLDOWN = "cooldown";
        private static final String LEVEL = "level";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(COOLDOWN, cooldown);
            bundle.put(LEVEL, lvl);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (bundle.contains(COOLDOWN)) {
                cooldown = bundle.getInt(COOLDOWN);
                lvl = bundle.getInt(LEVEL);
            }
        }
    }

    private static class WndShivaWeapon extends Window {

        private static final int BTN_SIZE	= 32;
        private static final float GAP		= 2;
        private static final float BTN_GAP	= 12;
        private static final int WIDTH		= 116;

        private ItemButton btnWeapon3;
        private ItemButton btnWeapon4;

        WndShivaWeapon(final ShivaBangle bangle){

            IconTitle titlebar = new IconTitle();
            titlebar.icon( new ItemSprite(bangle) );
            titlebar.label( Messages.get(this, "title") );
            titlebar.setRect( 0, 0, WIDTH, 0 );
            add( titlebar );

            RenderedTextBlock message =
                    PixelScene.renderTextBlock(Messages.get(this, "desc"), 6);
            message.maxWidth( WIDTH );
            message.setPos(0, titlebar.bottom() + GAP);
            add( message );

            // 武器3按钮
            btnWeapon3 = new ItemButton(){
                @Override
                protected void onClick() {
                    Hero hero = Dungeon.hero;
                    if (hero.belongings.weapon3 != null){
                        item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
                        if (!hero.belongings.weapon3.doPickUp(hero)){
                            Dungeon.level.drop(hero.belongings.weapon3, hero.pos);
                        }
                        hero.belongings.weapon3 = null;

                        // 卸下weapon3的武器后，更新weapon4的占位符
                        if (hero.belongings.weapon4 != null) {
                            btnWeapon4.item(hero.belongings.weapon4);
                        } else {
                            btnWeapon4.item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
                        }
                    } else {
                        GameScene.selectItem(new WndBag.ItemSelector() {

                            @Override
                            public String textPrompt() {
                                return Messages.get(this, "weapon3_prompt");
                            }

                            @Override
                            public Class<?extends Bag> preferredBag(){
                                return Belongings.Backpack.class;
                            }

                            @Override
                            public boolean itemSelectable(Item item) {
                                return item instanceof KindOfWeapon;
                            }

                            @Override
                            public void onSelect(Item item) {
                                if (!(item instanceof KindOfWeapon)) {
                                    //do nothing, should only happen when window is cancelled
                                } else if (item.cursed || !item.cursedKnown) {
                                    GLog.w(Messages.get(ShivaBangle.class, "cant_cursed"));
                                    hide();
                                } else {
                                    KindOfWeapon weapon = (KindOfWeapon) item;
                                    boolean isTwoHanded = weapon instanceof MeleeWeapon && ((MeleeWeapon) weapon).isTwoHanded();

                                    // 如果是双手武器，需要卸下weapon4的武器
                                    if (isTwoHanded && hero.belongings.weapon4 != null) {
                                        if (!hero.belongings.weapon4.doPickUp(hero)){
                                            Dungeon.level.drop(hero.belongings.weapon4, hero.pos);
                                        }
                                        hero.belongings.weapon4 = null;
                                    }

                                    if (item.isEquipped(hero)){
                                        weapon.doUnequip(hero, false, false);
                                    } else {
                                        item.detach(hero.belongings.backpack);
                                    }
                                    hero.belongings.weapon3 = weapon;
                                    hero.belongings.weapon3.activate(hero);
                                    item(hero.belongings.weapon3);

                                    // 装备weapon3的武器后，更新weapon4的占位符
                                    if (isTwoHanded) {
                                        // 如果装备的是双手武器，将weapon4的占位符修改为X_NO
                                        btnWeapon4.item(new WndBag.Placeholder(ItemSpriteSheet.X_NO));
                                    } else if (hero.belongings.weapon4 != null) {
                                        btnWeapon4.item(hero.belongings.weapon4);
                                    } else {
                                        btnWeapon4.item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
                                    }
                                }
                            }
                        });
                    }
                }

                @Override
                protected boolean onLongClick() {
                    if (item() != null && item().name() != null){
                        GameScene.show(new WndInfoItem(item()));
                        return true;
                    }
                    return false;
                }
            };
            btnWeapon3.setRect( (WIDTH - BTN_GAP) / 2 - BTN_SIZE, message.top() + message.height() + GAP, BTN_SIZE, BTN_SIZE );
            if (Dungeon.hero.belongings.weapon3 != null) {
                btnWeapon3.item(Dungeon.hero.belongings.weapon3);
            } else {
                btnWeapon3.item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
            }
            add( btnWeapon3 );

            // 武器4按钮
            btnWeapon4 = new ItemButton(){
                @Override
                protected void onClick() {
                    Hero hero = Dungeon.hero;
                    // 检查weapon3是否装备了双手武器
                    boolean weapon3IsTwoHanded = hero.belongings.weapon3 instanceof MeleeWeapon &&
                            ((MeleeWeapon) hero.belongings.weapon3).isTwoHanded();

                    if (weapon3IsTwoHanded) {
                        // 如果weapon3装备了双手武器，显示警告消息
                        GLog.w(Messages.get(this, "cant_two_handed"));
                        return;
                    }

                    if (hero.belongings.weapon4 != null){
                        item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
                        if (!hero.belongings.weapon4.doPickUp(hero)){
                            Dungeon.level.drop(hero.belongings.weapon4, hero.pos);
                        }
                        hero.belongings.weapon4 = null;
                    } else {
                        GameScene.selectItem(new WndBag.ItemSelector() {

                            @Override
                            public String textPrompt() {
                                return Messages.get(this, "weapon4_prompt");
                            }

                            @Override
                            public Class<?extends Bag> preferredBag(){
                                return Belongings.Backpack.class;
                            }

                            @Override
                            public boolean itemSelectable(Item item) {
                                // weapon4不能装备双手武器
                                if (item instanceof MeleeWeapon && ((MeleeWeapon) item).isTwoHanded()) {
                                    return false;
                                }
                                return item instanceof KindOfWeapon;
                            }

                            @Override
                            public void onSelect(Item item) {
                                if (!(item instanceof KindOfWeapon)) {
                                    //do nothing, should only happen when window is cancelled
                                } else if (item.cursed || !item.cursedKnown) {
                                    GLog.w(Messages.get(ShivaBangle.class, "cant_cursed"));
                                    hide();
                                } else {
                                    KindOfWeapon weapon = (KindOfWeapon) item;
                                    // 再次检查是否为双手武器
                                    if (weapon instanceof MeleeWeapon && ((MeleeWeapon) weapon).isTwoHanded()) {
                                        GLog.w(Messages.get(this, "cant_two_handed"));
                                        hide();
                                        return;
                                    }
                                    if (item.isEquipped(hero)){
                                        weapon.doUnequip(hero, false, false);
                                    } else {
                                        item.detach(hero.belongings.backpack);
                                    }
                                    hero.belongings.weapon4 = weapon;
                                    hero.belongings.weapon4.activate(hero);
                                    item(hero.belongings.weapon4);
                                }
                            }
                        });
                    }
                }

                @Override
                protected boolean onLongClick() {
                    if (item() != null && item().name() != null){
                        GameScene.show(new WndInfoItem(item()));
                        return true;
                    }
                    return false;
                }
            };
            btnWeapon4.setRect( btnWeapon3.right() + BTN_GAP, btnWeapon3.top(), BTN_SIZE, BTN_SIZE );

            // 检查weapon3是否装备了双手武器
            boolean weapon3IsTwoHanded = Dungeon.hero.belongings.weapon3 instanceof MeleeWeapon &&
                    ((MeleeWeapon) Dungeon.hero.belongings.weapon3).isTwoHanded();

            if (Dungeon.hero.belongings.weapon4 != null) {
                btnWeapon4.item(Dungeon.hero.belongings.weapon4);
            } else if (weapon3IsTwoHanded) {
                // 如果weapon3装备了双手武器，weapon4的占位符修改为X_NO
                btnWeapon4.item(new WndBag.Placeholder(ItemSpriteSheet.X_NO));
            } else {
                btnWeapon4.item(new WndBag.Placeholder(ItemSpriteSheet.WEAPON_HOLDER));
            }
            add( btnWeapon4 );

            resize(WIDTH, (int)(btnWeapon4.bottom() + GAP));
        }

    }

}
