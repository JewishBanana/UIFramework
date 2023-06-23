package com.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.items.Ability.Action;
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
	
	public GenericItem(ItemStack item) {
		this.item = item;
	}
	public void interacted(PlayerInteractEvent event) {
		switch (event.getAction()) {
		case LEFT_CLICK_BLOCK:
			id.simulateAction(Ability.Action.LEFT_CLICK_BLOCK, event, this);
			if(event.getPlayer().isSneaking())
				id.simulateAction(Ability.Action.SHIFT_LEFT_CLICK_BLOCK, event, this);
		case LEFT_CLICK_AIR:
			id.simulateAction(Ability.Action.LEFT_CLICK, event, this);
			if(event.getPlayer().isSneaking())
				id.simulateAction(Ability.Action.SHIFT_LEFT_CLICK, event, this);
			break;
		case RIGHT_CLICK_BLOCK:
			id.simulateAction(Ability.Action.RIGHT_CLICK_BLOCK, event, this);
			if(event.getPlayer().isSneaking())
				id.simulateAction(Ability.Action.SHIFT_RIGHT_CLICK_BLOCK, event, this);
		case RIGHT_CLICK_AIR:
			id.simulateAction(Ability.Action.RIGHT_CLICK, event, this);
			if(event.getPlayer().isSneaking())
				id.simulateAction(Ability.Action.SHIFT_RIGHT_CLICK, event, this);
			break;
		default:
			break;
		}
	}
	public void hitEntity(EntityDamageByEntityEvent event) {
		double damage = id.getDamage();
		if (event.getDamager() instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) event.getDamager();
			if (entity.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE))
				damage += entity.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier() * 3.0;
			if (event.getEntity() instanceof LivingEntity && entity.getFallDistance() > 0.0 && !entity.isOnGround() && !entity.isClimbing() && !entity.isInWater() && !entity.hasPotionEffect(PotionEffectType.BLINDNESS) && !entity.hasPotionEffect(PotionEffectType.SLOW_FALLING)
					&& !entity.isInsideVehicle() && !(entity instanceof Player && (((Player) entity).isSprinting() || ((Player) entity).getAttackCooldown() < 0.9)))
				damage *= 1.5;
			damage += UIFUtils.getEnchantDamage(item, (event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null));
		}
		event.setDamage(damage);
		id.simulateAction(Action.HIT_ENTITY, event, this);
	}
	public void wasHit(EntityDamageByEntityEvent event) {
		id.simulateAction(Action.WAS_HIT, event, this);
	}
	public void projectileHit(ProjectileHitEvent event) {
		id.simulateAction(Action.PROJECTILE_HIT, event, this);
	}
	public void shotBow(EntityShootBowEvent event) {
		id.simulateAction(Action.SHOT_BOW, event, this);
	}
	public abstract ItemBuilder createItem();
	
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
	public static void removeBaseItem(ItemStack item) {
		itemMap.remove(item);
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
}
