package io.github.viciscat.mineralcontest.datagen;

import io.github.viciscat.mineralcontest.MineralContestKeys;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class MineralContestBlockLootTableProvider extends FabricBlockLootTableProvider {
    protected MineralContestBlockLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        lootTables.put(MineralContestKeys.CHEST_LOOT_TABLE, new LootTable.Builder().pool(LootPool.builder()
                        .rolls(new ConstantLootNumberProvider(27))
                        .with(ItemEntry.builder(Items.EMERALD).weight(1))
                        .with(ItemEntry.builder(Items.GOLD_INGOT).weight(8))
                        .with(ItemEntry.builder(Items.IRON_INGOT).weight(8))
                        .with(ItemEntry.builder(Items.COPPER_INGOT).weight(8))
                .build())
        );
    }
}
