package io.github.viciscat.mineralcontest.util;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sidebars.api.lines.SidebarLine;
import eu.pb4.sidebars.api.lines.SuppliedSidebarLine;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.text.NumberFormat;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class GuiUtils {

    public static final GuiElementInterface EMPTY_SLOT = new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE).hideTooltip().build();
    public static final NumberFormat SMALL_FLOAT_FORMATTER = Util.make(NumberFormat.getInstance(), format -> {
        format.setMaximumFractionDigits(1);
        format.setGroupingUsed(false);
    });

    private static final NumberFormat TIME_FORMATTER = Util.make(NumberFormat.getInstance(), format -> format.setMinimumIntegerDigits(2));

    private static final Text BAR = Text.literal("| ").formatted(Formatting.GRAY);
    public static final Text GAME_TEXT = BAR.copy().append(Text.translatable("gameType.mineral_contest.sidebar.game").styled(style -> style.withColor(Formatting.WHITE)));
    private static final UnaryOperator<Text> TEAM_TEXT = t -> BAR.copy().append(Text.translatable("gameType.mineral_contest.sidebar.team", t).styled(style -> style.withColor(Formatting.WHITE)));
    private static final UnaryOperator<Text> CLASS_TEXT = t -> Text.translatable("gameType.mineral_contest.sidebar.class", t).formatted(Formatting.GRAY);
    public static final Text MINERAL_CONTEST_TEXT = Text.translatable("gameType.mineral_contest.mineral_contest").styled(style -> style.withColor(Formatting.AQUA).withBold(true));
    public static final Text AQUA = Text.empty().styled(style -> style.withColor(Formatting.AQUA));
    public static final Text UNKNOWN_TEXT = Text.literal("???");

    private GuiUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static SidebarLine createNameLine(int value) {
        return new SuppliedSidebarLine(value,
                player -> BAR.copy().append(player.getName().copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE))),
                player -> BlankNumberFormat.INSTANCE
        );
    }

    public static SidebarLine createTeamLine(int value, Function<PlayerRef, Text> teamName) {
        return new SuppliedSidebarLine(
                value,
                player -> TEAM_TEXT.apply(AQUA.copy().append(teamName.apply(PlayerRef.of(player)))),
                player -> BlankNumberFormat.INSTANCE
        );
    }

    public static SidebarLine createClassLine(int value, Function<PlayerRef, Text> className) {
        return new SuppliedSidebarLine(
                value,
                player -> CLASS_TEXT.apply(AQUA.copy().append(className.apply(PlayerRef.of(player)))),
                player -> BlankNumberFormat.INSTANCE
        );
    }

    public static Text createTimer(int seconds) {
        seconds = Math.max(seconds, 0);
        int minutes = seconds / 60;
        seconds %= 60;
        return Text.literal(TIME_FORMATTER.format(minutes)).styled(style -> style.withColor(Formatting.AQUA))
                .append(":")
                .append(TIME_FORMATTER.format(seconds));
    }
}
