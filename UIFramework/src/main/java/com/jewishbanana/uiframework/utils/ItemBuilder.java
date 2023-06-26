package com.jewishbanana.uiframework.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.common.collect.Multimap;
import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability;
import com.jewishbanana.uiframework.items.GenericItem;
import com.jewishbanana.uiframework.items.ItemType;

public class ItemBuilder {
	
	private ItemStack item;
	private ItemMeta meta;
	
	public static ItemBuilder create(ItemStack item) {
		ItemBuilder builder = new ItemBuilder();
		builder.item = item;
		builder.meta = builder.item.getItemMeta();
		return builder;
	}
	public static ItemBuilder create(Material material) {
		ItemBuilder builder = new ItemBuilder();
		builder.item = new ItemStack(material);
		builder.meta = builder.item.getItemMeta();
		return builder;
	}
	public ItemBuilder registerName(String name) {
		if (name != null)
			meta.setDisplayName(name);
		return this;
	}
	public ItemBuilder assembleLore(ItemType id) {
		assembleLore(item, meta, id);
		return this;
	}
	public ItemStack assembleLore(ItemStack tempItem, ItemMeta tempMeta, ItemType id) {
		List<String> lore = new ArrayList<>();
		for (String s : id.getLore())
			lore.add(s);
		if (!id.abilityMap.isEmpty()) {
			lore.add(" ");
			id.abilityMap.forEach((k, v) -> {
				for (Ability ability : v)
					lore.add(UIFUtils.convertString(k.getLangName()+' '+ability.getType().getName()+' '+ability.getType().getDescription()));
			});
		}
		if (tempMeta.getEnchants().size() > 0) {
			lore.add(" ");
			tempMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			tempMeta.getEnchants().forEach((k, v) -> {
				lore.add(UIFUtils.convertString("&7"+UIFDataUtils.capitalizeFormat(k.getKey().getKey())+' '+UIFUtils.getNumerical(v)));
			});
		}
		if (id.getDamage() > 0.0 || id.getAttackSpeed() > 0.0) {
			lore.add(" ");
			tempMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.main_hand_lore")));
			if (id.getDamage() == 0.0)
				id.setDamage(1.0);
			double damage = id.getDamage();
			if (tempMeta.hasEnchant(Enchantment.DAMAGE_ALL))
				damage += 0.5 * (tempMeta.getEnchantLevel(Enchantment.DAMAGE_ALL) - 1) + 1.0;
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.attack_damage").replaceAll("%damage%", UIFDataUtils.getDecimalFormatted(damage))));
			if (id.getAttackSpeed() == 0.0)
				id.setAttackSpeed(1.0);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.attack_speed").replaceAll("%attackSpeed%", UIFDataUtils.getDecimalFormatted(id.getAttackSpeed()))));
			if (tempMeta.hasAttributeModifiers() && tempMeta.getAttributeModifiers().containsKey(Attribute.GENERIC_ATTACK_SPEED))
				tempMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
			tempMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.fromString("545ff361-b6e6-4531-9c4c-398ef5589a8a"), "generic.attackSpeed", id.getAttackSpeed()-4.01, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		}
		tempMeta.setLore(UIFUtils.chopLore(lore));
		tempItem.setItemMeta(tempMeta);
		return tempItem;
	}
	public ItemBuilder setLoreList(List<String> lore) {
		meta.setLore(lore);
		return this;
	}
	public ItemBuilder setAttributes(Multimap<Attribute, AttributeModifier> attributes) {
		meta.setAttributeModifiers(attributes);
		return this;
	}
	public ItemBuilder addItemFlags(ItemFlag... flags) {
		meta.addItemFlags(flags);
		return this;
	}
	public ItemBuilder attachID(int ID) {
		meta.getPersistentDataContainer().set(GenericItem.generalKey, PersistentDataType.INTEGER, ID);
		return this;
	}
	public ItemBuilder setCustomModelData(int modelData) {
		meta.setCustomModelData(modelData);
		return this;
	}
	public ItemBuilder build() {
		item.setItemMeta(meta);
		return this;
	}
	public ItemStack getItem() {
		return item;
	}
}
