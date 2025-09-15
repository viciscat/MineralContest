package io.github.viciscat.mineralcontest.util;

import io.github.viciscat.mineralcontest.MineralContest;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class Utils {
    public static boolean boxIntersectsCylinder(Box box, Vec3d center, double radius) {
        double halfWidthX = box.getLengthX() / 2;
        double halfWidthZ = box.getLengthZ() / 2;
        double distX = Math.abs(center.x - (box.minX + halfWidthX));
        double distZ = Math.abs(center.z - (box.minZ + halfWidthZ));
        if (distX > halfWidthX + radius || distZ > halfWidthZ + radius) {
            return false;
        }
        if (distX <= halfWidthX || distZ <= halfWidthZ) {
            return true;
        }
        double x = distX - halfWidthX;
        double z = distZ - halfWidthZ;
        return x * x + z * z <= radius * radius;
    }

    public static TeamManager copyTeamManagerTo(TeamManager manager, GameActivity newActivity) {
        TeamManager copy = TeamManager.addTo(newActivity);
        for (GameTeam team : manager) {
            copy.addTeam(team);
            for (PlayerRef ref : manager.allPlayersIn(team.key())) {
                copy.addPlayerTo(ref, team.key());
            }
        }
        return copy;
    }

    public static void copyTeamManager(GameActivity newActivity) {
        GameSpace space = newActivity.getGameSpace();
        TeamManager manager = space.getAttachmentOrThrow(Attachments.TEAM_MANAGER);
        TeamManager newManager = copyTeamManagerTo(manager, newActivity);
        space.setAttachment(Attachments.TEAM_MANAGER, newManager);
    }

    @Contract("null -> false")
    public static boolean isMineralContestSpace(@Nullable GameSpace space) {
        return space != null && space.getMetadata().sourceConfig().value().type() == MineralContest.GAME_TYPE;
    }

}
