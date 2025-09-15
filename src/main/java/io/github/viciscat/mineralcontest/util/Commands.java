package io.github.viciscat.mineralcontest.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.GameSpaceManager;

public class Commands {
    public static final SimpleCommandExceptionType GAME_SPACE_EXCEPTION = new SimpleCommandExceptionType(Text.literal("Not in a mineral contest game space!"));
    public static final SimpleCommandExceptionType CHEST_MANAGER_EXCEPTION = new SimpleCommandExceptionType(Text.literal("No arena chest manager in this space!"));

    public static @Nullable GameSpace getSpace(CommandContext<ServerCommandSource> context) {
        return context.getSource().getPlayer() == null ? null : GameSpaceManager.get().byPlayer(context.getSource().getPlayer());
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mcontest")
                .requires(source -> source.hasPermissionLevel(2) && isInMineralContest(source))
                .then(CommandManager.literal("chest")
                        .executes(context -> executeArenaChestInfo(context.getSource(), getSpace(context)))
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(-1))
                                .executes(context -> executeSetArenaChestTicks(context.getSource(), getSpace(context), IntegerArgumentType.getInteger(context, "ticks")))
                        )
                        
                )
        );
    }

    private static int executeArenaChestInfo(ServerCommandSource source, GameSpace space) throws CommandSyntaxException {
        if (!Utils.isMineralContestSpace(space)) throw GAME_SPACE_EXCEPTION.create();
        ArenaChestManager chestManager = space.getAttachment(Attachments.ARENA_CHEST_MANAGER);
        if (chestManager == null) throw CHEST_MANAGER_EXCEPTION.create();
        source.sendMessage(Text.literal("Ticks until next chest: " + chestManager.getTicksBeforeChest()));
        return Command.SINGLE_SUCCESS;
    }
    
    private static int executeSetArenaChestTicks(ServerCommandSource source, GameSpace space, int ticks) throws CommandSyntaxException {
        if (!Utils.isMineralContestSpace(space)) throw GAME_SPACE_EXCEPTION.create();
        ArenaChestManager chestManager = space.getAttachment(Attachments.ARENA_CHEST_MANAGER);
        if (chestManager == null) throw CHEST_MANAGER_EXCEPTION.create();
        chestManager.setTicksBeforeChest(ticks);
        source.sendMessage(Text.literal("Set arena chest ticks: " + ticks));
        return Command.SINGLE_SUCCESS;
    }

    public static boolean isInMineralContest(ServerCommandSource source) {
        return Utils.isMineralContestSpace(GameSpaceManager.get().byPlayer(source.getPlayer()));
    }
}
