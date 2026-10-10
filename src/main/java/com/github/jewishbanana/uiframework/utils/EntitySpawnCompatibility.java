package com.github.jewishbanana.uiframework.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/** Keeps the randomizeData flag on older Bukkit APIs which used Bukkit's Consumer. */
public final class EntitySpawnCompatibility {
    private static final Method LEGACY_SPAWN = findLegacySpawn();

    private EntitySpawnCompatibility() {}

    private static Method findLegacySpawn() {
        try {
            World.class.getMethod("spawn", Location.class, Class.class, boolean.class, java.util.function.Consumer.class);
            return null; // Modern servers retain the normal, non-reflective hot path.
        } catch (NoSuchMethodException oldApi) {
            try {
                return World.class.getMethod("spawn", Location.class, Class.class, boolean.class,
                        Class.forName("org.bukkit.util.Consumer"));
            } catch (ReflectiveOperationException unsupported) {
                throw new IllegalStateException("No supported Bukkit entity spawn method", unsupported);
            }
        }
    }

    public static <T extends Entity> T spawn(Location location, Class<T> type, boolean randomizeData) {
        if (LEGACY_SPAWN == null)
            return location.getWorld().spawn(location, type, randomizeData, null);
        try {
            return type.cast(LEGACY_SPAWN.invoke(location.getWorld(), location, type, randomizeData, null));
        } catch (InvocationTargetException failure) {
            // Propagate the actual spawn failure; never retry and risk creating two entities.
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Bukkit could not spawn " + type.getName(), cause);
        } catch (IllegalAccessException failure) {
            throw new IllegalStateException("Cannot access Bukkit's entity spawn method", failure);
        }
    }
}
