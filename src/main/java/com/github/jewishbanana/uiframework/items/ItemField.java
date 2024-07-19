package com.github.jewishbanana.uiframework.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemField<T> {
	
	private String lore;
	private T setting;
	private PersistentDataType<T, T> dataType;
	protected NamespacedKey key;
	private GenericItem base;
	
	/**
	 * Creates a new ItemField that can be used to store data on the item itself.
	 * 
	 * @param item The item to attach the field to
	 * @param key The key for the stored data
	 * @param defaultValue The default value to store if the field did not exist on the item yet
	 */
	protected ItemField(GenericItem base, NamespacedKey key, PersistentDataType<T, T> dataType, T defaultValue) {
		this.base = base;
		this.dataType = dataType;
		if (!base.item.hasItemMeta()) {
			this.setting = defaultValue;
			return;
		}
		this.key = key;
		ItemMeta meta = base.item.getItemMeta();
		T currentValue = meta.getPersistentDataContainer().get(key, dataType);
		if (currentValue != null) {
			this.setting = currentValue;
			return;
		}
		meta.getPersistentDataContainer().set(key, dataType, defaultValue);
		base.item.setItemMeta(meta);
		this.setting = defaultValue;
	}
	/**
	 * Gets the set lore for this field.
	 * 
	 * @return The lore for this field
	 */
	public String getLore() {
		return lore;
	}
	/**
	 * Set the lore for this field. ItemField lore is applied by default under the items name in the lore list.
	 * 
	 * @param lore The lore for this field
	 */
	public void setLore(String lore) {
		this.lore = lore;
	}
	/**
	 * Gets the set value for this field on the item.
	 * 
	 * @return The stored value
	 */
	public T getSetting() {
		return setting;
	}
	/**
	 * Sets the stored value to the specified value for this field.
	 * 
	 * @param setting The new value
	 * @param item The item to store to
	 */
	public void setSetting(T setting, ItemStack item) {
		this.setting = setting;
		if (key != null) {
			ItemMeta meta = item.getItemMeta();
			meta.getPersistentDataContainer().set(key, dataType, setting);
			item.setItemMeta(meta);
			base.item = item;
		}
	}
}
