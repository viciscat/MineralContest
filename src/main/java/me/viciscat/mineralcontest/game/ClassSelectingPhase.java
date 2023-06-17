package me.viciscat.mineralcontest.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Random;
import java.util.UUID;

public class ClassSelectingPhase {

    static String[] classes = new String[]{"agile", "worker", "robust", "warrior", "miner"};

    public static void phase(GameHandler gameHandler) {
        Random r = new Random();
        if (gameHandler.classSelectSecondsLeft < 0) {
            gameHandler.startGame();
            for (Player player : gameHandler.gameWorld.getPlayers()) {
                if (gameHandler.getPlayerClass(player) == null) {
                    gameHandler.setClass(player, classes[r.nextInt(5)]);
                }
            }
        }
    }

    static void updatePlayerScoreboards(GameHandler game) {

        int minutes = game.classSelectSecondsLeft / 60;
        int seconds = game.classSelectSecondsLeft % 60;

        for (Player player : game.gameWorld.getPlayers()) {
            UUID uuid = player.getUniqueId();
            String teamString = game.getTeamString(player);
            String classString = game.getPlayerClass(player);

            Scoreboard scoreboard = game.playerScoreboards.get(uuid);
            Objective objective = scoreboard.getObjective("mineral_contest_gui");

            if (objective != null) {
                //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                for (String entry : scoreboard.getEntries()) {
                    switch (objective.getScore(entry).getScore()) {
                        case 7 -> {
                            scoreboard.resetScores(entry);
                            objective.getScore("§7Class: §f" + classString).setScore(7);
                        }
                        case 4 -> {
                            scoreboard.resetScores(entry);
                            String zero = seconds < 10 ? "0" : "";
                            objective.getScore("§7Starting in: §b" + minutes + ":" + zero + seconds).setScore(4);
                        }
                        case 2 -> {
                            scoreboard.resetScores(entry);
                            objective.getScore("§7> §fTeam: " + teamString).setScore(2);
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }
}
