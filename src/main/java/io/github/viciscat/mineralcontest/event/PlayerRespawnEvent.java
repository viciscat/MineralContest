package io.github.viciscat.mineralcontest.event;

import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.StimulusEvent;

@FunctionalInterface
public interface PlayerRespawnEvent {
    StimulusEvent<PlayerRespawnEvent> EVENT = StimulusEvent.create(PlayerRespawnEvent.class, ctx -> player -> {
        try {
            for (var listener : ctx.getListeners()) {
                listener.onRespawn(player);
            }
        } catch (Throwable throwable) {
            ctx.handleException(throwable);
        }
    });

    void onRespawn(ServerPlayerEntity player);
}
