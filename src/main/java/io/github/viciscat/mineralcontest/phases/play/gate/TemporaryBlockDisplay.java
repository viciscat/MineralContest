package io.github.viciscat.mineralcontest.phases.play.gate;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.world.World;

public class TemporaryBlockDisplay extends DisplayEntity.BlockDisplayEntity {
    public TemporaryBlockDisplay(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
