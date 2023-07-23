package me.viciscat.mineralcontest.ui;

import me.viciscat.mineralcontest.game.GameHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class ClassSelectUI {

    public static void openUI(Player player, GameHandler gameHandler) {
        Inventory inventory = Bukkit.createInventory(new ClassSelectUI.Holder(), 27, Component.translatable("mineral-contest.ui.class_select.title"));
        for (int i = 0; i < 10; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.displayName(Component.text(""));
            glass.setItemMeta(glassMeta);
            inventory.setItem(i, glass);
            inventory.setItem(26 - i, new ItemStack(glass));
        }

        // Agile
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherMeta = feather.getItemMeta();
        featherMeta.displayName(Component.text("Agile").decoration(TextDecoration.ITALIC, false));
        feather.setItemMeta(featherMeta);
        feather.lore(Arrays.asList(
                Component.text("- You move 20% faster"),
                Component.text("- You don't take fall damage")
        ));

        // Worker
        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = gold.getItemMeta();
        goldMeta.displayName(Component.text("Worker").decoration(TextDecoration.ITALIC, false));
        gold.setItemMeta(goldMeta);
        gold.lore(Arrays.asList(
                Component.text("- +25% Team score"),
                Component.text("- You only have 5 hearts")
        ));

        // Robust
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        chestplateMeta.displayName(Component.text("Robust").decoration(TextDecoration.ITALIC, false));
        chestplate.setItemMeta(chestplateMeta);
        chestplate.lore(Arrays.asList(
                Component.text("- 12 Hearts"),
                Component.text("- Take 10% less damage")
        ));

        // Warrior
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.displayName(Component.text("Warrior").decoration(TextDecoration.ITALIC, false));
        sword.setItemMeta(swordMeta);
        sword.lore(Arrays.asList(
                Component.text("- You deal +25% damage"),
                Component.text("- You only have 8 hearts")
        ));

        // Miner
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        pickaxeMeta.displayName(Component.text("Miner").decoration(TextDecoration.ITALIC, false));
        pickaxe.setItemMeta(pickaxeMeta);
        pickaxe.lore(Arrays.asList(
                Component.text("- Ores auto smelt when you mine them"),
                Component.text("- You only have 2 rows in your inventory")
        ));

        inventory.setItem(11, feather);
        inventory.setItem(12, gold);
        inventory.setItem(13, chestplate);
        inventory.setItem(14, sword);
        inventory.setItem(15, pickaxe);

        player.openInventory(inventory);
    }

    static public class Holder implements InventoryHolder {

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
