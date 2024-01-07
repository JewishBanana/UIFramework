package com.jewishbanana.uiframework.items;

import java.util.UUID;

import org.bukkit.entity.Entity;
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

import com.jewishbanana.uiframework.UIFramework;
import com.jewishbanana.uiframework.listeners.AbilityListener;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class Ability {
	
	private AbilityType type;
	private int cooldownTicks;
	
	private static boolean opCooldowns;
	private static boolean immuneCooldowns;
	
	/**
	 * Will attempt to determine if the given entity is on cooldown for this ability. If the entity is not on cooldown then this will immediatly set them to the full length cooldown of this ability.
	 * Also optionally will send players on cooldown an indicating message of the cooldown which can be configured by the user in UIFramework's config.
	 * 
	 * @param entity The entity to perform the check on
	 * @param sendMessage If a cooldown message should be sent or not
	 * @return If the entity was on cooldown for this ability or not
	 */
	public boolean use(Entity entity, boolean sendMessage) {
		if (entity == null)
			return false;
		if (entity instanceof Player && ((!opCooldowns && entity.isOp()) || (!immuneCooldowns && UIFUtils.isPlayerImmune((Player) entity))))
			return true;
		if (type.isEntityOnCooldown(entity.getUniqueId())) {
			if (sendMessage && entity instanceof Player)
				((Player) entity).sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.abilityCooldown")
						.replace("%cooldown%", UIFDataUtils.getDecimalFormatted((double) (type.getEntityCooldown(entity.getUniqueId())) / 20.0))
						.replace("%ability%", type.getDisplayName())));
			return false;
		} else {
			type.putEntityOnCooldown(entity.getUniqueId(), getCooldownTicks());
			return true;
		}
	}
	/**
	 * <STRONG>This method should be overriden in the abilities class.</STRONG>
	 * <p>
	 * Empty activation method that will be run whenever the ability is activated. This is used by the default listener methods to fire a default action of the ability when activated.
	 * 
	 * @param activatingEntity The activating entity
	 */
	public void activate(Entity activatingEntity) {
	}
	/**
	 * Run whenever a PlayerInteractEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the interacting player
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean interacted(PlayerInteractEvent event, GenericItem base) {
		if (use(event.getPlayer(), true)) {
			activate(event.getPlayer());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever a PlayerInteractEntityEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the right clicked entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean interactedEntity(PlayerInteractEntityEvent event, GenericItem base) {
		if (use(event.getPlayer(), true)) {
			activate(event.getRightClicked());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityDamageByEntityEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the damaged entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean hitEntity(EntityDamageByEntityEvent event, GenericItem base) {
		if (use(event.getDamager(), true)) {
			activate(event.getEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityDamageByEntityEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the damaged entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean wasHit(EntityDamageByEntityEvent event, GenericItem base) {
		if (use(event.getEntity(), true)) {
			activate(event.getEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever a ProjectileLaunchEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the projectile entity. Will always activate if 
	 * the shooter is null or N/A.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean projectileThrown(ProjectileLaunchEvent event, GenericItem base) {
		if (event.getEntity().getShooter() != null && event.getEntity().getShooter() instanceof Entity) {
			if (use((Entity) event.getEntity().getShooter(), true)) {
				activate(event.getEntity());
				return true;
			}
			return false;
		}
		activate(event.getEntity());
		return true;
	}
	/**
	 * Run whenever a ProjectileHitEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the projectile entity. Will always activate if 
	 * the shooter is null or N/A.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean projectileHit(ProjectileHitEvent event, GenericItem base) {
		if (event.getEntity().getShooter() != null && event.getEntity().getShooter() instanceof Entity) {
			if (use((Entity) event.getEntity().getShooter(), true)) {
				activate(event.getEntity());
				return true;
			}
			return false;
		}
		activate(event.getEntity());
		return true;
	}
	/**
	 * Run whenever a ProjectileHitEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the hit entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean hitByProjectile(ProjectileHitEvent event, GenericItem base) {
		if (use(event.getHitEntity(), true)) {
			activate(event.getHitEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityShootBowEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the shooting entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean shotBow(EntityShootBowEvent event, GenericItem base) {
		if (use(event.getEntity(), true)) {
			activate(event.getEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an InventoryClickEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the clicking entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean inventoryClick(InventoryClickEvent event, GenericItem base) {
		if (use(event.getWhoClicked(), true)) {
			activate(event.getWhoClicked());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever a PlayerItemConsumeEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the consuming player
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean consumeItem(PlayerItemConsumeEvent event, GenericItem base) {
		if (use(event.getPlayer(), true)) {
			activate(event.getPlayer());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityDropItemEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the dropped item entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean dropItem(EntityDropItemEvent event, GenericItem base) {
		if (use(event.getEntity(), true)) {
			activate(event.getItemDrop());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityPickupItemEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the entity picking up
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean pickupItem(EntityPickupItemEvent event, GenericItem base) {
		if (use(event.getEntity(), true)) {
			activate(event.getEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever an EntityDeathEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the dying entity
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean entityDeath(EntityDeathEvent event, GenericItem base) {
		if (use(event.getEntity(), true)) {
			activate(event.getEntity());
			return true;
		}
		return false;
	}
	/**
	 * Run whenever a PlayerRespawnEvent is fired involving the ability.
	 * <p>
	 * <STRONG>Default Behavior:</STRONG> Will activate on the player respawning
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 * @return If the ability activated or not
	 */
	public boolean entityRespawn(PlayerRespawnEvent event, GenericItem base) {
		if (use(event.getPlayer(), true)) {
			activate(event.getPlayer());
			return true;
		}
		return false;
	}
	/**
	 * <STRONG>This method should be overriden if needed in your abilities class.</STRONG>
	 * <p>
	 * Run when the server is shutting down or being reloaded. This method should remove anything from your ability that is not meant to persist through these actions.
	 */
	public void clean() {
	}
	/**
	 * Assigns a projectile to this ability and base class. Should be called on any spawned projectiles by this ability so that they will trigger other events within this ability.
	 * 
	 * @param projectile The projectile to assign to
	 * @param base The base class of the associated item
	 */
	public void assignProjectile(Projectile projectile, GenericItem base) {
		AbilityListener.assignProjectile(projectile.getUniqueId(), this, base);
	}
	/**
	 * Removes any association with this ability and the projectile
	 * 
	 * @param uuid The unique id of the projectile
	 */
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
		DROP_ITEM("action.drop_item"),
		PICKUP_ITEM("action.pickup_item"),
		
		HIT_ENTITY("action.passive"),
		WAS_HIT("action.passive"),
		WAS_THROWN("action.passive"),
		PROJECTILE_HIT("action.passive"),
		HIT_BY_PROJECTILE("action.passive"),
		SHOT_BOW("action.passive"),
		ENTITY_DEATH("action.passive"),
		ENTITY_RESPAWN("action.passive");
		
		private String path;
		private boolean passive;
		
		private Action(String path) {
			this.path = path;
			this.passive = path.equals("action.passive");
		}
		/**
		 * Convienience method for getting enum by name and will return null instead of throwing an exception.
		 * 
		 * @param name The name of the ability
		 * @return The ability or null
		 */
		public static Action forName(String name) {
			for (Action id : values())
				if (id.toString().equals(name.toUpperCase().replace('-', '_')))
					return id;
			return null;
		}
		/**
		 * Gets the UIFramework lang file configured name of this action
		 * 
		 * @return The config name of this action
		 */
		public String getLangName() {
			return UIFUtils.convertString(UIFramework.getLangString(path));
		}
		public boolean isPassive() {
			return passive;
		}
	}
}
