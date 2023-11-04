package me.viciscat.mineralcontest.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class ChestUnlockingUI {

    static DecimalFormat decimalFormat = new DecimalFormat("#.0");


    public static void open(Player player) {
        player.openInventory(Bukkit.createInventory(new Holder(), InventoryType.HOPPER, Component.text("Unlocking...")));
    }

    public static void update(Player player, int timer, int maxTime) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (!(inventory.getHolder() instanceof Holder)) return;
        float progressPercentage = timer/(float) maxTime;
        for (int i = 0; i < inventory.getSize(); i++) {

            float slotProgression = (progressPercentage - i/(float)inventory.getSize())/(1/(float)inventory.getSize());
            if (slotProgression < 0) {
                inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            } else if (slotProgression > 0.67f) {
                inventory.setItem(i, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
            } else if (slotProgression > 0.33f) {
                inventory.setItem(i, new ItemStack(Material.WHITE_STAINED_GLASS_PANE));
            }
        }
        player.getOpenInventory().setTitle("Unlocking... " + decimalFormat.format(progressPercentage * 100) + "%");
    }


    public static class Holder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
