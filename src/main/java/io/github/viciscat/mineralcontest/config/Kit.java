package io.github.viciscat.mineralcontest.config;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.viciscat.mineralcontest.MineralContest;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record Kit(List<RegistryEntry<Kit>> parents, List<KitItem> items) {
    private static final String CUSTOM_RANGE = "slot.custom";
    public static SlotRange createCustom(IntList range) {
        return SlotRange.create(CUSTOM_RANGE, range);
    }

    public static final RegistryKey<Registry<Kit>> REGISTRY_KEY = RegistryKey.ofRegistry(MineralContest.id("kit"));

    public static final Codec<Kit> CODEC = Codec.recursive("Kit", codec -> RecordCodecBuilder.create(instance -> instance.group(
            RegistryElementCodec.of(REGISTRY_KEY, codec).listOf().optionalFieldOf("parents", List.of()).forGetter(Kit::parents),
            KitItem.CODEC.listOf().fieldOf("items").forGetter(Kit::items)).apply(instance, Kit::new)
    ));
    public static final Codec<RegistryEntry<Kit>> REGISTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC);

    public static final Kit EMPTY = new Kit(List.of(), List.of());

    public record KitItem(ItemStack itemStack, Optional<SlotRange> range, int repeat) {
        public static final Codec<KitItem> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
                ItemStack.CODEC.fieldOf("item_stack").forGetter(KitItem::itemStack),
                Codec.either(
                        SlotRanges.CODEC,
                        Codec.withAlternative(
                                Codec.INT.listOf(1, Integer.MAX_VALUE),
                                Codec.INT,
                                List::of
                        )).xmap(
                                either -> either.map(Function.identity(), l -> SlotRange.create(CUSTOM_RANGE, new IntImmutableList(l))),
                        slotRange -> slotRange.toString().equals(CUSTOM_RANGE) ? Either.right(slotRange.getSlotIds()) : Either.left(slotRange)
                ).optionalFieldOf("slots").forGetter(KitItem::range),
                Codec.INT.optionalFieldOf("repeat", 1).forGetter(KitItem::repeat)
        ).apply(instance, KitItem::new));
    }
}
