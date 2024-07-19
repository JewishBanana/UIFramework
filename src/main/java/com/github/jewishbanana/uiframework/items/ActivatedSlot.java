package com.github.jewishbanana.uiframework.items;

public enum ActivatedSlot {
	/**
	 * <STRONG>This is the default selector for every custom item</STRONG>
	 * <p>
	 * Represents a default slot that varies based on event (e.g. EntityDamageEntityEvent, when an entity uses this to hit another entity; PlayerInteractEvent, when a player is holding this item and interacts with something; ProjectileLaunchEvent, when used as a projectile)
	 */
	ACTIVE_SLOT,
	/**
	 * Represents either main hand or off hand slots
	 */
	HAND,
	/**
	 * Represents specifically the main hand slot
	 */
	MAIN_HAND,
	/**
	 * Represents specifically the off hand slot
	 */
	OFF_HAND,
	/**
	 * Represents any of the armor slots meaning this must be worn as armor to activate
	 */
	ARMOR,
	/**
	 * Represents all 36 storage slots of the inventory including the hotbar
	 */
	STORAGE,
	/**
	 * Represents specifically any of the 9 storage slots that are part of the hotbar
	 */
	HOTBAR,
	/**
	 * Universal selector, represents any slot in an entities inventory
	 */
	ANY,
	/**
	 * <STRONG>This is the default selector for every custom ability or enchant</STRONG>
	 * <p>
	 * This selector is unique and means that the activating slot of this enchant or ability will inherit the activating slot of whatever custom item it is present on
	 */
	PARENT;
}
