package com.github.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Projectile;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.Ability.Action;
import com.github.jewishbanana.uiframework.listeners.AbilityListener;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class GenericItem {
	
	private static Map<String, GenericItem> itemMap = new ConcurrentHashMap<>(13335);
	
	protected static NamespacedKey generalKey;
	private static NamespacedKey identityKey;
	private static NamespacedKey defaultsKey;
	private static NamespacedKey hiddenEnchant;
	static {
		generalKey = new NamespacedKey(UIFramework.getInstance(), "ui-key");
		identityKey = new NamespacedKey(UIFramework.getInstance(), "uii");
		defaultsKey = new NamespacedKey(UIFramework.getInstance(), "uie-dkey");
		hiddenEnchant = new NamespacedKey(UIFramework.getInstance(), "uie-henc");
	}
	private ItemType type;
	protected ItemStack item;
	private Map<NamespacedKey, ItemField<?>> fields = new HashMap<>();
	private Set<UIEnchantment> enchants = new HashSet<>();
	protected Map<Ability, Set<Ability.Action>> uniqueAbilities = new LinkedHashMap<>();
	
	private ActivatedSlot activatingSlot = ActivatedSlot.ACTIVE_SLOT;
	
	private boolean alwaysAllowAbilities = true;
	private Enchantment enchantment;
	
	/**
	 * Creates a new custom item class for the supplied item. This is the base class for all custom items in UIFramework, all custom item classes should extend off of this.
	 * 
	 * @param item The item to create the class for
	 */
	public GenericItem(ItemStack item) {
		this.item = item;
		if (item.hasItemMeta()) {
			PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
			if (container.has(UIEnchantment.enchantKey, PersistentDataType.INTEGER_ARRAY)) {
				for (int i : container.get(UIEnchantment.enchantKey, PersistentDataType.INTEGER_ARRAY)) {
					UIEnchantment enchant = UIEnchantment.getByID(i);
					if (enchant == null) {
						if (UIFramework.debugMessages)
							UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eERROR could not initialize enchant by id '"+i+"' on a custom item! Has the custom enchants host plugin been removed? No data loss has occurred, the enchant will be temporarily disabled. To avoid seeing these messages you can disable debug messages in the config."));
						continue;
					}
					if (!enchant.hasEnchant(item))
						continue;
					enchant.loadEnchant(this);
					enchants.add(enchant);
				}
			}
			if (container.has(hiddenEnchant, PersistentDataType.STRING))
				this.enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(container.get(hiddenEnchant, PersistentDataType.STRING)));
			ConfigurationSection section = UIFramework.dataFile.getConfigurationSection("itemData."+container.get(identityKey, PersistentDataType.STRING));
			if (section != null) {
				if (section.contains("abilities"))
					for (String s : section.getConfigurationSection("abilities").getKeys(false)) {
						ConfigurationSection temp = section.getConfigurationSection("abilities."+s);
						try {
							AbilityType type = AbilityType.getAbilityType(temp.getString("ability._abilityType"));
							if (type == null) {
								if (UIFramework.debugMessages)
									UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eERROR could not initialize unique ability '"+temp.getString("ability._abilityType")+"' on a custom item! Has the abilities host plugin been removed? No data loss has occurred, the ability will be temporarily disabled. To avoid seeing these messages you can disable debug messages in the config."));
								continue;
							}
							Ability ability = type.createNewInstance();
							ability.deserialize(temp.getConfigurationSection("ability").getValues(true));
							Set<Ability.Action> actions = new HashSet<>();
							for (String action : temp.getStringList("actions"))
								actions.add(Ability.Action.valueOf(action));
							uniqueAbilities.put(ability, actions);
						} catch (Exception e) {
							e.printStackTrace();
							UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR could not initialize unique ability '"+temp.getString("ability._abilityType")+"' on a custom item! Inform the author of this ability as they did not serialize their ability properly."));
						}
					}
			}
		}
	}
	/**
	 * Triggered whenever a PlayerInteractEvent happens and this custom item is involved
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean interacted(PlayerInteractEvent event) {
		return true;
	}
	/**
	 * Triggered whenever a PlayerInteractEntityEvent happens and this custom item is involved
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean interactedEntity(PlayerInteractEntityEvent event) {
		return true;
	}
	/**
	 * Triggered whenever a holder of this custom item hits an entity
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean hitEntity(EntityDamageByEntityEvent event) {
		return true;
	}
	/**
	 * Triggered whenever a holder of this custom item is hit by an entity
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean wasHit(EntityDamageByEntityEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item was thrown as a projectile
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean projectileThrown(ProjectileLaunchEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item hits something as a projectile
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean projectileHit(ProjectileHitEvent event) {
		return true;
	}
	/**
	 * Triggered whenever the holder of this custom item is hit by a projectile
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean hitByProjectile(ProjectileHitEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item is shot as a bow
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean shotBow(EntityShootBowEvent event) {
		return true;
	}
	/**
	 * Triggered whenever an InventoryClickEvent happens and this custom item is involved
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean inventoryClick(InventoryClickEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item is consumed by a player
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean consumeItem(PlayerItemConsumeEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item was used as a splash potion
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean splashPotion(PotionSplashEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item is dropped by an entity
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean dropItem(EntityDropItemEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this custom item is picked up by an entity
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean pickupItem(EntityPickupItemEvent event) {
		return true;
	}
	/**
	 * Triggered whenever a holder of this custom item dies
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean entityDeath(EntityDeathEvent event) {
		return true;
	}
	/**
	 * Triggered whenever a holder of this custom item respawns
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean entityRespawn(PlayerRespawnEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this item is placed as a block (If it's a placeable item type)
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean placeBlock(BlockPlaceEvent event) {
		return true;
	}
	/**
	 * Triggered whenever this item is used to break a block
	 * 
	 * @param event The event involved
	 * @return If this items abilities should be activated if applicable or not (Basically return false if you don't want this items abilities to activate even if they could)
	 */
	public boolean breakBlock(BlockBreakEvent event) {
		return true;
	}
	/**
	 * Creates the ItemBuilder for the custom item.
	 * <p>
	 * <STRONG>This method is required in every items class. You should utilize the ItemBuilder create method to construct your ItemStack.</STRONG>
	 * 
	 * @return The created ItemBuilder
	 * 
	 * @see ItemBuilder#create(ItemType, ItemStack)
	 */
	public ItemBuilder createItem() {
		return ItemBuilder.create(getType(), Material.EGG);
	}
	/**
	 * Strips the unwanted item tags to prime item for comparison in crafting recipes. This will by default strip all ItemFields for you so only override if you have excess tags.
	 * <p>
	 * <STRONG>If you override this ensure you still call the super method so the ItemFields will be stripped!</STRONG>
	 * 
	 * @param meta The meta to strip the tags from
	 */
	public void stripTags(ItemMeta meta) {
		fields.values().stream().filter(e -> e.key != null).forEach(e -> meta.getPersistentDataContainer().remove(e.key));
		PersistentDataContainer container = meta.getPersistentDataContainer();
		container.remove(identityKey);
		container.remove(defaultsKey);
		container.remove(hiddenEnchant);
		enchants.forEach(e -> container.remove(e.getEnchantKey()));
		container.remove(UIEnchantment.enchantKey);
	}
	
	/**
	 * Gets the GenericItem base class of the given ItemStack. Will create a new GenericItem base if one does not already exist.
	 * 
	 * @param item The ItemStack of the base class
	 * @return The ItemStack's base class or null
	 */
	public static GenericItem getItemBase(ItemStack item) {
		if (item == null)
			return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return null;
		String uuid = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
		if (uuid != null) {
			GenericItem base = itemMap.get(uuid);
			if (base != null) {
				base.item = item;
				return base;
			}
		}
		try {
			String key = meta.getPersistentDataContainer().get(GenericItem.generalKey, PersistentDataType.STRING);
			if (key == null)
				return null;
			ItemType type = ItemType.getItemType(key);
			if (type == null) {
				if (UIFramework.debugMessages)
					UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR could not initialize custom item '"+key+"'. This is most likely caused by removal of the plugin that created this item before."));
				return null;
			}
			if (itemMap.size() > 10000)
				unloadItems();
			if (uuid == null && item.getType().getMaxStackSize() == 1) {
				uuid = UUID.randomUUID().toString();
				meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, uuid);
				item.setItemMeta(meta);
				GenericItem base = type.createNewInstance(item);
				itemMap.put(uuid, base);
				return base;
			}
			return type.createNewInstance(item);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}
	/**
	 * Special variation of getting the item base. This will create a new base class without attaching an ID to the item.
	 * <p>
	 * <STRONG>This should generally never be used aside from creating one time instances of items e.g. navigatable item menus.</STRONG>
	 * 
	 * @param item The item to create the base for
	 * @return The newly created base class or null if N/A
	 */
	public static GenericItem getItemBaseNoID(ItemStack item) {
		if (item == null)
			return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return null;
		try {
			String key = meta.getPersistentDataContainer().get(GenericItem.generalKey, PersistentDataType.STRING);
			if (key == null)
				return null;
			ItemType type = ItemType.getItemType(key);
			if (type == null) {
				if (UIFramework.debugMessages)
					UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR could not initialize custom item '"+key+"'. This is most likely caused by removal of the plugin that created this item before."));
				return null;
			}
			GenericItem base = type.createNewInstance(item);
			return base;
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}
	/**
	 * If you want to create an item base for a vanilla item to add custom abilities or enchants to.
	 * <p>
	 * <i>If you are using regular custom items then use the get method below</i>
	 * 
	 * @param item The item stack to create the base for
	 * @return The created item base or null
	 * 
	 * @see GenericItem#getBaseItem(ItemStack)
	 */
	public static GenericItem createItemBase(ItemStack item) {
		if (item == null)
			return null;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return null;
		if (!meta.getPersistentDataContainer().has(generalKey, PersistentDataType.STRING)) {
			meta.getPersistentDataContainer().set(generalKey, PersistentDataType.STRING, "_null");
			item.setItemMeta(meta);
		}
		return getItemBase(item);
	}
	/**
	 * Remove any item base associated with the ItemStack converting it to a normal item.
	 * 
	 * @param item The ItemStack to detach from
	 */
	public static void removeBaseItem(ItemStack item) {
		if (item == null || !item.hasItemMeta())
			return;
		String uuid = item.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
		if (uuid != null)
			itemMap.remove(uuid);
	}
	/**
	 * Unloads all custom items unique abilities to the data file.
	 * <p>
	 * <STRONG>This is handled automatically by UIFramework and should not be used!</STRONG>
	 */
	public static void unloadItems() {
		if (!UIFramework.dataFile.contains("itemData"))
			UIFramework.dataFile.createSection("itemData");
		ConfigurationSection section = UIFramework.dataFile.getConfigurationSection("itemData");
		itemMap.forEach((uuid, base) -> {
			base.enchants.forEach(e -> e.unloadEnchant(base));
			ItemStack item = base.item;
			if (item == null || !item.hasItemMeta() || base.uniqueAbilities.isEmpty() || !base.uniqueAbilities.keySet().stream().anyMatch(e -> e.persist))
				return;
			ConfigurationSection itemSection = section.getConfigurationSection(uuid);
			if (itemSection == null)
				itemSection = section.createSection(uuid);
			ConfigurationSection abilitySection = itemSection.getConfigurationSection("abilities");
			if (abilitySection != null)
				itemSection.set("abilities", null);
			abilitySection = itemSection.createSection("abilities");
			int i = 0;
			for (Entry<Ability, Set<Action>> entry : base.uniqueAbilities.entrySet()) {
				if (!entry.getKey().persist)
					continue;
				ConfigurationSection temp = abilitySection.createSection("a"+i);
				temp.set("actions", new ArrayList<String>(entry.getValue().stream().map(e -> e.toString()).collect(Collectors.toList())));
				ConfigurationSection created = temp.createSection("ability");
				entry.getKey().serialize().forEach((k, v) -> created.set(k, v));
				i++;
			}
		});
		itemMap.clear();
	}
	/**
	 * Assign a projectile to this item making it the shooter. Will run events from this class when landing/hitting.
	 * 
	 * @param projectile The projectile entity
	 */
	public void assignProjectile(Projectile projectile) {
		AbilityListener.assignProjectile(projectile.getUniqueId(), this, null);
	}
	/**
	 * Assign a projectile to this item making it the shooter. Will run events from this class when landing/hitting.
	 * 
	 * @param projectile The projectile entity
	 * @param projectileBase The base class to use for a custom projectile if wanted
	 */
	public void assignProjectile(Projectile projectile, GenericItem projectileBase) {
		AbilityListener.assignProjectile(projectile.getUniqueId(), this, projectileBase);
	}
	/**
	 * Remove a projectile from this items ability listeners.
	 * 
	 * @param uuid The unique id of the projectile to remove
	 */
	public void removeProjectile(UUID uuid) {
		AbilityListener.removeProjectile(uuid);
	}
	/**
	 * Adds a unique ability that is only applied to this specific item.
	 * 
	 * @param ability The ability to add
	 * @param actions The actions that will activate this ability
	 * @param persist If this ability will save on server restarts/reloads
	 */
	public void addUniqueAbility(Ability ability, Collection<Ability.Action> actions, boolean persist) {
		ability.persist = persist;
		this.uniqueAbilities.put(ability, new HashSet<Ability.Action>(actions));
	}
	/**
	 * Adds a unique ability that is only applied to this specific item.
	 * 
	 * @param ability The ability to add
	 * @param actions The actions that will activate this ability
	 */
	public void addUniqueAbility(Ability ability, Collection<Ability.Action> actions) {
		addUniqueAbility(ability, actions, true);
	}
	/**
	 * Removes the unique ability from this specific item.
	 * 
	 * @param ability The ability to remove
	 */
	public void removeUniqueAbility(Ability ability) {
		this.uniqueAbilities.remove(ability);
	}
	/**
	 * Removes all unique abilities of the given type from this specific item.
	 * 
	 * @param type The type of ability to remove
	 */
	public void removeUniqueAbilities(AbilityType type) {
		Iterator<Entry<Ability, Set<Action>>> iterator = this.uniqueAbilities.entrySet().iterator();
		while (iterator.hasNext())
			if (iterator.next().getKey().getType().getRegisteredName().equals(type.getRegisteredName()))
				iterator.remove();
	}
	/**
	 * Retrieves a set of all custom enchants on this item.
	 * 
	 * @return The set of the custom enchants
	 */
	public Set<UIEnchantment> getEnchants() {
		return enchants;
	}
	protected GenericItem applyDefaultEnchants() {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();
		if (container.has(defaultsKey, PersistentDataType.INTEGER_ARRAY))
			for (int i : container.get(defaultsKey, PersistentDataType.INTEGER_ARRAY)) {
				UIEnchantment enchant = UIEnchantment.getByID(i);
				if (enchant == null)
					continue;
				enchant.removeEnchant(this);
			}
		List<Integer> defaults = new ArrayList<>();
		type.enchants.forEach((k, v) -> {
			if (k.addEnchant(this, v, false))
				defaults.add(k.getId());
			if (k.getEnchantLevel(item) != 0)
				enchants.add(k);
		});
		meta = item.getItemMeta();
		meta.getPersistentDataContainer().set(defaultsKey, PersistentDataType.INTEGER_ARRAY, defaults.stream().mapToInt(i -> i).toArray());
		item.setItemMeta(meta);
		type.getBuilder().assembleLore(item, meta, type, this);
		return this;
	}
	/**
	 * Gets the ItemType of this base. Non-custom items with custom enchants will have an ID of -1.
	 * 
	 * @return The ItemType of the item or null
	 */
	public ItemType getType() {
		return type;
	}
	protected GenericItem setType(ItemType type) {
		this.type = type;
		return this;
	}
	public ItemStack getItem() {
		return item;
	}
	public void setItem(ItemStack item) {
		this.item = item;
	}
	public Map<NamespacedKey, ItemField<?>> getFields() {
		return fields;
	}
	/**
	 * Get the ItemField present on this base class from the key.
	 * 
	 * @param key The key of the ItemField to get
	 * @return The ItemField or null
	 */
	public ItemField<?> getField(NamespacedKey key) {
		return fields.get(key);
	}
	/**
	 * Registers a new item field to the key on this item's base.
	 * 
	 * @param key The key of the field to register to this item
	 * @param dataType The type of data that this key is storing
	 * @param defaultValue The default value to store on this field if no value was present
	 * @return The newly created ItemField of your dataType
	 */
	public <T> ItemField<T> registerItemField(NamespacedKey key, PersistentDataType<T, T> dataType, T defaultValue) {
		ItemField<T> field = new ItemField<T>(this, key, dataType, defaultValue);
		this.fields.put(key, field);
		return field;
	}
	/**
	 * Get the activating slot that this item must be in for the items events to run.
	 * 
	 * @return This items activating slot
	 */
	public ActivatedSlot getActivatingSlot() {
		return activatingSlot;
	}
	/**
	 * Sets the activating slot that this item must be in for the items events to run.
	 * <p>
	 * <i>Custom enchants and abilities with different activating slots will still run their events ignoring the parent items activating slot</i>
	 * 
	 * @param activatingSlot The activating slot that this custom item must be in to run the items events
	 */
	public void setActivatingSlot(ActivatedSlot activatingSlot) {
		this.activatingSlot = activatingSlot != ActivatedSlot.PARENT ? activatingSlot : ActivatedSlot.ACTIVE_SLOT;
	}
	/**
	 * Controls if abilities on this item can activate if the item is not in its proper activation slot but the abilities activation slot was triggered.
	 * 
	 * @return If abilities can activate regardless of this items activation slot
	 */
	public boolean isAlwaysAllowAbilities() {
		return alwaysAllowAbilities;
	}
	/**
	 * Controls if abilities on this item can activate if the item is not in its proper activation slot but the abilities activation slot was triggered.
	 * <p>
	 * <i>By default this is set to true</i>
	 * 
	 * @param alwaysAllowAbilities If abilities can activate regardless of this items activation slot
	 */
	public void setAlwaysAllowAbilities(boolean alwaysAllowAbilities) {
		this.alwaysAllowAbilities = alwaysAllowAbilities;
	}
	/**
	 * Will add the given enchantment to your item but not display it in the item's lore. Used to obtain a secret enchantment effect. The enchantment type should generally be something that does not affect the item's gameplay.
	 * 
	 * @param enchantment The enchantment to apply and hide for the item
	 */
	public void setHiddenEnchanted(Enchantment enchantment) {
		if (item == null || this.enchantment != null)
			return;
		this.enchantment = enchantment;
		ItemMeta meta = item.getItemMeta();
		if (meta == null)
			return;
		meta.addEnchant(enchantment, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.getPersistentDataContainer().set(hiddenEnchant, PersistentDataType.STRING, enchantment.getKey().getKey());
		item.setItemMeta(meta);
	}
	protected void setHiddenEnchanted(Enchantment enchantment, ItemMeta meta) {
		if (this.enchantment != null)
			return;
		this.enchantment = enchantment;
		meta.addEnchant(enchantment, 1, true);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.getPersistentDataContainer().set(hiddenEnchant, PersistentDataType.STRING, enchantment.getKey().getKey());
	}
	/**
	 * Gets the hidden enchantment on this item if one is present.
	 * 
	 * @return The fake enchant placed on this item or null
	 * 
	 * @see GenericItem#setHiddenEnchanted(Enchantment)
	 */
	public Enchantment getHiddenEnchant() {
		return enchantment;
	}
	/**
	 * Gets the presented display name of this item (i.e. The name presented from the give command). If this item already has a set display name on the itemstack then that will be used instead.
	 * <p>
	 * <i>If you wish to change this value simply override this getter in your items class</i>
	 * 
	 * @return The display name of this item
	 */
	public String getDisplayName() {
		try {
			return type.getDisplayName() == null ? "&f"+this.getClass().getSimpleName() : "&f"+type.getDisplayName();
		} catch (NullPointerException e) {
			return "&f"+this.getClass().getSimpleName();
		}
	}
	/**
	 * Gets the item category that this items type resides in. Item category is only used to determine the order in which the items will appear in the UI recipes menu, adding categories to your items will help users easily navigate UI menus for your item.
	 * <p>
	 * <i>The default category returned is miscellaneous. To change this simply override this method and return the desired category in your items class.
	 * You can also create new item categories of your own by changing the value returned (Items are sorted by ascending order of its category value), see the ItemCategory class available values.</i>
	 * 
	 * @return The category of this items type
	 * 
	 * @see ItemCategory
	 */
	public ItemCategory getItemCategory() {
		return ItemCategory.DefaultCategory.MISCELLANEOUS.getItemCategory();
	}
	/**
	 * Checks if this item has the given ability or not.
	 * 
	 * @param ability The registered name of the ability to check for
	 * @return If this item has the ability or not
	 */
	public boolean hasAbility(String ability) {
		for (Ability temp : uniqueAbilities.keySet())
			if (temp.getType().getRegisteredName().equals(ability))
				return true;
		for (Ability temp : type.getAbilities().keySet())
			if (temp.getType().getRegisteredName().equals(ability))
				return true;
		return false;
	}
	/**
	 * Gets the given ability from this item if it exists otherwise returns null.
	 * 
	 * @param ability The registered name of the ability to get
	 * @return The ability or null
	 */
	public Ability getAbility(String ability) {
		for (Ability temp : uniqueAbilities.keySet())
			if (temp.getType().getRegisteredName().equals(ability))
				return temp;
		for (Ability temp : type.getAbilities().keySet())
			if (temp.getType().getRegisteredName().equals(ability))
				return temp;
		return null;
	}
	/**
	 * Checks if this item has the given custom enchant or not.
	 * 
	 * @param enchant The registered name of the custom enchant to check for
	 * @return If this item has the custom enchant or not
	 */
	public boolean hasEnchant(String enchant) {
		return enchants.stream().anyMatch(e -> e.getRegisteredName().equals(enchant));
	}
	/**
	 * Gets the given custom enchant from this item if it exists otherwise returns null.
	 * 
	 * @param enchant The registered name of the custom enchant to get
	 * @return The custom enchant or null
	 */
	public UIEnchantment getEnchant(String enchant) {
		for (UIEnchantment e : enchants)
			if (e.getRegisteredName().equals(enchant))
				return e;
		return null;
	}
	/**
	 * Gets the level of the custom enchant present on this item. Convienence method for checking and getting the level.
	 * 
	 * @param enchant The custom enchant to check
	 * @return The level of the custom enchant on this item or 0
	 */
	public int getEnchantLevel(String enchant) {
		for (UIEnchantment e : enchants)
			if (e.getRegisteredName().equals(enchant))
				return e.getEnchantLevel(item);
		return 0;
	}
	/**
	 * Strips the item ID from this base classes item. This will detach the item from its identifying custom item type meaning it will convert it to a regular vanilla item.
	 * 
	 * @return This base class
	 */
	public GenericItem stripItemID() {
		ItemMeta meta = item.getItemMeta();
		meta.getPersistentDataContainer().remove(generalKey);
		item.setItemMeta(meta);
		return this;
	}
	/**
	 * Gets the UUID on this exact items instance
	 * 
	 * @return The UUID on this item or null
	 */
	public UUID getUniqueId() {
		if (item == null || !item.hasItemMeta())
			return null;
		String uuid = item.getItemMeta().getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
		return uuid == null ? null : UUID.fromString(uuid);
	}
	/**
	 * Refresh this base item's lore with the default UIFramework lore format (Will just call the assemble lore function for this base class).
	 */
	public void refreshItemLore() {
		getType().getBuilder().assembleLore(this);
	}
}
