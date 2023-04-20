package me.viciscat.mineralcontest;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

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

    public GameHandler(World world, int durationSec, int firstChestDelay, int chestPeriod, int finalHeight) {
        gameWorld = world;
        secondsLeft = durationSec;
        nextChest = durationSec - firstChestDelay;
        CHEST_PERIOD = chestPeriod;
        groundHeight = finalHeight;
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
}
