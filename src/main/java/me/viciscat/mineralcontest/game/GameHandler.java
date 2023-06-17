package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.ui.ClassSelectUI;
import me.viciscat.mineralcontest.ui.TeamSelectUI;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
    private BukkitTask schedulerTask;

    public int getSecondsLeft() {
        return secondsLeft;
    }

    int secondsLeft; // seconds left till the end of the game
    int classSelectSecondsLeft = 36; // seconds left till the end of the game
    int nextChest; // Time when next chest spawns
    int CHEST_PERIOD; // How often chest spawns
    public int groundHeight;

    BossBar gameBar;


    public Phase gamePhase = Phase.PREGAME;

    private final MineralTeam[] teams = new MineralTeam[4];
    private final Location[] spawnLocations = new Location[4];
    private final Map<Location, Integer> ecLocationTeamID = new HashMap<>(4);


    public final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();

    public void setClass(Player player, String classString) {
        setClass(player.getUniqueId(), classString);
    }
    public void setClass(UUID playerUUID, String classString) {
        if (playerClasses.containsKey(playerUUID)) {
            playerClasses.replace(playerUUID, classString);
        } else {
            playerClasses.put(playerUUID, classString);
        }
    }

    public String getPlayerClass(Player player) {
        return getPlayerClass(player.getUniqueId());
    }

    public String getPlayerClass(UUID playerUUID) {
        return playerClasses.get(playerUUID);
    }


    private final Map<UUID, String> playerClasses = new HashMap<>();

    public Map<Material, Integer> scoreMap = new HashMap<>();

    private final Scoreboard gameScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

    public Team pregameTeam = gameScoreboard.registerNewTeam("pregame");

    private final Team[] playerTeams = new Team[]{
            gameScoreboard.registerNewTeam("yellow"),
            gameScoreboard.registerNewTeam("red"),
            gameScoreboard.registerNewTeam("blue"),
            gameScoreboard.registerNewTeam("green")};

    public GameHandler(World world, int durationSec, int firstChestDelay, int chestPeriod, int finalHeight) {
        gameWorld = world;
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        secondsLeft = durationSec;
        nextChest = durationSec - firstChestDelay;
        CHEST_PERIOD = chestPeriod;
        groundHeight = finalHeight;
        for (int i = 0; i < teams.length; i++) {
            teams[i] = new MineralTeam();
        }

        spawnLocations[0] = new Location(gameWorld, -55, groundHeight+3, 0);
        spawnLocations[1] = new Location(gameWorld, 55, groundHeight+3, 0);
        spawnLocations[2] = new Location(gameWorld, 0, groundHeight+3, -55);
        spawnLocations[3] = new Location(gameWorld, 0, groundHeight+3, 55);

        ecLocationTeamID.put(new Location(gameWorld, -59, groundHeight+3, 0), 0);
        ecLocationTeamID.put(new Location(gameWorld, 59, groundHeight+3, 0), 1);
        ecLocationTeamID.put(new Location(gameWorld, 0, groundHeight+3, -59), 2);
        ecLocationTeamID.put(new Location(gameWorld, 0, groundHeight+3, 59), 3);

        scoreMap.put(Material.COPPER_INGOT, 4);
        scoreMap.put(Material.IRON_INGOT, 10);
        scoreMap.put(Material.GOLD_INGOT, 50);
        scoreMap.put(Material.DIAMOND, 150);
        scoreMap.put(Material.EMERALD, 300);

        pregameTeam.setAllowFriendlyFire(false);
        for (Team playerTeam : playerTeams) {
            playerTeam.setAllowFriendlyFire(false);
        }


        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 20, 20);
    }

    public void startGame() {
        gamePhase = Phase.GAME;
        gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        for (Player player : gameWorld.getPlayers()) {
            int teamID = getTeamID(player);
            if (teamID != -1) {
                Team playerTeam = playerTeams[teamID];
                playerTeam.addPlayer(player);

                switch (getPlayerClass(player)){
                    case "worker" -> getTeam(teamID).addScoreMultiplier(0.25f);
                    case "agile" -> {
                        player.registerAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                        assert speedAttr != null;
                        speedAttr.addModifier(new AttributeModifier("mineralcontest_agile", 0.2, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
                    }
                    case "robust" -> {
                        player.registerAttribute(Attribute.GENERIC_MAX_HEALTH);
                        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        assert healthAttr != null;
                        healthAttr.addModifier(new AttributeModifier("mineralcontest_robust", 10, AttributeModifier.Operation.ADD_NUMBER));

                    }
                }
                AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double health = 20;
                if (healthAttr != null) {
                    health = healthAttr.getValue();
                }
                player.setHealth(health);

                if (Objects.equals(getPlayerClass(player), "worker")) {
                    getTeam(teamID).addScoreMultiplier(0.25f);
                }
            }
        }
        for (Player player : gameWorld.getPlayers()) {
            resetPlayerScoreboard(player);
        }
    }

    public void startClassSelection() {
        gamePhase = Phase.CLASS_SELECTING;
        gameWorld.getWorldBorder().setSize(800);
        for (Player player : gameWorld.getPlayers()) {
            resetPlayerScoreboard(player);
            player.getInventory().clear();
            if (getTeamID(player) == -1) {
                player.setGameMode(GameMode.SPECTATOR);
            } else {
                player.teleport(spawnLocations[getTeamID(player)]);
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                ItemMeta swordMeta = sword.getItemMeta();
                swordMeta.displayName(Component.text("Right click to select your class!").decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
                sword.setItemMeta(swordMeta);
                player.getInventory().setItem(8, sword);
                ClassSelectUI.openUI(player, this);
            }
        }
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





    public int getTeamID(Player player) {
        for (int i = 0; i < teams.length; i++) {
            if (teams[i].playerInTeam(player)) return i;
        }
        return -1;
    }

    public int getTeamID(Location enderChestLocation) {
        if (!ecLocationTeamID.containsKey(enderChestLocation)) return -1;
        return ecLocationTeamID.get(enderChestLocation);
    }

    public MineralTeam getTeam(int id) {
        return teams[id];
    }

    public void resetPlayerScoreboard(Player player) {
        // Get player scoreboard and stuff
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        Objective objective = scoreboard.getObjective("mineral_contest_gui");
        if (objective == null) return;

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Clear it
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        MineralTeam team = getTeam(getTeamID(player));
        String teamString = getTeamString(player);


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
                objective.getScore("§7Class: §bNone" + getPlayerClass(player)).setScore(7);
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
                objective.getScore("§7Class: §b" + getPlayerClass(player)).setScore(7);
                objective.getScore("  ").setScore(6);
                objective.getScore("§7> §fGame").setScore(5);
                objective.getScore("Time").setScore(4);
                objective.getScore("   ").setScore(3);
                objective.getScore("§7> §fTeam: " + teamString).setScore(2);
                objective.getScore("§7Booster: §f" + (100*team.getScoreMultiplier()-100) + "%").setScore(1);
                objective.getScore("Score").setScore(0);
            }

        }
        player.setScoreboard(scoreboard);
    }

    public String getTeamString(Player player) {
        String[] colorCode = new String[] {"§c", "§9", "§e", "§a"};
        int teamID = getTeamID(player);
        String teamString;
        if (teamID == -1) {
            teamString = "None";
        } else {
            teamString = colorCode[teamID] + TeamSelectUI.teamColors[teamID];
        }
        return teamString;
    }
}
