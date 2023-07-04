package me.viciscat.mineralcontest;

import me.viciscat.mineralcontest.commands.ArenaCommand;
import me.viciscat.mineralcontest.commands.MCCommandHandler;
import me.viciscat.mineralcontest.commands.TabCompletion;
import me.viciscat.mineralcontest.commands.testing2Command;
import me.viciscat.mineralcontest.game.GameHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class MineralContest extends JavaPlugin {

    public StructureManager structureManager;
    public Map<String, Structure> structureMap = new HashMap<>();
    public Map<World, GameHandler> gameHandlerMap = new HashMap<>();

    public FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        // Plugin startup logic

        getLogger().info("Hello World!");
        structureManager = Bukkit.getStructureManager();
        try {
            structureMap.put("arene", structureManager.loadStructure(getResource("structures/arene.nbt")));
            structureMap.put("arene_cleaner", structureManager.loadStructure(getResource("structures/arene_cleaner.nbt")));
            structureMap.put("castle_red", structureManager.loadStructure(getResource("structures/red_castle.nbt")));
            structureMap.put("castle_blue", structureManager.loadStructure(getResource("structures/blue_castle.nbt")));
            structureMap.put("castle_yellow", structureManager.loadStructure(getResource("structures/yellow_castle.nbt")));
            structureMap.put("castle_green", structureManager.loadStructure(getResource("structures/green_castle.nbt")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(new MineralListener(), this);
        Objects.requireNonNull(getCommand("test2")).setExecutor(new testing2Command());
        PluginCommand mainCommand = getCommand("mineralcontest");
        assert mainCommand != null;
        mainCommand.setExecutor(new MCCommandHandler());
        mainCommand.setTabCompleter(new TabCompletion());
        PluginCommand slashArena = getCommand("arena");
        assert slashArena != null;
        slashArena.setExecutor(new ArenaCommand());

        config.addDefault("gameDuration", 3600);
        config.addDefault("firstChestDelay", 900);
        config.addDefault("chestPeriod", 1200);
        config.options().copyDefaults(true);
        saveConfig();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static public MineralContest instance;
    public MineralContest() {instance = this;}
    public static MineralContest getInstance() {return instance;}

    public void actuallyReloadConfig() {
        reloadConfig();
        config = getConfig();
    }
}
