package io.github.viciscat.mineralcontest;

import com.mojang.logging.LogUtils;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.viciscat.mineralcontest.config.Kit;
import io.github.viciscat.mineralcontest.config.MainConfig;
import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import io.github.viciscat.mineralcontest.generation.MCChunkGenerator;
import io.github.viciscat.mineralcontest.hacks.OceanRemoving;
import io.github.viciscat.mineralcontest.phases.MineralContestWaiting;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestBlock;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestBlockEntity;
import io.github.viciscat.mineralcontest.phases.play.chest.ArenaChestCommand;
import io.github.viciscat.mineralcontest.phases.play.gate.GateEntity;
import io.github.viciscat.mineralcontest.util.Attachments;
import io.github.viciscat.mineralcontest.util.Commands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.block.Block;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;
import org.slf4j.Logger;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntitySpawnEvent;

import java.util.Random;

public class MineralContest implements ModInitializer {

    public static final String NAMESPACE = "mineral_contest";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.of(NAMESPACE, path);
    }

    public static final Identifier WORKER_CLASS = id("worker");
    public static final Identifier WARRIOR_CLASS = id("warrior");
    public static final Identifier AGILE_CLASS = id("agile");
    public static final Identifier ROBUST_CLASS = id("robust");
    public static final Identifier MINER_CLASS = id("miner");

    public static GameType<MainConfig> GAME_TYPE;

    public static final TagKey<Block> SPAWN_BREAKABLE = TagKey.of(RegistryKeys.BLOCK, id("spawn_breakable"));

    @Override
    public void onInitialize() {
        DynamicRegistries.register(PlayerClass.REGISTRY_KEY, PlayerClass.CODEC);
        DynamicRegistries.register(Kit.REGISTRY_KEY, Kit.CODEC);
        DynamicRegistries.register(MapConfig.REGISTRY_KEY, MapConfig.CODEC);

        // blocks
        Registry.register(Registries.BLOCK, ArenaChestBlock.REGISTRY_KEY, ArenaChestBlock.INSTANCE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, ArenaChestBlockEntity.REGISTRY_KEY, ArenaChestBlockEntity.TYPE);
        PolymerBlockUtils.registerBlockEntity(ArenaChestBlockEntity.TYPE);

        // entities
        Registry.register(Registries.ENTITY_TYPE, GateEntity.REGISTRY_KEY, GateEntity.TYPE);
        PolymerEntityUtils.registerType(GateEntity.TYPE);

        GAME_TYPE = GameTypes.register(
                id("mineral_contest"),
                MainConfig.CODEC,
                MineralContest::open);

        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> {
            ArenaChestCommand.register(commandDispatcher);
            Commands.register(commandDispatcher);
            commandDispatcher.register(CommandManager.literal("stupid").executes(context -> {
                        ServerWorld world = context.getSource().getWorld();
                        GateEntity entity = GateEntity.TYPE.create(world, SpawnReason.COMMAND);
                        entity.setPosition(context.getSource().getPosition());
                        entity.width = 3;
                        entity.height = 5;
                        entity.axis = Direction.Axis.X;
                        world.spawnEntity(entity);
                        return 1;
                    }
            ));
        });
    }

    private static GameOpenProcedure open(GameOpenContext<MainConfig> context) {
        MapConfig mapConfig = context.config().mapConfig().value();
        WorldPreset preset = context.server().getRegistryManager().getOrThrow(RegistryKeys.WORLD_PRESET).get(mapConfig.preset());
        assert preset != null;
        DimensionOptions options = preset.createDimensionsRegistryHolder().getOrEmpty(mapConfig.dimension()).orElseThrow();

        if (mapConfig.removeOceans()) {
            options = OceanRemoving.removeOceans(options, context.server());
        }
        MCChunkGenerator generator = new MCChunkGenerator(mapConfig, options.chunkGenerator().getBiomeSource(), options.chunkGenerator(), context.server());
        RuntimeWorldConfig config = new RuntimeWorldConfig()
                .setGenerator(generator)
                .setTimeOfDay(6000)
                .setShouldTickTime(true)
                .setSeed(mapConfig.seed().orElse(new Random().nextLong()))
                .setDimensionType(options.dimensionTypeEntry());

        return context.openWithWorld(config, (gameActivity, serverWorld) -> {
            context.server().getPlayerManager().setMainWorld(serverWorld);
            gameActivity.getGameSpace().setAttachment(Attachments.CONFIG, context.config());
            gameActivity.getGameSpace().setAttachment(Attachments.MAIN_WORLD, serverWorld);
            gameActivity.listen(EntitySpawnEvent.EVENT, entity -> {
                if (entity.getType().getSpawnGroup() == SpawnGroup.MISC) return EventResult.PASS;
                Vec3d pos = entity.getEntityPos();
                int radius = context.config().mapConfig().value().spawnRadius() + 2;
                return pos.x * pos.x + pos.z * pos.z < radius * radius ? EventResult.DENY : EventResult.PASS;
            });
            int floorY = generator.prepareWorld(serverWorld);
            gameActivity.getGameSpace().setAttachment(Attachments.FLOOR_Y, floorY);
            MineralContestWaiting.open(gameActivity, serverWorld, context.config(), floorY);
        });
    }
}
