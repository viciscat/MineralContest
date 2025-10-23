package io.github.viciscat.mineralcontest.phases;

import com.google.common.collect.ImmutableMap;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import eu.pb4.sidebars.api.lines.SidebarLine;
import eu.pb4.sidebars.api.lines.SuppliedSidebarLine;
import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.injected.PersonalWorldBorderHolder;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.GuiUtils;
import io.github.viciscat.mineralcontest.util.MineralContestRules;
import io.github.viciscat.mineralcontest.util.Utils;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.border.WorldBorder;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinIntent;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.Guis;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.item.ItemUseEvent;

import java.util.*;

class MineralContestClasses {

    private final GameSpace space;
    private final TeamManager teamManager;
    private final ServerWorld world;
    private final MainConfig config;

    private final Map<GameTeamKey, TeamConfig> teamKeyToConfig;
    private final Map<PlayerRef, PlayerClass> playerToClass = new HashMap<>();
    private boolean everyoneChoseClass = false;

    private final ItemStack classItem = Util.make(new ItemStack(Items.IRON_SWORD), stack -> stack.applyChanges(ComponentChanges.builder()
            .add(DataComponentTypes.ITEM_NAME, Text.translatable("game.mineral_contest.classItem"))
            .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(Util.make(new NbtCompound(), nbtCompound -> nbtCompound.putBoolean("mineral_contest:class_item", true))))
            .build()));

    private int timeLeft = 90 * 20;

    MineralContestClasses(GameActivity activity) {
        Utils.copyTeamManager(activity);
        this.space = activity.getGameSpace();
        this.teamManager = space.getAttachmentOrThrow(Attachments.TEAM_MANAGER);
        this.world = space.getAttachmentOrThrow(Attachments.MAIN_WORLD);
        this.config = space.getAttachmentOrThrow(Attachments.CONFIG);

        activity.listen(GamePlayerEvents.ADD, this::onPlayerAdded);
        activity.listen(GamePlayerEvents.REMOVE, this::onPlayerRemoved);
        activity.listen(ItemUseEvent.EVENT, this::onItemUse);
        activity.listen(GameActivityEvents.TICK, this::onTick);

        activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
        activity.listen(GamePlayerEvents.ACCEPT, acceptor -> acceptor.teleport(world, new Vec3d(0, space.getAttachmentOrThrow(Attachments.FLOOR_Y), 0)));

        activity.deny(MineralContestRules.ALL_DAMAGE);
        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.INTERACTION);

        @SuppressWarnings("resource") GlobalWidgets globalWidgets = GlobalWidgets.addTo(activity);
        SidebarWidget sidebar = globalWidgets.addSidebar(GuiUtils.MINERAL_CONTEST_TEXT);
        sidebar.addLines(
                SidebarLine.createEmpty(9),
                GuiUtils.createNameLine(8),
                GuiUtils.createClassLine(4, ref -> {
                    PlayerClass playerClass = this.playerToClass.get(ref);
                    if (playerClass == null) return GuiUtils.UNKNOWN_TEXT;
                    return playerClass.name();
                }),
                SidebarLine.createEmpty(7),
                SidebarLine.createEmpty(6),
                SidebarLine.create(5, GuiUtils.GAME_TEXT, BlankNumberFormat.INSTANCE),
                new SuppliedSidebarLine(4,
                        player -> Text.translatable("game.mineral_contest.sidebar.startingIn", GuiUtils.createTimer(this.timeLeft / 20)).styled(style -> style.withColor(Formatting.GRAY)),
                        p -> BlankNumberFormat.INSTANCE),
                SidebarLine.createEmpty(3),
                GuiUtils.createTeamLine(2, ref -> {
                    GameTeamKey key = this.teamManager.teamFor(ref);
                    if (key == null) return GuiUtils.UNKNOWN_TEXT;
                    return this.teamManager.getTeamConfig(key).name();
                }),
                SidebarLine.createEmpty(1),
                SidebarLine.createEmpty(0)
        );
        this.world.getWorldBorder().setSize(this.config.mapConfig().value().spawnRadius() * 2);

        List<TeamConfig> teamConfigs = this.config.mapConfig().value().teamConfigs();
        ImmutableMap.Builder<GameTeamKey, TeamConfig> builder = ImmutableMap.builder();
        teamConfigs.forEach(teamConfig -> builder.put(teamConfig.id(), teamConfig));
        teamKeyToConfig = builder.build();
    }

    private void onPlayerAdded(ServerPlayerEntity player) {
        GameTeamKey key = teamManager.teamFor(player);
        if (key == null) {
            player.changeGameMode(GameMode.SPECTATOR);
            if (!space.getPlayers().spectators().contains(player))
                space.getPlayers().modifyIntent(player, JoinIntent.SPECTATE);
            return;
        }
        TeamConfig teamConfig = teamKeyToConfig.get(key);
        TeamConfig.PositionOrientation spawn = teamConfig.spawn();
        spawn.teleport(player, new Vec3d(0, space.getAttachmentOrThrow(Attachments.FLOOR_Y) + 1, 0));
        Vec3d pos = spawn.pos();
        sendGuiTo(player);
        player.getInventory().clear();
        player.giveOrDropStack(classItem.copy());

        // Send fake world border
        WorldBorder fakeBorder = new WorldBorder();
        fakeBorder.setSize(9);
        fakeBorder.setCenter(pos.x, pos.z);
        fakeBorder.setWarningBlocks(-1);
        fakeBorder.setWarningTime(0);
        ((PersonalWorldBorderHolder) player).setWorldBorder(fakeBorder);
    }

    private void onPlayerRemoved(ServerPlayerEntity player) {
        ((PersonalWorldBorderHolder) player).removeWorldBorder();
    }

    private void onTick() {
        if (!everyoneChoseClass && space.getPlayers().participants().stream().map(PlayerRef::of).map(playerToClass::get).allMatch(Objects::nonNull) && timeLeft > 15 * 20) {
            everyoneChoseClass = true;
            timeLeft = 15 * 20;
            space.getPlayers().sendMessage(Text.translatable("game.mineral_contest.allChoseClass"));
        }
        timeLeft--;
        if (timeLeft <= 0) {
            space.setActivity(activity -> new MineralContestPlay(activity, playerToClass));
        }
    }

    private ActionResult onItemUse(ServerPlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component != null && component.copyNbt().getBoolean("mineral_contest:class_item", false)) {
            sendGuiTo(player);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }

    private void sendGuiTo(ServerPlayerEntity player) {
        List<GuiElementInterface> list = new ArrayList<>();
        PlayerRef ref = PlayerRef.of(player);
        for (RegistryEntry<PlayerClass> playerClass : config.classes()) {
            list.add(new ClassGuiElement(playerClass.value(), ref));
        }
        SimpleGui gui = Guis.createSelectorGui(player, Text.translatable("game.mineral_contest.selectClass"), false, list);
        gui.open();
    }

    private class ClassGuiElement implements GuiElementInterface {

        private final PlayerClass playerClass;
        private final PlayerRef player;
        private final ItemStack itemStack;


        private ClassGuiElement(PlayerClass playerClass, PlayerRef player) {
            this.playerClass = playerClass;
            this.player = player;
            itemStack = new ItemStack(playerClass.icon(), 1, ComponentChanges.builder()
                    .add(DataComponentTypes.ITEM_NAME, playerClass.name())
                    .add(DataComponentTypes.LORE, new LoreComponent(playerClass.description(), playerClass.description()))
                    .build());
        }

        @Override
        public ItemStack getItemStack() {
            return itemStack;
        }

        @Override
        public ClickCallback getGuiCallback() {
            return (index, type, action, gui) -> playerToClass.put(player, playerClass);
        }

        @Override
        public ItemStack getItemStackForDisplay(GuiInterface gui) {
            ItemStack copy = itemStack.copy();
            PlayerClass aClass = playerToClass.get(player);
            if (aClass != null && playerClass.id().equals(aClass.id()))
                copy.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
            return GuiElementInterface.super.getItemStackForDisplay(gui);
        }
    }
}
