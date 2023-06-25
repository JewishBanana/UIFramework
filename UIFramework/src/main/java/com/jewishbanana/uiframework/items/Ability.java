package com.jewishbanana.uiframework.items;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class Ability {
	
	private AbilityType type;
	private int cooldownTicks;
	
	private static boolean opCooldowns;
	private static boolean immuneCooldowns;
	
	public boolean use(Entity entity) {
		if (entity instanceof Player && ((!opCooldowns && entity.isOp()) || (!immuneCooldowns && UIFUtils.isPlayerImmune((Player) entity))))
			return true;
		if (type.isEntityOnCooldown(entity.getUniqueId())) {
			if (entity instanceof Player)
				((Player) entity).sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.abilityCooldown")
						.replace("%cooldown%", UIFDataUtils.getDecimalFormatted((double) (type.getEntityCooldown(entity.getUniqueId())) / 20.0))
						.replace("%ability%", type.getName())));
			return false;
		} else {
			type.putEntityOnCooldown(entity.getUniqueId(), cooldownTicks);
			return true;
		}
	}
	public boolean interacted(PlayerInteractEvent event, GenericItem base) {
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
	public boolean shotBow(EntityShootBowEvent event, GenericItem base) {
		return false;
	}
	public void clean() {
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
		
		HIT_ENTITY("action.passive"),
		WAS_HIT("action.passive"),
		WAS_THROWN("action.passive"),
		PROJECTILE_HIT("action.passive"),
		SHOT_BOW("action.passive");
		
		private static Set<Action> passiveActions = new HashSet<>();
		static {
			Stream.of(values()).forEach(e -> {
				if (e.path.equals("passive"))
					passiveActions.add(e);
			});
		}
		private String path;
		
		private Action(String path) {
			this.path = path;
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
		public static boolean isActionPassive(Action action) {
			return passiveActions.contains(action);
		}
	}
}
