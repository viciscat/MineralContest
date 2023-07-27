package me.viciscat.mineralcontest.ui;

import me.viciscat.mineralcontest.game.GameHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
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

        featherMeta.displayName(GlobalTranslator.render(Component.translatable("mineral-contest.ui.class_select.agile").decoration(TextDecoration.ITALIC, false), player.locale()));
        feather.setItemMeta(featherMeta);

        MessageFormat agileDescription = GlobalTranslator.translator().translate("mineral-contest.ui.class_select.agile.description", player.locale());
        assert agileDescription != null;
        feather.lore(
                Arrays.stream(agileDescription.toPattern().split("\n")).map(s -> Component.text(s, NamedTextColor.GRAY)).toList()
        );


        // Worker
        ItemStack gold = new ItemStack(Material.GOLD_INGOT);
        ItemMeta goldMeta = gold.getItemMeta();

        goldMeta.displayName(GlobalTranslator.render(Component.translatable("mineral-contest.ui.class_select.worker").decoration(TextDecoration.ITALIC, false), player.locale()));
        gold.setItemMeta(goldMeta);

        MessageFormat workerDescription = GlobalTranslator.translator().translate("mineral-contest.ui.class_select.worker.description", player.locale());
        assert workerDescription != null;
        gold.lore(
                Arrays.stream(workerDescription.toPattern().split("\n")).map(s -> Component.text(s, NamedTextColor.GRAY)).toList()
        );


        // Robust
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();

        chestplateMeta.displayName(GlobalTranslator.render(Component.translatable("mineral-contest.ui.class_select.robust").decoration(TextDecoration.ITALIC, false), player.locale()));
        chestplate.setItemMeta(chestplateMeta);

        MessageFormat robustDescription = GlobalTranslator.translator().translate("mineral-contest.ui.class_select.robust.description", player.locale());
        assert robustDescription != null;
        chestplate.lore(
                Arrays.stream(robustDescription.toPattern().split("\n")).map(s -> Component.text(s, NamedTextColor.GRAY)).toList()
        );


        // Warrior
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();

        swordMeta.displayName(GlobalTranslator.render(Component.translatable("mineral-contest.ui.class_select.warrior").decoration(TextDecoration.ITALIC, false), player.locale()));
        sword.setItemMeta(swordMeta);

        MessageFormat warriorDescription = GlobalTranslator.translator().translate("mineral-contest.ui.class_select.warrior.description", player.locale());
        assert warriorDescription != null;
        sword.lore(
                Arrays.stream(warriorDescription.toPattern().split("\n")).map(s -> Component.text(s, NamedTextColor.GRAY)).toList()
        );


        // Miner
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();

        pickaxeMeta.displayName(GlobalTranslator.render(Component.translatable("mineral-contest.ui.class_select.miner").decoration(TextDecoration.ITALIC, false), player.locale()));
        pickaxe.setItemMeta(pickaxeMeta);

        MessageFormat minerDescription = GlobalTranslator.translator().translate("mineral-contest.ui.class_select.miner.description", player.locale());
        assert minerDescription != null;
        pickaxe.lore(
                Arrays.stream(minerDescription.toPattern().split("\n")).map(s -> Component.text(s, NamedTextColor.GRAY)).toList()
        );

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
