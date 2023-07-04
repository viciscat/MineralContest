package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.*;
import me.viciscat.mineralcontest.ui.ClassSelectUI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

public class GameHandler implements Runnable{

    public enum Phase {
        PREGAME,
        CLASS_SELECTING,
        GAME
    }

    MineralContest plugin = JavaPlugin.getPlugin(MineralContest.class);
    Logger logger = plugin.getLogger();

    World gameWorld;
    private final BukkitTask schedulerTask;

    public int getSecondsLeft() {
        return secondsLeft;
    }

    int secondsLeft; // seconds left till the end of the game
    int classSelectSecondsLeft = 45; // seconds left till the end class selection CRAZY
    int nextChest; // Time when next chest spawns

    public int getNextChest() {
        return nextChest;
    }

    int CHEST_PERIOD; // How often chest spawns
    public int groundHeight;

    BossBar gameBar;


    public Phase gamePhase = Phase.PREGAME;

    private final MineralTeam[] teams;

    public Map<Material, Integer> scoreMap = new HashMap<>();

    public final Scoreboard gameScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    public Team pregameTeam = gameScoreboard.registerNewTeam("pregame");

    public PlayerManager playerManager = new PlayerManager();



    public GameHandler(World world, int durationSec, int firstChestDelay, int chestPeriod, int finalHeight) {
        gameWorld = world;
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        secondsLeft = durationSec;
        nextChest = durationSec - firstChestDelay;
        CHEST_PERIOD = chestPeriod;
        groundHeight = finalHeight;

        teams = new MineralTeam[]{
                new MineralTeam(Component.text("RED", NamedTextColor.RED),
                        "§cRED",
                        BoundingBox.of(new Location(gameWorld, -49, groundHeight, 9), new Location(gameWorld, -67, groundHeight + 20, -9)),
                        new Location(gameWorld, -55, groundHeight+3, 0),
                        new Location(gameWorld, -59, groundHeight+3, 0),
                        new Location(gameWorld, -19d, groundHeight-8, 0.5),
                        gameScoreboard.registerNewTeam("red")),
                new MineralTeam(Component.text("BLUE", NamedTextColor.BLUE),
                        "§9BLUE",
                        BoundingBox.of(new Location(gameWorld, 49, groundHeight, -9), new Location(gameWorld, 67, groundHeight + 20, 9)),
                        new Location(gameWorld, 55, groundHeight+3, 0),
                        new Location(gameWorld, 59, groundHeight+3, 0),
                        new Location(gameWorld, 20d, groundHeight-8, 0.5),
                        gameScoreboard.registerNewTeam("blue")),
                new MineralTeam(Component.text("YELLOW", NamedTextColor.YELLOW),
                        "§eYELLOW",
                        BoundingBox.of(new Location(gameWorld, -9, groundHeight, -49), new Location(gameWorld, 9, groundHeight + 20, -67)),
                        new Location(gameWorld, 0, groundHeight+3, -55),
                        new Location(gameWorld, 0, groundHeight+3, -59),
                        new Location(gameWorld, 0.5d, groundHeight-8, -19d),
                        gameScoreboard.registerNewTeam("yellow")),
                new MineralTeam(Component.text("GREEN", NamedTextColor.GREEN),
                        "§aGREEN",
                        BoundingBox.of(new Location(gameWorld, 9, groundHeight, 49), new Location(gameWorld, -9, groundHeight + 20, 67)),
                        new Location(gameWorld, 0, groundHeight+3, 55),
                        new Location(gameWorld, 0, groundHeight+3, 59),
                        new Location(gameWorld, 0.5d, groundHeight-8, 20d),
                        gameScoreboard.registerNewTeam("green")),
        };

        scoreMap.put(Material.COPPER_INGOT, 4);
        scoreMap.put(Material.IRON_INGOT, 10);
        scoreMap.put(Material.GOLD_INGOT, 50);
        scoreMap.put(Material.DIAMOND, 150);
        scoreMap.put(Material.EMERALD, 300);

        pregameTeam.setAllowFriendlyFire(false);
        for (MineralTeam mineralTeam : teams) {
            mineralTeam.getTeam().setAllowFriendlyFire(false);
        }


        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 20, 20);
    }

    public void startGame() {
        gamePhase = Phase.GAME;
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        for (MineralPlayer mineralPlayer : playerManager.getPlayers()) {
            Player player = mineralPlayer.Player();
            player.getInventory().clear();
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            if (mineralTeam != null) {
                Team team = mineralTeam.getTeam();
                team.addPlayer(player);
                player.registerAttribute(Attribute.GENERIC_MAX_HEALTH);
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                assert healthAttr != null;
                switch (mineralPlayer.ClassString()){
                    case "worker" -> {
                        mineralTeam.addScoreMultiplier(0.25f);
                        healthAttr.addModifier(new AttributeModifier("mineralcontest_worker", -10, AttributeModifier.Operation.ADD_NUMBER));
                    }
                    case "agile" -> {
                        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        assert speedAttr != null;
                        double base = speedAttr.getBaseValue();
                        speedAttr.addModifier(new AttributeModifier("mineralcontest_agile", 0.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                        speedAttr.setBaseValue(base);
                    }
                    case "robust" -> healthAttr.addModifier(new AttributeModifier("mineralcontest_robust", 10, AttributeModifier.Operation.ADD_NUMBER));

                    case "miner" -> {
                        for (int i = 9; i < 18; i++) {
                            player.getInventory().setItem(i, new ItemStack(Material.BARRIER));
                        }
                    }
                    case "warrior" -> healthAttr.addModifier(new AttributeModifier("mineralcontest_warrior", -4, AttributeModifier.Operation.ADD_NUMBER));

                }
                double health = healthAttr.getValue();
                player.setHealth(health);
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setGameMode(GameMode.SURVIVAL);
                player.getInventory().addItem(
                        new ItemStack(Material.STONE_SWORD),
                        new ItemStack(Material.BOW),
                        new ItemStack(Material.ARROW, 32),
                        new ItemStack(Material.COOKED_BEEF, 16));
            }
        }
        for (Player player : gameWorld.getPlayers()) {
            resetPlayerScoreboard(player);
        }
        fillArea(1, groundHeight + 3, 52, -1, groundHeight + 6, 52, Material.AIR, Material.OAK_FENCE);
        fillArea(1, groundHeight + 3, -52, -1, groundHeight + 6, -52, Material.AIR, Material.OAK_FENCE);
        fillArea(52, groundHeight + 3, 1, 52, groundHeight + 6, -1, Material.AIR, Material.OAK_FENCE);
        fillArea(-52, groundHeight + 3, 1, -52, groundHeight + 6, -1, Material.AIR, Material.OAK_FENCE);
    }

    public void startClassSelection() {
        gamePhase = Phase.CLASS_SELECTING;
        gameWorld.getWorldBorder().setSize(800);
        for (MineralPlayer mineralPlayer: playerManager.getPlayers()) {
            Player player = mineralPlayer.Player();
            resetPlayerScoreboard(player);
            player.getInventory().clear();
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            if (mineralTeam == null) {
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                player.teleport(mineralTeam.getSpawnLocation());
                player.setBedSpawnLocation(mineralTeam.getSpawnLocation(), true);
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                ItemMeta swordMeta = sword.getItemMeta();
                swordMeta.getPersistentDataContainer().set(NamespacedKey.fromString("selection_item", MineralContest.getInstance()), PersistentDataType.BOOLEAN, true);
                swordMeta.displayName(Component.text("Right click to select your class!").decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
                sword.setItemMeta(swordMeta);
                player.getInventory().setItem(8, sword);
                ClassSelectUI.openUI(player, this);
            }
        }
        fillArea(1, groundHeight + 3, 52, -1, groundHeight + 6, 52, Material.OAK_FENCE, Material.AIR);
        fillArea(1, groundHeight + 3, -52, -1, groundHeight + 6, -52, Material.OAK_FENCE, Material.AIR);
        fillArea(52, groundHeight + 3, 1, 52, groundHeight + 6, -1, Material.OAK_FENCE, Material.AIR);
        fillArea(-52, groundHeight + 3, 1, -52, groundHeight + 6, -1, Material.OAK_FENCE, Material.AIR);
    }


    /**
     * Ran every second by the {@link org.bukkit.scheduler.BukkitScheduler}
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (gamePhase == Phase.PREGAME) {
            PreGamePhase.updatePlayerScoreboards(this);
        } else if (gamePhase == Phase.CLASS_SELECTING) {
            classSelectSecondsLeft --;
            ClassSelectingPhase.phase(this);
            ClassSelectingPhase.updatePlayerScoreboards(this);
        } else if (gamePhase == Phase.GAME) {

            secondsLeft--;

            MainGamePhase.phase(this);
            MainGamePhase.updatePlayerScoreboards(this);
        }
    }

    public @Nullable MineralTeam getTeam(Location enderChestLocation) {
        for (MineralTeam mineralTeam : teams) {
            if (mineralTeam.getEnderChestLocation().equals(enderChestLocation)) {
                return mineralTeam;
            }
        }
        return null;
    }

    public @Nullable MineralTeam getTeam(int id) {
        if (id < 0) return null;
        if (id > teams.length-1) return null;
        return teams[id];
    }

    public void resetPlayerScoreboard(Player player) {
        resetPlayerScoreboard(playerManager.getPlayer(player));
    }
    public void resetPlayerScoreboard(MineralPlayer mineralPlayer) {

        // Get player scoreboard and stuff
        Scoreboard scoreboard = mineralPlayer.PlayerScoreboard();
        Objective objective = scoreboard.getObjective("mineral_contest_gui");
        if (objective == null) return;

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear it
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        MineralTeam team = mineralPlayer.MineralTeam();
        String teamString;
        if (team == null) {
            teamString = "None";
        } else {
            teamString = team.getTeamNameScoreboard();
        }
        Player player = mineralPlayer.Player();
        // If game is running
        switch (gamePhase) {
            case PREGAME -> {
                objective.getScore(" ").setScore(9);
                objective.getScore("§7> §f" + PlainTextComponentSerializer.plainText().serialize(player.displayName())).setScore(8);
                objective.getScore("  ").setScore(7);
                objective.getScore("   ").setScore(6);
                objective.getScore("§7> §fSelect your team!").setScore(5);
                objective.getScore("§o/mineralcontest start").setScore(4);
                objective.getScore("§oto start!").setScore(3);
                objective.getScore("§7> §fTeam: " + "None").setScore(2);
                objective.getScore("     ").setScore(1);
                objective.getScore("      ").setScore(0);
            }

            case CLASS_SELECTING -> {
                objective.getScore(" ").setScore(9);
                objective.getScore("§7> §f" + PlainTextComponentSerializer.plainText().serialize(player.displayName())).setScore(8);
                objective.getScore("§7Class: §bNone" + mineralPlayer.ClassString()).setScore(7);
                objective.getScore("  ").setScore(6);
                objective.getScore("§7> §fSelect your class!").setScore(5);
                objective.getScore("Time").setScore(4);
                objective.getScore("   ").setScore(3);
                objective.getScore("§7> §fTeam: " + teamString).setScore(2);
                objective.getScore("    ").setScore(1);
                objective.getScore("     ").setScore(0);
            }

            case GAME -> {

                objective.getScore(" ").setScore(9);
                objective.getScore("§7> §f" + PlainTextComponentSerializer.plainText().serialize(player.displayName())).setScore(8);
                objective.getScore("§7Class: §b" + mineralPlayer.ClassString()).setScore(7);
                objective.getScore("  ").setScore(6);
                objective.getScore("§7> §fGame").setScore(5);
                objective.getScore("Time").setScore(4);
                objective.getScore("   ").setScore(3);
                objective.getScore("§7> §fTeam: " + teamString).setScore(2);
                if (team == null) {
                    objective.getScore("§7Booster: §fNone").setScore(1);
                }else {
                    objective.getScore("§7Booster: §f" + (100 * team.getScoreMultiplier() - 100) + "%").setScore(1);
                }
                objective.getScore("Score").setScore(0);
            }

        }
        player.setScoreboard(scoreboard);
    }

    public boolean isInEnemyCastle(MineralTeam team, Location location) {
        if (team == null) return false;
        for (MineralTeam mineralTeam : teams) {
            if (mineralTeam.BoundingBox().contains(location.toVector()) && mineralTeam != team) {
                return true;}
        }
        return false;
    }

    private void fillArea(int x1, int y1, int z1, int x2, int y2, int z2, Material materialToPlace, Material materialToReplace) {
        for (int i = Math.min(x1, x2); i <= Math.max(x1, x2); i++) {
            for (int j = Math.min(y1, y2); j <= Math.max(y1, y2); j++) {
                for (int k = Math.min(z1, z2); k <= Math.max(z1, z2); k++) {
                    Block block = gameWorld.getBlockAt(i, j, k);
                    if (block.getType() == materialToReplace) {
                        block.setType(materialToPlace);
                    }
                }
            }
        }
    }
}
