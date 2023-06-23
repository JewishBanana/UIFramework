package com.jewishbanana.uiframework.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.jewishbanana.uiframework.UIFramework;

public abstract class RepeatingTask implements Runnable {

	private static JavaPlugin javaPlugin;
	static {
		javaPlugin = UIFramework.getInstance();
	}
    private BukkitTask taskId;

    public RepeatingTask(int arg1, int arg2) {
        taskId = javaPlugin.getServer().getScheduler().runTaskTimer(javaPlugin, this, arg1, arg2);
    }
    public void cancel() {
        taskId.cancel();
    }
}