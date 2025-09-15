package io.github.viciscat.mineralcontest.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.Utils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.waypoint.ServerWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

@Mixin(ServerWaypoint.class)
public interface ServerWaypointMixin {

    @Definition(id = "distanceTo", method = "Lnet/minecraft/entity/LivingEntity;distanceTo(Lnet/minecraft/entity/Entity;)F")
    @Expression("(double)?.distanceTo(?) >= ?")
    @ModifyExpressionValue(method = "cannotReceive", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean verifySameTeam(boolean original, LivingEntity source, ServerPlayerEntity receiver) {
        if (original) return true;
        if (!(source instanceof ServerPlayerEntity playerSource)) return false;
        GameSpace space = GameSpaceManager.get().byPlayer(playerSource);
        if (!Utils.isMineralContestSpace(space)) return false;
        if (space != GameSpaceManager.get().byPlayer(receiver)) return false;
        TeamManager teamManager = space.getAttachment(Attachments.TEAM_MANAGER);
        if (teamManager == null) return false;
        GameTeamKey teamKey = teamManager.teamFor(playerSource);
        return teamKey == null || !teamKey.equals(teamManager.teamFor(receiver));
    }
}
