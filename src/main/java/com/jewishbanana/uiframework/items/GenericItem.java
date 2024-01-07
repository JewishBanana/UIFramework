package com.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability.Action;
import com.jewishbanana.uiframework.listeners.AbilityListener;
import com.jewishbanana.uiframework.utils.ItemBuilder;
import com.jewishbanana.uiframework.utils.UIFUtils;

public abstract class GenericItem {
	
	private static Map<ItemStack, GenericItem> itemMap = new HashMap<>();
	
	public static NamespacedKey generalKey;
	static {
		generalKey = new NamespacedKey(UIFramework.getInstance(), "ui-key");
	}
	protected ItemType id;
	protected ItemStack item;
	protected Map<String, ItemField> fields = new HashMap<>();
	
	public GenericItem(ItemStack item) {
		this.item = item;
	}
	public boolean interacted(PlayerInteractEvent event) {
		boolean flag = false;
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			if (id.simulateAction(Ability.Action.LEFT_CLICK_BLOCK, event, this))
				flag = true;
			if(event.getPlayer().isSneaking())
				if (id.simulateAction(Ability.Action.SHIFT_LEFT_CLICK_BLOCK, event, this))
					flag = true;
		case LEFT_CLICK_AIR:
			if (id.simulateAction(Ability.Action.LEFT_CLICK, event, this))
				flag = true;
			if(event.getPlayer().isSneaking())
				if (id.simulateAction(Ability.Action.SHIFT_LEFT_CLICK, event, this))
					flag = true;
			return flag;
		case RIGHT_CLICK_BLOCK:
			if (id.simulateAction(Ability.Action.RIGHT_CLICK_BLOCK, event, this))
				flag = true;
			if(event.getPlayer().isSneaking())
				if (id.simulateAction(Ability.Action.SHIFT_RIGHT_CLICK_BLOCK, event, this))
					flag = true;
		case RIGHT_CLICK_AIR:
			if (id.simulateAction(Ability.Action.RIGHT_CLICK, event, this))
				flag = true;
			if(event.getPlayer().isSneaking())
				if (id.simulateAction(Ability.Action.SHIFT_RIGHT_CLICK, event, this))
					flag = true;
			return flag;
		default:
			return flag;
		}
	}
	public boolean interactedEntity(PlayerInteractEntityEvent event) {
		return id.simulateAction(Action.INTERACT_ENTITY, event, this);
	}
	public boolean hitEntity(EntityDamageByEntityEvent event) {
		double damage = id.getDamage();
		if (damage > 0.0) {
			if (event.getDamager() instanceof LivingEntity) {
				LivingEntity entity = (LivingEntity) event.getDamager();
				double enchantDamage = UIFUtils.getEnchantDamage(item, (event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null));
				if (entity.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE))
					damage += entity.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier() * 3.0;
				if (event.getDamager() instanceof Player) {
					double cooldown = ((Player) event.getDamager()).getAttackCooldown();
					damage *= 0.2 + ((cooldown * cooldown) * 0.8);
					damage += (enchantDamage * cooldown);
				}
				if (event.getEntity() instanceof LivingEntity && entity.getFallDistance() > 0.0 && !entity.isOnGround() && !entity.isClimbing() && !entity.isInWater() && !entity.hasPotionEffect(PotionEffectType.BLINDNESS) && !entity.hasPotionEffect(PotionEffectType.SLOW_FALLING)
						&& !entity.isInsideVehicle() && !(entity instanceof Player && (((Player) entity).isSprinting() || ((Player) entity).getAttackCooldown() < 0.9)))
					damage *= 1.5;
			}
			event.setDamage(damage);
		}
		return id.simulateAction(Action.HIT_ENTITY, event, this);
	}
	public boolean wasHit(EntityDamageByEntityEvent event) {
		return id.simulateAction(Action.WAS_HIT, event, this);
	}
	public boolean projectileThrown(ProjectileLaunchEvent event) {
		return id.simulateAction(Action.WAS_THROWN, event, this);
	}
	public boolean projectileHit(ProjectileHitEvent event) {
		return id.simulateAction(Action.PROJECTILE_HIT, event, this);
	}
	public boolean hitByProjectile(ProjectileHitEvent event) {
		return id.simulateAction(Action.HIT_BY_PROJECTILE, event, this);
	}
	public boolean shotBow(EntityShootBowEvent event) {
		return id.simulateAction(Action.SHOT_BOW, event, this);
	}
	public boolean inventoryClick(InventoryClickEvent event) {
		return id.simulateAction(Action.INVENTORY_CLICK, event, this);
	}
	public boolean consumeItem(PlayerItemConsumeEvent event) {
		return id.simulateAction(Action.CONSUME, event, this);
	}
	public boolean dropItem(EntityDropItemEvent event) {
		return id.simulateAction(Action.DROP_ITEM, event, this);
	}
	public boolean pickupItem(EntityPickupItemEvent event) {
		return id.simulateAction(Action.PICKUP_ITEM, event, this);
	}
	public boolean entityDeath(EntityDeathEvent event) {
		return id.simulateAction(Action.ENTITY_DEATH, event, this);
	}
	public boolean entityRespawn(PlayerRespawnEvent event) {
		return id.simulateAction(Action.ENTITY_RESPAWN, event, this);
	}
	/**
	 * Creates the ItemBuilder for the custom item.
	 * <p>
	 * <STRONG>This method is required in every items class. You should utilize the ItemBuilder create method to construct your ItemStack.</STRONG>
	 * 
	 * @return The created ItemBuilder
	 * 
	 * @see ItemBuilder#create(ItemStack)
	 */
	public abstract ItemBuilder createItem();
	
	public void stripFields(ItemMeta meta) {
	}
	
	/**
	 * Gets the GenericItem base class of the given ItemStack. Will create a new GenericItem base if one does not already exist.
	 * 
	 * @param item The ItemStack of the base class
	 * @return The ItemStack's base class
	 */
	public static GenericItem getItemBase(ItemStack item) {
		if (itemMap.containsKey(item))
			return GenericItem.itemMap.get(item);
		else {
			if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(GenericItem.generalKey, PersistentDataType.INTEGER)) {
				try {
					GenericItem base = ItemType.getByID(item.getItemMeta().getPersistentDataContainer().get(GenericItem.generalKey, PersistentDataType.INTEGER)).createNewInstance(item);
					itemMap.put(item, base);
					return base;
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			}
			return null;
		}
	}
	/**
	 * Remove any item base associated with the ItemStack converting it to a normal item
	 * 
	 * @param item The ItemStack to detach from
	 */
	public static void removeBaseItem(ItemStack item) {
		itemMap.remove(item);
	}
	/**
	 * Assign a projectile to this item making it the shooter. Will run events from this ability when landing.
	 * 
	 * @param projectile The projectile entity
	 */
	public void assignProjectile(Projectile projectile) {
		AbilityListener.assignProjectile(projectile.getUniqueId(), this);
	}
	/**
	 * Remove a projectile from this items ability listeners.
	 * 
	 * @param uuid The unique id of the projectile to remove
	 */
	public void removeProjectile(UUID uuid) {
		AbilityListener.removeProjectile(uuid);
	}
	public ItemType getId() {
		return id;
	}
	public GenericItem setId(ItemType id) {
		this.id = id;
		return this;
	}
	public ItemStack getItem() {
		return item;
	}
	/**
	 * Registers the ItemStack to this base item class. Will swap with the current ItemStack if applicable.
	 * 
	 * @param item The ItemStack to register to
	 * @return This base class
	 */
	public GenericItem setItem(ItemStack item) {
		Iterator<Entry<ItemStack, GenericItem>> it = itemMap.entrySet().iterator();
		while (it.hasNext())
			if (it.next().getKey().equals(this.item)) {
				it.remove();
				break;
			}
		this.item = item;
		itemMap.put(item, this);
		return this;
	}
	public Map<String, ItemField> getFields() {
		return fields;
	}
	public void setFields(Map<String, ItemField> fields) {
		this.fields = fields;
	}
}
