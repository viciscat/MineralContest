package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;
import java.util.Collections;

public class MainGamePhase {

    static void phase(GameHandler game) {

        TranslatableComponent announcement = Component.translatable("mineral-contest.arena.appear_announcement", Style.style(TextColor.color(NamedTextColor.BLUE)));
        TranslatableComponent announcementSub = Component.translatable("mineral-contest.arena.appear_sub", Style.style(TextColor.color(NamedTextColor.AQUA)));

        int secBeforeChest = game.secondsLeft - game.nextChest;

        // CHEST SPAWNING
        if (secBeforeChest <= 10 && secBeforeChest >= 0) {
            if (secBeforeChest == 10) {
                game.gameBar = BossBar.bossBar(
                        timerComponent(secBeforeChest),
                        1,
                        BossBar.Color.BLUE,
                        BossBar.Overlay.NOTCHED_10);

                for (Player player : game.gameWorld.getPlayers()) {
                    player.showTitle(Title.title(
                            announcement,
                            announcementSub));
                    player.sendMessage(announcement);
                    player.sendMessage(announcementSub);
                    player.showBossBar(game.gameBar);
                }

            }
            game.gameBar.progress((float) secBeforeChest / 10);
            game.gameBar.name(timerComponent(secBeforeChest));


            if (secBeforeChest == 0) {
                GameParameters params = game.parameters;
                int randomizedDelay = game.random.nextInt(params.MIN_SPAWN_DELAY, params.MAX_SPAWN_DELAY);
                if (game.nextChest <= 180) { // Don't want to spawn a chest less than 3 minutes before the end
                    game.nextChest = Integer.MIN_VALUE;
                } else if (params.SPAWN_FINAL_CHEST) {
                    // Will the time before the final chest be too smol?
                    if (game.nextChest - randomizedDelay + 180 < params.MIN_SPAWN_DELAY) {
                        // If we skip to final chest will it too big
                        if (game.nextChest + 180 > params.MAX_SPAWN_DELAY) {
                            game.nextChest -= (params.MIN_SPAWN_DELAY + params.MAX_SPAWN_DELAY) / 2;
                        } else {
                            game.nextChest = 180;
                        }
                    } else {
                        game.nextChest -= randomizedDelay;
                    }


                } else {
                    game.nextChest -= game.random.nextInt(params.MIN_SPAWN_DELAY, params.MAX_SPAWN_DELAY);
                }
                for (Player player : game.gameWorld.getPlayers()) {
                    player.hideBossBar(game.gameBar);

                }
                // spawn the funni chest
                game.arenaChestHandler.start();

            }
        }

        // GAME END
        if (game.secondsLeft == 0) {
            for (Player player : game.gameWorld.getPlayers()) {
                game.pregameTeam.addPlayer(player);
                player.showTitle(Title.title(
                        Component.text("FINISH !"),
                        Component.text("")
                ));
                player.teleport(new Location(game.gameWorld, -25, game.groundHeight + 1, 0));
                player.getInventory().clear();
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
        if (game.secondsLeft == -3) {
            game.gameWorld.sendMessage(Component.text("===== SCORES:"));
        }

        if (game.secondsLeft == -5) {
            MineralTeam[] mineralTeams = new MineralTeam[4];
            for (int i = 0; i < 4; i++) {
                mineralTeams[i] = game.getTeam(i);
            }
            Arrays.sort(mineralTeams, Collections.reverseOrder());
            for (MineralTeam mineralTeam : mineralTeams) {
                game.gameWorld.sendMessage(mineralTeam.getTeamName().append(
                        Component.text(" -> " + mineralTeam.getScore())
                ));
            }
        }
    }

    static void updatePlayerScoreboards(GameHandler game) {

        int minutes = 0;
        int seconds = 0;
        if (game.secondsLeft > 0) {
            minutes = game.secondsLeft / 60;
            seconds = game.secondsLeft % 60;
        }
        for (MineralPlayer mineralPlayer : game.playerManager.getPlayers()) {
            int score;
            MineralTeam mineralTeam = mineralPlayer.getMineralTeam();
            if (mineralTeam == null) {
                score = -1;
            } else {
                score = mineralTeam.getScore();
            }
            Scoreboard scoreboard = mineralPlayer.PlayerScoreboard();
            Objective objective = scoreboard.getObjective("mineral_contest_gui");

            if (objective != null) {
                //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                for (String entry : scoreboard.getEntries()) {
                    switch (objective.getScore(entry).getScore()) {
                        case 4 -> {
                            scoreboard.resetScores(entry);

                            String zero = seconds < 10 ? "0" : "";
                            String timeLeft = GlobalTranslator.translator().translate("mineral-contest.time_left", mineralPlayer.getPlayer().locale()).toPattern();

                            objective.getScore("§7" + timeLeft + ": §b" + minutes + ":" + zero + seconds).setScore(4);
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
        return Component.translatable("mineral-contest.arena.appear_in",
                        Style.style(TextColor.color(NamedTextColor.AQUA)))
                .append(Component.text(time,
                        Style.style(TextColor.color(NamedTextColor.BLUE))));
    }
}
