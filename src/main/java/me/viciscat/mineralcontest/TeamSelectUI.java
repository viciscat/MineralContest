package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TeamSelectUI {
    public static InventoryHolder Holder;

    public static void openUI(Player player, GameHandler gameHandler) {
        Inventory inventory = Bukkit.createInventory(new TeamSelectUI.Holder(), 27, Component.text("Select your team!"));
        for (int i = 0; i < 10; i++) {
            inventory.setItem(i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            inventory.setItem(26 - i, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            ItemStack itemStack = new ItemStack(Material.RED_WOOL);
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to join the ").append(Component.text("RED", TextColor.color(NamedTextColor.RED))).append(Component.text(" team!")));
            itemStack.lore(lore);
            inventory.setItem(10, itemStack);
        }
        player.openInventory(inventory);
    }

    static class Holder implements InventoryHolder {

        /**
         * Get the object's inventory.
         *
         * @return The inventory.
         */
        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }
}
