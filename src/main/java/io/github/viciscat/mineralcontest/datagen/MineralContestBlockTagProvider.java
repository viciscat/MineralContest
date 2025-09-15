package io.github.viciscat.mineralcontest.datagen;

import io.github.viciscat.mineralcontest.MineralContest;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class MineralContestBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public MineralContestBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        valueLookupBuilder(MineralContest.SPAWN_BREAKABLE)
                .add(Blocks.SHORT_GRASS)
                .add(Blocks.TALL_GRASS)
                .add(Blocks.FERN)
                .add(Blocks.SWEET_BERRY_BUSH)
                .forceAddTag(ConventionalBlockTags.FLOWERS);
    }
}
