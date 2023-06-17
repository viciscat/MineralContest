package me.viciscat.mineralcontest.game;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.UUID;

public class PreGamePhase {

    public static void phase(GameHandler gameHandler) {

    }

    static void updatePlayerScoreboards(GameHandler game) {
        for (Player player : game.gameWorld.getPlayers()) {
            UUID uuid = player.getUniqueId();
            String teamString = game.getTeamString(player);

            Scoreboard scoreboard = game.playerScoreboards.get(uuid);
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
