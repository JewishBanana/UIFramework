package com.github.jewishbanana.uiframework.entities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;

import com.github.jewishbanana.uiframework.UIFramework;
import com.github.jewishbanana.uiframework.commands.UICommand.UISummonCommandParameters;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class UIEntityManager {
	
	private static final NamespacedKey key;
	private static final Map<String, UIEntityManager> registry = new HashMap<>();
	private static final Map<UUID, CustomEntity<? extends Entity>> entities = new HashMap<>();
	static {
		key = new NamespacedKey(UIFramework.getInstance(), "uif-entity");
	}
	
	private Class<? extends CustomEntity<? extends Entity>> entityClass;
	private Class<? extends Entity> minecraftEntityType;
	private String registeredName;
	private double spawnRate;
	private Predicate<CreatureSpawnEvent> spawnConditions;
	private Predicate<UISummonCommandParameters> spawnByCommand;
	private boolean randomizeData;
	private Function<Location, Entity> entityCreator;
	
	private UIEntityManager(Class<? extends CustomEntity<? extends Entity>> entityClass, String registeredName) {
		this.entityClass = entityClass;
		this.registeredName = registeredName;
		this.minecraftEntityType = resolveEntityClass(entityClass);
		this.spawnConditions = e -> true;
		this.entityCreator = location -> location.getWorld().spawn(location, minecraftEntityType, randomizeData, null);
	}
	@SuppressWarnings("unchecked")
	private static Class<? extends Entity> resolveEntityClass(Class<? extends CustomEntity<? extends Entity>> clazz) {
		Type superClass = clazz.getGenericSuperclass();
		if (superClass instanceof ParameterizedType)
			return (Class<? extends Entity>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
		throw new IllegalStateException("Generic type information is missing!");
	}
	public static UIEntityManager registerEntity(String registeredName, Class<? extends CustomEntity<? extends Entity>> entityClass) {
		if (registry.containsKey(registeredName))
			throw new IllegalArgumentException("[UIFramework]: Cannot register custom entity '"+registeredName+"' as a custom entity with that name is already registered!");
		UIEntityManager type = new UIEntityManager(entityClass, registeredName);
		registry.put(registeredName, type);
		return type;
	}
	public static UIEntityManager getEntityType(String registeredName) {
		return registry.get(registeredName);
	}
	private static <T extends CustomEntity<? extends Entity>> T spawnEntity(Location location, UIEntityManager type, Class<T> registeredClass) {
		if (type == null)
			throw new IllegalArgumentException("[UIFramework]: Cannot spawn custom entity type null!");
		Entity entity = type.entityCreator.apply(location);
		try {
			T customEntity = registeredClass.getDeclaredConstructor(type.minecraftEntityType).newInstance(entity);
			entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.registeredName);
			if (entity instanceof LivingEntity) {
				LivingEntity le = (LivingEntity) entity;
				le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				boolean despawn = le.getRemoveWhenFarAway();
				le.setCustomName(customEntity.getDisplayName());
				le.setRemoveWhenFarAway(despawn);
			} else
				entity.setCustomName(customEntity.getDisplayName());
			customEntity.spawn(entity);
			entities.put(entity.getUniqueId(), customEntity);
			return customEntity;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static <T extends CustomEntity<? extends Entity>> T spawnEntity(Location location, Class<T> registeredClass) {
		for (Entry<String, UIEntityManager> entry : registry.entrySet())
			if (entry.getValue().entityClass.equals(registeredClass))
				return spawnEntity(location, entry.getValue(), registeredClass);
		throw new IllegalArgumentException("[UIFramework]: Cannot spawn custom entity '"+registeredClass.getName()+"' as that custom entity is not registered!");
	}
	public static CustomEntity<? extends Entity> getEntity(UUID uuid) {
		return entities.get(uuid);
	}
	public static CustomEntity<? extends Entity> getEntity(Entity entity) {
		return entity == null ? null : entities.get(entity.getUniqueId());
	}
	public static void loadEntity(Entity entity) {
		if (entity == null)
			return;
		String species = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
		if (species == null)
			return;
		UIEntityManager type = registry.get(species);
		if (type == null) {
			if (UIFramework.debugMessages)
				UIFramework.consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&eERROR could not load a custom entity type '"+species+"'! Has the custom entities host plugin been removed? No data loss has occurred, the custom entity will be temporarily disabled. To avoid seeing these messages you can disable debug messages in the config."));
			return;
		}
		try {
			CustomEntity<? extends Entity> customEntity = type.entityClass.getDeclaredConstructor(type.minecraftEntityType).newInstance(entity);
			entities.put(entity.getUniqueId(), customEntity);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	public static CustomEntity<? extends Entity> removeEntity(CustomEntity<? extends Entity> entity) {
		return entities.remove(entity.getUniqueId());
	}
	public static CustomEntity<? extends Entity> removeEntity(UUID uuid) {
		return entities.remove(uuid);
	}
	public static void unloadEntities() {
		entities.values().forEach(e -> e.unload());
		entities.clear();
	}
	public static void reload(UIFramework plugin) {
		unloadEntities();
		plugin.getServer().getScheduler().runTask(plugin, () -> Bukkit.getWorlds().forEach(world -> world.getEntities().forEach(e -> loadEntity(e))));
	}
	public static Map<String, UIEntityManager> getRegistry() {
		return registry;
	}
	public Class<? extends CustomEntity<? extends Entity>> getEntityClass() {
		return entityClass;
	}
	public static UIEntityManager getManager(Class<?> class1) {
		for (UIEntityManager manager : registry.values())
			if (manager.entityClass.equals(class1))
				return manager;
		return null;
	}
	public String getRegisteredName() {
		return registeredName;
	}
	public double getSpawnRate() {
		return spawnRate;
	}
	public void setSpawnRate(double getSpawnRate) {
		this.spawnRate = getSpawnRate;
	}
	public Predicate<CreatureSpawnEvent> getSpawnConditions() {
		return spawnConditions;
	}
	/**
	 * Set the spawn conditions for your entity. This will be tested and must pass to replace the creature spawn event with your custom entity instead. 
	 * This is run whenever a vanilla creature spawn event is triggered and the spawn rate of your custom entity is also tested successfully.
	 * 
	 * @param conditions The predicate of the event to test your custom entities spawn conditions, failing to pass will mean your custom entity will not replace the vanilla entity spawn!
	 */
	public void setSpawnConditions(Predicate<CreatureSpawnEvent> conditions) {
		this.spawnConditions = conditions != null ? conditions : e -> true;
	}
	public Predicate<UISummonCommandParameters> getSpawnByCommandConditions() {
		return spawnByCommand;
	}
	/**
	 * Is run whenever the entity is spawned via a command by either a player or the server console. Useful for modifying spawn behaviors. 
	 * Return as false to cancel the spawn.
	 * 
	 * @param conditions The predicate to test when trying to spawn this entity type via command
	 * @return If the entity should spawn or not
	 */
	public void setSpawnByCommandConditions(Predicate<UISummonCommandParameters> conditions) {
		this.spawnByCommand = conditions != null ? conditions : e -> true;
	}
	public boolean doesRandomizeData() {
		return randomizeData;
	}
	/**
	 * If the entity type should randomize the spawns of the mob, for example if set to true and your entity is based on a zombie then this will make the zombie possibly spawn with gear or as a baby.
	 * 
	 * @param randomizeData If the data should be randomized or not
	 */
	public void setRandomizeData(boolean randomizeData) {
		this.randomizeData = randomizeData;
	}
	/**
	 * The entity creator is the function that is used to spawn your custom entity. By default this just spawns the regular entity. 
	 * This can be useful if you are trying to do something very specific with your entity and need to directly modify the entity the same tick it spawns.
	 * 
	 * @param creator The creator for your entity as a function with the provided location being the spawn location
	 */
	public void setEntityCreator(Function<Location, Entity> creator) {
		if (creator == null)
			throw new NullPointerException("Creator cannot be null!");
		this.entityCreator = creator;
	}
}
