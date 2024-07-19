package com.github.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.Ability.Action;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.github.jewishbanana.uiframework.utils.VersionUtils;
import com.google.common.collect.Multimap;

public class ItemBuilder {
	
	private ItemType type;
	private ItemStack item;
	private ItemMeta meta;
	private boolean enchanted;
	private Enchantment enchantment;
	
	/**
	 * Constructs a new ItemBuilder with the given ItemStack as the item and the provided ItemType
	 * 
	 * @param type The item type to use
	 * @param item The item to use
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(ItemType type, ItemStack item) {
		ItemBuilder builder = new ItemBuilder();
		builder.type = type;
		builder.item = item;
		builder.meta = builder.item.getItemMeta();
		if (type != null)
			builder.registerName(type.getDisplayName()).build();
		return builder;
	}
	/**
	 * Constructs a new ItemBuilder with the given Material as the item and the provided ItemType
	 * 
	 * @param type The item type to use
	 * @param material The material of the item to create
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(ItemType type, Material material) {
		ItemBuilder builder = new ItemBuilder();
		builder.type = type;
		builder.item = new ItemStack(material);
		builder.meta = builder.item.getItemMeta();
		if (type != null)
			builder.registerName(type.getDisplayName()).build();
		return builder;
	}
	/**
	 * Constructs a new ItemBuilder with the given ItemStack as the item
	 * 
	 * @param item The item to use
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(ItemStack item) {
		return create(null, item);
	}
	/**
	 * Constructs a new ItemBuilder with the given Material as the item
	 * 
	 * @param material The material of the item to create
	 * @return The ItemBuilder instance created
	 */
	public static ItemBuilder create(Material material) {
		return create(null, material);
	}
	/**
	 * Sets the custom display name of the item being used, will also set this name to the ItemBuilders provided ItemType as the items display name if applicable.
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
	 * return ItemBuilder.create(...).assembleLore(this).build();
	 * </pre>
	 * 
	 * @param base The GenericItem base of an item (Will use the bases ItemType for the lore)
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder assembleLore(GenericItem base) {
		assembleLore(base.item, base.item.getItemMeta(), base.getType(), base);
		return this;
	}
	/**
	 * Sets the lore of the item to a default format supplied by UIFramework.
	 * <p>
	 * This method should be called in your item classes create chain
	 * <pre>
	 * return ItemBuilder.create(...).assembleLore().build();
	 * </pre>
	 * 
	 * @return This ItemBuilder instance
	 */
	public ItemBuilder assembleLore() {
		try {
			assembleLore(item, meta, type, null);
		} catch (NullPointerException e) {
			if (UIFramework.debugMessages) {
				UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eWARNING A plugin attempted to assemble an items lore with no ItemType assigned to the builder! Please report this to the proper plugin author of the following error as this is NOT a UIFramework bug! If you wish to disable these debug messages you can do so within the UIFramework config file, keep in mind that something will not work properly with the associated plugin of this error."));
				e.printStackTrace();
			}
		}
		return this;
	}
	/**
	 * Sets the lore of the item to a default format supplied by UIFramework.
	 * <p>
	 * <STRONG>This method is a special variation used internally and is not recommended! Use alternative method instead.</STRONG>
	 * 
	 * @param tempItem The item to pass through
	 * @param tempMeta The meta to add
	 * @param id The ItemType of the item's lore to build
	 * @param base The base class to modify
	 * @return This ItemBuilder instance
	 * 
	 * @see ItemBuilder#assembleLore()
	 */
	public ItemStack assembleLore(ItemStack tempItem, ItemMeta tempMeta, ItemType id, GenericItem base) {
		if (!id.doesUseLoreFormat())
			return tempItem;
		List<String> lore = new ArrayList<>();
		boolean firstSpace = false;
		if (!id.getLore().isEmpty()) {
			firstSpace = true;
			for (String s : id.getLore())
				lore.add(s);
		}
		if (base != null && !base.getFields().isEmpty() && base.getFields().values().stream().anyMatch(e -> e.getLore() != null)) {
			if (firstSpace)
				lore.add(" ");
			else
				firstSpace = true;
			base.getFields().values().stream().filter(e -> e.getLore() != null).forEach(e -> lore.add(e.getLore()));
		}
		if (!id.abilityMap.isEmpty() || (base != null && !base.uniqueAbilities.isEmpty())) {
			if (firstSpace)
				lore.add(" ");
			else
				firstSpace = true;
			Map<Ability, String> abilities = new LinkedHashMap<>();
			if (base != null && !base.uniqueAbilities.isEmpty())
				base.uniqueAbilities.forEach((k, v) -> {
					String actions = "";
					boolean passive = false;
					for (Action action : v)
						if (action == Ability.Action.UNBOUND)
							continue;
						else if (action.isPassive()) {
							if (!passive) {
								actions += action.getLangName()+' ';
								passive = true;
							}
						} else
							actions += action.getLangName()+' ';
					if (abilities.containsKey(k))
						abilities.replace(k, abilities.get(k)+actions);
					else
						abilities.put(k, actions);
				});
			if (!id.abilityMap.isEmpty())
				id.abilityMap.forEach((k, v) -> {
					String actions = "";
					boolean passive = false;
					for (Action action : v)
						if (action == Ability.Action.UNBOUND)
							continue;
						else if (action.isPassive()) {
							if (!passive) {
								actions += action.getLangName()+' ';
								passive = true;
							}
						} else
							actions += action.getLangName()+' ';
					if (abilities.containsKey(k))
						abilities.replace(k, abilities.get(k)+actions);
					else
						abilities.put(k, actions);
				});
			for (Entry<Ability, String> entry : abilities.entrySet())
				lore.add(UIFUtils.convertString(entry.getValue()+entry.getKey().getDisplayName()+' '+entry.getKey().getDescription()));
		}
		boolean spacing = false;
		if (!tempMeta.getEnchants().isEmpty()) {
			tempMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			if (base == null ? !(enchanted && tempMeta.getEnchants().size() == 1) : (tempMeta.getEnchants().size() != (enchanted ? (base.getHiddenEnchant() != null ? 2 : 1) : base.getHiddenEnchant() != null ? 1 : 0))) {
				if (firstSpace)
					lore.add(" ");
				else
					firstSpace = true;
				spacing = true;
				tempMeta.getEnchants().forEach((k, v) -> {
					if (!(enchanted && k == enchantment) && !(base != null && k == base.getHiddenEnchant()))
						lore.add(UIFUtils.convertString("&7"+VersionUtils.getFormattedEnchantName(k)+(k.getMaxLevel() == 1 ? "" : ' '+UIFUtils.getNumerical(v))));
				});
			}
		}
		if (base != null && !base.getEnchants().isEmpty()) {
			if (!spacing && firstSpace)
				lore.add(" ");
			else
				firstSpace = true;
			base.getEnchants().forEach(e -> {
				int level = e.getEnchantLevel(tempItem);
				lore.add(UIFUtils.convertString(("&7"+e.getDisplayName().replace("%l%", ""+level).replace("%nl%", UIFUtils.getNumerical(level)))));
			});
		}
		boolean attributeSpacing = false;
		if (id.getDamage() != 0.0 || id.getAttackSpeed() != 0.0) {
			if (firstSpace) {
				lore.add(" ");
				attributeSpacing = true;
			} else
				firstSpace = true;
			tempMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.main_hand_lore")));
			if (id.getDamage() == 0.0)
				id.setDamage(1.0);
			double damage = id.getDamage();
			if (tempMeta.hasEnchant(VersionUtils.getSharpness()))
				damage += 0.5 * (tempMeta.getEnchantLevel(VersionUtils.getSharpness()) - 1) + 1.0;
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.attack_damage").replaceAll("%value%", UIFDataUtils.getDecimalFormatted(damage))));
			if (id.getAttackSpeed() == 0.0)
				id.setAttackSpeed(1.0);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.attack_speed").replaceAll("%value%", UIFDataUtils.getDecimalFormatted(id.getAttackSpeed()))));
			if (tempMeta.hasAttributeModifiers() && tempMeta.getAttributeModifiers().containsKey(Attribute.GENERIC_ATTACK_SPEED))
				tempMeta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
			tempMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(UUID.fromString("545ff361-b6e6-4531-9c4c-398ef5589a8a"), "generic.attackSpeed", id.getAttackSpeed()-4.01, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		}
		if (id.getProjectileDamage() != 0.0) {
			if (firstSpace && !attributeSpacing) {
				lore.add(" ");
				attributeSpacing = true;
			} else
				firstSpace = true;
			tempMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.thrown_projectile")));
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.projectile_damage").replaceAll("%value%", UIFDataUtils.getDecimalFormatted(id.getProjectileDamage()))));
		}
		if (id.getProjectileDamageMultiplier() != 1.0) {
			if (firstSpace && !attributeSpacing) {
				lore.add(" ");
				attributeSpacing = true;
			} else
				firstSpace = true;
			tempMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.shot_projectiles")));
			lore.add(UIFUtils.convertString(UIFramework.getLangString("attributes.shot_multiplier").replaceAll("%value%", UIFDataUtils.getDecimalFormatted(id.getProjectileDamageMultiplier()))));
		}
		if (!tempMeta.hasDisplayName() && id.getDisplayName() != null)
			tempMeta.setDisplayName(id.getDisplayName());
		// Debug Item ID's
//		if (base != null) lore.addAll(Arrays.asList(" ", UIFUtils.convertString("&8ID: &a["+base.getUniqueId()+"]")));
		tempMeta.setLore(UIFUtils.chopLore(lore));
		tempItem.setItemMeta(tempMeta);
		return tempItem;
	}
	/**
	 * Directly set the lore of the item. If you are using the {@link #assembleLore()} methods then do not use this as it will just overwrite the default UIFramework lore layout.
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
	protected ItemBuilder attachID(String id) {
		meta.getPersistentDataContainer().set(GenericItem.generalKey, PersistentDataType.STRING, id);
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
