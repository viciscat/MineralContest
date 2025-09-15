package io.github.viciscat.mineralcontest.phases.play.chest;

import com.mojang.serialization.MapCodec;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import io.github.viciscat.mineralcontest.MineralContest;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

public class ArenaChestBlock extends BlockWithEntity implements PolymerBlock {
    private static final MapCodec<ArenaChestBlock> CODEC = createCodec(ArenaChestBlock::new);

    public static final RegistryKey<Block> REGISTRY_KEY = RegistryKey.of(RegistryKeys.BLOCK, MineralContest.id("arena_chest"));
    public static final ArenaChestBlock INSTANCE = new ArenaChestBlock(AbstractBlock.Settings.create().registryKey(REGISTRY_KEY));

    protected ArenaChestBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.CHEST.getDefaultState();
    }

    @Override
    public void onPolymerBlockSend(BlockState blockState, BlockPos.Mutable pos, PacketContext.NotNullWithPlayer contexts) {
        NbtCompound compound = new NbtCompound();
        contexts.getPlayer().networkHandler.sendPacket(PolymerBlockUtils.createBlockEntityPacket(pos.toImmutable(), BlockEntityType.CHEST, compound));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ArenaChestBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof ArenaChestBlockEntity arenaChest) {
            arenaChest.tryOpen((ServerPlayerEntity) player);
            return ActionResult.SUCCESS_SERVER;
        }
        return ActionResult.PASS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ArenaChestBlockEntity.TYPE, (world1, pos, state1, blockEntity) -> blockEntity.tick());
    }
}
