package io.github.viciscat.mineralcontest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mojang.authlib.GameProfile;
import io.github.viciscat.mineralcontest.event.EditPlayerDamageEvent;
import io.github.viciscat.mineralcontest.injected.PersonalWorldBorderHolder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements PersonalWorldBorderHolder {
    @Shadow public ServerPlayNetworkHandler networkHandler;

    public ServerPlayerEntityMixin(World world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Unique
    private @Nullable WorldBorder worldBorder;

    @Override
    public final void mineral_contest$setWorldBorder(@NotNull WorldBorder worldBorder) {
        this.worldBorder = worldBorder;
        networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(worldBorder));
    }

    @Override
    public final WorldBorder mineral_contest$getWorldBorder() {
        return worldBorder;
    }

    @Override
    public final void mineral_contest$removeWorldBorder() {
        worldBorder = null;
        networkHandler.sendPacket(new WorldBorderInitializeS2CPacket(getWorld().getWorldBorder()));
    }

    @Override
    public final boolean mineral_contest$hasWorldBorder() {
        return worldBorder != null;
    }

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(CallbackInfoReturnable<Boolean> cir, @Local(argsOnly = true) LocalFloatRef damage, @Local(argsOnly = true) DamageSource source) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        try (EventInvokers invokers = Stimuli.select().forEntity(player)) {
            float result = invokers.get(EditPlayerDamageEvent.EVENT).onDamage(player, source, damage.get());
            damage.set(result);
        }
    }
}
