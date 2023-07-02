package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.MineralTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class RespawnPeriod extends BukkitRunnable {

    Player respawningPlayer;
    GameHandler gameHandler;
    int time = 10;

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        if (!respawningPlayer.isOnline()) {
            cancel();
            return;
        }
        if (time == -1) {
            respawningPlayer.setGameMode(GameMode.SURVIVAL);
            MineralTeam mineralTeam = gameHandler.playerManager.getPlayer(respawningPlayer).MineralTeam();
            respawningPlayer.teleport(mineralTeam == null ? new Location(gameHandler.gameWorld, 0, gameHandler.groundHeight + 15, 0) : mineralTeam.getSpawnLocation());
            cancel();

            return;
        }
        respawningPlayer.showTitle(Title.title(
                Component.text("Tu es mort sale noub", Style.style(TextColor.color(NamedTextColor.DARK_RED))),
                Component.text("Tu vas réaparaitre comme par magie dans ", Style.style(TextColor.color(NamedTextColor.GOLD))).append(
                        Component.text(time, TextColor.color(NamedTextColor.YELLOW))).append(
                                Component.text(" secondes !", TextColor.color(NamedTextColor.GOLD))
                ), Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ZERO)));
        time--;
    }

    public RespawnPeriod(Player player, GameHandler gameHandler1) {
        respawningPlayer = player;
        gameHandler = gameHandler1;
        runTaskTimer(JavaPlugin.getPlugin(MineralContest.class), 20, 20);
    }
}
