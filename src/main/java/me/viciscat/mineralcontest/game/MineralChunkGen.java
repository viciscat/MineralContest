package me.viciscat.mineralcontest.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class MineralChunkGen extends ChunkGenerator {

    @Override
    public @Nullable Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        return new Location(world, 0, 70, 0);
    }

    @Override
    public boolean shouldGenerateNoise() {
        return true;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return true;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return true;
    }

    @Override
    public boolean shouldGenerateDecorations(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        boolean isInX = chunkX >= -5 && chunkX <= 4;
        boolean isInZ = chunkZ >= -5 && chunkZ <= 4;
        return !worldInfo.getEnvironment().equals(World.Environment.NORMAL) || !isInX || !isInZ;
    }

    @Override
    public boolean shouldGenerateMobs(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ) {
        boolean isInX = chunkX >= -5 && chunkX <= 4;
        boolean isInZ = chunkZ >= -5 && chunkZ <= 4;
        return !worldInfo.getEnvironment().equals(World.Environment.NORMAL) || !isInX || !isInZ;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return true;
    }
}
