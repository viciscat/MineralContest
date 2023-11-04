package me.viciscat.mineralcontest;

import me.viciscat.mineralcontest.commands.*;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.listeners.ArenaChestListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public final class MineralContest extends JavaPlugin {

    public StructureManager structureManager;
    public Map<String, Structure> structureMap = new HashMap<>();
    public Map<World, GameHandler> gameHandlerMap = new HashMap<>();

    public InputStream levelDat;

    public FileConfiguration config = getConfig();

    @SuppressWarnings("DataFlowIssue")
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
            levelDat = getResource("level.dat");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getServer().getPluginManager().registerEvents(new MineralListener(), this);
        getServer().getPluginManager().registerEvents(new ArenaChestListener(), this);
        Objects.requireNonNull(getCommand("test2")).setExecutor(new testing2Command());
        Objects.requireNonNull(getCommand("test3")).setExecutor(new testing3Command());
        PluginCommand mainCommand = getCommand("mineralcontest");
        assert mainCommand != null;
        mainCommand.setExecutor(new MCCommandHandler());
        mainCommand.setTabCompleter(new TabCompletion());
        PluginCommand slashArena = getCommand("arena");
        assert slashArena != null;
        slashArena.setExecutor(new ArenaCommand());

        // CONFIG DEFAULTS
        config.addDefault("gameDuration", 3600);
        config.setComments("gameDuration", List.of("How long the game will last, in seconds. Default: 3600"));
        config.addDefault("minChestDelay", 450);
        config.setComments("minChestDelay", List.of("Minimum delay between chest spawns. (seconds) Default: 450"));
        config.addDefault("maxChestDelay", 1200);
        config.setComments("maxChestDelay", List.of("Maximum delay between chest spawns. (seconds) Default: 1200"));
        config.addDefault("spawnFinalChest", true);
        config.setComments("spawnFinalChest", List.of("Should a chest always spawn 3 minutes before the end. Default: true"));
        config.options().copyDefaults(true);

        // START LOOT
        List<ItemStack> stuff = new ArrayList<>(List.of(
                new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.BOW),
                new ItemStack(Material.COOKED_BEEF, 16),
                new ItemStack(Material.IRON_HELMET),
                new ItemStack(Material.IRON_CHESTPLATE),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.IRON_BOOTS)));

        for (ItemStack itemStack : stuff) {
            itemStack.editMeta(itemMeta -> {
                //noinspection DataFlowIssue
                itemMeta.getPersistentDataContainer().set(NamespacedKey.fromString("no_drop", MineralContest.getInstance()), PersistentDataType.BOOLEAN, true);
                itemMeta.lore(List.of(Component.text("Item does not drop on death", Style.style(NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))));
            });
        }
        stuff.add(new ItemStack(Material.ARROW, 32));

        config.addDefault("startLoot", stuff);
        config.setComments("startLoot", List.of(
                "The stuff that the players will have when the game starts and when they respawn. Anything that is armor will be auto-equipped",
                "Please don't edit it here, use '/mcontest config startLoot set' to set your current inventory as the new load-out.",
                "To make it not drop on death add:",
                "meta:",
                "    PublicBukkitValues:",
                "      mineral-contest:no_drop: 1b",
                "to the ItemStack object."));
        config.addDefault("minimumFood", 10);
        config.setComments("minimumFood", List.of(
                "The players' food level will never go underneath this value. Default: 10 (5 little meat things)"
        ));
        config.addDefault("worldNamePrefix", "mineral-contest_");
        config.setComments("worldNamePrefix", List.of(
                "All mini game worlds name will start with that string"
        ));
        saveConfig();
        actuallyReloadConfig();

        //noinspection DataFlowIssue
        TranslationRegistry translationRegistry = TranslationRegistry.create(NamespacedKey.fromString("localization", this));
        ResourceBundle resourceBundleEN = ResourceBundle.getBundle("translations.main", Locale.ENGLISH, UTF8ResourceBundleControl.get());
        ResourceBundle resourceBundleFR = ResourceBundle.getBundle("translations.main", Locale.FRENCH, UTF8ResourceBundleControl.get());
        translationRegistry.registerAll(Locale.ENGLISH, resourceBundleEN, false);
        translationRegistry.registerAll(Locale.FRENCH, resourceBundleFR, false);
        GlobalTranslator.translator().addSource(translationRegistry);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    static public MineralContest instance;

    public MineralContest() {
        instance = this;
    }

    public static MineralContest getInstance() {
        return instance;
    }

    public void actuallyReloadConfig() {
        reloadConfig();
        config = getConfig();
    }
}
