package io.github.viciscat.mineralcontest.generation;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.experimental.Delegate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MineralContestAccess implements StructureWorldAccess {

    @Delegate
    private final StructureWorldAccess delegate;
    private final Predicate<Box> intersectsSpawn;

    private boolean allowed = true;
    private final Map<BlockPos, BlockPlacementInfo> placements = new Object2ObjectOpenHashMap<>();

    public MineralContestAccess(StructureWorldAccess delegate, Predicate<Box> intersectsSpawn) {
        this.delegate = delegate;
        this.intersectsSpawn = intersectsSpawn;
    }

    private static boolean isValidSnowBlock(BlockState state) {
        return state.getBlock() == Blocks.SNOW && state.get(Properties.LAYERS) == 1;
    }

    @Override
    public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        if (!allowed) return false;
        if (intersectsSpawn.test(Box.enclosing(pos, pos))) {
            if ((!state.getCollisionShape(EmptyBlockView.INSTANCE, pos).isEmpty() && !isValidSnowBlock(state)) || !state.getFluidState().isEmpty()) {
                allowed = false;
                return false;
            }
        }
        placements.put(pos.toImmutable(), new BlockPlacementInfo(state, flags));
        return true;
    }

    public boolean setBlockState(BlockPos pos, BlockState state, int flags) {
        return this.setBlockState(pos, state, flags, 512);
    }

    public void setCurrentlyGeneratingStructureName(@Nullable Supplier<String> structureName) {
        if (allowed) {
            for (Map.Entry<BlockPos, BlockPlacementInfo> placement : placements.entrySet()) {
                delegate.setBlockState(placement.getKey(), placement.getValue().state(), placement.getValue().flags());
            }
        }
        reset();
        delegate.setCurrentlyGeneratingStructureName(structureName);
    }

    @Override
    public boolean testBlockState(BlockPos pos, Predicate<BlockState> state) {
        if (placements.containsKey(pos)) return state.test(placements.get(pos).state());
        return delegate.testBlockState(pos, state);
    }

    public BlockState getBlockState(BlockPos pos) {
        if (placements.containsKey(pos)) {
            return placements.get(pos).state;
        }
        return delegate.getBlockState(pos);
    }

    private void reset() {
        allowed = true;
        placements.clear();
    }

    private record BlockPlacementInfo(BlockState state, int flags) {}
}
