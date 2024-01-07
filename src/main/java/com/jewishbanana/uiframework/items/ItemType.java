package com.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability.Action;
import com.jewishbanana.uiframework.utils.AnvilRecipe;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;
import com.mojang.datafixers.util.Pair;

public class ItemType {

	private static UIFramework plugin;
	static {
		plugin = UIFramework.getInstance();
	}
	private static Map<String, ItemType> itemsMap = new LinkedHashMap<>();
	private static Set<Integer> registeredIdList = new HashSet<>();
	private static Set<Integer> preIdList = new HashSet<>();
	private static Map<String, Pair<String, ConfigurationSection>> recipeNames = new HashMap<>();
	
	private Class<? extends GenericItem> instance;
	private int ID;
	private ItemBuilder item;
	private String dataPath, registeredName;
	private double damage;
	private double attackSpeed;
	private double durability;
	private List<String> lore = new ArrayList<>();
	private boolean allowVanillaCrafts;
	
	public Map<Ability, Set<Ability.Action>> abilityMap = new LinkedHashMap<>();
	public List<Recipe> recipes = new ArrayList<>();
	public List<AnvilRecipe> anvilRecipes = new ArrayList<>();
	
	private ItemType(Class<? extends GenericItem> instance, int ID, String registeredName) {
		this.instance = instance;
		this.ID = ID;
		this.registeredName = registeredName;
		
		try {
			this.item = instance.getDeclaredConstructor(ItemStack.class).newInstance(new ItemStack(Material.DIAMOND_SWORD)).setId(this).createItem().attachID(ID).build();
			this.durability = item.getItem().getType().getMaxDurability();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * Register a new custom item to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins</i>
	 * 
	 * @param name The unique registered name of your item
	 * @param instance The associated class of your custom item
	 * @return The ItemType instance created
	 */
	public static ItemType registerItem(String name, Class<? extends GenericItem> instance) {
		if (itemsMap.containsKey(name))
			throw new IllegalArgumentException("[UIFramework]: Cannot register item '"+name+"' as an item with that name is already registered!");
		if (UIFramework.dataFile.contains("item."+name)) {
			int id = UIFDataUtils.getDataFileInt("item."+name+".id");
			ItemType type = null;
			if (registeredIdList.contains(id)) {
				for (int i=0; i < 999999; i++)
					if (!registeredIdList.contains(i)) {
						type = new ItemType(instance, i, name);
						type.dataPath = "item."+name;
						itemsMap.put(name, type);
						registeredIdList.add(i);
						UIFramework.dataFile.set("item."+name+".id", i);
						id = i;
						break;
					}
				UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR while registering item &e'"+name+"' &can item with that ID already exists! Rebounding the items ID to "+id+". All currently existing instances of this item will no longer work."));
				return type;
			}
			type = new ItemType(instance, id, name);
			type.dataPath = "item."+name;
			itemsMap.put(name, type);
			registeredIdList.add(id);
			return type;
		}
		for (int i=0; i < 999999; i++)
			if (!registeredIdList.contains(i) && !preIdList.contains(i)) {
				ItemType type = new ItemType(instance, i, name);
				type.dataPath = "item."+name;
				itemsMap.put(name, type);
				registeredIdList.add(i);
				UIFramework.dataFile.createSection("item."+name);
				UIFramework.dataFile.set("item."+name+".id", i);
				return type;
			}
		throw new IllegalArgumentException("[UIFramework]: Cannot register item '"+name+"' as no ID in the range 0-999999 is available!");
	}
	/**
	 * <STRONG>For internal use only!</STRONG>
	 */
	public static void registerDefaults() {
		for (String s : UIFramework.dataFile.getConfigurationSection("item").getKeys(false))
			if (UIFramework.dataFile.contains("item."+s+".recipes"))
				for (String path : UIFramework.dataFile.getConfigurationSection("item."+s+".recipes").getKeys(false))
					recipeNames.put(path, Pair.of(s, UIFramework.dataFile.getConfigurationSection("item."+s+".recipes."+path)));
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> recipeNames.forEach((k, v) -> {
			ItemType type = getItemType(v.getFirst());
			if (type != null)
				type.addRecipe(UIFDataUtils.createRecipeFromSection(v.getSecond(), type.getBuilder().getItem(), new NamespacedKey(plugin, k)));
		}), 1);
	}
	/**
	 * Register a ShapedRecipe for this ItemType
	 * 
	 * @param recipe The recipe to add
	 */
	public void registerRecipe(ShapedRecipe recipe) {
		ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), recipe.getResult());
		shapedRecipe.shape(recipe.getShape());
		recipe.getChoiceMap().forEach((k, v) -> shapedRecipe.setIngredient(k, v));
		addRecipe(shapedRecipe);
	}
	/**
	 * Register a ShapelessRecipe for this ItemType
	 * 
	 * @param recipe The recipe to add
	 */
	public void registerRecipe(ShapelessRecipe recipe) {
		ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), recipe.getResult());
		recipe.getChoiceList().forEach(k -> shapelessRecipe.addIngredient(k));
		addRecipe(shapelessRecipe);
	}
	/**
	 * Register a AnvilRecipe for this ItemType
	 * 
	 * @param recipe The recipe to add
	 */
	public void registerRecipe(AnvilRecipe recipe) {
		this.anvilRecipes.add(recipe);
	}
	private <T extends Recipe & Keyed> void addRecipe(T recipe) {
		if (!itemsMap.containsValue(this))
			throw new IllegalArgumentException("[UIFramework]: Cannot register recipe because the ItemType is not registered!");
		if (plugin.getServer().getRecipe(recipe.getKey()) == null) {
			this.recipes.add(recipe);
			plugin.getServer().addRecipe(recipe);
			if (!UIFramework.dataFile.contains(this.dataPath+".recipes."+recipe.getKey().getKey())) {
				if (!UIFramework.dataFile.contains(this.dataPath+".recipes"))
					UIFramework.dataFile.createSection(this.dataPath+".recipes");
				UIFramework.dataFile.createSection(this.dataPath+".recipes."+recipe.getKey().getKey());
				UIFDataUtils.writeRecipeToSection(UIFramework.dataFile, recipe, this.dataPath+".recipes."+recipe.getKey().getKey());
			}
		}
	}
	/**
	 * Runs the clean method on all abilities for all currently registered and active abilities.
	 * <p>
	 * <STRONG>This method is handled automatically by UIFramework for server restarts/reloads</STRONG>
	 */
	public static void cleanAbilities() {
		itemsMap.values().forEach(i -> i.abilityMap.keySet().forEach(k -> k.clean()));
	}
	/**
	 * Get an ItemType by its registered name.
	 * 
	 * @param name The registered name of the item
	 * @return The ItemType instance or null
	 */
	public static ItemType getItemType(String name) {
		return itemsMap.get(name);
	}
	/**
	 * Get an ItemType by its registerd unique id
	 * 
	 * @param id The unique id of the ItemType
	 * @return The ItemType instance or null
	 */
	public static ItemType getByID(int id) {
		for (ItemType item : itemsMap.values())
			if (item.getID() == id)
				return item;
		return null;
	}
	/**
	 * Creates a new GenericItem instance of this ItemType and attaches it to the given ItemStack.
	 * 
	 * @param item The ItemStack to bind the GenericItem class to
	 * @return The created GenericItem instance
	 */
	public GenericItem createNewInstance(ItemStack item) {
		try {
			return instance.getDeclaredConstructor(ItemStack.class).newInstance(item).setId(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public boolean simulateAction(Action action, PlayerInteractEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().interacted(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, PlayerInteractEntityEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().interactedEntity(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, EntityDamageByEntityEvent event, GenericItem base) {
		boolean flag = false;
		if (action == Action.HIT_ENTITY) {
			for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
				if (entry.getValue().contains(action))
					if (entry.getKey().hitEntity(event, base))
						flag = true;
		} else {
			for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
				if (entry.getValue().contains(action))
					if (entry.getKey().wasHit(event, base))
						flag = true;
		}
		return flag;
	}
	public boolean simulateAction(Action action, ProjectileLaunchEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().projectileThrown(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, ProjectileHitEvent event, GenericItem base) {
		boolean flag = false;
		if (action == Action.PROJECTILE_HIT) {
			for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
				if (entry.getValue().contains(action))
					if (entry.getKey().projectileHit(event, base))
						flag = true;
		} else {
			for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
				if (entry.getValue().contains(action))
					if (entry.getKey().hitByProjectile(event, base))
						flag = true;
		}
		return flag;
	}
	public boolean simulateAction(Action action, EntityShootBowEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().shotBow(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, InventoryClickEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().inventoryClick(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, PlayerItemConsumeEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().consumeItem(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, EntityDropItemEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().dropItem(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, EntityPickupItemEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().pickupItem(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, EntityDeathEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().entityDeath(event, base))
					flag = true;
		return flag;
	}
	public boolean simulateAction(Action action, PlayerRespawnEvent event, GenericItem base) {
		boolean flag = false;
		for (Entry<Ability, Set<Action>> entry : abilityMap.entrySet())
			if (entry.getValue().contains(action))
				if (entry.getKey().entityRespawn(event, base))
					flag = true;
		return flag;
	}
	/**
	 * Bind the given ability to the ItemType with the given action. Actions will stack for abilities.
	 * 
	 * @param action The action that will trigger the ability
	 * @param ability The ability that will activate
	 */
	public void addAbility(Action action, Ability ability) {
		if (abilityMap.containsKey(ability))
			abilityMap.get(ability).add(action);
		else
			abilityMap.put(ability, new HashSet<>(Arrays.asList(action)));
	}
	/**
	 * Bind the given ability to the ItemType with the given actions. Actions will stack for abilities.
	 * 
	 * @param actions The actions that will trigger the ability
	 * @param ability The ability that will activate
	 */
	public void addAbility(Collection<Action> actions, Ability ability) {
		if (abilityMap.containsKey(ability))
			abilityMap.get(ability).addAll(actions);
		else
			abilityMap.put(ability, new HashSet<>(actions));
	}
	public static void addPreId(int id) {
		preIdList.add(id);
	}
	/**
	 * Gets the global map of all custom ItemTypes with their registered names
	 * 
	 * @return The global map of all ItemTypes
	 */
	public static Map<String, ItemType> getItemsMap() {
		return itemsMap;
	}
	public Class<? extends GenericItem> getInstance() {
		return instance;
	}
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public ItemBuilder getBuilder() {
		return item;
	}
	public String getDataPath() {
		return dataPath;
	}
	public double getDamage() {
		return damage;
	}
	/**
	 * Set the weapon types damage attribute.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param damage The damage value
	 */
	public void setDamage(double damage) {
		this.damage = damage;
	}
	public double getAttackSpeed() {
		return attackSpeed;
	}
	/**
	 * Set the weapon types attack speed attribute.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param attackSpeed The attack speed value
	 */
	public void setAttackSpeed(double attackSpeed) {
		this.attackSpeed = attackSpeed;
	}
	public double getDurability() {
		return durability;
	}
	/**
	 * Set the weapon types max durability attribute.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param durability The max durability value
	 */
	public void setDurability(double durability) {
		this.durability = durability;
	}
	public List<String> getLore() {
		return lore;
	}
	/**
	 * Set the defualt included lore of the custom item.
	 * <p>
	 * <i>Item lore is automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param lore The list of the lore
	 */
	public void setLore(List<String> lore) {
		this.lore = lore;
	}
	public String getRegisteredName() {
		return registeredName;
	}
	public boolean isAllowVanillaCrafts() {
		return allowVanillaCrafts;
	}
	/**
	 * Set wether to allow your item to be used as an ingrediant in vanilla minecraft recipes.
	 * <p>
	 * <STRONG>Default behavior is to disable vanilla recipes with this item</STRONG>
	 * 
	 * @param allowVanillaCrafts To allow use in vanilla recipes or not
	 */
	public void setAllowVanillaCrafts(boolean allowVanillaCrafts) {
		this.allowVanillaCrafts = allowVanillaCrafts;
	}
}
