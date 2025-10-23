package io.github.viciscat.mineralcontest.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.viciscat.mineralcontest.MineralContest;
import net.minecraft.block.Block;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

import java.util.List;
import java.util.Optional;

public record MapConfig(
        Optional<Long> seed,
        RegistryKey<WorldPreset> preset,
        RegistryKey<DimensionOptions> dimension,
        boolean removeOceans,
        List<TeamConfig> teamConfigs,
        List<StructureConfig> structureConfigs,
        int spawnRadius,
        BlockPos arenaChestPosition,
        TagKey<Block> breakableSpawnBlocks
) {
    public static final RegistryKey<Registry<MapConfig>> REGISTRY_KEY = RegistryKey.ofRegistry(MineralContest.id("map"));

    public static final Codec<MapConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("seed").forGetter(MapConfig::seed),
            RegistryKey.createCodec(RegistryKeys.WORLD_PRESET).optionalFieldOf("preset", WorldPresets.DEFAULT).forGetter(MapConfig::preset),
            RegistryKey.createCodec(RegistryKeys.DIMENSION).fieldOf("dimension").forGetter(MapConfig::dimension),
            Codec.BOOL.fieldOf("remove_oceans").forGetter(MapConfig::removeOceans),
            TeamConfig.CODEC.listOf(2, Integer.MAX_VALUE).fieldOf("teams").forGetter(MapConfig::teamConfigs),
            StructureConfig.CODEC.listOf().fieldOf("structures").forGetter(MapConfig::structureConfigs),
            Codec.INT.fieldOf("spawn_radius").forGetter(MapConfig::spawnRadius),
            BlockPos.CODEC.fieldOf("arena_chest_position").forGetter(MapConfig::arenaChestPosition),
            TagKey.codec(RegistryKeys.BLOCK).optionalFieldOf("breakable_spawn_blocks", MineralContest.SPAWN_BREAKABLE).forGetter(MapConfig::breakableSpawnBlocks)
    ).apply(instance, MapConfig::new));

    public static final Codec<RegistryEntry<MapConfig>> REGISTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC);

    public Optional<TeamConfig> teamConfig(GameTeamKey team) {
        return teamConfigs.stream().filter(c -> c.id().equals(team)).findFirst();
    }
}
