package com.github.jewishbanana.uiframework;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.jewishbanana.uiframework.commands.UICommand;
import com.github.jewishbanana.uiframework.entities.UIEntityManager;
import com.github.jewishbanana.uiframework.items.Ability;
import com.github.jewishbanana.uiframework.items.GenericItem;
import com.github.jewishbanana.uiframework.items.RecipeBook;
import com.github.jewishbanana.uiframework.items.UIAbilityType;
import com.github.jewishbanana.uiframework.items.UIEnchantment;
import com.github.jewishbanana.uiframework.items.UIItemType;
import com.github.jewishbanana.uiframework.listeners.AbilityListener;
import com.github.jewishbanana.uiframework.listeners.EntitiesListener;
import com.github.jewishbanana.uiframework.listeners.ItemListener;
import com.github.jewishbanana.uiframework.listeners.menus.ItemsMenu;
import com.github.jewishbanana.uiframework.listeners.menus.MenuManager;
import com.github.jewishbanana.uiframework.listeners.menus.RecipeMenu;
import com.github.jewishbanana.uiframework.utils.AnvilRecipe;
import com.github.jewishbanana.uiframework.utils.ConfigUpdater;
import com.github.jewishbanana.uiframework.utils.Metrics;
import com.github.jewishbanana.uiframework.utils.UIFDataUtils;
import com.github.jewishbanana.uiframework.utils.UIFUtils;

public class UIFramework extends JavaPlugin {

	private static UIFramework instance;
	public static ConsoleCommandSender consoleSender;
	public static YamlConfiguration langFile, defaultLang, dataFile;
	public FixedMetadataValue fixedData;
	private static Map<JavaPlugin, Runnable> reloadRunnables = new HashMap<>();
	public static boolean debugMessages;
	private static ItemListener itemListener;
	
	@SuppressWarnings("deprecation")
	public void onEnable() {
		instance = this;
		consoleSender = this.getServer().getConsoleSender();
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
		try {
			ConfigUpdater.update(this, "files/language.yml", langLocation, null);
			ConfigUpdater.update(this, "config.yml", new File(getDataFolder().getAbsolutePath(), "config.yml"), null);
			this.reloadConfig();
		} catch (IOException e) {
			e.printStackTrace();
			consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cUnable to initialize config! Please report the full error above to the discord."));
		}
		try {
			if (!dataFile.contains("item"))
				dataFile.createSection("item");
			if (!dataFile.contains("enchant"))
				dataFile.createSection("enchant");
			for (String s : dataFile.getConfigurationSection("enchant").getKeys(false))
				UIEnchantment.addPreId(UIFDataUtils.getDataFileInt("enchant."+s+".id"));
		} catch (Exception e) {
			e.printStackTrace();
			consoleSender.sendMessage(UIFUtils.convertString(UIFUtils.prefix+"&cERROR your data.yml file has a formatting error and cannot properly be read! This is likely due to user modification or file corruption. Saved data related to custom items and configurations will not be loaded!"));
		}
		
		UIItemType.registerItem("uif:recipe_book", RecipeBook.class);
		saveDataFile();
		init();
		UIItemType.registerDefaults();
	}
	private void init() {
		UIFUtils.descriptionLine = UIFDataUtils.getConfigInt("general.lore_line_length");
		this.getCommand("uiframework").setTabCompleter(new UICommand(this));
		UIAbilityType.init(this);
		
		reload();
		
		new AbilityListener(this);
		itemListener = new ItemListener(this);
		new MenuManager(this);
		new EntitiesListener(this);
		
		getServer().getScheduler().runTaskLater(this, () -> Metrics.configureMetrics(instance), 40);
	}
	public void onDisable() {
		UIItemType.cleanAbilities();
		UIEntityManager.unloadEntities();
		if (new File(getDataFolder().getAbsolutePath(), "data.yml").exists()) {
			GenericItem.unloadItems();
			saveDataFile();
		}
	}
	public void reload() {
		reloadConfig();
		reloadRunnables.forEach((k, v) -> {
			try {
				v.run();
			} catch (Exception e) {
				e.printStackTrace();
				consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: An error has occurred while &d"+k.getName()+" &etried to reload with UIFramework! This is NOT a UIFramework bug! Report this to the proper plugin author(s): "+k.getDescription().getAuthors().toString()));
			}
		});
		ItemsMenu.reload();
		RecipeMenu.reload();
		GenericItem.unloadItems();
		Ability.pluginReload();
		UIEntityManager.reload(this);
		debugMessages = UIFDataUtils.getConfigBoolean("general.debug_messages");
		UIItemType.getRegistry().values().forEach(temp -> temp.updateRecipeResults());
	}
	/**
	 * Gets the user configured language string from the UIFramework lang yaml config.
	 * 
	 * @param path The yaml path to the string
	 * @return The user configured value or built in default if non-existent
	 */
	public static String getLangString(String path) {
		try {
			return langFile.contains(path) ? langFile.getString(path) : defaultLang.getString(path);
		} catch (IllegalArgumentException e) {
			consoleSender.sendMessage(UIFUtils.convertString("&e[UIFramework]: WARNING while reading &dstring &evalue from language.yml path '"+path+"' please fix this value!"));
			return null;
		}
	}
	private static void saveDataFile() {
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
	/**
	 * Registers a runnable that will be attached to UIFrameworks reload command <i>(/ui reload)</i> which will be run every time UIFramework is reloaded by command. This is much preferred over your 
	 * plugins own reload command as this can serve as a universal all in one reload command. It is suggested to store your reload functions inside a reload method and pass that method to be run 
	 * inside the provided runnable. This only needs to be done once upon your plugins startup.
	 * 
	 * @param plugin Your plugin instance
	 * @param runnable The reload runnable that will run everytime UIFramework is reloaded
	 */
	public static void registerReloadRunnable(JavaPlugin plugin, Runnable runnable) {
		reloadRunnables.put(plugin, runnable);
	}
	/**
	 * Convienience method for checking the version of UIFramework by comparison to a certain version or higher. This is useful for if you are writing your plugin for a specific version of UIFramework where 
	 * certain features may not exist in older versions. Simply compare by the version you are writing for (e.g. #isVersionOrAbove("2.2.14") or "2.1" or "2") and if this returns false it means that the version of 
	 * UIFramework present on the server was below the supplied version, from there you can disable your plugin and send an appropriate error message.
	 * 
	 * @param version The minimum version your plugin is made to run on
	 * @return If the UIFramework version on the server was higher than or the same version your supplied to the method
	 */
	public static boolean isVersionOrAbove(String version) {
		try {
			String[] current = instance.getDescription().getVersion().split("\\.");
			String[] test = version.split("\\.");
			for (int i = 0; i < test.length; i++) {
	            if (i >= current.length)
	                return false;
	            int currentSegment = Integer.parseInt(current[i]);
	            int testSegment = Integer.parseInt(test[i]);
	            if (currentSegment > testSegment)
	                return true;
	            else if (currentSegment < testSegment)
	                return false;
	        }
	        return true;
		} catch (NumberFormatException ex) {
			throw new NumberFormatException("The version string you supplied '"+version+"' is not a valid version string! Format must be as follows: '1.2.3' or '1.2' or '1'!");
		}
	}
	public static void registerAnvilRecipe(AnvilRecipe recipe) {
		itemListener.anvilRecipes.add(recipe);
	}
}
