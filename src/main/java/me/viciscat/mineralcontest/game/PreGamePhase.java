package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class PreGamePhase {

    public static void phase(GameHandler gameHandler) {

    }

    static void updatePlayerScoreboards(GameHandler game) {
        for (MineralPlayer mineralPlayer: game.playerManager.getPlayers()) {
            MineralTeam mineralTeam = mineralPlayer.MineralTeam();
            String teamString = mineralTeam == null ? "None" : mineralTeam.getTeamNameScoreboard();

            Scoreboard scoreboard = mineralPlayer.PlayerScoreboard();
            Objective objective = scoreboard.getObjective("mineral_contest_gui");

            if (objective != null) {
                //objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                for (String entry : scoreboard.getEntries()) {
                    switch (objective.getScore(entry).getScore()) {
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
