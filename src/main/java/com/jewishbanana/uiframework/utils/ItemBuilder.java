package com.jewishbanana.uiframework.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
import com.jewishbanana.uiframework.items.Ability.Action;
import com.jewishbanana.uiframework.items.GenericItem;
import com.jewishbanana.uiframework.items.ItemType;

public class ItemBuilder {
	
	private ItemStack item;
	private ItemMeta meta;
	private boolean enchanted;
	private Enchantment enchantment;
	
	/**
	 * Constructs a new ItemBuilder with the given ItemStack as the item
	 * 
	 * @param item The item to use
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(ItemStack item) {
		ItemBuilder builder = new ItemBuilder();
		builder.item = item;
		builder.meta = builder.item.getItemMeta();
		return builder;
	}
	/**
	 * Constructs a new ItemBuilder with the given Material as the item
	 * 
	 * @param material The material of the item to create
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(Material material) {
		ItemBuilder builder = new ItemBuilder();
		builder.item = new ItemStack(material);
		builder.meta = builder.item.getItemMeta();
		return builder;
	}
	/**
	 * Sets the custom display name of the item being used
	 * 
	 * @param name The name to use
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder registerName(String name) {
		if (name != null)
			meta.setDisplayName(name);
		return this;
	}
	/**
	 * Sets the lore of the item to a default format supplied by UIFramework.
	 * <p>
	 * This method should be called in your chain with the classes global ItemType
	 * <pre>
	 * return ItemBuilder.create(null).assembleLore(id).build();
	 * </pre>
	 * 
	 * @param id The ItemType of the item's lore to build
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder assembleLore(ItemType id) {
		assembleLore(item, meta, id, null);
		return this;
	}
	/**
	 * Sets the lore of the item to a defualt format supplied by UIFramework.
	 * <p>
	 * <STRONG>This method is a special variation used internally and is not recommended! Use alternative method instead.</STRONG>
	 * 
	 * @param tempItem The item to pass through
	 * @param tempMeta The meta to add
	 * @param id The ItemType of the item's lore to build
	 * @param base The base class to modify
	 * @return This ItemBuilder instance
	 * 
	 * @see ItemBuilder#assembleLore(ItemType)
	 */
	public ItemStack assembleLore(ItemStack tempItem, ItemMeta tempMeta, ItemType id, GenericItem base) {
		List<String> lore = new ArrayList<>();
		for (String s : id.getLore())
			lore.add(s);
		if (base != null && !base.getFields().isEmpty()) {
			lore.add(" ");
			base.getFields().values().forEach(k -> lore.add(UIFUtils.convertString(k.getLore())));
		}
		if (!id.abilityMap.isEmpty()) {
			lore.add(" ");
			id.abilityMap.forEach((k, v) -> {
				String actions = "";
				boolean passive = false;
				for (Action action : v)
					if (action.isPassive()) {
						if (!passive) {
							actions += action.getLangName()+' ';
							passive = true;
						}
					} else
						actions += action.getLangName()+' ';
				lore.add(UIFUtils.convertString(actions+k.getType().getDisplayName()+' '+k.getType().getDescription()));
			});
		}
		if (tempMeta.getEnchants().size() > 0 && (enchanted || !tempMeta.hasItemFlag(ItemFlag.HIDE_ENCHANTS))) {
			tempMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			if (!(enchanted && tempMeta.getEnchants().size() == 1 && tempMeta.hasEnchant(enchantment))) {
				lore.add(" ");
				tempMeta.getEnchants().forEach((k, v) -> {
					if (!enchanted || k != enchantment)
						lore.add(UIFUtils.convertString("&7"+UIFDataUtils.capitalizeFormat(k.getKey().getKey())+' '+UIFUtils.getNumerical(v)));
				});
			}
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
	/**
	 * Directly set the lore of the item.
	 * 
	 * @param lore The lore to use
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder setLoreList(List<String> lore) {
		meta.setLore(lore);
		return this;
	}
	/**
	 * Sets the item's attributes.
	 * 
	 * @param attributes The attribute map to use
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder setAttributes(Multimap<Attribute, AttributeModifier> attributes) {
		meta.setAttributeModifiers(attributes);
		return this;
	}
	/**
	 * Adds the ItemFlags to the item.
	 * 
	 * @param flags The ItemFlags to add
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder addItemFlags(ItemFlag... flags) {
		meta.addItemFlags(flags);
		return this;
	}
	/**
	 * Attaches the custom item types key to the item allowing it to persist through server restarts/reloads.
	 * <p>
	 * <STRONG>This is handled automatically and should not be called in your builder!</STRONG>
	 * 
	 * @param ID The unique ID of the ItemType
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder attachID(int ID) {
		meta.getPersistentDataContainer().set(GenericItem.generalKey, PersistentDataType.INTEGER, ID);
		return this;
	}
	/**
	 * Sets the custom model data of your item
	 * 
	 * @param modelData The model data to use
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder setCustomModelData(int modelData) {
		meta.setCustomModelData(modelData);
		return this;
	}
	/**
	 * Will add the given enchantment to your item but not display it in the item's lore. Used to obtain a secret enchantment effect. The enchantment type should generally be something that does not affect the item's gameplay.
	 * 
	 * @param enchantment The enchantment to apply and hide for the item
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder setHiddenEnchanted(Enchantment enchantment) {
		this.enchanted = true;
		this.enchantment = enchantment;
		meta.addEnchant(enchantment, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		return this;
	}
	/**
	 * Modify directly the meta of the builder.
	 * 
	 * @param access The consumer that the meta will accept
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder accessMeta(Consumer<ItemMeta> access) {
		access.accept(meta);
		return this;
	}
	/**
	 * Sets the meta to the ItemStack fully preparing it for use
	 * <p>
	 * <STRONG>This should always be called at the very end of your builder chain!</STRONG>
	 * 
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder build() {
		item.setItemMeta(meta);
		return this;
	}
	/**
	 * Get a copy of the custom ItemStack for general use.
	 * 
	 * @return A clone of the builders item
	 */
	public ItemStack getItem() {
		return item.clone();
	}
}
