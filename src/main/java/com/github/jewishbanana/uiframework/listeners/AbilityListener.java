package com.github.jewishbanana.uiframework.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
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
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.events.EnchantTriggerEvent;
import com.github.jewishbanana.uiframework.items.Ability;
import com.github.jewishbanana.uiframework.items.ActivatedSlot;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.utils.UIFUtils;
import com.mojang.datafixers.util.Pair;

public class AbilityListener implements Listener {
	
	private static Map<UUID, Pair<GenericItem, GenericItem>> itemProjectiles = new HashMap<>();
	private static Map<UUID, Pair<Ability, GenericItem>> abilityProjectiles = new HashMap<>();
	
	private PluginManager manager = Bukkit.getServer().getPluginManager();

	public AbilityListener(UIFramework plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getOnlinePlayers().forEach(this::refreshHeldCombatItem));
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
				k.getEnchants().keySet().forEach(enchant -> {
					if (UIFUtils.isActivatingSlot(v, enchant.getActivatingSlot(), hand, k)) {
						EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.INTERACTION, k, event.getPlayer());
						manager.callEvent(enchantTrigger);
						if (!enchantTrigger.isCancelled())
							enchantTrigger.getEnchant().interacted(event, enchantTrigger.getBaseItem());
					}
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
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInteractEntity(PlayerInteractEntityEvent event) {
		ActivatedSlot hand = event.getHand() == EquipmentSlot.HAND ? ActivatedSlot.MAIN_HAND : ActivatedSlot.OFF_HAND;
		Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(event.getPlayer());
		if (!map.isEmpty())
			map.forEach((k, v) -> {
				boolean flag = k.isAlwaysAllowAbilities();
				if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), hand, k))
					flag = k.interactedEntity(event);
				k.getEnchants().keySet().forEach(enchant -> {
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), hand, k)) {
						EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.INTERACT_ENTITY, k, event.getPlayer());
						manager.callEvent(enchantTrigger);
						if (!enchantTrigger.isCancelled())
							enchantTrigger.getEnchant().interactedEntity(event, enchantTrigger.getBaseItem());
					}
				});
				if (flag)
					k.getType().simulateAction(Ability.Action.INTERACT_ENTITY, event, k, v, hand);
			});
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onHitEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Projectile damager && itemProjectiles.containsKey(damager.getUniqueId())) {
			Pair<GenericItem, GenericItem> pair = itemProjectiles.remove(damager.getUniqueId());
			if (pair.getSecond() != null && pair.getSecond().getType().getProjectileDamage() != 0.0)
				event.setDamage(pair.getSecond().getType().getProjectileDamage());
			if (pair.getFirst() != null)
				event.setDamage(event.getDamage() * pair.getFirst().getType().getProjectileDamageMultiplier());
		}
		if (event.getDamager() instanceof LivingEntity living) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(living);
			if (!map.isEmpty()) {
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, k))
						flag = k.hitEntity(event);
					k.getEnchants().keySet().forEach(enchant -> {
						if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.MAIN_HAND, k)) {
							EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.HIT_ENTITY, k, event.getDamager());
							manager.callEvent(enchantTrigger);
							if (!enchantTrigger.isCancelled())
								enchantTrigger.getEnchant().hitEntity(event, enchantTrigger.getBaseItem());
						}
					});
					if (flag)
						k.getType().simulateAction(Ability.Action.HIT_ENTITY, event, k, v);
					// A mob cannot right-click an entity, so treat its melee hit as an interact-entity activation for any
					// abilities the held item binds to that player-only action.
					if (flag && !(living instanceof Player))
						k.getType().simulateMobInteractAsHit(event, k, v);
				});
			}
		}
		if (event.isCancelled())
			return;
		if (event.getEntity() instanceof LivingEntity living) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(living);
			if (!map.isEmpty())
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
						flag = k.wasHit(event);
					k.getEnchants().keySet().forEach(enchant -> {
						if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k)) {
							EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.WAS_HIT, k, event.getEntity());
							manager.callEvent(enchantTrigger);
							if (!enchantTrigger.isCancelled())
								enchantTrigger.getEnchant().wasHit(event, enchantTrigger.getBaseItem());
						}
					});
					if (flag)
						k.getType().simulateAction(Ability.Action.WAS_HIT, event, k, v);
				});
		}
	}
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void refreshMeleeCombatAttributes(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.ENTITY_ATTACK || !(event.getDamager() instanceof LivingEntity damager))
			return;
		ItemStack heldItem = damager.getEquipment().getItemInMainHand();
		if (heldItem == null || !heldItem.hasItemMeta())
			return;
		ItemMeta preSyncMeta = heldItem.getItemMeta();
		GenericItem item = GenericItem.getItemBase(heldItem);
		if (item == null)
			return;
		double previousDamage = item.getType().getBuilder().getAppliedAttackDamage(preSyncMeta);
		double configuredDamage = item.getType().getDamage() == 0.0 ? 1.0 : item.getType().getDamage();
		if (Math.abs(previousDamage - configuredDamage) < 0.000001)
			return;
		double multiplier = 1.0;
		if (damager instanceof Player player) {
			double cooldown = player.getAttackCooldown();
			multiplier = 0.2 + (cooldown * cooldown * 0.8);
		}
		if (isCriticalHit(damager))
			multiplier *= 1.5;
		event.setDamage(event.getDamage() + ((configuredDamage - previousDamage) * multiplier));
	}
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	@SuppressWarnings("deprecation")
	public void normalizeNearFullMeleeDamage(EntityDamageByEntityEvent event) {
		if (event.getCause() != DamageCause.ENTITY_ATTACK || !(event.getDamager() instanceof LivingEntity damager))
			return;
		GenericItem item = GenericItem.getItemBase(damager.getEquipment().getItemInMainHand());
		if (item == null)
			return;
		double configuredDamage = item.getType().getDamage();
		double dealtDamage = event.getDamage();
		if (configuredDamage > 0.0 && dealtDamage < configuredDamage && dealtDamage >= configuredDamage * 0.98)
			event.setDamage(configuredDamage);
		if (event.getEntity() instanceof LivingEntity target && hasNoEquippedArmor(target) && event.isApplicable(DamageModifier.ARMOR))
			event.setDamage(DamageModifier.ARMOR, 0.0);
	}
	private boolean hasNoEquippedArmor(LivingEntity entity) {
		if (entity.getEquipment() == null)
			return true;
		for (ItemStack armor : entity.getEquipment().getArmorContents())
			if (armor != null && armor.getType() != Material.AIR)
				return false;
		return true;
	}
	private boolean isCriticalHit(LivingEntity entity) {
		return entity.getFallDistance() > 0.0 && !entity.isOnGround() && !entity.isClimbing() && !entity.isInWater()
				&& !entity.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
				&& !entity.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING)
				&& !entity.isInsideVehicle()
				&& !(entity instanceof Player player && (player.isSprinting() || player.getAttackCooldown() < 0.9));
	}
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		refreshHeldCombatItem(event.getPlayer());
	}
	@EventHandler
	public void onHeldItemChange(PlayerItemHeldEvent event) {
		Bukkit.getScheduler().runTask(UIFramework.getInstance(), () -> refreshHeldCombatItem(event.getPlayer()));
	}
	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent event) {
		Bukkit.getScheduler().runTask(UIFramework.getInstance(), () -> refreshHeldCombatItem(event.getPlayer()));
	}
	private void refreshHeldCombatItem(Player player) {
		GenericItem item = GenericItem.getItemBase(player.getInventory().getItemInMainHand());
		if (item != null && (item.getType().getDamage() != 0.0 || item.getType().getAttackSpeed() != 0.0))
			item.refreshItemLore();
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileThrown(ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof ThrowableProjectile projectile) {
			GenericItem base = GenericItem.getItemBase(projectile.getItem());
			if (base != null) {
				boolean flag = base.projectileThrown(event);
				base.getEnchants().keySet().forEach(enchant -> {
					EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.WAS_THROWN, base, event.getEntity());
					manager.callEvent(enchantTrigger);
					if (!enchantTrigger.isCancelled())
						enchantTrigger.getEnchant().projectileThrown(event, enchantTrigger.getBaseItem());
				});
				if (flag)
					base.getType().simulateAction(Ability.Action.WAS_THROWN, event, base);
				itemProjectiles.put(event.getEntity().getUniqueId(), Pair.of(null, base));
			}
		} else if (event.getEntity() instanceof AbstractArrow arrow && arrow.getItem() != null) {
			// Thrown tridents (and other custom-item arrows) carry their item via AbstractArrow#getItem(), so a custom one
			// fires its was-thrown ability the same way a thrown snowball/egg does.
			GenericItem base = GenericItem.getItemBase(arrow.getItem());
			if (base != null) {
				boolean flag = base.projectileThrown(event);
				base.getEnchants().keySet().forEach(enchant -> {
					EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.WAS_THROWN, base, event.getEntity());
					manager.callEvent(enchantTrigger);
					if (!enchantTrigger.isCancelled())
						enchantTrigger.getEnchant().projectileThrown(event, enchantTrigger.getBaseItem());
				});
				if (flag)
					base.getType().simulateAction(Ability.Action.WAS_THROWN, event, base);
				itemProjectiles.put(event.getEntity().getUniqueId(), Pair.of(null, base));
			}
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onProjectileHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof ThrowableProjectile projectile) {
			ItemStack item = projectile.getItem();
			GenericItem base = GenericItem.getItemBase(item);
			if (base != null) {
				boolean flag = base.projectileHit(event);
				base.getEnchants().keySet().forEach(enchant -> {
					EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.PROJECTILE_HIT, base, event.getEntity());
					manager.callEvent(enchantTrigger);
					if (!enchantTrigger.isCancelled())
						enchantTrigger.getEnchant().projectileHit(event, enchantTrigger.getBaseItem());
				});
				if (flag)
					base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
			}
		} else {
			Pair<GenericItem, GenericItem> itemPair = itemProjectiles.get(event.getEntity().getUniqueId());
			if (itemPair != null) {
				GenericItem base = itemPair.getFirst();
				if (base != null) {
					boolean flag = base.projectileHit(event);
					base.getEnchants().keySet().forEach(enchant -> {
						EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.PROJECTILE_HIT, itemPair.getFirst(), event.getEntity());
						manager.callEvent(enchantTrigger);
						if (!enchantTrigger.isCancelled())
							enchantTrigger.getEnchant().projectileHit(event, enchantTrigger.getBaseItem());
					});
					if (flag)
						base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
				}
				base = itemPair.getSecond();
				if (base != null) {
					boolean flag = base.projectileHit(event);
					base.getEnchants().keySet().forEach(enchant -> {
						EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.PROJECTILE_HIT, itemPair.getSecond(), event.getEntity());
						manager.callEvent(enchantTrigger);
						if (!enchantTrigger.isCancelled())
							enchantTrigger.getEnchant().projectileHit(event, enchantTrigger.getBaseItem());
					});
					if (flag)
						base.getType().simulateAction(Ability.Action.PROJECTILE_HIT, event, base, null);
				}
			}
			Pair<Ability, GenericItem> pair = abilityProjectiles.remove(event.getEntity().getUniqueId());
			if (pair != null)
				pair.getFirst().projectileHit(event, pair.getSecond());
		}
		if (event.getHitEntity() != null && event.getHitEntity() instanceof LivingEntity hitEntity) {
			Map<GenericItem, ActivatedSlot> map = UIFUtils.getEntityContents(hitEntity);
			if (!map.isEmpty())
				map.forEach((k, v) -> {
					boolean flag = k.isAlwaysAllowAbilities();
					if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k))
						flag = k.hitByProjectile(event);
					k.getEnchants().keySet().forEach(enchant -> {
						if (UIFUtils.isActivatingSlot(v, k.getActivatingSlot(), ActivatedSlot.ARMOR, k)) {
							EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.HIT_BY_PROJECTILE, k, event.getHitEntity());
							manager.callEvent(enchantTrigger);
							if (!enchantTrigger.isCancelled())
								enchantTrigger.getEnchant().hitByProjectile(event, enchantTrigger.getBaseItem());
						}
					});
					if (flag)
						k.getType().simulateAction(Ability.Action.HIT_BY_PROJECTILE, event, k, v);
				});
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBowShot(EntityShootBowEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getBow());
		GenericItem arrow = event.shouldConsumeItem() ? GenericItem.getItemBase(event.getConsumable()) : null;
		if (base != null) {
			boolean flag = base.shotBow(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.SHOT_BOW, base, event.getEntity());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().shotBow(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.SHOT_BOW, event, base);
		}
		if (base != null || arrow != null)
			itemProjectiles.put(event.getProjectile().getUniqueId(), Pair.of(base, arrow));
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
			return;
		GenericItem base = GenericItem.getItemBase(event.getCurrentItem());
		if (base != null) {
			boolean flag = base.inventoryClick(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.INVENTORY_CLICK, base, event.getWhoClicked());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().inventoryClick(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.INVENTORY_CLICK, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onConsumption(PlayerItemConsumeEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getItem());
		if (base != null) {
			boolean flag = base.consumeItem(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.CONSUME, base, event.getPlayer());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().consumeItem(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.CONSUME, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onSplashPotion(PotionSplashEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getPotion().getItem());
		if (base != null) {
			boolean flag = base.splashPotion(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.SPLASH_POTION, base, event.getPotion());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().splashPotion(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.SPLASH_POTION, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onItemDrop(EntityDropItemEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getItemDrop().getItemStack());
		if (base != null) {
			boolean flag = base.dropItem(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.DROP_ITEM, base, event.getEntity());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().dropItem(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.DROP_ITEM, event, base);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onItemPickup(EntityPickupItemEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getItem().getItemStack());
		if (base != null) {
			boolean flag = base.pickupItem(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.PICKUP_ITEM, base, event.getEntity());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().pickupItem(event, enchantTrigger.getBaseItem());
			});
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
			k.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.ENTITY_DEATH, k, event.getEntity());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().entityDeath(event, enchantTrigger.getBaseItem());
			});
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
			k.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.ENTITY_RESPAWN, k, event.getPlayer());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().entityRespawn(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				k.getType().simulateAction(Ability.Action.ENTITY_RESPAWN, event, k, v);
		});
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlaceBlock(BlockPlaceEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getItemInHand());
		if (base != null) {
			boolean flag = base.placeBlock(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.PLACE_BLOCK, base, event.getPlayer());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().placeBlock(event, enchantTrigger.getBaseItem());
			});
			if (flag)
				base.getType().simulateAction(Ability.Action.PLACE_BLOCK, event, base, event.getHand() == EquipmentSlot.HAND ? ActivatedSlot.MAIN_HAND : ActivatedSlot.OFF_HAND);
		}
	}
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBreakBlock(BlockBreakEvent event) {
		GenericItem base = GenericItem.getItemBase(event.getPlayer().getEquipment().getItemInMainHand());
		if (base != null) {
			boolean flag = base.breakBlock(event);
			base.getEnchants().keySet().forEach(enchant -> {
				EnchantTriggerEvent enchantTrigger = new EnchantTriggerEvent(enchant, Ability.Action.BREAK_BLOCK, base, event.getPlayer());
				manager.callEvent(enchantTrigger);
				if (!enchantTrigger.isCancelled())
					enchantTrigger.getEnchant().breakBlock(event, enchantTrigger.getBaseItem());
			});
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
