package io.github.viciscat.mineralcontest.generation;

import io.github.viciscat.mineralcontest.mixin.StructureAccessorAccessor;
import lombok.experimental.Delegate;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.StructureHolder;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import java.util.function.Predicate;

public class MCStructureAccessor extends StructureAccessor {

    @Delegate
    private final StructureAccessor delegate;
    private final Predicate<Box> intersectsSpawn;

    public MCStructureAccessor(StructureAccessor delegate, Predicate<Box> intersectsSpawn) {
        super(((StructureAccessorAccessor) delegate).getWorld(), ((StructureAccessorAccessor) delegate).getOptions(), ((StructureAccessorAccessor) delegate).getLocator());
        this.delegate = delegate;
        this.intersectsSpawn = intersectsSpawn;
    }

    @Override
    public void setStructureStart(ChunkSectionPos pos, Structure structure, StructureStart structureStart, StructureHolder holder) {
        if (intersectsSpawn.test(Box.from(structureStart.getBoundingBox()))) {
            for (StructurePiece child : structureStart.getChildren()) {
                if (intersectsSpawn.test(Box.from(child.getBoundingBox()))) return;
            }
        }
        super.setStructureStart(pos, structure, structureStart, holder);
    }
}
