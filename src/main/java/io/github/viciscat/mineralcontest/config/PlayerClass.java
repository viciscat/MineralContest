package io.github.viciscat.mineralcontest.config;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.config.classbehavior.AttributesBehavior;
import io.github.viciscat.mineralcontest.mixin.PlayerInventoryAccessor;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SlotRange;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;

import java.util.List;

public record PlayerClass(
        Identifier id,
        Text name,
        RegistryEntry<Item> icon,
        List<Text> description,
        List<AttributesBehavior.Modifier> attributeModifiers,
        RegistryEntry<Kit> kit
) {
    public static final RegistryKey<Registry<PlayerClass>> REGISTRY_KEY = RegistryKey.ofRegistry(MineralContest.id("player_class"));

    public static final Codec<PlayerClass> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(PlayerClass::id),
            TextCodecs.CODEC.fieldOf("name").forGetter(PlayerClass::name),
            Item.ENTRY_CODEC.fieldOf("icon").forGetter(PlayerClass::icon),
            TextCodecs.CODEC.listOf().optionalFieldOf("description", List.of()).forGetter(PlayerClass::description),
            AttributesBehavior.Modifier.CODEC.listOf().optionalFieldOf("attribute_modifiers", List.of()).forGetter(PlayerClass::attributeModifiers),
            Kit.REGISTRY_CODEC.optionalFieldOf("kit", RegistryEntry.of(Kit.EMPTY)).forGetter(PlayerClass::kit)
    ).apply(instance, PlayerClass::new));

    public static final Codec<RegistryEntry<PlayerClass>> REGISTRY_CODEC = RegistryElementCodec.of(REGISTRY_KEY, CODEC);

    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiersMap() {
        return attributeModifiers.stream().collect(
                ImmutableSetMultimap.toImmutableSetMultimap(AttributesBehavior.Modifier::attribute, AttributesBehavior.Modifier::modifier));
    }

    public void giveKitTo(ServerPlayerEntity player) {
        giveKitTo(player, kit());
    }

    private void giveKitTo(ServerPlayerEntity player, RegistryEntry<Kit> kitEntry) {
        Kit kit = kitEntry.value();
        kit.parents().forEach(k -> giveKitTo(player, k));
        for (Kit.KitItem item : kit.items()) {
            for (int i = 0; i < item.repeat(); i++) {
                ItemStack stack = item.itemStack().copy();
                if (item.range().isEmpty() || item.range().get().getSlotCount() == 0) {
                    player.giveOrDropStack(stack);
                    continue;
                }
                SlotRange range = item.range().get();

                for (int slotId : range.getSlotIds()) {
                    if (range.toString().startsWith("armor")) slotId -= (100 - 36); // I hate it here.
                    PlayerInventory inventory = player.getInventory();
                    ItemStack existingStack = inventory.getStack(slotId);
                    if ((existingStack.isEmpty() || ((PlayerInventoryAccessor) inventory).invokeCanStackAddMore(existingStack, stack)) && inventory.insertStack(slotId, stack)) break;
                }
                if (!stack.isEmpty()) {
                    MineralContest.LOGGER.warn("Couldn't give all items in kit {}: {}", kitEntry, stack.getItem());
                }
            }
        }
    }


}
