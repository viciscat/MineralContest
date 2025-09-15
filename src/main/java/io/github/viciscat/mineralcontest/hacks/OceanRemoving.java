package io.github.viciscat.mineralcontest.hacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class OceanRemoving {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static boolean removing = false;
    public static MinecraftServer server;

    public static final Logger LOGGER = LoggerFactory.getLogger("MineralContestOceanRemoving");

    public static DimensionOptions removeOceans(DimensionOptions options, MinecraftServer server) {
        LOGGER.info("Removing oceans from:{}", options.dimensionTypeEntry().getKey().map(Object::toString).orElse("?"));
        if (!(options.chunkGenerator() instanceof NoiseChunkGenerator noiseChunkGenerator)) {
            LOGGER.warn("Chunk generator does not implement NoiseChunkGenerator");
            return options;
        }
        Optional<RegistryKey<ChunkGeneratorSettings>> key = noiseChunkGenerator.getSettings().getKey();
        if (key.isEmpty()) {
            LOGGER.warn("Chunk generator settings key not found");
            return options;
        }
        Identifier identifier = key.get().getValue();
        Optional<Resource> resource = server.getResourceManager().getResource(ResourceFinder.json(RegistryKeys.CHUNK_GENERATOR_SETTINGS).toResourcePath(identifier));
        if (resource.isEmpty()) {
            LOGGER.warn("Could not find chunk generator settings");
            return options;
        }
        try {
            JsonObject jsonObject = GSON.fromJson(resource.get().getReader(), JsonObject.class);
            OceanRemoving.server = server;
            OceanRemoving.removing = true;
            ChunkGeneratorSettings settings = ChunkGeneratorSettings.REGISTRY_CODEC.decode(RegistryOps.of(JsonOps.INSTANCE, server.getRegistryManager()), jsonObject).getOrThrow().getFirst().value();
            OceanRemoving.removing = false;
            LOGGER.info("Oceans removed successfully from: {}", options.dimensionTypeEntry().getKey().map(Object::toString).orElse("?"));
            return new DimensionOptions(options.dimensionTypeEntry(), new NoiseChunkGenerator(noiseChunkGenerator.getBiomeSource(), RegistryEntry.of(settings)));
        } catch (IOException e) {
            LOGGER.warn("Could not read or decode chunk generator settings", e);
        }
        return options;
    }
}
