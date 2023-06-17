package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralTeam;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

public class MainGamePhase {
    static void phase(GameHandler game) {
        int secBeforeChest = game.secondsLeft - game.nextChest;
        if (secBeforeChest <= 10 && secBeforeChest >= 0) {
            if (secBeforeChest == 10) {
                game.gameBar = BossBar.bossBar(
                        timerComponent(secBeforeChest),
                        1,
                        BossBar.Color.BLUE,
                        BossBar.Overlay.NOTCHED_10);

                for (Player player : game.gameWorld.getPlayers()) {
                    player.showTitle(Title.title(
                            Component.text("A chest is going to appear!", Style.style(TextColor.color(NamedTextColor.BLUE))),
                            Component.text("Do /arene to teleport your team!", Style.style(TextColor.color(NamedTextColor.AQUA)))));
                    player.showBossBar(game.gameBar);
                }

            }
            game.gameBar.progress((float) secBeforeChest /10);
            game.gameBar.name(timerComponent(secBeforeChest));


            if (secBeforeChest == 0) {
                for (Player player : game.gameWorld.getPlayers()) {
                    player.hideBossBar(game.gameBar);
                    game.nextChest -= game.CHEST_PERIOD;
                    game.gameWorld.getBlockAt(0, game.groundHeight - 11, 0).setType(Material.CHEST);
                    BlockState state = game.gameWorld.getBlockAt(0, game.groundHeight - 11, 0).getState();
                    if (state instanceof Chest chest) {
                        Inventory inventory = chest.getInventory();
                        inventory.setItem(13, new ItemStack(Material.PAPER));
                    }
                }
            }
        }
    }

    static void updatePlayerScoreboards(GameHandler game) {

        int minutes = game.secondsLeft / 60;
        int seconds = game.secondsLeft % 60;

        for (Player player : game.gameWorld.getPlayers()) {

            UUID uuid = player.getUniqueId();
            int score;
            int teamID = game.getTeamID(player);
            MineralTeam team;
            if (teamID == -1) {
                score = -1;
            } else {
                team = game.getTeam(teamID);
                score = team.getScore();
            }
            Scoreboard scoreboard = game.playerScoreboards.get(uuid);
            Objective objective = scoreboard.getObjective("mineral_contest_gui");

            if (objective != null) {
                //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                for (String entry : scoreboard.getEntries()) {
                    switch (objective.getScore(entry).getScore()) {
                        case 4 -> {
                            scoreboard.resetScores(entry);
                            String zero = seconds < 10 ? "0" : "";
                            objective.getScore("§7Time left: §b" + minutes + ":" + zero + seconds).setScore(4);
                        }
                        case 0 -> {
                            scoreboard.resetScores(entry);
                            objective.getScore("§7Points: §b" + score).setScore(0);
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }

    static Component timerComponent(int time) {
        return Component.text("Chest spawn in arena in: ",
                        Style.style(TextColor.color(NamedTextColor.AQUA)))
                .append(Component.text(time,
                        Style.style(TextColor.color(NamedTextColor.BLUE))));
    }
}
