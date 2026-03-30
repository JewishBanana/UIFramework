package com.github.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.listeners.AbilityListener;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class Ability {
	
	private static boolean opCooldowns;
	private static boolean immuneCooldowns;
	private static boolean cooldownMessages;
	private static boolean chatMessages;
	
	private UIAbilityType type;
	private int cooldownTicks;
	private ActivatedSlot activatingSlot = ActivatedSlot.PARENT;
	private String displayName;
	private String description;
	private final Map<String, StoredField<?>> fields = new HashMap<>();
	
	protected boolean persist;
	
	public Ability(UIAbilityType type) {
		this.type = type;
	}
	/**
	 * Will attempt to determine if the given entity is on cooldown for this ability. If the entity is not on cooldown then this will immediatly set them to the full length cooldown of this ability. 
	 * Will send players on cooldown an indicating message of the cooldown which can be configured by the user in UIFramework's config.
	 * 
	 * @param entity The entity to perform the check on
	 * @return If the entity was on cooldown for this ability or not
	 */
	public boolean use(Entity entity) {
		return use(entity, true);
	}
	/**
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
		if (entity instanceof Player player && ((!opCooldowns && entity.isOp()) || (!immuneCooldowns && UIFUtils.isPlayerImmune(player))))
			return true;
		UUID uuid = entity.getUniqueId();
		if (type.isEntityOnCooldown(uuid)) {
			if (cooldownMessages && sendMessage && entity instanceof Player player)
				if (chatMessages)
					player.sendMessage(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.abilityCooldown")
							.replace("%cooldown%", UIFDataUtils.getDecimalFormatted((double) (type.getEntityCooldown(uuid)) / 20.0))
							.replace("%ability%", type.getDisplayName())));
				else
					player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(UIFUtils.convertString(UIFDataUtils.getConfigString("messages.abilityCooldown")
							.replace("%cooldown%", UIFDataUtils.getDecimalFormatted((double) (type.getEntityCooldown(uuid)) / 20.0))
							.replace("%ability%", type.getDisplayName()))));
			return false;
		} else {
			type.putEntityOnCooldown(uuid, getCooldownTicks());
			return true;
		}
	}
	/**
	 * Run whenever a PlayerInteractEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void interacted(PlayerInteractEvent event, GenericItem base) {}
	/**
	 * Run whenever a PlayerInteractEntityEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void interactedEntity(PlayerInteractEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityDamageByEntityEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void hitEntity(EntityDamageByEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityDamageByEntityEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void wasHit(EntityDamageByEntityEvent event, GenericItem base) {}
	/**
	 * Run whenever a ProjectileLaunchEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void projectileThrown(ProjectileLaunchEvent event, GenericItem base) {}
	/**
	 * Run whenever a ProjectileHitEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void projectileHit(ProjectileHitEvent event, GenericItem base) {}
	/**
	 * Run whenever a ProjectileHitEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void hitByProjectile(ProjectileHitEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityShootBowEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void shotBow(EntityShootBowEvent event, GenericItem base) {}
	/**
	 * Run whenever an InventoryClickEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void inventoryClick(InventoryClickEvent event, GenericItem base) {}
	/**
	 * Run whenever a PlayerItemConsumeEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void consumeItem(PlayerItemConsumeEvent event, GenericItem base) {}
	/**
	 * Run whenever a PotionSplashEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void splashPotion(PotionSplashEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityDropItemEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void dropItem(EntityDropItemEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityPickupItemEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void pickupItem(EntityPickupItemEvent event, GenericItem base) {}
	/**
	 * Run whenever an EntityDeathEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void entityDeath(EntityDeathEvent event, GenericItem base) {}
	/**
	 * Run whenever a PlayerRespawnEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void entityRespawn(PlayerRespawnEvent event, GenericItem base) {}
	/**
	 * Run whenever a BlockPlaceEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void placeBlock(BlockPlaceEvent event, GenericItem base) {}
	/**
	 * Run whenever a BlockPlaceEvent is fired involving the ability.
	 * 
	 * @param event The event involved
	 * @param base The base class of the firing item
	 */
	public void breakBlock(BlockBreakEvent event, GenericItem base) {}
	/**
	 * <STRONG>This method should be overriden if needed in your abilities class.</STRONG>
	 * <p>
	 * Run when the server is shutting down or being reloaded. This method should remove anything from your ability that is not meant to persist through these actions (e.g. Entities spawned, return item that was thrown during ability)
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
	public Map<String, Object> serialize() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("_abilityType", type.getRegisteredName());
		fields.entrySet().stream().filter(entry -> entry.getValue().persists).forEach(entry -> map.put(entry.getKey(), entry.getValue().value));
		return map;
	}
	public void deserialize(Map<String, Object> map) {
		cooldownTicks = registerSerializedField("_cooldownTicks", map, 0).getValue();
		activatingSlot = ActivatedSlot.valueOf(registerSerializedField("_activatingSlot", map, getActivatingSlot().toString()).getValue());
	}
	@SuppressWarnings("unchecked")
	public <T> StoredField<T> registerSerializedField(String identifier, Map<String, Object> map, T value, boolean persists) {
		T stored = (T) map.get(identifier);
		if (stored != null)
			return registerField(identifier, stored, true);
		return registerField(identifier, value, persists);
	}
	public <T> StoredField<T> registerSerializedField(String identifier, Map<String, Object> map, T value) {
		return registerSerializedField(identifier, map, value, false);
	}
	public <T> StoredField<T> registerField(String identifier, T value, boolean persists) {
		StoredField<T> field = new StoredField<T>(value, persists);
		this.fields.put(identifier, field);
		return field;
	}
	public <T> StoredField<T> registerField(String identifier, T value) {
		return registerField(identifier, value, false);
	}
	public Map<String, StoredField<?>> getFields() {
		return fields;
	}
	public static void pluginReload() {
		opCooldowns = UIFDataUtils.getConfigBoolean("general.give_op_cooldowns");
		immuneCooldowns = UIFDataUtils.getConfigBoolean("general.give_immune_cooldowns");
		cooldownMessages = UIFDataUtils.getConfigBoolean("general.send_cooldown_messages");
		chatMessages = !(UIFDataUtils.getConfigString("general.cooldown_message_appearance").equalsIgnoreCase("hotbar") && UIFUtils.usingSpigot);
	}
	
	public UIAbilityType getType() {
		return type;
	}
	public String getDisplayName() {
		return displayName == null ? type.getDisplayName() : displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getDescription() {
		return description == null ? type.getDescription() : description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getCooldownTicks() {
		return cooldownTicks;
	}
	public void setCooldownTicks(int cooldownTicks) {
		this.cooldownTicks = cooldownTicks;
	}
	public ActivatedSlot getActivatingSlot() {
		return activatingSlot;
	}
	public void setActivatingSlot(ActivatedSlot activatingSlot) {
		this.activatingSlot = activatingSlot;
	}
	public enum Action {
		INTERACTION("action."),
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
		SPLASH_POTION("action.splash_potion"),
		INTERACT_ENTITY("action.interact_entity"),
		DROP_ITEM("action.drop_item"),
		PICKUP_ITEM("action.pickup_item"),
		PLACE_BLOCK("action.place_block"),
		BREAK_BLOCK("action.break_block"),
		
		HIT_ENTITY("action.passive"),
		WAS_HIT("action.passive"),
		WAS_THROWN("action.passive"),
		PROJECTILE_HIT("action.passive"),
		HIT_BY_PROJECTILE("action.passive"),
		SHOT_BOW("action.passive"),
		ENTITY_DEATH("action.passive"),
		ENTITY_RESPAWN("action.passive"),
		PASSIVE("action.passive"),
		UNBOUND("action.passive");
		
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
