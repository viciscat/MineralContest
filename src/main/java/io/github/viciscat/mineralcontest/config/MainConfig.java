package io.github.viciscat.mineralcontest.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record MainConfig(
        int duration,
        List<RegistryEntry<PlayerClass>> classes,
        RegistryEntry<MapConfig> mapConfig,
        Object2FloatMap<RegistryEntry<Item>> itemWorth
) {
    public static final MapCodec<MainConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("duration").forGetter(MainConfig::duration),
            PlayerClass.REGISTRY_CODEC.listOf().fieldOf("classes").forGetter(MainConfig::classes),
            MapConfig.REGISTRY_CODEC.fieldOf("map").forGetter(MainConfig::mapConfig),
            map(Codec.unboundedMap(Item.ENTRY_CODEC, Codec.FLOAT)).fieldOf("item_scores").forGetter(MainConfig::itemWorth)
    ).apply(instance, MainConfig::new));

    private static Codec<Object2FloatMap<RegistryEntry<Item>>> map(Codec<Map<RegistryEntry<Item>, Float>> codec) {
        return codec.xmap(
                Object2FloatOpenHashMap::new,
                Function.identity()
        );
    }
}
