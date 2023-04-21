package me.viciscat.mineralcontest;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GameHandler implements Runnable{

    MineralContest plugin = JavaPlugin.getPlugin(MineralContest.class);
    Logger logger = plugin.getLogger();

    World gameWorld;
    private BukkitTask schedulerTask;

    public int getSecondsLeft() {
        return secondsLeft;
    }

    int secondsLeft;
    int nextChest;
    int CHEST_PERIOD;
    int groundHeight;

    private BossBar gameBar;


    public boolean gameStarted = false;

    private final MineralTeam[] teams = new MineralTeam[4];
    private final Map<Location, Integer> ecLocationTeamID = new HashMap<>(4);

    public Map<Material, Integer> scoreMap = new HashMap<>();

    public GameHandler(World world, int durationSec, int firstChestDelay, int chestPeriod, int finalHeight) {
        gameWorld = world;
        secondsLeft = durationSec;
        nextChest = durationSec - firstChestDelay;
        CHEST_PERIOD = chestPeriod;
        groundHeight = finalHeight;
        Arrays.fill(teams, new MineralTeam());

        ecLocationTeamID.put(new Location(gameWorld, -59, groundHeight+3, 0), 0);
        ecLocationTeamID.put(new Location(gameWorld, 59, groundHeight+3, 0), 1);
        ecLocationTeamID.put(new Location(gameWorld, 0, groundHeight+3, -59), 2);
        ecLocationTeamID.put(new Location(gameWorld, 0, groundHeight+3, 59), 3);

        scoreMap.put(Material.IRON_INGOT, 10);
        scoreMap.put(Material.GOLD_INGOT, 50);
        scoreMap.put(Material.DIAMOND, 150);
        scoreMap.put(Material.EMERALD, 300);

        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this, 20, 20);
    }

    public void startGame() {
        gameStarted = true;
    }


    /**
     * Ran every second by the {@link org.bukkit.scheduler.BukkitScheduler}
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!gameStarted) return;

        secondsLeft--;

        int secBeforeChest = secondsLeft - nextChest;
        if (secBeforeChest <= 10 && secBeforeChest >= 0) {
            if (secBeforeChest == 10) {
                gameBar = BossBar.bossBar(
                        timerComponent(secBeforeChest),
                        1,
                        BossBar.Color.BLUE,
                        BossBar.Overlay.NOTCHED_10);

                for (Player player : gameWorld.getPlayers()) {
                    player.showTitle(Title.title(
                            Component.text("A chest is going to appear!", Style.style(TextColor.color(NamedTextColor.BLUE))),
                            Component.text("Do /arene to teleport your team!", Style.style(TextColor.color(NamedTextColor.AQUA)))));
                    player.showBossBar(gameBar);
                }

            }
            gameBar.progress((float) secBeforeChest /10);
            gameBar.name(timerComponent(secBeforeChest));


            if (secBeforeChest == 0) {
                for (Player player : gameWorld.getPlayers()) {
                    player.hideBossBar(gameBar);
                    nextChest -= CHEST_PERIOD;
                    gameWorld.getBlockAt(0, groundHeight - 11, 0).setType(Material.CHEST);
                    BlockState state = gameWorld.getBlockAt(0, groundHeight - 11, 0).getState();
                    if (state instanceof Chest) {
                        Chest chest = (Chest) state;
                        Inventory inventory = chest.getInventory();
                        inventory.setItem(13, new ItemStack(Material.PAPER));
                    }
                }
            }
        }

    }

    private Component timerComponent(int time) {
        return Component.text("Chest spawn in arena in: ",
                        Style.style(TextColor.color(NamedTextColor.AQUA)))
                .append(Component.text(time,
                        Style.style(TextColor.color(NamedTextColor.BLUE))));
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
}
