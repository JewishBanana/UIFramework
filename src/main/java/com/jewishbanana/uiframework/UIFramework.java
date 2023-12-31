package com.jewishbanana.uiframework;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.jewishbanana.uiframework.commands.UICommand;
import com.jewishbanana.uiframework.items.Ability;
import com.jewishbanana.uiframework.items.AbilityType;
import com.jewishbanana.uiframework.items.ItemType;
import com.jewishbanana.uiframework.items.RecipeBook;
import com.jewishbanana.uiframework.listeners.AbilityListener;
import com.jewishbanana.uiframework.listeners.ItemListener;
import com.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.jewishbanana.uiframework.utils.ConfigUpdater;
import com.jewishbanana.uiframework.utils.Metrics;
import com.jewishbanana.uiframework.utils.UIFDataUtils;
import com.jewishbanana.uiframework.utils.UIFUtils;

public class UIFramework extends JavaPlugin {

	private static UIFramework instance;
	public static ConsoleCommandSender consoleSender;
	public double mcVersion;
	public static YamlConfiguration langFile, defaultLang, dataFile;
	public FixedMetadataValue fixedData;
	private Map<JavaPlugin, Runnable> reloadRunnables = new HashMap<>();
	
	public void onEnable() {
		instance = this;
		consoleSender = this.getServer().getConsoleSender();
		this.mcVersion = Double.parseDouble(Bukkit.getBukkitVersion().substring(0,4));
		this.fixedData = new FixedMetadataValue(this, "protected");
		
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		File dataLocation = new File(getDataFolder().getAbsolutePath(), "data.yml");
		if (!dataLocation.exists()) {
			try {
				dataLocation.getParentFile().mkdirs();
				dataLocation.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		dataFile = YamlConfiguration.loadConfiguration(dataLocation);
		defaultLang = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("files/language.yml")));
		File langLocation = new File(getDataFolder().getAbsolutePath(), "language.yml");
		if (!langLocation.exists()) {
			langLocation.getParentFile().mkdirs();
			try {
				FileUtils.copyInputStreamToFile(getResource("files/language.yml"), langLocation);
				langFile = YamlConfiguration.loadConfiguration(langLocation);
			} catch (IOException e) {
				langFile = YamlConfiguration.loadConfiguration(new InputStreamReader(getResource("files/language.yml")));
				e.printStackTrace();
			}
		} else
			langFile = YamlConfiguration.loadConfiguration(langLocation);
		if (!langFile.contains("data.version") || !langFile.getString("data.version").equals(this.getDescription().getVersion())) {
			try {
				ConfigUpdater.update(this, getResource("files/language.yml"), langLocation, null);
				langFile.set("data.version", this.getDescription().getVersion());
				langFile.save(langLocation);
				ConfigUpdater.update(this, getResource("config.yml"), new File(getDataFolder().getAbsolutePath(), "config.yml"), null);
				this.reloadConfig();
			} catch (IOException e) {
				e.printStackTrace();
				consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cUnable to initialize config! Please report the full error above to the discord."));
			}
		}
		if (!dataFile.contains("item"))
			dataFile.createSection("item");
		for (String s : dataFile.getConfigurationSection("item").getKeys(false))
			ItemType.addPreId(UIFDataUtils.getDataFileInt("item."+s+".id"));
		
		ItemType.registerItem("recipe_book", RecipeBook.class);
		saveDataFile();
		init();
		ItemType.registerDefaults();
	}
	private void init() {
		UIFUtils.descriptionLine = UIFDataUtils.getConfigInt("general.lore_line_length");
		this.getCommand("uiframework").setTabCompleter(new UICommand(this));
		AbilityType.init(this);
		
		reload();
		
		new AbilityListener(this);
		new ItemListener(this);
		new MenuManager(this);
		
		getServer().getScheduler().runTaskLater(this, () -> Metrics.configureMetrics(instance), 40);
	}
	public void onDisable() {
		ItemType.cleanAbilities();
		if (new File(getDataFolder().getAbsolutePath(), "data.yml").exists())
			saveDataFile();
	}
	public void reload() {
		reloadConfig();
		Ability.pluginReload();
		reloadRunnables.forEach((k, v) -> {
			try {
				v.run();
			} catch (Exception e) {
				e.printStackTrace();
				consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: An error has occurred while &d"+k.getName()+" &etried to reload with UIFramework! This is NOT a UIFramework bug! Report this to the proper plugin author(s): "+k.getDescription().getAuthors().toString()));
			}
		});
	}
	public static String getLangString(String path) {
		try {
			return langFile.contains(path) ? langFile.getString(path) : defaultLang.getString(path);
		} catch (IllegalArgumentException e) {
			consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: WARNING while reading &dstring &evalue from language.yml path '"+path+"' please fix this value!"));
			return null;
		}
	}
	public static void saveDataFile() {
		try {
			dataFile.save(new File(instance.getDataFolder().getAbsolutePath(), "data.yml"));
		} catch (IOException e) {
			e.printStackTrace();
			consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR could not save data file!"));
		}
	}
	public static UIFramework getInstance() {
		return instance;
	}
	public void registerReloadRunnable(JavaPlugin plugin, Runnable runnable) {
		reloadRunnables.put(plugin, runnable);
	}
}
