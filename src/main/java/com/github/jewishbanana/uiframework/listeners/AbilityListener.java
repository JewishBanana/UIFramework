package com.github.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.items.Ability;
import com.github.jewishbanana.uiframework.items.ActivatedSlot;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.github.jewishbanana.uiframework.utils.VersionUtils;
import com.mojang.datafixers.util.Pair;

public class AbilityListener implements Listener {
	
	private static Map<UUID, Pair<GenericItem, GenericItem>> itemProjectiles = new HashMap<>();
	private static Map<UUID, Pair<Ability, GenericItem>> abilityProjectiles = new HashMap<>();

	public AbilityListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL || event.useItemInHand() == Result.DENY || (event.getClickedBlock() != null && UIFUtils.isInteractable(event.getClickedBlock())))
			return;
		ActivatedSlot hand = event.getHand() == EquipmentSlot.HAND ? ActivatedSlot.MAIN_HAND : ActivatedSlot.OFF_HAND;
		Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(event.getPlayer());
		if (!map.isEmpty())
			map.forEach((k, v) -> {
				boolean flag = k.isAlwaysAllowAbilities();
				if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), hand, k))
					flag = k.interacted(event);
				if (!k.getEnchants().isEmpty())
					k.getEnchants().forEach(enchant -> {
						if (UIFUtils.isActivatingSlot(v, enchant.getActivatingSlot(), hand, k))
							enchant.interacted(event, k);
					});
				if (flag)
					switch (event.getAction()) {
					case LEFT_CLICK_BLOCK:
						k.getType().simulateAction(Ability.Action.LEFT_CLICK_BLOCK, event, k, v, hand);
						if(event.getPlayer().isSneaking())
							k.getType().simulateAction(Ability.Action.SHIFT_LEFT_CLICK_BLOCK, event, k, v, hand);
					case LEFT_CLICK_AIR:
						k.getType().simulateAction(Ability.Action.LEFT_CLICK, event, k, v, hand);
						if(event.getPlayer().isSneaking())
							k.getType().simulateAction(Ability.Action.SHIFT_LEFT_CLICK, event, k, v, hand);
						break;
					case RIGHT_CLICK_BLOCK:
						k.getType().simulateAction(Ability.Action.RIGHT_CLICK_BLOCK, event, k, v, hand);
						if(event.getPlayer().isSneaking())
							k.getType().simulateAction(Ability.Action.SHIFT_RIGHT_CLICK_BLOCK, event, k, v, hand);
					case RIGHT_CLICK_AIR:
						k.getType().simulateAction(Ability.Action.RIGHT_CLICK, event, k, v, hand);
						if(event.getPlayer().isSneaking())
							k.getType().simulateAction(Ability.Action.SHIFT_RIGHT_CLICK, event, k, v, hand);
						break;
					default:
						break;
					}
			});
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		if (event.isCancelled())
			return;
		ActivatedSlot hand = event.getHand() == EquipmentSlot.HAND ? ActivatedSlot.MAIN_HAND : ActivatedSlot.OFF_HAND;
		Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(event.getPlayer());
		if (!map.isEmpty())
			map.forEach((k, v) -> {
				boolean flag = k.isAlwaysAllowAbilities();
				if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), hand, k))
					flag = k.interactedEntity(event);
				if (!k.getEnchants().isEmpty())
					k.getEnchants().forEach(e -> {
						if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), hand, k))
							e.interactedEntity(event, k);
					});
				if (flag)
					k.getType().simulateAction(Ability.Action.INTERACT_ENTITY, event, k, v, hand);
			});
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onHitEntity(EntityDamageByEntityEvent event) {
		if (event.isCancelled())
			return;
		if (event.getDamager() instanceof Projectile && itemProjectiles.containsKey(event.getDamager().getUniqueId())) {
			Pair<GenericItem, GenericItem> pair = itemProjectiles.remove(event.getDamager().getUniqueId());
			if (pair.getSecond() != null && pair.getSecond().getType().getProjectileDamage() != 0.0)
				event.setDamage(pair.getSecond().getType().getProjectileDamage());
			if (pair.getFirst() != null)
				event.setDamage(event.getDamage() * pair.getFirst().getType().getProjectileDamageMultiplier());
		}
		if (event.getDamager() instanceof LivingEntity) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents((LivingEntity) event.getDamager());
			if (!map.isEmpty()) {
				if (map.entrySet().iterator().next().getValue() == ActivatedSlot.MAIN_HAND) {
					GenericItem type = map.entrySet().iterator().next().getKey();
					double damage = type.getType().getDamage();
					if (damage > 0.0 && event.getCause() == DamageCause.ENTITY_ATTACK) {
						if (event.getDamager() instanceof LivingEntity) {
							LivingEntity entity = (LivingEntity) event.getDamager();
							double enchantDamage = UIFUtils.getEnchantDamage(type.getItem(), (event.getEntity() instanceof LivingEntity ? (LivingEntity) event.getEntity() : null));
							if (entity.hasPotionEffect(VersionUtils.getStrength()))
								damage += entity.getPotionEffect(VersionUtils.getStrength()).getAmplifier() * 3.0;
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
				}
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, k))
						flag = k.hitEntity(event);
					if (!k.getEnchants().isEmpty())
						k.getEnchants().forEach(e -> {
							if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, k))
								e.hitEntity(event, k);
						});
					if (flag)
						k.getType().simulateAction(Ability.Action.HIT_ENTITY, event, k, v);
				});
			}
		}
		if (event.isCancelled())
			return;
		if (event.getEntity() instanceof LivingEntity) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents((LivingEntity) event.getEntity());
			if (!map.isEmpty())
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
						flag = k.wasHit(event);
					if (!k.getEnchants().isEmpty())
						k.getEnchants().forEach(e -> {
							if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
								e.wasHit(event, k);
						});
					if (flag)
						k.getType().simulateAction(Ability.Action.WAS_HIT, event, k, v);
				});
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onProjectileThrown(ProjectileLaunchEvent event) {
		if (event.isCancelled())
			return;
		if (event.getEntity() instanceof ThrowableProjectile) {
			GenericItem base = GenericItem.getItemBase(((ThrowableProjectile) event.getEntity()).getItem());
			if (base != null) {
				boolean flag = base.projectileThrown(event);
				if (!base.getEnchants().isEmpty())
					base.getEnchants().forEach(e -> e.projectileThrown(event, base));
				if (flag)
					base.getType().simulateAction(Ability.Action.WAS_THROWN, event, base);
				itemProjectiles.put(event.getEntity().getUniqueId(), Pair.of(null, base));
			}
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onProjectileHit(ProjectileHitEvent event) {
		if (event.isCancelled())
			return;
		if (event.getEntity() instanceof ThrowableProjectile) {
			ItemStack item = ((ThrowableProjectile) event.getEntity()).getItem();
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null) {
				boolean flag = base.projectileHit(event);
				if (!base.getEnchants().isEmpty())
					base.getEnchants().forEach(e -> e.projectileHit(event, base));
				if (flag)
					base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
			}
		} else {
			Pair<GenericItem, GenericItem> itemPair = itemProjectiles.get(event.getEntity().getUniqueId());
			if (itemPair != null) {
				GenericItem base = itemPair.getFirst();
				if (base != null) {
					boolean flag = base.projectileHit(event);
					if (!base.getEnchants().isEmpty())
						base.getEnchants().forEach(e -> e.projectileHit(event, itemPair.getFirst()));
					if (flag)
						base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
				}
				base = itemPair.getSecond();
				if (base != null) {
					boolean flag = base.projectileHit(event);
					if (!base.getEnchants().isEmpty())
						base.getEnchants().forEach(e -> e.projectileHit(event, itemPair.getSecond()));
					if (flag)
						base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
				}
			}
			Pair<Ability, GenericItem> pair = abilityProjectiles.remove(event.getEntity().getUniqueId());
			if (pair != null)
				pair.getFirst().projectileHit(event, pair.getSecond());
		}
		if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents((LivingEntity) event.getHitEntity());
			if (!map.isEmpty())
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
						flag = k.hitByProjectile(event);
					if (!k.getEnchants().isEmpty())
						k.getEnchants().forEach(e -> {
							if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
								e.hitByProjectile(event, k);
						});
					if (flag)
						k.getType().simulateAction(Ability.Action.HIT_BY_PROJECTILE, event, k, v);
				});
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBowShot(EntityShootBowEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getBow());
		GenericItem arrow = event.shouldConsumeItem() ? GenericItem.getItemBase(event.getConsumable()) : null;
		if (base != null) {
			boolean flag = base.shotBow(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.shotBow(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.SHOT_BOW, event, base);
		}
		if (base != null || arrow != null)
			itemProjectiles.put(event.getProjectile().getUniqueId(), Pair.of(base, arrow));
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.isCancelled() || event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
			return;
		GenericItem base = GenericItem.getItemBase(event.getCurrentItem());
		if (base != null) {
			boolean flag = base.inventoryClick(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.inventoryClick(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.INVENTORY_CLICK, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onConsumption(PlayerItemConsumeEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getItem());
		if (base != null) {
			boolean flag = base.consumeItem(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.consumeItem(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.CONSUME, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onSplashPotion(PotionSplashEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getPotion().getItem());
		if (base != null) {
			boolean flag = base.splashPotion(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.splashPotion(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.SPLASH_POTION, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemDrop(EntityDropItemEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getItemDrop().getItemStack());
		if (base != null) {
			boolean flag = base.dropItem(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.dropItem(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.DROP_ITEM, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getItem().getItemStack());
		if (base != null) {
			boolean flag = base.pickupItem(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.pickupItem(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.PICKUP_ITEM, event, base);
		}
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDeath(EntityDeathEvent event) {
		UIFUtils.getEntityContents(event.getEntity()).forEach((k, v) -> {
			boolean flag = k.isAlwaysAllowAbilities();
			if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ANY, k))
				flag = k.entityDeath(event);
			if (!k.getEnchants().isEmpty())
				k.getEnchants().forEach(e -> e.entityDeath(event, k));
			if (flag)
				k.getType().simulateAction(Ability.Action.ENTITY_DEATH, event, k, v);
		});
	}
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		UIFUtils.getEntityContents(event.getPlayer()).forEach((k, v) -> {
			boolean flag = k.isAlwaysAllowAbilities();
			if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ANY, k))
				flag = k.entityRespawn(event);
			if (!k.getEnchants().isEmpty())
				k.getEnchants().forEach(e -> e.entityRespawn(event, k));
			if (flag)
				k.getType().simulateAction(Ability.Action.ENTITY_RESPAWN, event, k, v);
		});
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlaceBlock(BlockPlaceEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getItemInHand());
		if (base != null) {
			boolean flag = base.placeBlock(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.placeBlock(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.PLACE_BLOCK, event, base, event.getHand() == EquipmentSlot.HAND ? ActivatedSlot.MAIN_HAND : ActivatedSlot.OFF_HAND);
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onBreakBlock(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		GenericItem base = GenericItem.getItemBase(event.getPlayer().getEquipment().getItemInMainHand());
		if (base != null) {
			boolean flag = base.breakBlock(event);
			if (!base.getEnchants().isEmpty())
				base.getEnchants().forEach(e -> e.breakBlock(event, base));
			if (flag)
				base.getType().simulateAction(Ability.Action.BREAK_BLOCK, event, base, ActivatedSlot.MAIN_HAND);
		}
	}
	public static void assignProjectile(UUID uuid, GenericItem bow, GenericItem projectile) {
		itemProjectiles.put(uuid, Pair.of(bow, projectile));
	}
	public static void assignProjectile(UUID uuid, Ability ability, GenericItem base) {
		abilityProjectiles.put(uuid, Pair.of(ability, base));
	}
	public static void removeProjectile(UUID uuid) {
		itemProjectiles.remove(uuid);
		abilityProjectiles.remove(uuid);
	}
}
