package com.github.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.List;

public class ItemCategory {
	
	private static List<ItemCategory> placements = new ArrayList<>();
	
	public enum DefaultCategory {
		WEAPONS,
		ARMOR,
		TOOLS,
		EXTRA_EQUIPMENT,
		CONSUMABLES,
		ENCHANTED_BOOKS,
		CRAFTING_INGREDIENTS,
		MISCELLANEOUS;
		
		private DefaultCategory() {
			new ItemCategory(this);
		}
		public ItemCategory getItemCategory() {
			return getCategory(this);
		}
		public int getCategoryValue() {
			return getCategory(this).getCategoryValue();
		}
	}
	
	private String name;
	private int place;
	
	private ItemCategory(String name) {
		this.name = name;
	}
	private ItemCategory(DefaultCategory category) {
		this(category.toString());
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
	 * Adds a new item category to the server. This method will add your item category to the end of the list meaning it will have last priority.
	 * <p>
	 * <STRONG>New item categories must be added via these add methods upon your plugins startup everytime!</STRONG>
	 * 
	 * @param newCategory The name of the new item category to add
	 */
	public static void addItemCategory(String newCategory) {
		preconditions(newCategory);
		ItemCategory category = new ItemCategory(newCategory);
		category.place = placements.size();
		placements.add(category);
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
	public static boolean addItemCategoryBefore(String newCategory, DefaultCategory before) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).name.equalsIgnoreCase(before.toString())) {
				placements.add(i, new ItemCategory(newCategory));
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
	public static boolean addItemCategoryBefore(String newCategory, String before) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).name.equalsIgnoreCase(before)) {
				placements.add(i, new ItemCategory(newCategory));
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
	public static boolean addItemCategoryAfter(String newCategory, DefaultCategory after) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).name.equalsIgnoreCase(after.toString())) {
				placements.add(i+1, new ItemCategory(newCategory));
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
	public static boolean addItemCategoryAfter(String newCategory, String after) {
		preconditions(newCategory);
		for (int i=0; i < placements.size(); i++)
			if (placements.get(i).name.equalsIgnoreCase(after)) {
				placements.add(i+1, new ItemCategory(newCategory));
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
			if (temp.name.equalsIgnoreCase(category.toString()))
				return temp;
		return null;
	}
	/**
	 * Gets the item category.
	 * 
	 * @param category The category to get
	 * @return The ItemCategory or null if non-existent
	 */
	public static ItemCategory getCategory(String category) {
		for (ItemCategory temp : placements)
			if (temp.name.equalsIgnoreCase(category))
				return temp;
		return null;
	}
	private static void preconditions(String category) {
		for (ItemCategory temp : placements)
			if (temp.name.equalsIgnoreCase(category))
				throw new IllegalArgumentException("The category'"+category+"' already exists!");
	}
	private static void refreshList() {
		for (int i=0; i < placements.size(); i++)
			placements.get(i).place = i;
	}
}
