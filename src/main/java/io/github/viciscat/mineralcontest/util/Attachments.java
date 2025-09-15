package io.github.viciscat.mineralcontest.util;

import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.phases.play.PlayerClassManager;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestManager;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;

public final class Attachments {
    public static final GameAttachment<MainConfig> CONFIG = GameAttachment.create(MineralContest.id("config"));
    public static final GameAttachment<ServerWorld> MAIN_WORLD = GameAttachment.create(MineralContest.id("main_world"));
    public static final GameAttachment<TeamManager> TEAM_MANAGER = GameAttachment.create(MineralContest.id("team_manager"));
    public static final GameAttachment<PlayerClassManager> PLAYER_CLASS_MANAGER = GameAttachment.create(MineralContest.id("player_class_manager"));
    public static final GameAttachment<ArenaChestManager> ARENA_CHEST_MANAGER = GameAttachment.create(MineralContest.id("arena_chest_manager"));
    public static final GameAttachment<Integer> FLOOR_Y = GameAttachment.create(MineralContest.id("floor_y"));
    /**
     * The map is mutable. However, it becomes immutable when the game ends.
     */
    public static final GameAttachment<Object2FloatMap<GameTeamKey>> SCORES = GameAttachment.create(MineralContest.id("scores"));

}
