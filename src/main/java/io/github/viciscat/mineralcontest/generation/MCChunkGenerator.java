package io.github.viciscat.mineralcontest.generation;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.mojang.logging.LogUtils;
import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.StructureConfig;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.util.Utils;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import lombok.experimental.Delegate;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.processor.StructureProcessor;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import xyz.nucleoid.fantasy.util.ChunkGeneratorSettingsProvider;
import xyz.nucleoid.plasmid.api.game.world.generator.GameChunkGenerator;
import xyz.nucleoid.plasmid.api.util.ColoredBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class MCChunkGenerator extends GameChunkGenerator implements ChunkGeneratorSettingsProvider {
    private static final int ORE_MULTIPLIER = 2;
    private static final Logger LOGGER = LogUtils.getLogger();

    @Delegate
    private final ChunkGenerator subGenerator;
    private final ChunkGeneratorSettings settings;
    private int targetY;
    private final List<PositionedStructure> structures = new ArrayList<>();
    private final Predicate<Box> intersectsSpawn;
    private final int spawnRadius;
    private final ImmutableSetMultimap<Biome, PlacedFeature> oreFeatures;

    public MCChunkGenerator(MapConfig mapConfig, BiomeSource biomeSource, ChunkGenerator subGenerator, MinecraftServer server, ChunkGeneratorSettings settings) {
        super(biomeSource);
        this.subGenerator = subGenerator;
        this.settings = settings;
        this.targetY = getSeaLevel() + 1;
        this.spawnRadius = mapConfig.spawnRadius();
        for (StructureConfig structureConfig : mapConfig.structureConfigs()) {
            structures.add(PositionedStructure.createStruct(server.getStructureTemplateManager(), structureConfig, null));
        }
        for (TeamConfig teamConfig : mapConfig.teamConfigs()) {
            for (StructureConfig structureConfig : teamConfig.structureConfigs()) {
                structures.add(PositionedStructure.createStruct(server.getStructureTemplateManager(), structureConfig, teamConfig));
            }
        }
        intersectsSpawn = box -> Utils.boxIntersectsCylinder(box, new Vec3d(0.5, 0.5, 0.5), mapConfig.spawnRadius() + 0.5);
        oreFeatures = biomeSource.getBiomes().stream()
                .map(RegistryEntry::value)
                .collect(ImmutableSetMultimap.flatteningToImmutableSetMultimap(Function.identity(),
                        biome -> biome.getGenerationSettings().getFeatures().stream()
                                .flatMap(RegistryEntryList::stream)
                                .map(RegistryEntry::value)
                                .filter(feature -> {
                                    ConfiguredFeature<?, ?> configuredFeature = feature.feature().value();
                                    if (configuredFeature.feature() != Feature.ORE) return false;
                                    OreFeatureConfig config = (OreFeatureConfig) configuredFeature.config();
                                    return config.targets.stream().map(target -> target.state).allMatch(state -> state.isIn(ConventionalBlockTags.ORES));
                                })
                ));
    }

    public MCChunkGenerator(MapConfig mapConfig, BiomeSource biomeSource, ChunkGenerator subGenerator, MinecraftServer server) {
        this(mapConfig, biomeSource, subGenerator, server, subGenerator instanceof NoiseChunkGenerator noiseChunkGenerator ? noiseChunkGenerator.getSettings().value() : null);
    }

    public int prepareWorld(World world) {
        IntStream.Builder builder = IntStream.builder();
        final int step = 4;
        for (int x = -spawnRadius; x <= spawnRadius; x+=step) {
            for (int z = -spawnRadius; z <= spawnRadius; z+=step) {
                Chunk chunk = world.getChunkManager().getChunk(x >> 4, z >> 4, ChunkStatus.NOISE, true);
                if (chunk == null) continue;
                int i = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, x & 15, z & 15);
                builder.accept(i);
            }
        }
        double average = builder.build().average().orElse(world.getSeaLevel());
        targetY = (int) average;
        int smoothStartSquared = (spawnRadius + 2) * (spawnRadius + 2);
        int smoothEndSquared = (spawnRadius + 16) * (spawnRadius + 16);
        for (int x = -spawnRadius - 16; x <= spawnRadius + 16; x++) {
            for (int z = -spawnRadius - 16; z <= spawnRadius + 16; z++) {
                Chunk chunk = world.getChunkManager().getChunk(x >> 4, z >> 4, ChunkStatus.NOISE, true);
                if (chunk == null) continue;
                int squaredDistance = x * x + z * z;
                float percentage; // 0 is flat and 1 is normal terrain
                if (squaredDistance < smoothStartSquared) percentage = 0.f;
                else if (squaredDistance > smoothEndSquared) percentage = 1.f;
                else {
                    percentage = (MathHelper.cos((Math.min((squaredDistance - smoothStartSquared) / (float) (smoothEndSquared - smoothStartSquared), 1.f)) * MathHelper.PI) - 1) / -2.f;
                }
                int buildY = MathHelper.lerp(percentage, targetY, chunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR_WG, x & 15, z & 15));
                int y = chunk.getTopYInclusive();
                BlockState state = Blocks.AIR.getDefaultState();
                while (y >= chunk.getBottomY() && (state.isAir() || y > buildY || !state.getFluidState().isEmpty())) {
                    if (y > buildY) {
                        BlockState newState;
                        if (state.getFluidState().isEmpty()) newState = Blocks.AIR.getDefaultState();
                        else newState = Blocks.WATER.getDefaultState();
                        chunk.setBlockState(new BlockPos(x & 15, y, z & 15), newState, 0);
                    } else
                        chunk.setBlockState(new BlockPos(x & 15, y, z & 15), Blocks.STONE.getDefaultState(), 0);
                    y--;
                    state = chunk.getBlockState(new BlockPos(x & 15, y, z & 15));
                }

            }
        }
        return targetY;

    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        Set<RegistryEntry<Biome>> set = new ObjectArraySet<>();
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk.getPos(), world.getBottomSectionCoord());
        BlockPos blockPos = chunkSectionPos.getMinPos();
        ChunkPos.stream(chunkSectionPos.toChunkPos(), 1).forEach(pos -> {
            Chunk chunkx = world.getChunk(pos.x, pos.z);

            for (ChunkSection chunkSection : chunkx.getSectionArray()) {
                chunkSection.getBiomeContainer().forEachValue(set::add);
            }
        });
        set.retainAll(this.biomeSource.getBiomes());

        ChunkRandom chunkRandom = new ChunkRandom(new Xoroshiro128PlusPlusRandom(RandomSeed.getSeed()));
        long popSeed = chunkRandom.setPopulationSeed(world.getSeed(), blockPos.getX(), blockPos.getZ());

        Set<PlacedFeature> featuresToPlace = new ObjectArraySet<>();

        for (RegistryEntry<Biome> registryEntry : set) {
            ImmutableSet<PlacedFeature> placedFeatures = oreFeatures.get(registryEntry.value());
            featuresToPlace.addAll(placedFeatures);
        }
        for (int j = 0; j < ORE_MULTIPLIER - 1; j++) {
            int i = 0;
            for (PlacedFeature placedFeature : featuresToPlace) {
                chunkRandom.setDecoratorSeed(popSeed, i++, j);
                world.setCurrentlyGeneratingStructureName(() -> placedFeature.toString() + " Extra");
                placedFeature.generate(world, this, chunkRandom, blockPos);
            }
        }
        world.setCurrentlyGeneratingStructureName(null);

        this.subGenerator.generateFeatures(intersectsSpawn.test(Box.from(getBlockBox(chunk)).expand(8)) ? new MineralContestAccess(world, intersectsSpawn) : world, chunk, structureAccessor);
        BlockPos origin = new BlockPos(0, targetY, 0);
        BlockBox chunkBox = getBlockBox(chunk);
        for (PositionedStructure structure : structures) {
            BlockPos structurePos = structure.absolutePosition ? structure.pos : structure.pos.add(origin);
            if (!chunkBox.intersects(structure.template.calculateBoundingBox(structure.placement, structurePos))) continue;
            structure.template.place(world, structurePos, structurePos, structure.placement.copy().setBoundingBox(chunkBox), world.getRandom(), Block.NO_REDRAW | Block.FORCE_STATE);
        }
    }

    private @NotNull BlockBox getBlockBox(Chunk chunk) {
        ChunkPos pos = chunk.getPos();
        return new BlockBox(pos.getStartX(), chunk.getBottomY(), pos.getStartZ(), pos.getEndX(), chunk.getTopYInclusive(), pos.getEndZ());
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess world, StructureAccessor structureAccessor, Chunk chunk) {
        if (intersectsSpawn.test(Box.from(getBlockBox(chunk)))) return;
        subGenerator.carve(chunkRegion, seed, noiseConfig, world, new MCStructureAccessor(structureAccessor, intersectsSpawn), chunk);
    }

    @Override
    public void setStructureStarts(DynamicRegistryManager registryManager, StructurePlacementCalculator placementCalculator, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager, RegistryKey<World> dimension) {
        subGenerator.setStructureStarts(registryManager, placementCalculator, new MCStructureAccessor(structureAccessor, intersectsSpawn), chunk, structureTemplateManager, dimension);
    }

    @SuppressWarnings("RedundantMethodOverride")
    @Override
    public void populateEntities(ChunkRegion region) {}

    @Override
    public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(RegistryEntry<Biome> biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
        return super.getEntitySpawnList(biome, accessor, group, pos);
    }

    @Override
    public @Nullable ChunkGeneratorSettings getSettings() {
        return settings;
    }

    private record PositionedStructure(StructureTemplate template, BlockPos pos, StructurePlacementData placement, boolean absolutePosition) {
        static PositionedStructure createStruct(StructureTemplateManager templateManager, StructureConfig structureConfig, @Nullable TeamConfig teamConfig) {
            StructureTemplate template = templateManager.getTemplate(structureConfig.structure()).orElseGet(() -> {
                LOGGER.warn("Couldn't find structure {}", structureConfig.structure());
                return new StructureTemplate();
            });
            StructurePlacementData placementData = new StructurePlacementData();
            placementData.setMirror(structureConfig.mirror());
            placementData.setRotation(structureConfig.rotation());
            if (teamConfig != null) placementData.addProcessor(new TeamColorStructureProcessor(teamConfig));
            return new PositionedStructure(template, structureConfig.position(), placementData, structureConfig.positionAbsolute());
        }
    }

    private static class TeamColorStructureProcessor extends StructureProcessor {

        private final TeamConfig teamConfig;

        private TeamColorStructureProcessor(@NotNull TeamConfig teamConfig) {
            this.teamConfig = teamConfig;
        }

        @Override
        public @Nullable StructureTemplate.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot, StructureTemplate.StructureBlockInfo originalBlockInfo, StructureTemplate.StructureBlockInfo currentBlockInfo, StructurePlacementData data) {
            if (currentBlockInfo.state().getBlock() == Blocks.WHITE_WOOL) {
                return new StructureTemplate.StructureBlockInfo(currentBlockInfo.pos(), ColoredBlocks.wool(teamConfig.color()).getDefaultState(), currentBlockInfo.nbt());
            }
            return currentBlockInfo;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return null;
        }
    }
}
