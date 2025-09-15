package io.github.viciscat.mineralcontest.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.StructureLocator;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.StructureAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureAccessor.class)
public interface StructureAccessorAccessor {

    @Accessor("world")
    WorldAccess getWorld();

    @Accessor("options")
    GeneratorOptions getOptions();

    @Accessor("locator")
    StructureLocator getLocator();
}
