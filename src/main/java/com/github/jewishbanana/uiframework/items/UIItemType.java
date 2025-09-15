package com.github.jewishbanana.uiframework.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
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
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.PluginManager;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.events.AbilityTriggerEvent;
import com.github.jewishbanana.uiframework.items.Ability.Action;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.mojang.datafixers.util.Pair;

@SuppressWarnings("deprecation")
public class UIItemType {

	private static final UIFramework plugin;
	private static final Map<String, UIItemType> registry;
	private static final PluginManager manager = Bukkit.getServer().getPluginManager();
	static {
		plugin = UIFramework.getInstance();
		registry = new LinkedHashMap<>();
		
		registry.put("_null", createItemType(GenericItem.class, "_null"));
	}
	
	private Class<? extends GenericItem> instance;
	private ItemBuilder builder;
	private String dataPath, registeredName, displayName;
	private double damage;
	private double attackSpeed;
	private double durability;
	private double projectileDamage;
	private double projectileDamageMultiplier = 1.0;
	private List<String> lore = new ArrayList<>();
	private boolean allowVanillaCrafts = true;
	private boolean allowVanillaEnchanting = true;
	private boolean useLoreFormat = true;
	private ItemCategory itemCategory = ItemCategory.DefaultCategory.MISCELLANEOUS.getItemCategory();
	
	protected Map<Ability, Set<Ability.Action>> abilityMap = new LinkedHashMap<>();
	protected Map<UIEnchantment, Integer> enchants = new LinkedHashMap<>();
	
	private List<Recipe> recipes = new ArrayList<>();
	private List<Recipe> usedRecipes = new ArrayList<>();
	
