package io.github.viciscat.mineralcontest.event;

import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.StimulusEvent;

public interface PlayerInventoryActionEvent {
    StimulusEvent<PlayerInventoryActionEvent> EVENT = StimulusEvent.create(PlayerInventoryActionEvent.class, ctx -> (player, slot, actionType, button) -> {
        try {
            for (var listener : ctx.getListeners()) {
                var result = listener.onInventoryAction(player, slot, actionType, button);
                if (result != EventResult.PASS) {
                    return result;
                }
            }
        } catch (Throwable t) {
            ctx.handleException(t);
        }
        return EventResult.PASS;
    });

    EventResult onInventoryAction(ServerPlayerEntity player, int slot, SlotActionType actionType, int button);
}
