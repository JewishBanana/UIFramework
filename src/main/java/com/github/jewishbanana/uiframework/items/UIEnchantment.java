package com.github.jewishbanana.uiframework.items;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class UIEnchantment {
	
	protected static NamespacedKey enchantKey;
	private static Enchantment fakeEnchant;
	static {
		enchantKey = new NamespacedKey(UIFramework.getInstance(), "uie-key");
		fakeEnchant = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("power"));
	}
	private static Map<String, UIEnchantment> enchantsMap = new LinkedHashMap<>();
	private static Set<Integer> registeredIdList = new HashSet<>();
	private static Set<Integer> preIdList = new HashSet<>();
	
	private int id;
	private int maxLevel = 1;
	private String registeredName, displayName;
	private NamespacedKey registeredKey;
	private Set<Material> applicableTypes = new HashSet<>();
	private Set<Enchantment> conflictVanilla = new HashSet<>();
	private Set<String> conflictCustom = new HashSet<>();
	
	private ActivatedSlot activatingSlot = ActivatedSlot.PARENT;
	
	public UIEnchantment(String registeredName, int id) {
		this.registeredName = registeredName;
		this.id = id;
		try {
			this.registeredKey = new NamespacedKey(UIFramework.getInstance(), registeredName.replaceFirst(":", "-"));
		} catch (IllegalArgumentException e) {
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR while registering enchant &e'"+registeredName+"' &cThe following error is not a UIFramework bug! Contact the proper author of this enchant with the error below. The enchant cannot be registered with more than one ':' if this symbol is present multiple times in the name."));
			throw e;
		}
	}
	/**
	 * Register a new custom enchant to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins (e.g. uif:freezing)</i>
	 * 
	 * @param name The unique registered name of your enchant
	 * @param instance The associated class of your custom enchant
	 * @return The UIEnchantment instance created
	 */
	public static UIEnchantment registerEnchant(String name, Class<? extends UIEnchantment> instance) {
		if (enchantsMap.containsKey(name))
			throw new IllegalArgumentException("[UIFramework]: Cannot register enchant '"+name+"' as an enchant with that name is already registered!");
		if (UIFramework.dataFile.contains("enchant."+name)) {
			int id = UIFDataUtils.getDataFileInt("enchant."+name+".id");
			UIEnchantment type = null;
			if (registeredIdList.contains(id)) {
				for (int i=0; i < 999999; i++)
					if (!registeredIdList.contains(i)) {
						try {
							type = instance.getDeclaredConstructor(String.class, int.class).newInstance(name, id);
						} catch (Exception e) {
							e.printStackTrace();
						}
						enchantsMap.put(name, type);
						registeredIdList.add(i);
						UIFramework.dataFile.set("enchant."+name+".id", i);
						id = i;
						break;
					}
				UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR while registering enchant &e'"+name+"' &can enchant with that ID already exists! Rebounding the enchants ID to "+id+". All currently existing instances of this enchant will no longer work."));
				return type;
			}
			try {
				type = instance.getDeclaredConstructor(String.class, int.class).newInstance(name, id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			enchantsMap.put(name, type);
			registeredIdList.add(id);
			return type;
		}
		for (int i=0; i < 999999; i++)
			if (!registeredIdList.contains(i) && !preIdList.contains(i)) {
				UIEnchantment type = null;
				try {
					type = instance.getDeclaredConstructor(String.class, int.class).newInstance(name, i);
				} catch (Exception e) {
					e.printStackTrace();
				}
				enchantsMap.put(name, type);
				registeredIdList.add(i);
				UIFramework.dataFile.createSection("enchant."+name);
				UIFramework.dataFile.set("enchant."+name+".id", i);
				return type;
			}
		throw new IllegalArgumentException("[UIFramework]: Cannot register enchant '"+name+"' as no ID in the range 0-999999 is available!");
	}
	public static void addPreId(int id) {
		preIdList.add(id);
	}
	/**
	 * Runs the clean method on all abilities for all currently registered and active abilities.
	 * <p>
	 * <STRONG>This method is handled automatically by UIFramework for server restarts/reloads</STRONG>
	 */
	public static void cleanEnchants() {
		enchantsMap.values().forEach(i -> i.clean());
	}
	/**
	 * Get a UIEnchantment by its registered name.
	 * 
	 * @param name The registered name of the enchant
	 * @return The UIEnchantment instance or null
	 */
	public static UIEnchantment getEnchant(String name) {
		return enchantsMap.get(name);
	}
	/**
	 * Get a UIEnchantment by its registerd unique id
	 * 
	 * @param id The unique id of the UIEnchantment
	 * @return The UIEnchantment instance or null
	 */
	public static UIEnchantment getByID(int id) {
		for (UIEnchantment item : enchantsMap.values())
			if (item.id == id)
				return item;
		return null;
	}
	/**
	 * Adds this custom enchant to the specified item.
	 * 
	 * @param item The item to apply the enchant to
	 * @param level The level of the enchant to apply
	 * @param overwrite If this enchant already exists on the item should it overwrite the level or ignore
	 * @return If the enchant was applied or not
	 */
	public boolean addEnchant(GenericItem base, int level, boolean overwrite) {
		ItemStack item = base.getItem();
		if (item == null || item.getItemMeta() == null)
			return false;
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();
		if (container.has(enchantKey, PersistentDataType.INTEGER_ARRAY)) {
			int[] array = container.get(enchantKey, PersistentDataType.INTEGER_ARRAY);
			boolean flag = false;
			for (int i : array)
				if (i == id)
					if (container.has(registeredKey, PersistentDataType.INTEGER)) {
						if (!overwrite)
							return false;
						flag = true;
						unloadEnchant(base);
						break;
					}
			container.set(registeredKey, PersistentDataType.INTEGER, level);
			if (!flag) {
				int[] replace = new int[array.length+1];
				for (int i=0; i < array.length; i++)
					replace[i] = array[i];
				replace[array.length+1] = id;
				container.set(enchantKey, PersistentDataType.INTEGER_ARRAY, replace);
			}
			if (item.getType() != Material.ENCHANTED_BOOK && meta.getEnchants().size() == 0 && getFakeEnchant() != null) {
				meta.addEnchant(getFakeEnchant(), 1, true);
				base.setHiddenEnchanted(getFakeEnchant(), meta);
			}
			item.setItemMeta(meta);
			base.item = item;
			base.getEnchants().add(this);
			loadEnchant(base);
			return true;
		}
		container.set(registeredKey, PersistentDataType.INTEGER, level);
		container.set(enchantKey, PersistentDataType.INTEGER_ARRAY, new int[] {id});
		if (item.getType() != Material.ENCHANTED_BOOK && meta.getEnchants().size() == 0 && getFakeEnchant() != null) {
			meta.addEnchant(getFakeEnchant(), 1, true);
			base.setHiddenEnchanted(getFakeEnchant(), meta);
		}
		item.setItemMeta(meta);
		base.item = item;
		base.getEnchants().add(this);
		loadEnchant(base);
		return true;
	}
	/**
	 * Removes the specified custom enchant from the given item
	 * 
	 * @param enchant The custom enchant to remove
	 * @param item The item to remove the enchant from
	 * @return If the enchant was removed from the item or not
	 */
	public boolean removeEnchant(GenericItem base) {
		ItemStack item = base.getItem();
		if (item == null || !item.hasItemMeta())
			return false;
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();
		if (!container.has(enchantKey, PersistentDataType.INTEGER_ARRAY) || !container.has(registeredKey, PersistentDataType.INTEGER))
			return false;
		unloadEnchant(base);
		int[] array = container.get(enchantKey, PersistentDataType.INTEGER_ARRAY);
		if (array.length == 1) {
			container.remove(enchantKey);
			container.remove(registeredKey);
			item.setItemMeta(meta);
			return true;
		}
		int[] replace = new int[array.length-1];
		int index = 0;
		for (int i : array)
			if (i != id) {
				if (index >= replace.length)
					return false;
				replace[index++] = i;
			}
		container.set(enchantKey, PersistentDataType.INTEGER_ARRAY, replace);
		container.remove(registeredKey);
		item.setItemMeta(meta);
		base.getEnchants().remove(this);
		return true;
	}
	/**
	 * Checks if the given ItemStack has this custom enchant or not.
	 * 
	 * @param item The ItemStack to check
	 * @return If the ItemStack has this custom enchant
	 */
	public boolean hasEnchant(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return false;
		return item.getItemMeta().getPersistentDataContainer().has(registeredKey, PersistentDataType.INTEGER);
	}
	/**
	 * Gets the current level of this enchantment on the supplied item. If the enchant is not present on the item it will return 0
	 * 
	 * @param item The item to get the enchant level from
	 * @return The level of this enchantment on the item
	 */
	public int getEnchantLevel(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return 0;
		PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
		if (!container.has(registeredKey, PersistentDataType.INTEGER))
			return 0;
		return container.get(registeredKey, PersistentDataType.INTEGER);
	}
	/**
	 * Checks if the supplied item can be enchanted with this custom enchantment without violating any of this enchantments rules
	 * 
	 * @param item The item to check with
	 * @return If the item does not violate any of this enchantments rules
	 */
	public boolean canBeEnchanted(ItemStack item) {
		if (item.getItemMeta() == null)
			return false;
		if (!applicableTypes.isEmpty() && !applicableTypes.contains(item.getType()))
			return false;
		if (!conflictVanilla.isEmpty() && conflictVanilla.stream().anyMatch(e -> item.containsEnchantment(e)))
			return false;
		if (!conflictCustom.isEmpty() && conflictCustom.stream().anyMatch(e -> {
			UIEnchantment ue = getEnchant(e);
			return ue != null && item.getItemMeta().getPersistentDataContainer().has(ue.registeredKey, PersistentDataType.INTEGER);
		}))
			return false;
		return true;
	}
	/**
	 * Run when an item with this custom enchant is initialized by the server.
	 * Useful for overriding to check level and modify functionality.
	 * <p>
	 * <STRONG>Initialization will happen for every item when it is first interacted with after server restart/reload</STRONG>
	 * 
	 * @param item The base of the item being initialized
	 */
	public void loadEnchant(GenericItem base) {}
	/**
	 * Run when this enchant is removed from the supplied base classes item.
	 * Useful for overriding to clean any data or remove custom abilities from the item that are associated with this enchant.
	 * <p>
	 * <STRONG>Keep in mind that this is run before the enchant is actually stripped from the item so getting the level of the enchant on the item is still possible</STRONG>
	 * 
	 * @param base The base of the item getting this enchant removed
	 */
	public void unloadEnchant(GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is directly involved in a PlayerInteractEvent
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void interacted(PlayerInteractEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is directly involved in a PlayerInteractEntityEvent
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void interactedEntity(PlayerInteractEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is used to hit another entity
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void hitEntity(EntityDamageByEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant's holder was hit by another entity
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void wasHit(EntityDamageByEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant was thrown as a projectile
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void projectileThrown(ProjectileLaunchEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant has hit something as a projectile
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void projectileHit(ProjectileHitEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant's holder was hit by a projectile
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void hitByProjectile(ProjectileHitEvent event, GenericItem base) {}
	/**
	 * Run whenever a bow with this enchant is fired by an entity
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void shotBow(EntityShootBowEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is directly involved in a InventoryClickEvent
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void inventoryClick(InventoryClickEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is consumed by a player
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void consumeItem(PlayerItemConsumeEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is splashed as a potion
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void splashPotion(PotionSplashEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is dropped
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void dropItem(EntityDropItemEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant is picked up
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void pickupItem(EntityPickupItemEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant's holder has died
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void entityDeath(EntityDeathEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant's holder has respawned (Mainly useful for totem related events)
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void entityRespawn(PlayerRespawnEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant has been placed as a block
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void placeBlock(BlockPlaceEvent event, GenericItem base) {}
	/**
	 * Run whenever an item with this enchant was used to break a block
	 * 
	 * @param event The event involved
	 * @param base The base of the item activating this enchant
	 */
	public void breakBlock(BlockBreakEvent event, GenericItem base) {}
	
	/**
	 * Cleans all entities and data related to the ability (For server reloads/restarts).
	 */
	public void clean() {}
	
	/**
	 * Gets the display name for this enchant (The visible name in game) but with the placeholders for the level replaced with the given level.
	 * 
	 * @param level The level of the enchant to display
	 * @return This enchants display name
	 */
	public String getDisplayName(int level) {
		return displayName != null ? displayName.replace("%l%", ""+level).replace("%nl%", UIFUtils.getNumerical(level)) : registeredName+' '+level;
	}
	/**
	 * Gets the display name for this enchant (The visible name in game).
	 * 
	 * @return This enchants display name
	 */
	public String getDisplayName() {
		return displayName != null ? displayName : registeredName;
	}
	/**
	 * Sets the display name of this enchant in game. Display name will be used for lore name and defaults to the registered name if not set.
	 * <p>
	 * <i>Use '%l%' for the placeholder of the integer level. Use '%nl%' for the roman numerical level.</i>
	 * 
	 * @param displayName The name of the enchant
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	/**
	 * Gets the cleaned display name of this enchant (Display name with the placeholders removed and trimmed).
	 * 
	 * @return The trimmed name of this enchant
	 */
	public String getTrimmedName() {
		return displayName != null ? displayName.replace("%l%", "").replace("%nl%", "").trim() : registeredName;
	}
	/**
	 * Set the list of applicable materials this enchant can be applied to. If no types are set then the enchant is assumed to be universal.
	 * 
	 * @param types A collection of materials which this enchant can be applied to
	 */
	public void setApplicableTypes(Collection<Material> types) {
		this.applicableTypes = new HashSet<Material>(types);
	}
	/**
	 * Add applicable materials this enchant can be applied to. If no types are set then the enchant is assumed to be universal.
	 * 
	 * @param types A collection of materials which this enchant can be applied to
	 */
	public void addApplicableTypes(Collection<Material> types) {
		this.applicableTypes.addAll(types);
	}
	/**
	 * Sets the vanilla minecraft enchants that this enchant conflicts with
	 * 
	 * @param enchants A collection of the enchants that this enchant will conflict with
	 */
	public void setVanillaConflicts(Collection<Enchantment> enchants) {
		this.conflictVanilla = new HashSet<Enchantment>(enchants);
	}
	/**
	 * Add vanilla minecraft enchants that this enchant will conflict with
	 * 
	 * @param enchants A collection of the enchants that this enchant will conflict with
	 */
	public void addVanillaConflicts(Collection<Enchantment> enchants) {
		this.conflictVanilla.addAll(enchants);
	}
	/**
	 * Sets the list of custom enchants that this enchant will conflict with. Use the exact registered name of the enchant you want to have conflict with.
	 * Non-existent custom enchants will NOT notify or throw any errors to the user so compatability with other UIF plugins that may or may not be installed will work.
	 * 
	 * @param enchants A collection of the registered keys of custom enchants that this enchant will conflict with
	 */
	public void setCustomConflicts(Collection<String> enchants) {
		this.conflictCustom = new HashSet<String>(enchants);
	}
	/**
	 * Add a custom enchant that this enchant will conflict with. Use the exact registered name of the enchant you want to have conflict with.
	 * Non-existent custom enchants will NOT notify or throw any errors to the user so compatability with other UIF plugins that may or may not be installed will work.
	 * 
	 * @param enchants A collection of the registered keys of custom enchants that this enchant will conflict with
	 */
	public void addCustomConflicts(Collection<String> enchants) {
		conflictCustom.addAll(enchants);
	}
	/**
	 * Get the map of all registered custom enchants on the server along with their registered names.
	 * 
	 * @return A map of all custom enchants on the server with the key as their registered names
	 */
	public static Map<String, UIEnchantment> getEnchantsMap() {
		return enchantsMap;
	}
	/**
	 * Get the activating slot that items with this enchant must be in for the enchants events to run
	 * 
	 * @return This enchants activating slot
	 */
	public ActivatedSlot getActivatingSlot() {
		return activatingSlot;
	}
	/**
	 * Sets the activating slot that items with this enchant must be in for the enchants events to run
	 * <p>
	 * <i>Custom enchant events will run regardless of the activating slot of its parent item</i>
	 * 
	 * @param activatingSlot The activating slot that this enchants item must be in to run the enchants events
	 */
	public void setActivatingSlot(ActivatedSlot activatingSlot) {
		this.activatingSlot = activatingSlot;
	}
	/**
	 * Get the unique id of this enchant in the UIFramework data file. This will be unique for each server!
	 * 
	 * @return The unique id of this enchant
	 */
	public int getId() {
		return id;
	}
	/**
	 * The fake enchantment used to simulate the glowing effect for this enchant (If no real enchants are on the item).
	 * <p>
	 * <i>The default enchant used is power, to change this simply override this method in your enchants class. If you would prefer to not have a glow effect with this enchant then override and return null.</i>
	 * 
	 * @return
	 */
	public Enchantment getFakeEnchant() {
		return fakeEnchant;
	}
	public int getMaxLevel() {
		return maxLevel;
	}
	public void setMaxLevel(int maxLevel) {
		if (maxLevel < 1)
			throw new IllegalArgumentException("Max level cannot be less than 1!");
		this.maxLevel = maxLevel;
	}
	/**
	 * Gets the internal registered name of this custom enchant (The key used to register the enchant with UIFramework).
	 * 
	 * @return The registered name of this custom enchant
	 */
	public String getRegisteredName() {
		return registeredName;
	}
	/**
	 * Get the NamespacedKey for this enchant. The key is stored within the persistent data container of items with this enchant.
	 * 
	 * @return This custom enchants NamespacedKey
	 */
	public NamespacedKey getEnchantKey() {
		return registeredKey;
	}
}