	private UIItemType(Class<? extends GenericItem> instance, String registeredName) {
		this.instance = instance;
		this.registeredName = registeredName;
	}
	private static UIItemType createItemType(Class<? extends GenericItem> instance, String registeredName) {
		UIItemType type = new UIItemType(instance, registeredName);
		try {
			GenericItem base = instance.getDeclaredConstructor(ItemStack.class).newInstance(new ItemStack(Material.EGG));
			type.builder = base.setType(type).createItem().attachID(registeredName).build();
			type.durability = type.builder.getItem().getType().getMaxDurability();
			type.itemCategory = base.getItemCategory();
			return type;
		} catch (Exception e) {
			e.printStackTrace();
			UIFramework.consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: An error has occurred above this message while trying to register custom item &d"+registeredName+" &e! This is NOT a UIFramework bug! Report this to the proper plugin author(s) of the related item."));
			return null;
		}
	}
	/**
	 * Register a new custom item to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins (e.g. uif-recipe_book)</i>
	 * 
	 * @param name The unique registered name of your item
	 * @param instance The associated class of your custom item
	 * @param silentFail If this should not throw an exception in case of already existing registry
	 * @return The ItemType instance created (or null if silent failed)
	 */
	public static UIItemType registerItem(String name, Class<? extends GenericItem> instance, boolean silentFail) {
		if (registry.containsKey(name)) {
			if (silentFail)
				return null;
			throw new IllegalArgumentException("[UIFramework]: Cannot register item with name '"+name+"' as an item with that name is already registered!");
		}
		if (!UIFramework.dataFile.contains("item."+name))
			UIFramework.dataFile.createSection("item."+name);
		UIItemType type = createItemType(instance, name);
		if (type == null)
			return null;
		type.dataPath = "item."+name;
		registry.put(name, type);
		return type;
	}
	/**
	 * Register a new custom item to UIFramework. <STRONG>This must be done every time your plugin starts!</STRONG>
	 * <p>
	 * <i>It is good practice to make your registered name start with a prefix of your plugins name to avoid any potential conflicts with other plugins (e.g. uif:recipe_book)</i>
	 * 
	 * @param name The unique registered name of your item
	 * @param instance The associated class of your custom item
	 * @return The ItemType instance created
	 */
	public static UIItemType registerItem(String name, Class<? extends GenericItem> instance) {
		return registerItem(name, instance, false);
	}
	/**
	 * @deprecated <STRONG>For internal use only!</STRONG>
	 */
	@Deprecated
	public static void registerDefaults() {
		Map<String, Pair<String, ConfigurationSection>> recipeNames = new LinkedHashMap<>();
		for (String s : UIFramework.dataFile.getConfigurationSection("item").getKeys(false)) {
			ConfigurationSection section = UIFramework.dataFile.getConfigurationSection("item."+s+".recipes");
			if (section != null)
				for (String path : section.getKeys(false))
					recipeNames.put(path, Pair.of(s, section.getConfigurationSection(path)));
		}
		plugin.getServer().getScheduler().runTaskLater(plugin, () -> recipeNames.forEach((k, v) -> {
			UIItemType type = getItemType(v.getFirst());
			if (type != null) {
				GenericItem base = GenericItem.getItemBaseNoID(type.getItem());
				type.addRecipe(UIFDataUtils.createRecipeFromSection(v.getSecond(), base.getItem(), new NamespacedKey(plugin, k)));
			}
		}), 1);
	}
	/**
	 * Register a ShapedRecipe for this ItemType
	 * 
	 * @param recipe The recipe to add
	 */
	public void registerRecipe(ShapedRecipe recipe) {
		if (recipe == null)
			return;
		GenericItem base = GenericItem.getItemBaseNoID(builder.getItem());
		enchants.forEach((k, v) -> k.loadEnchant(base));
		base.refreshItemLore();
		ShapedRecipe shapedRecipe = new ShapedRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), base.getItem());
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
		if (recipe == null)
			return;
		GenericItem base = GenericItem.getItemBaseNoID(builder.getItem());
		enchants.forEach((k, v) -> k.loadEnchant(base));
		base.refreshItemLore();
		ShapelessRecipe shapelessRecipe = new ShapelessRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), base.getItem());
		recipe.getChoiceList().forEach(k -> shapelessRecipe.addIngredient(k));
		addRecipe(shapelessRecipe);
	}
	/**
	 * Register an AnvilRecipe for this ItemType
	 * 
	 * @param recipe The recipe to add
	 */
	public void registerRecipe(AnvilRecipe recipe) {
		if (recipe == null)
			return;
		GenericItem base = GenericItem.getItemBaseNoID(builder.getItem());
		enchants.forEach((k, v) -> k.loadEnchant(base));
		base.refreshItemLore();
		AnvilRecipe anvilRecipe = recipe.getResult() != null ? new AnvilRecipe(new NamespacedKey(plugin, recipe.getKey().getKey()), recipe.getAnvilChoice(), base.getItem(), recipe.getLevelCost()) : recipe;
		addRecipe(anvilRecipe);
	}
	/**
	 * Updates this resulting item in all of its crafting recipes. Call this if you modify the item after registration in your plugin
	 * <p>
	 * <STRONG>This is automatically handled if the user reloads through UIFramework!</STRONG>
	 */
	public void updateRecipeResults() {
		List<Recipe> newList = new ArrayList<>(recipes);
		recipes.clear();
		for (Recipe recipe : newList) {
			plugin.getServer().removeRecipe(((Keyed) recipe).getKey());
			if (recipe instanceof ShapedRecipe temp)
				registerRecipe(temp);
			else if (recipe instanceof ShapelessRecipe temp)
				registerRecipe(temp);
			else if (recipe instanceof AnvilRecipe temp)
				registerRecipe(temp);
		}
	}
	private <T extends Recipe & Keyed> void addRecipe(T recipe) {
		if (!registry.containsValue(this))
			throw new IllegalArgumentException("[UIFramework]: Cannot register recipe because the ItemType is not registered!");
		if (plugin.getServer().getRecipe(recipe.getKey()) != null)
			return;
		if (UIFramework.dataFile.contains(this.dataPath+".removed_recipes") && UIFramework.dataFile.getStringList(this.dataPath+".removed_recipes").contains(recipe.getKey().getKey()))
			return;
		this.recipes.add(recipe);
		boolean writeRecipe = true;
		if (recipe instanceof ShapedRecipe) {
			((ShapedRecipe) recipe).getChoiceMap().forEach((k, v) -> {
				if (v == null)
					return;
				GenericItem tempBase = GenericItem.getItemBaseNoID(v.getItemStack());
				if (tempBase != null && !tempBase.getType().usedRecipes.contains(recipe))
					tempBase.getType().usedRecipes.add(recipe);
			});
			plugin.getServer().addRecipe(recipe);
		} else if (recipe instanceof ShapelessRecipe) {
			((ShapelessRecipe) recipe).getChoiceList().forEach(k -> {
				GenericItem tempBase = GenericItem.getItemBaseNoID(k.getItemStack());
				if (tempBase != null && !tempBase.getType().usedRecipes.contains(recipe))
					tempBase.getType().usedRecipes.add(recipe);
			});
			plugin.getServer().addRecipe(recipe);
		} else if (recipe instanceof AnvilRecipe) {
			AnvilRecipe anvilRecipe = (AnvilRecipe) recipe;
			if (anvilRecipe.getAnvilChoice().getFirstSlot() instanceof RecipeChoice.ExactChoice)
				((RecipeChoice.ExactChoice) anvilRecipe.getAnvilChoice().getFirstSlot()).getChoices().forEach(k -> {
					GenericItem tempBase = GenericItem.getItemBaseNoID(k);
					if (tempBase != null && !tempBase.getType().usedRecipes.contains(recipe))
						tempBase.getType().usedRecipes.add(recipe);
				});
			if (anvilRecipe.getAnvilChoice().getSecondSlot() instanceof RecipeChoice.ExactChoice)
				((RecipeChoice.ExactChoice) anvilRecipe.getAnvilChoice().getSecondSlot()).getChoices().forEach(k -> {
					GenericItem tempBase = GenericItem.getItemBaseNoID(k);
					if (tempBase != null && !tempBase.getType().usedRecipes.contains(recipe))
						tempBase.getType().usedRecipes.add(recipe);
				});
			if (anvilRecipe.getResult() == null && !anvilRecipe.isRepair())
				writeRecipe = false;
			UIFramework.registerAnvilRecipe(anvilRecipe);
		}
		if (writeRecipe && !UIFramework.dataFile.contains(this.dataPath+".recipes."+recipe.getKey().getKey().toString())) {
			if (!UIFramework.dataFile.contains(this.dataPath+".recipes"))
				UIFramework.dataFile.createSection(this.dataPath+".recipes");
			UIFDataUtils.writeRecipeToSection(UIFramework.dataFile.createSection(this.dataPath+".recipes."+recipe.getKey().getKey().toString()), recipe);
		}
	}
	/**
	 * Runs the clean method on all abilities for all currently registered and active abilities.
	 * <p>
	 * <STRONG>This method is handled automatically by UIFramework for server restarts/reloads</STRONG>
	 */
	public static void cleanAbilities() {
		registry.values().forEach(i -> i.abilityMap.keySet().forEach(k -> k.clean()));
	}
	/**
	 * Get an ItemType by its registered name.
	 * 
	 * @param name The registered name of the item
	 * @return The ItemType instance or null
	 */
	public static UIItemType getItemType(String name) {
		return registry.get(name);
	}
	/**
	 * Creates a new GenericItem instance of this ItemType and attaches it to the given ItemStack.
	 * 
	 * @param item The ItemStack to bind the GenericItem class to
	 * @return The created GenericItem instance
	 */
	public GenericItem createNewInstance(ItemStack item) {
		try {
			return instance.getDeclaredConstructor(ItemStack.class).newInstance(item).setType(this).applyDefaultEnchants();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public void simulateAction(Action action, PlayerInteractEvent event, GenericItem base, ActivatedSlot slot, ActivatedSlot hand) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), hand, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().interacted(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), hand, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().interacted(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, PlayerInteractEntityEvent event, GenericItem base, ActivatedSlot slot, ActivatedSlot hand) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), hand, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().interactedEntity(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), hand, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().interactedEntity(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, EntityDamageByEntityEvent event, GenericItem base, ActivatedSlot slot) {
		if (action == Action.HIT_ENTITY) {
			if (!base.uniqueAbilities.isEmpty())
				base.uniqueAbilities.forEach((k, v) -> {
					if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, base)) {
						AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getDamager());
						manager.callEvent(triggerEvent);
						if (!triggerEvent.isCancelled())
							triggerEvent.getAbility().hitEntity(event, triggerEvent.getBaseItem());
					}
				});
			abilityMap.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getDamager());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().hitEntity(event, triggerEvent.getBaseItem());
				}
			});
		} else {
			if (!base.uniqueAbilities.isEmpty())
				base.uniqueAbilities.forEach((k, v) -> {
					if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ARMOR, base)) {
						AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
						manager.callEvent(triggerEvent);
						if (!triggerEvent.isCancelled())
							triggerEvent.getAbility().wasHit(event, triggerEvent.getBaseItem());
					}
				});
			abilityMap.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ARMOR, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().wasHit(event, triggerEvent.getBaseItem());
				}
			});
		}
	}
	public void simulateAction(Action action, ProjectileLaunchEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().projectileThrown(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().projectileThrown(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, ProjectileHitEvent event, GenericItem base, ActivatedSlot slot) {
		if (action == Action.PROJECTILE_HIT) {
			if (!base.uniqueAbilities.isEmpty())
				base.uniqueAbilities.forEach((k, v) -> {
					if (v.contains(action)) {
						AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
						manager.callEvent(triggerEvent);
						if (!triggerEvent.isCancelled())
							triggerEvent.getAbility().projectileHit(event, triggerEvent.getBaseItem());
					}
				});
			abilityMap.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().projectileHit(event, triggerEvent.getBaseItem());
				}
			});
		} else {
			if (!base.uniqueAbilities.isEmpty())
				base.uniqueAbilities.forEach((k, v) -> {
					if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ARMOR, base)) {
						AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getHitEntity());
						manager.callEvent(triggerEvent);
						if (!triggerEvent.isCancelled())
							triggerEvent.getAbility().hitByProjectile(event, triggerEvent.getBaseItem());
					}
				});
			abilityMap.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ARMOR, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getHitEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().hitByProjectile(event, triggerEvent.getBaseItem());
				}
			});
		}
	}
	public void simulateAction(Action action, EntityShootBowEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().shotBow(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().shotBow(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, InventoryClickEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getWhoClicked());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().inventoryClick(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getWhoClicked());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().inventoryClick(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, PlayerItemConsumeEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().consumeItem(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().consumeItem(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, PotionSplashEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPotion());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().splashPotion(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPotion());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().splashPotion(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, EntityDropItemEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().dropItem(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().dropItem(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, EntityPickupItemEvent event, GenericItem base) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().pickupItem(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().pickupItem(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, EntityDeathEvent event, GenericItem base, ActivatedSlot slot) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ANY, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().entityDeath(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ANY, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getEntity());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().entityDeath(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, PlayerRespawnEvent event, GenericItem base, ActivatedSlot slot) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ANY, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().entityRespawn(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(slot, k.getActivatingSlot(), ActivatedSlot.ANY, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().entityRespawn(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, BlockPlaceEvent event, GenericItem base, ActivatedSlot hand) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(hand, k.getActivatingSlot(), hand, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().placeBlock(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(hand, k.getActivatingSlot(), hand, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().placeBlock(event, triggerEvent.getBaseItem());
			}
		});
	}
	public void simulateAction(Action action, BlockBreakEvent event, GenericItem base, ActivatedSlot hand) {
		if (!base.uniqueAbilities.isEmpty())
			base.uniqueAbilities.forEach((k, v) -> {
				if (v.contains(action) && UIFUtils.isActivatingSlot(hand, k.getActivatingSlot(), hand, base)) {
					AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
					manager.callEvent(triggerEvent);
					if (!triggerEvent.isCancelled())
						triggerEvent.getAbility().breakBlock(event, triggerEvent.getBaseItem());
				}
			});
		abilityMap.forEach((k, v) -> {
			if (v.contains(action) && UIFUtils.isActivatingSlot(hand, k.getActivatingSlot(), hand, base)) {
				AbilityTriggerEvent triggerEvent = new AbilityTriggerEvent(k, action, base, event.getPlayer());
				manager.callEvent(triggerEvent);
				if (!triggerEvent.isCancelled())
					triggerEvent.getAbility().breakBlock(event, triggerEvent.getBaseItem());
			}
		});
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
	/**
	 * Gets all the abilities associated with this ItemType.
	 * 
	 * @return The abilities as the key with the actions to activate as the value
	 */
	public Map<Ability, Set<Action>> getAbilities() {
		return abilityMap;
	}
	/**
	 * Puts the enchant on this ItemType meaning that all instances of this item will receive the enchant by default on the item.
	 * 
	 * @param enchant The custom enchant to add
	 * @param level The level of the enchant
	 */
	public void addEnchant(UIEnchantment enchant, int level) {
		enchants.put(enchant, level);
	}
	/**
	 * Gets all the default custom enchants on this ItemType.
	 * 
	 * @return The custom enchants as the key and the level as the value
	 */
	public Map<UIEnchantment, Integer> getEnchants() {
		return enchants;
	}
	/**
	 * Gets the global map of all custom ItemTypes with their registered names
	 * 
	 * @return The global map of all ItemTypes
	 */
	public static Map<String, UIItemType> getRegistry() {
		return registry;
	}
	public Class<? extends GenericItem> getInstance() {
		return instance;
	}
	/**
	 * Get the items builder class which is associated with this ItemType. This is the instance created with {@link GenericItem#createItem()} within your items class.
	 * 
	 * @return This item types builder class
	 */
	public ItemBuilder getBuilder() {
		return builder;
	}
	/**
	 * Creates a new copy of this item type as an ItemStack. Convienence method for getting the builder and creating the item there.
	 * 
	 * @return Copy of this item type as a new Bukkit ItemStack
	 */
	public ItemStack getItem() {
		return builder.getItem();
	}
	/**
	 * Convienence method for getting an ItemStack by the custom items registered name.
	 * 
	 * @param item The registered name of the item to get
	 * @return An ItemStack copy of the custom item or null if it is not registered
	 */
	public static ItemStack getItem(String item) {
		UIItemType type = registry.get(item);
		return type == null ? null : type.getItem();
	}
	/**
	 * Convienence method for getting an ItemStack by the custom items registered class.
	 * 
	 * @param itemClass The registered class of the item to get
	 * @return An ItemStack copy of the custom item or null if it is not registered
	 */
	public static ItemStack getItem(Class<? extends GenericItem> itemClass) {
		for (Entry<String, UIItemType> entry : registry.entrySet())
			if (entry.getValue().instance.equals(itemClass))
				return entry.getValue().getItem();
		return null;
	}
	public String getDataPath() {
		return dataPath;
	}
	public double getDamage() {
		return damage;
	}
	/**
	 * Set the item types melee damage attribute.
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
	 * Set the item types attack speed attribute.
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
	 * Set the weapon types max durability attribute. Setting to -1 will make the item unbreakable.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param durability The max durability value
	 */
	public void setDurability(double durability) {
		this.durability = durability;
	}
	public double getProjectileDamage() {
		return projectileDamage;
	}
	/**
	 * Set the item types projectile damage attribute meaning the damage that this item will do as a projectile.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param projectileDamage The damage value
	 */
	public void setProjectileDamage(double projectileDamage) {
		this.projectileDamage = projectileDamage;
	}
	public double getProjectileDamageMultiplier() {
		return projectileDamageMultiplier;
	}
	/**
	 * Set the item types projectile attack damage multiplier meaning that projectiles fired by this item will multiply the damage on impact by this value.
	 * <p>
	 * <i>Item attributes are automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param projectileDamageMultiplier The multiplier for the projectiles damage
	 */
	public void setProjectileDamageMultiplier(double projectileDamageMultiplier) {
		this.projectileDamageMultiplier = projectileDamageMultiplier;
	}
	public List<String> getLore() {
		return lore;
	}
	/**
	 * Set the default included lore of the custom item.
	 * <p>
	 * <i>Item lore is automatically synced with all currently spawned items of this type on the server</i>
	 * 
	 * @param lore The list of the lore
	 */
	public void setLore(List<String> lore) {
		this.lore = lore;
	}
	/**
	 * Get the registered key name of this ItemType (The key used to register the item with UIFramework).
	 * 
	 * @return This ItemType's registered key
	 */
	public String getRegisteredName() {
		return registeredName;
	}
	/**
	 * Get the display name of this ItemType which is used in game as the defaulted name of this custom item.
	 * 
	 * @return This ItemType's in-game display name on items
	 */
	public String getDisplayName() {
		return displayName;
	}
	/**
	 * Set the in-game display name on items of this ItemType.
	 * 
	 * @param displayName The display name to use
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
	public boolean isAllowVanillaEnchanting() {
		return allowVanillaEnchanting;
	}
	/**
	 * Set wether to allow your item to be enchanted with vanilla enchants in anvils and enchanting tables.
	 * <p>
	 * <STRONG>Default behavior is to allow vanilla enchants</STRONG>
	 * 
	 * @param allowVanillaEnchanting To allow vanilla enchanting of this item type or not
	 */
	public void setAllowVanillaEnchanting(boolean allowVanillaEnchanting) {
		this.allowVanillaEnchanting = allowVanillaEnchanting;
	}
	/**
	 * Gets the recipes registered with this custom item type.
	 * <p>
	 * <STRONG>You should add recipes through the registration methods. Adding recipes directly to this list can break things!</STRONG>
	 * 
	 * @return List of all registered recipes on this item type
	 * 
	 * @see UIItemType#registerRecipe(ShapedRecipe)
	 * @see UIItemType#registerRecipe(ShapelessRecipe)
	 */
	public List<Recipe> getRecipes() {
		return recipes;
	}
	/**
	 * Gets the recipes that have an ingredient of this custom item type.
	 * 
	 * @return List of all registered recipes that use this item as an ingredient
	 */
	public List<Recipe> getUsedRecipes() {
		return usedRecipes;
	}
	/**
	 * Gets the item category that this items type resides in. Item category is only used to determine the order in which the items will appear in the UI recipes menu, adding categories to your items will help users easily navigate UI menus for your item.
	 * <p>
	 * <i>The default category returned is miscellaneous. <STRONG>To change this simply override this method in your items base class</STRONG> and return the desired category in your items class.
	 * You can also create new item categories of your own by changing the value returned (Items are sorted by ascending order of its category value), see the ItemCategory class available values.</i>
	 * 
	 * @return The category of this items type
	 * 
	 * @see GenericItem#getItemCategory()
	 * @see ItemCategory
	 */
	public ItemCategory getItemCategory() {
		return itemCategory != null ? itemCategory : ItemCategory.DefaultCategory.MISCELLANEOUS.getItemCategory();
	}
	/**
	 * Refreshes this ItemType's builder with the default UIFramework lore format. Call this after making changes to this ItemType's meta.
	 * 
	 * @return This ItemType's builder
	 */
	public ItemBuilder refreshItemLore() {
		return getBuilder().assembleLore();
	}
	/**
	 * @return If this item uses the default UIFramework item lore format
	 */
	public boolean doesUseLoreFormat() {
		return useLoreFormat;
	}
	/**
	 * Sets if this item type should use the default UIFramework lore format (By default this is enabled).
	 * 
	 * @param useLoreFormat If this should use UIFrameworks default item lore format
	 */
	public void setUseLoreFormat(boolean useLoreFormat) {
		this.useLoreFormat = useLoreFormat;
	}
}
