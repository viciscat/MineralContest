package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Random;

public class ClassSelectingPhase {

    static String[] classes = new String[]{"agile", "worker", "robust", "warrior", "miner"};

    public static void phase(GameHandler gameHandler) {
        Random r = new Random();
        if (gameHandler.classSelectSecondsLeft < 0) {
            gameHandler.startGame();
            for (MineralPlayer mineralPlayer: gameHandler.playerManager.getPlayers()) {
                if (mineralPlayer.ClassString() == null) {
                    mineralPlayer.ClassString(classes[r.nextInt(5)]);
                }
            }
        }
    }

    static void updatePlayerScoreboards(GameHandler game) {

        int minutes = game.classSelectSecondsLeft / 60;
        int seconds = game.classSelectSecondsLeft % 60;

        for (MineralPlayer mineralPlayer: game.playerManager.getPlayers()) {
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            String teamString = mineralTeam == null ? "None" : LegacyComponentSerializer.legacySection().serialize(GlobalTranslator.render(mineralTeam.getTeamName(), mineralPlayer.Player().locale())).toUpperCase();
            String classString = mineralPlayer.ClassString();

            Scoreboard scoreboard = mineralPlayer.PlayerScoreboard();
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
                        default -> {
                        }
                    }
                }
            }
        }
    }
}
