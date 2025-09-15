package io.github.viciscat.mineralcontest.phases.play.chest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.Commands;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import xyz.nucleoid.plasmid.api.game.GameSpace;

public class ArenaChestCommand {

    private static final SimpleCommandExceptionType TELEPORT_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("game.mineral_contest.chest.couldNotTeleport"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("arena")
                .executes(context -> {
                    GameSpace space = Commands.getSpace(context);
                    if (space == null) throw Commands.GAME_SPACE_EXCEPTION.create();
                    ArenaChestManager chestManager = space.getAttachment(Attachments.ARENA_CHEST_MANAGER);
                    if (chestManager == null) throw Commands.CHEST_MANAGER_EXCEPTION.create();
                    if (chestManager.tryTeleportTeam(context.getSource().getPlayerOrThrow())) return Command.SINGLE_SUCCESS;
                    throw TELEPORT_EXCEPTION.create();
                })
                .requires(Commands::isInMineralContest)
        );
    }
}
