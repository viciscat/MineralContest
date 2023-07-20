package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.commons.lang3.StringUtils;
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
                            String wordTeam = StringUtils.capitalize(PlainTextComponentSerializer.plainText().serialize(GlobalTranslator.render(Component.translatable("mineral-contest.teams.team"), mineralPlayer.Locale())));
                            objective.getScore("§7> §f" + wordTeam + ": " + teamString).setScore(2);
                        }
                        default -> {
                        }
                    }
                }
            }
        }
    }
}
