package me.viciscat.mineralcontest.game;

import it.unimi.dsi.fastutil.Pair;
import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralUtils;
import me.viciscat.mineralcontest.ui.ChestUnlockingUI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class ArenaChestHandler {

    static int TIME = 60;

    static Pair<Double, Material>[] loot = new Pair[4];
    static {
        loot[0] = Pair.of(1.0d, Material.EMERALD);
        loot[1] = Pair.of(2.0d, Material.DIAMOND);
        loot[2] = Pair.of(30.0d, Material.IRON_INGOT);
        loot[3] = Pair.of(15.0d, Material.GOLD_INGOT);
    }

    public Block getBlock() {
        return block;
    }

    private final Block block; // The block where the chest is located

    private BukkitTask schedulerTask = null;

    public MineralPlayer getCurrentChestOpener() {
        return currentChestOpener;
    }
    private MineralPlayer currentChestOpener = null;
    int timer;

    public ArenaChestHandler(Block chestBlock) {
        this.block = chestBlock;
    }

    public void changeCurrentOpener(MineralPlayer player) {
        if (currentChestOpener == null) {
            currentChestOpener = player;
            timer = 0;
            ChestUnlockingUI.open(currentChestOpener.getPlayer());
            return;
        }
        if (currentChestOpener.equals(player)) return;
        currentChestOpener.getPlayer().closeInventory();
        currentChestOpener = player;
        ChestUnlockingUI.open(currentChestOpener.getPlayer());
        timer = 0;
        ChestUnlockingUI.update(currentChestOpener.getPlayer(), timer, TIME);
    }

    public void resetCurrentOpener() {currentChestOpener = null;}

    public void end() {
        block.setType(Material.AIR);
        currentChestOpener = null;
        if (schedulerTask == null) return;
        schedulerTask.cancel();
        schedulerTask = null;

    }

    /**
     * Starts the chest sequence, checking each tick for players opening the chest and showing them the progress bar
     */
    public void start() {
        block.setType(Material.CHEST);
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            chest.customName(Component.text("Arena chest"));
            for (int i = 0; i < 27; i++) {
                Material chosenMaterial = MineralUtils.weightedRandom(loot);
                if (chosenMaterial == null) {
                    chosenMaterial = Material.AIR;
                }
                inventory.setItem(i, new ItemStack(chosenMaterial));
            }
            inventory.setItem(13, new ItemStack(Material.EMERALD));
        }

        schedulerTask = Bukkit.getScheduler().runTaskTimer(MineralContest.instance, this::onTick, 0, 1);
    }

    public void onTick() {
        if (currentChestOpener == null) return;
        if (!currentChestOpener.getPlayer().isOnline()) {
            currentChestOpener = null;
            return;
        }
        timer++;
        ChestUnlockingUI.update(currentChestOpener.getPlayer(), timer, TIME);
        if (timer % 3 == 0 && timer < TIME) {
            currentChestOpener.getPlayer().playSound(currentChestOpener.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, (timer/(float) TIME)*2 + 0.2f);
        }
        if (timer == TIME) {
            BlockState state = block.getState();
            if (state instanceof Chest chest) currentChestOpener.getPlayer().openInventory(chest.getBlockInventory());
        }
    }



}
