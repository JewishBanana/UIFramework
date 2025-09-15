package com.github.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class UIAbilityType {
	
	private static final Map<String, UIAbilityType> registry = new HashMap<>();
	
	private Map<UUID, Integer> cooldown = new ConcurrentHashMap<>();
	private Class<? extends Ability> instance;
	private String displayName, registeredName;
	private String description;
	
	private UIAbilityType(String registeredName, Class<? extends Ability> instance) {
		this.registeredName = registeredName;
		this.instance = instance;
		this.displayName = '['+registeredName+']';
	}
	private static UIAbilityType createAbilityType(String registeredName, Class<? extends Ability> instance) {
		UIAbilityType type = new UIAbilityType(registeredName, instance);
		try {
			instance.getDeclaredConstructor(UIAbilityType.class).newInstance(type);
			return type;
		} catch (Exception e) {
			e.printStackTrace();
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: An error has occurred above this message while trying to register ability &d"+registeredName+" &e! This is NOT a UIFramework bug! Report this to the proper plugin author(s) of the related ability."));
			return null;
		}
	}
	/**
	 * Register a new custom ability to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins (e.g. uif-explosion)</i>
	 * 
	 * @param registeredName The unique registered name of your ability
	 * @param instance The associated class of your custom ability
	 * @return The AbilityType instance created
	 */
	public static UIAbilityType registerAbility(String registeredName, Class<? extends Ability> instance) {
		if (registry.containsKey(registeredName))
			throw new IllegalArgumentException("[UIFramework]: Cannot register ability '"+registeredName+"' as an ability with that name is already registered!");
		UIAbilityType type = createAbilityType(registeredName, instance);
		if (type == null)
			return null;
		registry.put(registeredName, type);
		return type;
	}
	/**
	 * Get the ability type instance by its registered name.
	 * 
	 * @param name The name of the ability
	 * @return The AbilityType instance or null
	 */
	public static UIAbilityType getAbilityType(String name) {
		return registry.get(name);
	}
	/**
	 * Gets the ability type instance by its registered class.
	 * 
	 * @param abilityClass The class of the ability type to get
	 * @return The ability type or null if it is not registered
	 */
	public static UIAbilityType getAbilityType(Class<? extends Ability> abilityClass) {
		for (Entry<String, UIAbilityType> entry : registry.entrySet())
			if (entry.getValue().instance.equals(abilityClass))
				return entry.getValue();
		return null;
	}
	/**
	 * Creates a new instance of the given ability.
	 * 
	 * @param abilityClass The class of the ability to create
	 * @return The new instance created
	 */
	public static <T extends Ability> T createAbilityInstance(Class<T> abilityClass) {
		for (Entry<String, UIAbilityType> entry : registry.entrySet())
			if (entry.getValue().instance.equals(abilityClass))
				try {
					return abilityClass.getDeclaredConstructor(UIAbilityType.class).newInstance(entry.getValue());
				} catch (Exception e) {
					e.printStackTrace();
				}
		return null;
	}
	/**
	 * <Strong>Internal use only!</STRONG>
	 * @param plugin
	 */
	public static void init(UIFramework plugin) {
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			for (UIAbilityType type : registry.values()) {
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
			return instance.getDeclaredConstructor(UIAbilityType.class).newInstance(this);
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
		return cooldown.getOrDefault(uuid, 0);
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
		return displayName;
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
		this.displayName = name;
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
	/**
	 * Gets the exact registered name of this ability which was registered by the owning plugin
	 * 
	 * @return The registered name of this ability
	 */
	public String getRegisteredName() {
		return registeredName;
	}
}
