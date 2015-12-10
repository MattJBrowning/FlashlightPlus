package net.mattslab.FlashlightPlus;

import net.mattslab.FlashlightPlus.api.API;
import net.mattslab.FlashlightPlus.listeners.Commander;
import net.mattslab.FlashlightPlus.listeners.EventListener;
import net.mattslab.FlashlightPlus.listeners.SignListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.mattslab.FlashlightPlus.api.API.checkConfig;

/**
 * Made by Matt
 * This plugin is using the ItemBuilder class made by CraftThatBlock.
 */

public class FlashlightPlus extends JavaPlugin {

    public void onEnable() {
        new API(this);
        new Commander();

        getConfig().options().copyDefaults(true);
        this.saveConfig();
        checkConfig();

        getLogger().info("Please report any issues with this plugin to:");
        getLogger().info("https://github.com/MattsLab/FlashlightPlus/issues/new");

        getServer().getPluginManager().registerEvents(new EventListener(), this);
        getServer().getPluginManager().registerEvents(new SignListener(), this);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                for (Object o : ((HashMap) API.cooldown.clone()).entrySet()) {
                    Map.Entry pairs = (Map.Entry) o;
                    API.cooldown.remove(pairs.getKey());
                    if (((Integer) pairs.getValue()) > 0) {
                        API.cooldown.put((UUID) pairs.getKey(), ((Integer) pairs.getValue()) - 1);
                    }
                }
            }
        }, 20, 20);
    }
}