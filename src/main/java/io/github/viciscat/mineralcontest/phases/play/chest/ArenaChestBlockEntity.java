package io.github.viciscat.mineralcontest.phases.play.chest;

import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.GuiInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.MineralContestKeys;
import io.github.viciscat.mineralcontest.util.GuiUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaChestBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    public static final int OPENING_TIME = 50;

    public static final RegistryKey<BlockEntityType<?>> REGISTRY_KEY = RegistryKey.of(RegistryKeys.BLOCK_ENTITY_TYPE, MineralContest.id("arena_chest"));
    public static final BlockEntityType<ArenaChestBlockEntity> TYPE = FabricBlockEntityTypeBuilder.create(ArenaChestBlockEntity::new, ArenaChestBlock.INSTANCE).build();

    private boolean unlocked = false;
    private final SimpleInventory lootInventory;

    private @Nullable ServerPlayerEntity opener = null;
    private @Nullable SimpleGui gui = null;
    private float previousOpenerHealth;
    private int timer = OPENING_TIME;

    public ArenaChestBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        lootInventory = new SimpleInventory(27){
            @Override
            public void onClose(PlayerEntity player) {
                super.onClose(player);
                onLootInventoryClosed();
            }
        };
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("game.mineral_contest.chest");
    }

    public void tryOpen(@NotNull ServerPlayerEntity player) {
        if (unlocked) {
            player.openHandledScreen(this);
            return;
        }
        if (opener != null) {
            player.sendMessage(Text.translatable("game.mineral_contest.chest.alreadyOpening"), true);
            return;
        }
        opener = player;
        previousOpenerHealth = player.getHealth();
        timer = 0;
        player.openHandledScreen(this);
    }

    public void tick() {
        assert world != null;
        if (opener != null && (opener.isRemoved() || opener.isDead() || gui == null || gui.getPlayer() != opener)) {
            opener = null;
            if (gui != null) gui.close();
            gui = null;
            world.playSound(null, getPos(), SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
        }
        if (opener == null) return;
        timer++;
        if (opener.getHealth() < previousOpenerHealth) {
            if (gui != null) {
                gui.close();
                gui = null;
                world.playSound(null, getPos(), SoundEvents.BLOCK_ANVIL_DESTROY, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            }
            return;
        }
        previousOpenerHealth = opener.getHealth();
        if (timer >= OPENING_TIME) {
            unlocked = true;
            assert world != null;
            world.addSyncedBlockEvent(getPos(), Blocks.CHEST, 1, 1); // open chest on client
            Vec3d centerPos = getPos().toCenterPos();
            world.playSound(null, getPos(), SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, world.random.nextFloat() * 0.1F + 0.9F);
            LootTable loot = world.getServer().getReloadableRegistries().getLootTable(MineralContestKeys.CHEST_LOOT_TABLE);

            LootWorldContext lootContext = new LootWorldContext.Builder((ServerWorld) world).add(LootContextParameters.ORIGIN, centerPos).build(LootContextTypes.CHEST);
            loot.supplyInventory(lootInventory, lootContext, world.getRandom().nextLong());
            opener.openHandledScreen(this);
            opener = null;

        }
    }

    private void onLootInventoryClosed() {
        if (lootInventory.isEmpty()) {
            assert world != null;
            world.setBlockState(getPos(), Blocks.AIR.getDefaultState());
        }
    }


    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        if (unlocked) {
            return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, lootInventory, 3);
        }

        return (gui = new UnlockGui((ServerPlayerEntity) player)).openAsScreenHandler(syncId, playerInventory, player);
    }

    private class UnlockGui extends SimpleGui {

        public UnlockGui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_9X3, player, true);
            for (int i = 0; i < getSize(); i++) setSlot(i, GuiUtils.EMPTY_SLOT);
            for (int i = 0; i < 7; i++) {
                float percentageStart = i / 7f;
                float percentageEnd = (i + 1) / 7f;
                setSlot(i + 10, new ProgressBarElement(percentageStart, percentageEnd));
            }
        }

        @Override
        public void onTick() {
            super.onTick();
            if (timer % 2 == 0) return;
            float v = OPENING_TIME / 2f;
            world.playSound(null, getPos(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.BLOCKS, 2.0F, (float) Math.pow(2, (timer - v) / v));
        }

        @Override
        public void onClose() {
            if (opener == player) opener = null;
        }
    }

    private class ProgressBarElement implements GuiElementInterface {
        private static final ItemStack GRAY = new ItemStack(Items.LIGHT_GRAY_STAINED_GLASS_PANE);
        private static final ItemStack YELLOW = new ItemStack(Items.YELLOW_STAINED_GLASS_PANE);
        private static final ItemStack GREEN = new ItemStack(Items.LIME_STAINED_GLASS_PANE);

        private final float percentageStart;
        private final float percentageEnd;

        private ProgressBarElement(float percentageStart, float percentageEnd) {
            this.percentageStart = percentageStart;
            this.percentageEnd = percentageEnd;
        }

        @Override
        public ItemStack getItemStack() {
            float v = (float) timer / OPENING_TIME;
            v = (v - percentageStart) / (percentageEnd - percentageStart);
            if (v < 0.5) return GRAY;
            else if (v < 1) return YELLOW;
            else return GREEN;
        }

        @Override
        public ItemStack getItemStackForDisplay(GuiInterface gui) {
            ItemStack display = GuiElementInterface.super.getItemStackForDisplay(gui);
            display.set(DataComponentTypes.ITEM_NAME, Text.literal(GuiUtils.SMALL_FLOAT_FORMATTER.format((float) timer / OPENING_TIME * 100) + "%"));
            return display;
        }
    }
}
