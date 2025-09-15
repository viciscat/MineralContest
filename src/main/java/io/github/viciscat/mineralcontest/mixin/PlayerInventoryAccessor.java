package io.github.viciscat.mineralcontest.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryAccessor {

    @Invoker("canStackAddMore")
    boolean invokeCanStackAddMore(ItemStack oldStack, ItemStack newStack);
}
