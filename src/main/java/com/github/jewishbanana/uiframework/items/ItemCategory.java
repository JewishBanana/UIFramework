package com.github.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.List;

import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class ItemCategory {
	
	private static List<ItemCategory> placements = new ArrayList<>();
	
	public enum DefaultCategory {
		WEAPONS("&6Weapons"),
		ARMOR("&6Armor"),
		TOOLS("&6Tools"),
		EXTRA_EQUIPMENT("&6Extra Equipment"),
		CONSUMABLES("&6Consumables"),
		ENCHANTED_BOOKS("&6Enchanted Books"),
		CRAFTING_INGREDIENTS("&6Crafting Ingredients"),
		MISCELLANEOUS("&6Miscellaneous");
		
		private DefaultCategory(String displayName) {
			new ItemCategory(this, UIFUtils.convertString(displayName));
		}
		public ItemCategory getItemCategory() {
			return getCategory(this);
		}
		public int getCategoryValue() {
			return getCategory(this).getCategoryValue();
		}
	}
	
	private String identifier;
	private String displayName;
	private int place;
	
	public ItemCategory(String identifier, String displayName) {
		this.identifier = identifier;
		this.displayName = displayName;
	}
	public ItemCategory(String identifier) {
		this(identifier, identifier);
	}
	private ItemCategory(DefaultCategory category, String displayName) {
		this(category.toString(), displayName);
		placements.add(this);
		this.place = placements.size()-1;
	}
	/**
	 * Gets the priority value of this category. The lower the number the higher the priority, higher priorities are displayed first.
	 * 
	 * @return This categories priority value
	 */
	public int getCategoryValue() {
		return place;
	}
	/**
	 * Gets the display name of this category. Display names are only used in the recipes menu to show what categories the item is a part of.
	 * 
	 * @return This categories display name
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * Adds a new item category to the server. This method will add your item category to the end of the list meaning it will have last priority.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 */
	public static void addItemCategory(ItemCategory newCategory) {
		preconditions(newCategory);
		newCategory.place = placements.size();
		placements.add(newCategory);
	}
	/**
	 * Adds a new item category to the server. This method adds your category as priority over the other given category meaning it will be displayed first before the given category.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 * @param before The item category to place your categories priority before
	 * @return If the category was successfully added before the supplied category or not
	 */
	public static boolean addItemCategoryBefore(ItemCategory newCategory, DefaultCategory before) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).identifier.equals(before.toString())) {
				placements.add(i, newCategory);
				refreshList();
				return true;
			}
		return false;
	}
	/**
	 * Adds a new item category to the server. This method adds your category as priority over the other given category meaning it will be displayed first before the given category.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 * @param before The item category to place your categories priority before
	 * @return If the category was successfully added before the supplied category or not
	 */
	public static boolean addItemCategoryBefore(ItemCategory newCategory, ItemCategory before) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).identifier.equals(before.identifier)) {
				placements.add(i, newCategory);
				refreshList();
				return true;
			}
		return false;
	}
	/**
	 * Adds a new item category to the server. This method adds your category as inferior under the other given category meaning it will be displayed directly after the given category.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 * @param before The item category to place your category after
	 * @return If the category was successfully added after the supplied category or not
	 */
	public static boolean addItemCategoryAfter(ItemCategory newCategory, DefaultCategory after) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).identifier.equals(after.toString())) {
				placements.add(i+1, newCategory);
				refreshList();
				return true;
			}
		return false;
	}
	/**
	 * Adds a new item category to the server. This method adds your category as inferior under the other given category meaning it will be displayed directly after the given category.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 * @param before The item category to place your category after
	 * @return If the category was successfully added after the supplied category or not
	 */
	public static boolean addItemCategoryAfter(ItemCategory newCategory, ItemCategory after) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).identifier.equals(after.identifier)) {
				placements.add(i+1, newCategory);
				refreshList();
				return true;
			}
		return false;
	}
	/**
	 * Gets the default item category.
	 * 
	 * @param category The default category to get
	 * @return The ItemCategory of the category or null if non-existent
	 */
	public static ItemCategory getCategory(DefaultCategory category) {
		for (ItemCategory temp : placements)
			if (temp.identifier.equals(category.toString()))
				return temp;
		return null;
	}
	/**
	 * Gets the item category.
	 * 
	 * @param category The category to get
	 * @return The ItemCategory or null if non-existent
	 */
	public static ItemCategory getCategory(String identifier) {
		for (ItemCategory temp : placements)
			if (temp.identifier.equals(identifier))
				return temp;
		return null;
	}
	private static void preconditions(ItemCategory category) {
		for (ItemCategory temp : placements)
			if (temp.identifier.equals(category.identifier))
				throw new IllegalArgumentException("The category'"+category.identifier+"' already exists!");
	}
	private static void refreshList() {
		for (int i=0; i < placements.size(); i++)
			placements.get(i).place = i;
	}
}
