package io.github.viciscat.mineralcontest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.viciscat.mineralcontest.injected.PersonalWorldBorderHolder;
import net.minecraft.entity.Entity;
import net.minecraft.world.CollisionView;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public class EntityMixin {
    @WrapOperation(method = "findCollisionsForMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getWorldBorder()Lnet/minecraft/world/border/WorldBorder;"))
    private static WorldBorder getPersonalWorldBorder(World instance, Operation<WorldBorder> original, @Local(argsOnly = true) Entity self) {
        if (self instanceof PersonalWorldBorderHolder worldBorderHolder && worldBorderHolder.hasWorldBorder()) {
            return worldBorderHolder.getWorldBorder();
        }
        return original.call(instance);
    }

    @Mixin(CollisionView.class)
    interface CollisionViewMixin {
        @WrapOperation(method = "getWorldBorderCollisions", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/CollisionView;getWorldBorder()Lnet/minecraft/world/border/WorldBorder;"))
        private WorldBorder getPersonalWorldBorder(CollisionView instance, Operation<WorldBorder> original, @Local(argsOnly = true) Entity self) {
            if (self instanceof PersonalWorldBorderHolder worldBorderHolder && worldBorderHolder.hasWorldBorder()) {
                return worldBorderHolder.getWorldBorder();
            }
            return original.call(instance);
        }
    }
}
