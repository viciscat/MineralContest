package io.github.viciscat.mineralcontest.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;

import java.util.ArrayList;
import java.util.List;

public class InvisibleBoxManager {

    private final List<InvisibleBox> invisibleBoxes = new ArrayList<>();

    public static InvisibleBoxManager addTo(GameActivity activity) {
        return new InvisibleBoxManager(activity);
    }

    private InvisibleBoxManager(GameActivity activity) {
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GameActivityEvents.DESTROY, this::onDestroy);
    }

    private void tick() {
        invisibleBoxes.forEach(InvisibleBox::tick);
    }

    private void onDestroy(GameCloseReason reason) {
        invisibleBoxes.forEach(InvisibleBox::close);
    }

    public void addBox(ServerWorld world, Box box) {
        invisibleBoxes.add(new InvisibleBox(world, box));
    }


}
