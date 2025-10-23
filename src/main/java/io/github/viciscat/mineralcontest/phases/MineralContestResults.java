package io.github.viciscat.mineralcontest.phases;

import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.GuiUtils;
import io.github.viciscat.mineralcontest.util.MineralContestRules;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

class MineralContestResults {

    private static final int[] TOP_3_COLORS = new int[]{
            0xFF_FFD700,
            0xFF_C0C0C0,
            0xFF_CD7F32
    };

    private final GameSpace space;

    private int ticks = 0;

    MineralContestResults(GameActivity activity) {
        activity.deny(MineralContestRules.ALL_DAMAGE);
        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.INTERACTION);

        space = activity.getGameSpace();

        activity.listen(GamePlayerEvents.ADD, this::onAddPlayer);
        activity.listen(GameActivityEvents.TICK, this::onTick);
    }


    private void onAddPlayer(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.teleport(20, space.getAttachmentOrThrow(Attachments.FLOOR_Y) + 1, 0, false);
        player.getInventory().clear();
    }

    private void onTick() {
        ticks++;
        switch (ticks) {
            case 10 * 20 -> {
                for (int i = 0; i < 5; i++) space.getPlayers().sendMessage(Text.empty());
                space.getPlayers().sendMessage(Text.translatable("game.mineral_contest.scores"));
            }
            case 15 * 20 -> {
                List<Object2FloatMap.Entry<GameTeamKey>> list = space.getAttachmentOrThrow(Attachments.SCORES).object2FloatEntrySet().stream()
                        .sorted(Comparator.<Object2FloatMap.Entry<GameTeamKey>>comparingDouble(Object2FloatMap.Entry::getFloatValue).reversed())
                        .toList();
                MapConfig config = space.getAttachmentOrThrow(Attachments.CONFIG).mapConfig().value();
                for (int i = 0; i < list.size(); i++) {
                    Object2FloatMap.Entry<GameTeamKey> entry = list.get(i);
                    Optional<TeamConfig> team = config.teamConfig(entry.getKey());
                    Text text = Text.literal(String.valueOf(i + 1))
                            .formatted(Formatting.BOLD)
                            .withColor(i < 3 ? TOP_3_COLORS[i] : -1)
                            .append(".")
                            .formatted(Formatting.RESET)
                            .append(Text.literal(" ")
                                    .styled(style -> style.withBold(false).withColor(-1))
                                    .append(team.map(TeamConfig::name).orElse(GuiUtils.UNKNOWN_TEXT)))
                            .append(Text.literal("  " + GuiUtils.SMALL_FLOAT_FORMATTER.format(entry.getFloatValue())).withColor(-1));
                    space.getPlayers().sendMessage(text);
                }
            }
            case 60 * 20 -> space.close(GameCloseReason.FINISHED);
        }
    }
}
