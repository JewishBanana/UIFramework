package com.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.utils.ItemBuilder;

public class AbilityType {
	
	private static Map<String, AbilityType> typeMap = new HashMap<>();
	
	private Map<UUID, Integer> cooldown = new ConcurrentHashMap<>();
	private Class<? extends Ability> instance;
	private String name;
	private String description;
	
	private AbilityType(Class<? extends Ability> instance) {
		this.instance = instance;
	}
	/**
	 * Register a new custom ability to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins</i>
	 * 
	 * @param name The unique registered name of your ability
	 * @param instance The associated class of your custom ability
	 * @return The AbilityType instance created
	 */
	public static AbilityType registerAbility(String name, Class<? extends Ability> instance) {
		if (typeMap.containsKey(name))
			throw new IllegalArgumentException("[UIFramework]: Cannot register ability '"+name+"' as an ability with that name is already registered!");
		AbilityType type = new AbilityType(instance);
		type.name = '['+name+']';
		typeMap.put(name, type);
		return type;
	}
	/**
	 * Get the ability type instance by its registered name.
	 * 
	 * @param name The name of the ability
	 * @return The AbilityType instance or null
	 */
	public static AbilityType getAbilityType(String name) {
		return typeMap.get(name);
	}
	/**
	 * <Strong>Internal use only!</STRONG>
	 * @param plugin
	 */
	public static void init(UIFramework plugin) {
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			for (AbilityType type : typeMap.values()) {
				Iterator<Entry<UUID, Integer>> it = type.cooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					if (entry.setValue(entry.getValue() - 1) <= 0)
						it.remove();
				}
			}
		}, 0, 1);
	}
	/**
	 * Creates a new instance of this Ability and sets the type to this AbilityType.
	 * 
	 * @return The created Ability instance
	 */
	public Ability createNewInstance() {
		try {
			return instance.getDeclaredConstructor().newInstance().setType(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Checks if the given entity is on a cooldown for this ability type or not.
	 * 
	 * @param uuid The unique id of the entity
	 * @return If the entity is on cooldown or not
	 */
	public boolean isEntityOnCooldown(UUID uuid) {
		return cooldown.containsKey(uuid);
	}
	public int getEntityCooldown(UUID uuid) {
		return cooldown.containsKey(uuid) ? cooldown.get(uuid) : 0;
	}
	/**
	 * Puts the given entity on cooldown for this ability type.
	 * <p>
	 * <i>20 ticks = 1 second</i>
	 * 
	 * @param uuid The unique id of the entity
	 * @param ticks The amount of ticks to set the cooldown for
	 */
	public void putEntityOnCooldown(UUID uuid, int ticks) {
		cooldown.put(uuid, ticks);
	}
	/**
	 * Get the display name of this ability
	 * 
	 * @return The display name of the ability
	 */
	public String getDisplayName() {
		return name;
	}
	/**
	 * Set the display name of this ability. Display name is the name of the ability that should be displayed in game for example while using the default format of custom items lore in the ItemBuilder.
	 * This is not related to the registered name.
	 * 
	 * @param name The display name to use
	 * 
	 * @see ItemBuilder#assembleLore(ItemType)
	 */
	public void setDisplayName(String name) {
		this.name = name;
	}
	/**
	 * Get the description of this ability
	 * 
	 * @return The description of the ability
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * Set the description of this ability. Ability description is utilized as the ability description in the default format of custom items lore in the ItemBuilder.
	 * 
	 * @param description The description to use
	 * 
	 * @see ItemBuilder#assembleLore(ItemType)
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
