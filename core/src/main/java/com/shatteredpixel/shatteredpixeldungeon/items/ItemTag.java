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

import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ItemTag implements Bundlable {
	
	// 标签类型 - 决定标签是否对玩家可见
	public enum Type {
		VISIBLE,    // 对玩家显示
		HIDDEN      // 对玩家隐藏
	}
	
	private String key;           // 标签的唯一标识符
	private Type type = Type.VISIBLE;
	private int level = 0;        // 标签的等级/强度
	private Bundle data;          // 附加数据
	
	// 显示顺序(数字越小优先级越高)
	private int order = 0;
	
	public ItemTag() {
		this.key = null;
	}
	
	public ItemTag(String key) {
		this.key = key;
	}
	
	public ItemTag(String key, Type type) {
		this.key = key;
		this.type = type;
	}
	
	public ItemTag(String key, Type type, int level) {
		this.key = key;
		this.type = type;
		this.level = level;
	}
	
	public String key() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public Type type() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public boolean isVisible() {
		return type == Type.VISIBLE;
	}
	
	public int level() {
		return level;
	}
	
	public void level(int level) {
		this.level = level;
	}
	
	public int order() {
		return order;
	}
	
	public void order(int order) {
		this.order = order;
	}
	
	// 通过该标签存储额外数据
	public Bundle data() {
		if (data == null) {
			data = new Bundle();
		}
		return data;
	}
	
	// 为该标签设置额外数据
	public void setData(Bundle data) {
		this.data = data;
	}
	
	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put("key", key);
		bundle.put("type", type.ordinal());
		bundle.put("level", level);
		bundle.put("order", order);
		if (data != null) {
			bundle.put("data", data);
		}
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		key = bundle.getString("key");
		// 兼容性修复: 将旧的驼峰命名键转换为蛇形命名
		key = convertLegacyKey(key);
		type = Type.values()[bundle.getInt("type")];
		level = bundle.getInt("level");
		order = bundle.getInt("order");
		if (bundle.contains("data")) {
			data = bundle.getBundle("data");
		}
	}
	
	/**
	 * 将旧版标签键转换为当前格式
	 * @param key 原始键
	 * @return 转换后的键
	 */
	private static String convertLegacyKey(String key) {
		if (key == null) return null;
		// 根据需要在此添加更多转换
		switch (key) {
			case "swordSystem": return "sword_system";
			// 在此添加未来的兼容性映射
			default: return key;
		}
	}
	
	// 用于按优先级排序标签的比较器
	public static final Comparator<ItemTag> orderByPriority = new Comparator<ItemTag>() {
		@Override
		public int compare(ItemTag t1, ItemTag t2) {
			return Integer.compare(t1.order(), t2.order());
		}
	};
	
	// ==================== 预定义标签 ====================
	// 格式: 名称 (键值, 显示类型, 默认等级, 优先级)
	// 优先级规则: 负数=高优先级, 正数=低优先级
	
	// --- 武器类型标签 (优先级: 1-10) ---
	public static final ItemTag SWORD_SYSTEM     = create("sword_system",     Type.VISIBLE, 0, 1);
	public static final ItemTag DAGGER_SYSTEM    = create("dagger_system",    Type.VISIBLE, 0, 2);
	public static final ItemTag MACE_SYSTEM      = create("mace_system",      Type.VISIBLE, 0, 3);
	public static final ItemTag SPEAR_SYSTEM     = create("spear_system",     Type.VISIBLE, 0, 4);
	public static final ItemTag AXE_SYSTEM       = create("axe_system",       Type.VISIBLE, 0, 5);
	public static final ItemTag BOW_SYSTEM       = create("bow_system",       Type.VISIBLE, 0, 6);
	public static final ItemTag SHIELD_SYSTEM    = create("shield_system",    Type.VISIBLE, 0, 7);
	public static final ItemTag RING_SYSTEM      = create("ring_system",      Type.VISIBLE, 0, 8);
	public static final ItemTag BLUNT_SYSTEM     = create("blunt_system",     Type.VISIBLE, 0, 9);
	public static final ItemTag UNARMED_SYSTEM   = create("unarmed_system",   Type.VISIBLE, 0, 10);
	
	// --- 物品大类标签 (优先级: 11-25) ---
	public static final ItemTag WEAPON_SYSTEM    = create("weapon_system",    Type.VISIBLE, 0, 11);
	public static final ItemTag WANDS_SYSTEM     = create("wands_system",     Type.VISIBLE, 0, 12);
	public static final ItemTag ARMOR_SYSTEM     = create("armor_system",     Type.VISIBLE, 0, 13);
	public static final ItemTag ARTIFACT_SYSTEM  = create("artifact_system",  Type.VISIBLE, 0, 14);
	public static final ItemTag MISSILE_WEAPON   = create("missile_weapon",   Type.VISIBLE, 0, 15);
	public static final ItemTag SPECIAL_SYSTEM   = create("special_system",   Type.VISIBLE, 0, 16);
	
	// --- 消耗品类型标签 (优先级: 26-35) ---
	public static final ItemTag FOOD             = create("food",             Type.VISIBLE, 0, 26);
	public static final ItemTag BOMBS            = create("bombs",            Type.VISIBLE, 0, 27);
	public static final ItemTag POTIONS          = create("potions",          Type.VISIBLE, 0, 28);
	public static final ItemTag SCROLLS          = create("scrolls",          Type.VISIBLE, 0, 29);
	public static final ItemTag STONES           = create("stones",           Type.VISIBLE, 0, 30);
	public static final ItemTag CONSUMABLE       = create("consumable",       Type.VISIBLE, 0, 31);
	
	// --- 材质标签 (优先级: 40-50) ---
	public static final ItemTag IRON           = create("iron",           Type.VISIBLE, 0, 40);
	public static final ItemTag STEEL          = create("steel",          Type.VISIBLE, 0, 41);
	public static final ItemTag CRYSTAL        = create("crystal",        Type.VISIBLE, 0, 42);
	public static final ItemTag WOODEN         = create("wooden",         Type.VISIBLE, 0, 43);
	
	// --- 状态标签 (优先级: -10 ~ -1, 负数表示高优先级) ---
	public static final ItemTag CURSED         = create("cursed",         Type.VISIBLE, 0, -10);
	public static final ItemTag BLESSED        = create("blessed",        Type.VISIBLE, 0, -9);
	public static final ItemTag BROKEN         = create("broken",         Type.VISIBLE, 0, -8);
	public static final ItemTag ENCHANTED      = create("enchanted",      Type.VISIBLE, 0, -6);
	public static final ItemTag GLOWING        = create("glowing",        Type.VISIBLE, 0, -5);
	
	// --- 隐藏标签 (用于内部逻辑, 不显示给玩家) ---
	public static final ItemTag FRAGILE        = create("fragile",        Type.HIDDEN, 0, -1);
	public static final ItemTag MELEE          = create("melee",          Type.HIDDEN, 0, -2);
	public static final ItemTag RANGED         = create("ranged",         Type.HIDDEN, 0, -3);
	public static final ItemTag MAGIC          = create("magic",          Type.HIDDEN, 0, -4);
	public static final ItemTag ONEHAND        = create("onehand",        Type.HIDDEN, 0, -5);
	public static final ItemTag TWOHANDED      = create("twohanded",      Type.HIDDEN, 0, -6);
	


	// ==================== 工厂方法 ====================
	
	/**
	 * 创建一个预定义的标签
	 */
	public static ItemTag create(String key, Type type, int level, int order) {
		ItemTag tag = new ItemTag(key, type, level);
		tag.order(order);
		return tag;
	}
	
	/**
	 * 复制一个标签实例(用于需要不同等级时)
	 */
	public ItemTag copy() {
		// 浅拷贝：共享 data（性能好，适合只读场景）
		ItemTag copy = new ItemTag(this.key, this.type, this.level);
		copy.order(this.order);
		if (this.data != null) {
			copy.setData(this.data);  // 共享引用
		}
		return copy;
	}
	
	/**
	 * 创建带自定义等级的副本
	 */
	public ItemTag copyWithLevel(int newLevel) {
		ItemTag copy = copy();
		copy.level(newLevel);
		return copy;
	}
}
