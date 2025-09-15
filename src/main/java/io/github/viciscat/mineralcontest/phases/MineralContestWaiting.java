package io.github.viciscat.mineralcontest.phases;

import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sidebars.api.lines.SidebarLine;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.MineralContestRules;
import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.util.GuiUtils;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;
import xyz.nucleoid.plasmid.api.util.Guis;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.item.ItemThrowEvent;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MineralContestWaiting {

    private final GameSpace space;
    private final ServerWorld world;
    private final int floorY;
    private final TeamManager teamManager;
    private final ItemStack teamItem = Util.make(new ItemStack(Items.COMPASS), stack -> stack.applyChanges(ComponentChanges.builder()
                    .add(DataComponentTypes.ITEM_NAME, Text.translatable("game.mineral_contest.teamItem"))
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(Util.make(new NbtCompound(), nbtCompound -> nbtCompound.putBoolean("mineral_contest:team_item", true))))
            .build()));


    private MineralContestWaiting(GameSpace space, TeamManager teamManager, ServerWorld world,
                                  MainConfig config, int floorY) {
        this.space = space;
        this.world = world;
        this.floorY = floorY;
        this.teamManager = teamManager;
        List<TeamConfig> teamConfigs = config.mapConfig().value().teamConfigs();
        for (TeamConfig teamConfig : teamConfigs) teamManager.addTeam(teamConfig.toNewGameTeam());



    }

    public static void open(GameActivity activity, ServerWorld world, MainConfig config, int floorY) {
        TeamManager teamManager = TeamManager.addTo(activity);
        GameSpace space = activity.getGameSpace();
        space.setAttachment(Attachments.TEAM_MANAGER, teamManager);
        //noinspection resource
        GlobalWidgets globalWidgets = GlobalWidgets.addTo(activity);
        MineralContestWaiting waiting = new MineralContestWaiting(space, teamManager, world, config, floorY);
        activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
        activity.listen(GamePlayerEvents.JOIN, waiting::onPlayerJoin);
        activity.listen(GamePlayerEvents.ACCEPT, waiting::onPlayerAccept);
        activity.listen(ItemThrowEvent.EVENT, (player, slot, stack) -> EventResult.DENY);
        activity.listen(ItemUseEvent.EVENT, waiting::onItemUse);
        activity.listen(GameActivityEvents.REQUEST_START, waiting::commenceTheGaming);


        activity.deny(MineralContestRules.ALL_DAMAGE);
        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.INTERACTION);

        SidebarWidget sidebar = globalWidgets.addSidebar();
        sidebar.addLines(
                SidebarLine.createEmpty(9),
                GuiUtils.createNameLine(8),
                SidebarLine.createEmpty(7),
                SidebarLine.createEmpty(6),
                SidebarLine.create(5, GuiUtils.GAME_TEXT, BlankNumberFormat.INSTANCE),
                SidebarLine.create(4, Text.literal(" /game start"), BlankNumberFormat.INSTANCE),
                SidebarLine.createEmpty(3),
                GuiUtils.createTeamLine(2, ref -> {
                    GameTeamKey key = teamManager.teamFor(ref);
                    if (key == null) return GuiUtils.UNKNOWN_TEXT;
                    return teamManager.getTeamConfig(key).name();
                }),
                SidebarLine.createEmpty(1),
                SidebarLine.createEmpty(0)
        );
        sidebar.setTitle(GuiUtils.MINERAL_CONTEST_TEXT);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(80);
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, space.getServer());
    }

    private void onPlayerJoin(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.ADVENTURE);
        player.giveOrDropStack(teamItem.copy());
    }

    private JoinAcceptorResult onPlayerAccept(JoinAcceptor acceptor) {
        return acceptor.teleport(world, new Vec3d(20, floorY + 1, 0));
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component != null && component.copyNbt().getBoolean("mineral_contest:team_item", false)) {
            List<GuiElementInterface> list = new ArrayList<>();
            PlayerRef ref = PlayerRef.of(player);
            for (GameTeam team : teamManager) {
                list.add(new TeamGuiElement(team, ref));
            }
            SimpleGui gui = Guis.createSelectorGui(player, Text.translatable("game.mineral_contest.selectTeam"), false, list);
            gui.open();
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private GameResult commenceTheGaming() {
        space.setActivity(MineralContestClasses::new);
        return GameResult.ok();
    }

    private class TeamGuiElement implements GuiElementInterface {

        private final ItemStack itemStack;
        private final PlayerRef player;
        private final GameTeam team;

        public TeamGuiElement(GameTeam team, PlayerRef player) {
            itemStack = new ItemStack(ColoredBlocks.wool(team.config().blockDyeColor()));
            itemStack.set(DataComponentTypes.ITEM_NAME, team.config().name());
            this.player = player;
            this.team = team;
        }

        @Override
        public ItemStack getItemStack() {
            return itemStack;
        }

        @Override
        public ClickCallback getGuiCallback() {
            return (index, type, action, gui) -> teamManager.addPlayerTo(player, team.key());
        }

        @Override
        public ItemStack getItemStackForDisplay(GuiInterface gui) {
            ItemStack copy = itemStack.copy();
            Set<PlayerRef>
                    playerRefs = teamManager.allPlayersIn(team.key());
            List<Text> list = new ArrayList<>();
            Style style = Style.EMPTY.withColor(Formatting.GRAY).withItalic(false);
            MutableText literal = Text.literal("- ").setStyle(style);
            for (PlayerRef ref : playerRefs) {
                ServerPlayerEntity entity = ref.getEntity(space);
                if (entity != null) list.add(literal.copy().append(entity.getName().copy().setStyle(style)));
                else list.add(literal.copy().append(GuiUtils.UNKNOWN_TEXT));
            }
            if (team.key().equals(teamManager.teamFor(player))) {
                copy.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
            copy.set(DataComponentTypes.LORE, new LoreComponent(list));
            return copy;
        }
    }
}
