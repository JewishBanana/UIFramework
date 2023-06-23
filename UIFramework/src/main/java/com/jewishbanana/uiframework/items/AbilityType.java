package com.jewishbanana.uiframework.items;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.jewishbanana.uiframework.UIFramework;

public class AbilityType {
	
	private static Map<String, AbilityType> typeMap = new HashMap<>();
	
	private Map<UUID, Integer> cooldown = new ConcurrentHashMap<>();
	private Class<? extends Ability> instance;
	private String name;
	private String description;
	
	private AbilityType(Class<? extends Ability> instance) {
		this.instance = instance;
	}
	public static AbilityType registerAbility(String name, Class<? extends Ability> instance) {
		if (typeMap.containsKey(name))
			throw new IllegalArgumentException("[UIFramework]: Cannot register ability '"+name+"' as an ability with that name is already registered!");
		AbilityType type = new AbilityType(instance);
		typeMap.put(name, type);
		return type;
	}
	public static AbilityType getAbilityType(String name) {
		return typeMap.get(name);
	}
	public static void init(UIFramework plugin) {
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
			for (AbilityType type : typeMap.values()) {
				Iterator<Entry<UUID, Integer>> it = type.cooldown.entrySet().iterator();
				while (it.hasNext()) {
					Entry<UUID, Integer> entry = it.next();
					if (entry.setValue(entry.getValue() - 1) <= 0)
						it.remove();
				}
			}
		}, 0, 1);
	}
	public Ability createNewInstance() {
		try {
			return instance.getDeclaredConstructor().newInstance().setType(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public boolean isEntityOnCooldown(UUID uuid) {
		return cooldown.containsKey(uuid);
	}
	public int getEntityCooldown(UUID uuid) {
		return cooldown.containsKey(uuid) ? cooldown.get(uuid) : 0;
	}
	public void putEntityOnCooldown(UUID uuid, int ticks) {
		cooldown.put(uuid, ticks);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
