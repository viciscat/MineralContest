package io.github.viciscat.mineralcontest.datagen;

import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.config.Kit;
import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.registry.PlasmidRegistryKeys;

public class MineralContestDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(MineralContestBlockTagProvider::new);
        pack.addProvider(MineralContestGameProvider::new);
        pack.addProvider(MineralContestBlockLootTableProvider::new);
    }

    @Override
    public @Nullable String getEffectiveModId() {
        return MineralContest.NAMESPACE;
    }

    @Override
    public void buildRegistry(RegistryBuilder registryBuilder) {
        DataGeneratorEntrypoint.super.buildRegistry(registryBuilder);
        registryBuilder.addRegistry(PlasmidRegistryKeys.GAME_CONFIG, MineralContestGameProvider::registerGameConfigs);
        registryBuilder.addRegistry(PlayerClass.REGISTRY_KEY, MineralContestGameProvider::registerPlayerClasses);
        registryBuilder.addRegistry(MapConfig.REGISTRY_KEY, MineralContestGameProvider::registerMaps);
        registryBuilder.addRegistry(Kit.REGISTRY_KEY, MineralContestGameProvider::registerKits);
    }
}
