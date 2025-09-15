package io.github.viciscat.mineralcontest.phases.play;

import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import io.github.viciscat.mineralcontest.event.EditPlayerDamageEvent;
import io.github.viciscat.mineralcontest.event.PlayerRespawnEvent;
import io.github.viciscat.mineralcontest.util.Attachments;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.DroppedItemsResult;
import xyz.nucleoid.stimuli.event.block.BlockDropItemsEvent;

import java.util.List;
import java.util.Map;

public class PlayerClassManager {

    private static final Map<Item, Item> MINER_CLASS_AUTO_SMELT = Map.of(
            Items.RAW_IRON, Items.IRON_INGOT,
            Items.RAW_GOLD, Items.GOLD_INGOT,
            Items.RAW_COPPER, Items.COPPER_INGOT
    );

    public static PlayerClassManager addTo(GameActivity activity, Map<PlayerRef, PlayerClass> playerToClas) {
        PlayerClassManager manager = new PlayerClassManager(playerToClas);
        activity.getGameSpace().setAttachment(Attachments.PLAYER_CLASS_MANAGER, manager);
        activity.listen(PlayerRespawnEvent.EVENT, manager::onPlayerRespawn);
        activity.listen(BlockDropItemsEvent.EVENT,manager::onBlockDropItems);
        activity.listen(EditPlayerDamageEvent.EVENT, manager::onDamage);
        return manager;
    }

    private final Map<PlayerRef, PlayerClass> playerToClass;

    private PlayerClassManager(Map<PlayerRef, PlayerClass> playerToClass) {
        this.playerToClass = Map.copyOf(playerToClass);
    }

    public @Nullable PlayerClass getPlayerClass(PlayerRef player) {
        return this.playerToClass.get(player);
    }

    public @Nullable PlayerClass getPlayerClass(ServerPlayerEntity player) {
        return this.getPlayerClass(PlayerRef.of(player));
    }

    public boolean hasClass(ServerPlayerEntity player, @NotNull Identifier classId) {
        PlayerClass playerClass = getPlayerClass(player);
        return playerClass != null && classId.equals(playerClass.id());
    }

    private void onPlayerRespawn(ServerPlayerEntity player) {
        applyPlayerClass(player);
    }

    private void applyPlayerClass(ServerPlayerEntity player) {
        PlayerClass playerClass = playerToClass.get(PlayerRef.of(player));
        if (playerClass == null) return;
        player.getAttributes().addTemporaryModifiers(playerClass.getModifiersMap());
        playerClass.giveKitTo(player);
    }

    private float onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        if (hasClass(player, MineralContest.ROBUST_CLASS)) {
            return amount * 0.85f;
        }
        return amount;
    }

    private DroppedItemsResult onBlockDropItems(@Nullable Entity breaker, ServerWorld world, BlockPos pos, BlockState state, List<ItemStack> dropStacks) {
        if (!(breaker instanceof ServerPlayerEntity player)) return DroppedItemsResult.pass(dropStacks);
        PlayerClass playerClass = playerToClass.get(PlayerRef.of(player));
        if (playerClass == null || !MineralContest.MINER_CLASS.equals(playerClass.id())) return DroppedItemsResult.pass(dropStacks);
        if (!state.isIn(ConventionalBlockTags.ORES)) return DroppedItemsResult.pass(dropStacks);
        dropStacks.replaceAll(itemStack -> itemStack.withItem(getReplacement(itemStack.getItem())));
        return DroppedItemsResult.pass(dropStacks);
    }

    private static ItemConvertible getReplacement(Item item) {
        return MINER_CLASS_AUTO_SMELT.getOrDefault(item, item);
    }


}
