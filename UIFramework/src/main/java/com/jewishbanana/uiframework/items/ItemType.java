package com.jewishbanana.uiframework.items;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability.Action;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.ItemBuilder;
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
	
	public Map<Ability.Action, Queue<Ability>> abilityMap = new HashMap<>();
	public List<Recipe> recipes = new ArrayList<>();
	
	private ItemType(Class<? extends GenericItem> instance, int ID) {
		this.instance = instance;
		this.ID = ID;
		
		try {
			this.item = instance.getDeclaredConstructor(ItemStack.class).newInstance(new ItemStack(Material.AIR)).setId(this).createItem().attachID(ID).build();
			this.durability = item.getItem().getType().getMaxDurability();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static ItemType registerItem(String name, Class<? extends GenericItem> instance) {
		if (itemsMap.containsKey(name))
			throw new IllegalArgumentException("[UIFramework]: Cannot register item '"+name+"' as an item with that name is already registered!");
		if (UIFramework.dataFile.contains("item."+name)) {
			int id = UIFDataUtils.getDataFileInt("item."+name+".id");
			ItemType type = null;
			if (registeredIdList.contains(id)) {
				for (int i=0; i < 999999; i++)
					if (!registeredIdList.contains(i)) {
						type = new ItemType(instance, i);
						type.dataPath = "item."+name;
						type.registeredName = name;
						itemsMap.put(name, type);
						registeredIdList.add(i);
						UIFramework.dataFile.set("item."+name+".id", i);
						id = i;
						break;
					}
				UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR while registering item &e'"+name+"' &can item with that ID already exists! Rebounding the items ID to "+id+". All currently existing instances of this item will no longer work."));
				return type;
			}
			type = new ItemType(instance, id);
			type.dataPath = "item."+name;
			type.registeredName = name;
			itemsMap.put(name, type);
			registeredIdList.add(id);
			return type;
		}
		for (int i=0; i < 999999; i++)
			if (!registeredIdList.contains(i) && !preIdList.contains(i)) {
				ItemType type = new ItemType(instance, i);
				type.dataPath = "item."+name;
				type.registeredName = name;
				itemsMap.put(name, type);
				registeredIdList.add(i);
				UIFramework.dataFile.createSection("item."+name);
				UIFramework.dataFile.set("item."+name+".id", i);
				return type;
			}
		throw new IllegalArgumentException("[UIFramework]: Cannot register item '"+name+"' as no ID in the range 0-999999 is available!");
	}
	public static void registerDefaults() {
		for (String s : UIFramework.dataFile.getConfigurationSection("item").getKeys(false))
			if (UIFramework.dataFile.contains("item."+s+".recipes"))
				for (String path : UIFramework.dataFile.getConfigurationSection("item."+s+".recipes").getKeys(false))
					recipeNames.put(path, Pair.of(s, UIFramework.dataFile.getConfigurationSection("item.recipes."+path)));
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> recipeNames.forEach((k, v) -> {
			ItemType type = getItemType(v.getFirst());
			if (type != null)
				addRecipe(type, UIFDataUtils.createRecipeFromSection(v.getSecond(), type.getBuilder().getItem(), new NamespacedKey(plugin, k)));
		}), 1);
	}
	public static void registerRecipe(ItemType type, ShapedRecipe recipe) {
		addRecipe(type, new ShapedRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), recipe.getResult()));
	}
	public static void registerRecipe(ItemType type, ShapelessRecipe recipe) {
		addRecipe(type, new ShapelessRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), recipe.getResult()));
	}
	private static <T extends Recipe & Keyed> void addRecipe(ItemType type, T recipe) {
		if (!itemsMap.containsValue(type))
			throw new IllegalArgumentException("[UIFramework]: Cannot register recipe because the item is not registered itself!");
		if (plugin.getServer().getRecipe(recipe.getKey()) == null) {
			type.recipes.add(recipe);
			plugin.getServer().addRecipe(recipe);
			if (!UIFramework.dataFile.contains(type.dataPath+".recipes."+recipe.getKey().getKey())) {
				if (!UIFramework.dataFile.contains(type.dataPath+".recipes"))
					UIFramework.dataFile.createSection(type.dataPath+".recipes");
				UIFramework.dataFile.createSection(type.dataPath+".recipes."+recipe.getKey().getKey());
				UIFDataUtils.writeRecipeToSection(UIFramework.dataFile, recipe, type.dataPath+".recipes."+recipe.getKey().getKey());
			}
		}
	}
	public static void cleanAbilities() {
		itemsMap.values().forEach(i -> i.abilityMap.forEach((k, v) -> v.stream().forEach(q -> q.clean())));
	}
	public static ItemType getItemType(String name) {
		return itemsMap.get(name);
	}
	public static ItemType getByID(int id) {
		for (ItemType item : itemsMap.values())
			if (item.getID() == id)
				return item;
		return null;
	}
	public GenericItem createNewInstance(ItemStack item) {
		try {
			return instance.getDeclaredConstructor(ItemStack.class).newInstance(item).setId(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public boolean simulateAction(Ability.Action action, Event event, GenericItem base) {
		if (abilityMap.containsKey(action)) {
			if (event instanceof PlayerInteractEvent)
				for (Ability ability : abilityMap.get(action))
					ability.interacted((PlayerInteractEvent) event, base);
			else if (event instanceof EntityDamageByEntityEvent)
				for (Ability ability : abilityMap.get(action))
					if (action == Action.HIT_ENTITY)
						ability.hitEntity((EntityDamageByEntityEvent) event, base);
					else
						ability.wasHit((EntityDamageByEntityEvent) event, base);
			else if (event instanceof ProjectileHitEvent)
				for (Ability ability : abilityMap.get(action))
					ability.projectileHit((ProjectileHitEvent) event, base);
			else if (event instanceof EntityShootBowEvent)
				for (Ability ability : abilityMap.get(action))
					ability.shotBow((EntityShootBowEvent) event, base);
			else
				return false;
			return true;
		}
		return false;
	}
	public void addAbility(Action action, Ability ability) {
		if (abilityMap.containsKey(action))
			abilityMap.get(action).add(ability);
		else
			abilityMap.put(action, new ArrayDeque<>(Arrays.asList(ability)));
	}
	public static void addPreId(int id) {
		preIdList.add(id);
	}
	public static Map<String, ItemType> getItemsMap() {
		return itemsMap;
	}
	public Class<? extends GenericItem> getInstance() {
		return instance;
	}
	public void setInstance(Class<? extends GenericItem> instance) {
		this.instance = instance;
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
	public void setDamage(double damage) {
		this.damage = damage;
	}
	public double getAttackSpeed() {
		return attackSpeed;
	}
	public void setAttackSpeed(double attackSpeed) {
		this.attackSpeed = attackSpeed;
	}
	public double getDurability() {
		return durability;
	}
	public void setDurability(double durability) {
		this.durability = durability;
	}
	public List<String> getLore() {
		return lore;
	}
	public void setLore(List<String> lore) {
		this.lore = lore;
	}
	public String getRegisteredName() {
		return registeredName;
	}
}
