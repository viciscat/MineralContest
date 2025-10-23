package io.github.viciscat.mineralcontest.phases;

import com.google.common.collect.ImmutableMap;
import eu.pb4.sidebars.api.lines.SidebarLine;
import eu.pb4.sidebars.api.lines.SuppliedSidebarLine;
import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.MineralContestKeys;
import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.event.PlayerInventoryActionEvent;
import io.github.viciscat.mineralcontest.event.PlayerRespawnEvent;
import io.github.viciscat.mineralcontest.phases.play.DisconnectManager;
import io.github.viciscat.mineralcontest.phases.play.OreChestInventory;
import io.github.viciscat.mineralcontest.phases.play.PlayerClassManager;
import io.github.viciscat.mineralcontest.phases.play.RespawnManager;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestManager;
import io.github.viciscat.mineralcontest.phases.play.gate.GateEntity;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.GuiUtils;
import io.github.viciscat.mineralcontest.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.stats.StatisticMap;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.DroppedItemsResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockBreakEvent;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;
import xyz.nucleoid.stimuli.event.block.BlockPlaceEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityDropItemsEvent;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;
import xyz.nucleoid.stimuli.event.player.PlayerConsumeHungerEvent;
import xyz.nucleoid.stimuli.event.world.ExplosionDetonatedEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class MineralContestPlay {

    public static boolean isMovable(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) return true;
        return component.copyNbt().getBoolean(MineralContestKeys.MOVABLE_KEY, true);
    }

    public static boolean shouldDropOnDeath(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (component == null) return true;
        return component.copyNbt().getBoolean(MineralContestKeys.DROPS_ON_DEATH_KEY, true);
    }



    private final GameSpace space;
    private final TeamManager teamManager;
    private final PlayerClassManager classManager;
    private final Map<GameTeamKey, TeamConfig> teamKeyToConfig;
    private final Object2FloatMap<GameTeamKey> teamScore;
    private final Object2FloatMap<GameTeamKey> scoreMultiplier;
    private final MainConfig config;
    private final int floorY;

    private boolean transferringPlayers = true;

    private int timeLeft;

    MineralContestPlay(GameActivity activity, Map<PlayerRef, PlayerClass> playerToClass) {
        Utils.copyTeamManager(activity);
        this.space = activity.getGameSpace();
        this.teamManager = space.getAttachmentOrThrow(Attachments.TEAM_MANAGER);
        this.config = space.getAttachmentOrThrow(Attachments.CONFIG);
        this.floorY = space.getAttachmentOrThrow(Attachments.FLOOR_Y);
        this.timeLeft = config.duration() * 20;

        classManager = PlayerClassManager.addTo(activity, playerToClass);
        RespawnManager.addTo(activity);
        ArenaChestManager.addTo(activity, () -> timeLeft);
        DisconnectManager.addTo(activity);

        ServerWorld world = space.getAttachmentOrThrow(Attachments.MAIN_WORLD);
        world.getWorldBorder().setSize(2_000);
        world.setTimeOfDay(0);
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(true, space.getServer());

        activity.listen(GamePlayerEvents.OFFER, offer -> offer.acceptSpectatorsOrElse(JoinOffer::pass));
        activity.listen(GamePlayerEvents.ACCEPT, a -> a.ifSpectator(acceptor -> acceptor.teleport(world, new Vec3d(0, space.getAttachmentOrThrow(Attachments.FLOOR_Y), 0))));

        List<TeamConfig> teamConfigs = this.config.mapConfig().value().teamConfigs();
        ImmutableMap.Builder<GameTeamKey, TeamConfig> builder = ImmutableMap.builder();
        teamConfigs.forEach(teamConfig -> builder.put(teamConfig.id(), teamConfig));
        teamKeyToConfig = builder.build();

        teamScore = new Object2FloatOpenHashMap<>(teamKeyToConfig.size());
        space.setAttachment(Attachments.SCORES, teamScore);
        scoreMultiplier = new Object2FloatOpenHashMap<>(teamKeyToConfig.size());
        scoreMultiplier.defaultReturnValue(1f);
        for (GameTeam team : teamManager) {
            float multiplier = 1f;
            for (ServerPlayerEntity entity : teamManager.playersIn(team.key())) {
                PlayerClass playerClass = classManager.getPlayerClass(entity);
                if (playerClass == null) {
                    MineralContest.LOGGER.error("Couldn't find player class for {} while calculating team multiplier", entity);
                    continue;
                }
                if (MineralContest.WORKER_CLASS.equals(playerClass.id())) multiplier += 0.25f;
            }
            scoreMultiplier.put(team.key(), multiplier);
        }

        activity.listen(PlayerInventoryActionEvent.EVENT, this::onInventoryAction);
        activity.listen(EntityDropItemsEvent.EVENT, this::onDropItems);
        activity.listen(GamePlayerEvents.ADD, this::onPlayerAdded);
        activity.listen(PlayerRespawnEvent.EVENT, this::onPlayerRespawn);
        activity.listen(GameActivityEvents.ENABLE, () -> transferringPlayers = false); // ENABLE is called right after all the ADDs from players transferring from the previous activity
        activity.listen(PlayerConsumeHungerEvent.EVENT, (player, foodLevel, saturation, exhaustion) -> foodLevel <= 10 && !classManager.hasClass(player, MineralContest.AGILE_CLASS) ? EventResult.DENY : EventResult.PASS);
        activity.listen(BlockPlaceEvent.BEFORE, (player, world1, pos, state, context) -> player.getGameMode() == GameMode.CREATIVE || isNotInSpawn(pos) ? EventResult.PASS : EventResult.DENY);
        activity.listen(BlockBreakEvent.EVENT, this::onBlockBreak);
        activity.listen(BlockDropItemsEvent.EVENT, this::onBlockDropItems);
        activity.listen(EntitySpawnEvent.EVENT, entity -> !(entity instanceof MobEntity) || isNotInSpawn(entity.getBlockPos()) ? EventResult.PASS : EventResult.DENY);
        activity.listen(GameActivityEvents.TICK, this::onTick);
        activity.listen(ExplosionDetonatedEvent.EVENT, this::onExplosion);

        activity.listen(BlockUseEvent.EVENT, this::onUse);

        for (TeamConfig teamConfig : teamConfigs) {
            if (teamConfig.gate().isEmpty()) {
                continue;
            }
            TeamConfig.Gate gate = teamConfig.gate().get();
            GateEntity entity = GateEntity.TYPE.create(world, SpawnReason.COMMAND);
            if (entity == null) {
                MineralContest.LOGGER.warn("Mineral contest gate {} could not be created", gate);
                return;
            }
            entity.width = gate.width();
            entity.height = gate.height();
            entity.axis = gate.axis();
            Vec3d gatePos = gate.pos().add(0, floorY, 0);
            entity.setPosition(gatePos);
            entity.openTrigger = gate.openTrigger();
            entity.opensGate = player -> teamConfig.id().equals(teamManager.teamFor(player));
            world.spawnEntity(entity);
        }


        @SuppressWarnings("resource") GlobalWidgets globalWidgets = GlobalWidgets.addTo(activity);
        SidebarWidget sidebar = globalWidgets.addSidebar(GuiUtils.MINERAL_CONTEST_TEXT);
        sidebar.addLines(
                SidebarLine.createEmpty(9),
                GuiUtils.createNameLine(8),
                GuiUtils.createClassLine(4, ref -> {
                    PlayerClass playerClass = this.classManager.getPlayerClass(ref);
                    if (playerClass == null) return GuiUtils.UNKNOWN_TEXT;
                    return playerClass.name();
                }),
                SidebarLine.createEmpty(7),
                SidebarLine.createEmpty(6),
                SidebarLine.create(5, GuiUtils.GAME_TEXT, BlankNumberFormat.INSTANCE),
                new SuppliedSidebarLine(4,
                        player -> Text.translatable("game.mineral_contest.sidebar.timeLeft", GuiUtils.createTimer(this.timeLeft / 20)).formatted(Formatting.GRAY),
                        p -> BlankNumberFormat.INSTANCE),
                SidebarLine.createEmpty(3),
                GuiUtils.createTeamLine(2, ref -> {
                    GameTeamKey key = this.teamManager.teamFor(ref);
                    if (key == null) return GuiUtils.UNKNOWN_TEXT;
                    return this.teamManager.getTeamConfig(key).name();
                }),
                new SuppliedSidebarLine(1,
                        player -> {
                            GameTeamKey key = teamManager.teamFor(player);
                            if (key == null) return Text.empty();
                            return Text.translatable("game.mineral_contest.sidebar.score", Text.literal(GuiUtils.SMALL_FLOAT_FORMATTER.format(teamScore.getFloat(key))).formatted(Formatting.AQUA)).formatted(Formatting.GRAY);
                        },
                        player -> BlankNumberFormat.INSTANCE
                ),
                new SuppliedSidebarLine(0,
                        player -> {
                            GameTeamKey key = teamManager.teamFor(player);
                            if (key == null) return Text.empty();
                            return Text.translatable("game.mineral_contest.sidebar.booster", Text.literal(GuiUtils.SMALL_FLOAT_FORMATTER.format((scoreMultiplier.getFloat(key) - 1) * 100)).formatted(Formatting.AQUA).append("%")).formatted(Formatting.GRAY);
                        },
                        player -> BlankNumberFormat.INSTANCE
                )
        );
    }

    private @NotNull EventResult onBlockBreak(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (player.getGameMode() == GameMode.CREATIVE) return EventResult.PASS;
        return isNotInSpawn(pos) || world.getBlockState(pos).isIn(MineralContest.SPAWN_BREAKABLE) ? EventResult.PASS : EventResult.DENY;
    }

    DroppedItemsResult onBlockDropItems(@Nullable Entity breaker, ServerWorld world, BlockPos pos, BlockState state, List<ItemStack> dropStacks) {
        if (!isNotInSpawn(pos) && state.isIn(MineralContest.SPAWN_BREAKABLE)) return DroppedItemsResult.deny();
        return DroppedItemsResult.pass(dropStacks);
    }

    private EventResult onExplosion(Explosion explosion, List<BlockPos> affectedBlocks) {
        affectedBlocks.removeIf(pos -> pos.isWithinDistance(new Vec3d(0, 0, 0), config.mapConfig().value().spawnRadius()));
        return EventResult.PASS;
    }

    private boolean isNotInSpawn(BlockPos pos) {
        int radius = config.mapConfig().value().spawnRadius() + 2;
        return (pos.getX() * pos.getX() + pos.getZ() * pos.getZ()) > radius * radius;
    }

    private void onTick() {
        timeLeft--;
        if (timeLeft <= 0) {
            StatisticMap global = space.getStatistics().bundle(MineralContest.NAMESPACE).global();
            for (Object2FloatMap.Entry<GameTeamKey> entry : teamScore.object2FloatEntrySet()) {
                global.set(MineralContestKeys.scoreStatisticKeyForTeam(entry.getKey()), entry.getFloatValue());
            }
            space.setAttachment(Attachments.SCORES, Object2FloatMaps.unmodifiable(teamScore));
            space.setActivity(MineralContestResults::new);
        }
    }

    private EventResult onInventoryAction(ServerPlayerEntity player, int slot, SlotActionType actionType, int button) {
        if (actionType == SlotActionType.SWAP) {
            ItemStack stack = player.currentScreenHandler.getSlot(button).getStack();
            if (!isMovable(stack)) return EventResult.DENY;
        }
        if (actionType == SlotActionType.THROW && slot == -999) {
            ItemStack stack = player.currentScreenHandler.getCursorStack();
            return isMovable(stack) ? EventResult.PASS : EventResult.DENY;
        }
        if (slot == -999) return EventResult.PASS;
        ItemStack stack = player.currentScreenHandler.getSlot(slot).getStack();
        return isMovable(stack) ? EventResult.PASS : EventResult.DENY;
    }

    private DroppedItemsResult onDropItems(LivingEntity dropper, List<ItemStack> items) {
        items.removeIf(stack -> !shouldDropOnDeath(stack));
        return DroppedItemsResult.pass(items);
    }

    private void onPlayerAdded(ServerPlayerEntity player) {
        space.getServer().getCommandManager().sendCommandTree(player);
        if (!transferringPlayers) return;
        player.closeHandledScreen();
        player.getInventory().clear();
        if (space.getPlayers().participants().contains(player)) player.changeGameMode(GameMode.SURVIVAL);
        space.getBehavior().propagatingInvoker(PlayerRespawnEvent.EVENT).onRespawn(player);
    }

    private void onPlayerRespawn(ServerPlayerEntity player) {
        TeamConfig teamConfig = getTeamConfig(player);
        if (teamConfig == null) return;
        if (!transferringPlayers) teamConfig.spawn().teleport(player, new Vec3d(0, floorY, 0));
        player.changeGameMode(GameMode.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(20f);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false));
    }

    private ActionResult onUse(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
        BlockEntity entity = player.getEntityWorld().getBlockEntity(hitResult.getBlockPos());
        if (entity == null) return ActionResult.PASS;
        Optional<String> result = entity.getComponents().getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getString("mineral_contest:ore_chest");
        if (result.isEmpty()) return ActionResult.PASS;
        GameTeamKey team = new GameTeamKey(result.get());
        if (teamManager.getTeamConfig(team) == null) return ActionResult.PASS;
        player.openHandledScreen(OreChestInventory.createScreenHandler(
                f -> {
                    float v = f * scoreMultiplier.getFloat(team);
                    teamScore.computeFloat(team, (key, oldScore) -> v + (oldScore == null ? 0 : oldScore));
                    space.getStatistics().bundle(MineralContest.NAMESPACE).forPlayer(player).increment(MineralContestKeys.SCORE_STATISTIC_KEY, v);
                },
                config.itemWorth()
        ));

        return ActionResult.SUCCESS_SERVER;
    }

    private @Nullable TeamConfig getTeamConfig(ServerPlayerEntity player) {
        GameTeamKey key = teamManager.teamFor(player);
        if (key == null) return null;
        return teamKeyToConfig.get(key);
    }
}
