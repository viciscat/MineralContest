package io.github.viciscat.mineralcontest.phases.play;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;

public class OreChestInventory extends SimpleInventory {

    private final Object2FloatMap<Item> itemWorth;
    private final FloatConsumer scoreAdder;

    public static NamedScreenHandlerFactory createScreenHandler(FloatConsumer scoreAdder, Object2FloatMap<RegistryEntry<Item>> itemWorth) {
        OreChestInventory inventory = new OreChestInventory(scoreAdder, itemWorth);
        return new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3),
                Text.literal("Team Chest")
        );
    }

    public OreChestInventory(FloatConsumer scoreAdder, Object2FloatMap<RegistryEntry<Item>> itemWorth) {
        super(27);

        this.itemWorth = new Object2FloatOpenHashMap<>(itemWorth.size());
        this.scoreAdder = scoreAdder;
        itemWorth.object2FloatEntrySet().forEach(entry -> this.itemWorth.put(entry.getKey().value(), entry.getFloatValue()));
    }

    @Override
    public void onClose(PlayerEntity player) {
        super.onClose(player);
        float score = 0;
        for (ItemStack stack : this) {
            if (stack.isEmpty()) continue;

            if (itemWorth.containsKey(stack.getItem())) {
                score += itemWorth.getFloat(stack.getItem()) * stack.getCount();
            } else {
                player.giveOrDropStack(stack);
            }
        }
        scoreAdder.accept(score);

    }
}
