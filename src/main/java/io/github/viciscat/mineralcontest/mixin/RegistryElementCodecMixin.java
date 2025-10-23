package io.github.viciscat.mineralcontest.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.hacks.OceanRemoving;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Optional;

@Mixin(RegistryElementCodec.class)
public abstract class RegistryElementCodecMixin<E> implements Codec<RegistryEntry<E>> {

	@Shadow @Final private Codec<E> elementCodec;

	@Shadow public abstract <T> DataResult<Pair<RegistryEntry<E>, T>> decode(DynamicOps<T> ops, T input);

	@Shadow @Final private RegistryKey<? extends Registry<E>> registryRef;

	@Inject(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/RegistryKey;of(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/util/Identifier;)Lnet/minecraft/registry/RegistryKey;"), cancellable = true)
	private <T> void thingy(DynamicOps<T> ops, T input, CallbackInfoReturnable<DataResult<Pair<RegistryEntry<E>, T>>> cir, @Local Pair<Identifier, T> pair) {
		if (OceanRemoving.removing) {
			Identifier identifier = pair.getFirst();
			boolean isContinents = identifier.getPath().contains("overworld/continents");
			if (isContinents) {
				identifier = MineralContest.id("worldgen/density_function/overworld/continents.json");
			}
			else if (registryRef.equals(RegistryKeys.DENSITY_FUNCTION)) {
				identifier = ResourceFinder.json(registryRef).toResourcePath(identifier);
			} else return;

			Optional<Resource> resource = OceanRemoving.server.getResourceManager().getResource(identifier);
			if (resource.isPresent()) {
				try {
					JsonObject jsonObject = OceanRemoving.GSON.fromJson(resource.get().getReader(), JsonObject.class);
					DataResult<Pair<E, JsonElement>> decode = elementCodec.decode(RegistryOps.of(JsonOps.INSTANCE, OceanRemoving.server.getRegistryManager()), jsonObject);
					if (decode.result().isPresent()) {
						cir.setReturnValue(DataResult.success(new Pair<>(RegistryEntry.of(decode.result().get().getFirst()), input)));
					}
					else decode.ifError(System.out::println);
				} catch (IOException e) {
					OceanRemoving.LOGGER.error("Failed to do stuff", e);
				}
			} else {
				OceanRemoving.LOGGER.error("Couldn't find resource {}", identifier);
			}
		}
	}
}
