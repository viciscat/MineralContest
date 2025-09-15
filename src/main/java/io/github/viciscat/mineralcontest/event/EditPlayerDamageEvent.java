package io.github.viciscat.mineralcontest.event;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface EditPlayerDamageEvent {
    StimulusEvent<EditPlayerDamageEvent> EVENT = StimulusEvent.create(EditPlayerDamageEvent.class, ctx -> (player, source, amount) -> {
        try {
            for (EditPlayerDamageEvent listener : ctx.getListeners()) {
                amount = listener.onDamage(player, source, amount);
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return amount;
    });

    float onDamage(ServerPlayerEntity player, DamageSource source, float amount);
}
