package io.github.viciscat.mineralcontest.phases.play.chest;

import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.util.Attachments;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;

import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.IntSupplier;

public class ArenaChestManager {
    private static final Random RANDOM = new Random();

    public static final int MIN_DELAY = 450 * 20;
    public static final int MAX_DELAY = 1200 * 20;
    public static final int ANNOUNCEMENT_TIME = 30 * 20;
    public static final int FINAL_CHEST_TIME = 120 * 20;
    public static void addTo(GameActivity activity, IntSupplier timeLeftSupplier) {
        ArenaChestManager manager = new ArenaChestManager(activity, timeLeftSupplier);
        activity.getGameSpace().setAttachment(Attachments.ARENA_CHEST_MANAGER, manager);
    }


    private final IntSupplier timeLeftSupplier;
    private final GlobalWidgets globalWidgets;
    private final GameSpace space;

    private final Set<GameTeamKey> teleportedTeams = new ObjectOpenHashSet<>();

    private @Nullable BossBarWidget bossBarWidget = null;
    @Getter
    @Setter
    private int ticksBeforeChest;

    private ArenaChestManager(GameActivity activity, IntSupplier timeLeftSupplier) {
        this.timeLeftSupplier = timeLeftSupplier;
        ticksBeforeChest = RANDOM.nextInt(MIN_DELAY, MAX_DELAY);
        this.globalWidgets = GlobalWidgets.addTo(activity);
        space = activity.getGameSpace();

        activity.listen(GameActivityEvents.TICK, this::onTick);
    }

    public boolean tryTeleportTeam(@NotNull ServerPlayerEntity teleporter) {
        if (ticksBeforeChest > ANNOUNCEMENT_TIME) return false;
        TeamManager teamManager = space.getAttachmentOrThrow(Attachments.TEAM_MANAGER);
        GameTeamKey team = teamManager.teamFor(teleporter);
        if (team == null) return false;
        if (!teleportedTeams.add(team)) return false;
        PlayerSet players = teamManager.playersIn(team);
        Optional<TeamConfig.PositionOrientation> posOpt = space.getAttachmentOrThrow(Attachments.CONFIG).mapConfig().value().teamConfig(team).map(TeamConfig::arenaSpawn);
        if (posOpt.isEmpty()) return false;
        TeamConfig.PositionOrientation pos = posOpt.get();
        for (ServerPlayerEntity player : players) {
            pos.teleport(player, new Vec3d(0, space.getAttachmentOrThrow(Attachments.FLOOR_Y), 0));
            if (player == teleporter) player.sendMessage(Text.translatable("game.mineral_contest.chest.teleportTeam"));
            else player.sendMessage(Text.translatable("game.mineral_contest.chest.teleported", teleporter.getName()));
        }
        return true;
    }

    private void onTick() {
        if (ticksBeforeChest < 0) return;
        ticksBeforeChest--;
        if (ticksBeforeChest == ANNOUNCEMENT_TIME) {
            teleportedTeams.clear();
            bossBarWidget = new BossBarWidget(Text.empty());
            bossBarWidget.setStyle(BossBar.Color.BLUE, BossBar.Style.PROGRESS);
            globalWidgets.addWidget(bossBarWidget);

            space.getPlayers().sendMessage(Text.translatable("game.mineral_contest.chest.message", ticksBeforeChest / 20));
            space.getPlayers().showTitle(Text.translatable("game.mineral_contest.chest.title"), Text.translatable("game.mineral_contest.chest.subTitle"), 2, 40, 10);
        }
        if (bossBarWidget != null) {
            bossBarWidget.setTitle(Text.translatable("game.mineral_contest.chest.in", ticksBeforeChest / 20));
            bossBarWidget.setProgress((float) ticksBeforeChest / ANNOUNCEMENT_TIME);
        }
        if (ticksBeforeChest == 0) {
            globalWidgets.removeWidget(bossBarWidget);
            bossBarWidget = null;

            int floorY = space.getAttachmentOrThrow(Attachments.FLOOR_Y);
            MainConfig config = space.getAttachmentOrThrow(Attachments.CONFIG);
            BlockPos chestPos = config.mapConfig().value().arenaChestPosition().add(0, floorY, 0);
            ServerWorld world = space.getAttachmentOrThrow(Attachments.MAIN_WORLD);

            world.setBlockState(chestPos, ArenaChestBlock.INSTANCE.getDefaultState());

            ticksBeforeChest = RANDOM.nextInt(MIN_DELAY, MAX_DELAY);
            int timeLeft = timeLeftSupplier.getAsInt();
            if (timeLeft - ticksBeforeChest <= FINAL_CHEST_TIME) { // random time is past the limit
                if (timeLeft - MIN_DELAY < FINAL_CHEST_TIME) {
                    ticksBeforeChest = -1;
                } else {
                    ticksBeforeChest = timeLeft - FINAL_CHEST_TIME;
                }
            }
        }
    }
}
