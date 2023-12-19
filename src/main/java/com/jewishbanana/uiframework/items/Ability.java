package com.jewishbanana.uiframework.items;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.listeners.AbilityListener;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class Ability {
	
	private AbilityType type;
	private int cooldownTicks;
	
	private static boolean opCooldowns;
	private static boolean immuneCooldowns;
	
	public boolean use(Entity entity, boolean sendMessage) {
		if (entity instanceof Player && ((!opCooldowns && entity.isOp()) || (!immuneCooldowns && UIFUtils.isPlayerImmune((Player) entity))))
			return true;
		if (type.isEntityOnCooldown(entity.getUniqueId())) {
			if (sendMessage && entity instanceof Player)
				((Player) entity).sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.abilityCooldown")
						.replace("%cooldown%", UIFDataUtils.getDecimalFormatted((double) (type.getEntityCooldown(entity.getUniqueId())) / 20.0))
						.replace("%ability%", type.getName())));
			return false;
		} else {
			type.putEntityOnCooldown(entity.getUniqueId(), getCooldownTicks());
			return true;
		}
	}
	public boolean interacted(PlayerInteractEvent event, GenericItem base) {
		return false;
	}
	public boolean interactedEntity(PlayerInteractEntityEvent event, GenericItem base) {
		return false;
	}
	public boolean hitEntity(EntityDamageByEntityEvent event, GenericItem base) {
		return false;
	}
	public boolean wasHit(EntityDamageByEntityEvent event, GenericItem base) {
		return false;
	}
	public boolean projectileThrown(ProjectileLaunchEvent event, GenericItem base) {
		return false;
	}
	public boolean projectileHit(ProjectileHitEvent event, GenericItem base) {
		return false;
	}
	public boolean hitByProjectile(ProjectileHitEvent event, GenericItem base) {
		return false;
	}
	public boolean shotBow(EntityShootBowEvent event, GenericItem base) {
		return false;
	}
	public boolean inventoryClick(InventoryClickEvent event, GenericItem base) {
		return false;
	}
	public boolean consumeItem(PlayerItemConsumeEvent event, GenericItem base) {
		return false;
	}
	public void clean() {
	}
	public void assignProjectile(Projectile projectile, GenericItem base) {
		AbilityListener.assignProjectile(projectile.getUniqueId(), this, base);
	}
	public void removeProjectile(UUID uuid) {
		AbilityListener.removeProjectile(uuid);
	}
	public static void pluginReload() {
		opCooldowns = UIFDataUtils.getConfigBoolean("general.give_op_cooldowns");
		immuneCooldowns = UIFDataUtils.getConfigBoolean("general.give_immune_cooldowns");
	}
	
	public AbilityType getType() {
		return type;
	}
	public Ability setType(AbilityType type) {
		this.type = type;
		return this;
	}
	public int getCooldownTicks() {
		return cooldownTicks;
	}
	public void setCooldownTicks(int cooldownTicks) {
		this.cooldownTicks = cooldownTicks;
	}
	public enum Action {
		LEFT_CLICK("action.left_click"),
		LEFT_CLICK_BLOCK("action.left_click_block"),
		SHIFT_LEFT_CLICK("action.shift_left_click"),
		SHIFT_LEFT_CLICK_BLOCK("action.shift_left_click_block"),
		RIGHT_CLICK("action.right_click"),
		RIGHT_CLICK_BLOCK("action.right_click_block"),
		SHIFT_RIGHT_CLICK("action.shift_right_click"),
		SHIFT_RIGHT_CLICK_BLOCK("action.shift_right_click_block"),
		INVENTORY_CLICK("action.inventory_click"),
		CONSUME("action.consume"),
		INTERACT_ENTITY("action.interact_entity"),
		
		HIT_ENTITY("action.passive"),
		WAS_HIT("action.passive"),
		WAS_THROWN("action.passive"),
		PROJECTILE_HIT("action.passive"),
		HIT_BY_PROJECTILE("action.passive"),
		SHOT_BOW("action.passive");
		
		private String path;
		private boolean passive;
		
		private Action(String path) {
			this.path = path;
			this.passive = path.equals("action.passive");
		}
		public static Action forName(String name) {
			for (Action id : values())
				if (id.toString().equals(name.toUpperCase().replace('-', '_')))
					return id;
			return null;
		}
		public String getLangName() {
			return UIFUtils.convertString(UIFramework.getLangString(path));
		}
		public boolean isPassive() {
			return passive;
		}
	}
}
