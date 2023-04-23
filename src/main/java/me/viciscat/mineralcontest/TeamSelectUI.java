package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamSelectUI {
    private final static Material[] wools = new Material[] {Material.RED_WOOL, Material.BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL};
    public final static NamedTextColor[] textColors = new NamedTextColor[] {NamedTextColor.RED, NamedTextColor.BLUE, NamedTextColor.YELLOW, NamedTextColor.GREEN};
    public final static String[] teamColors = new String[] {"RED", "BLUE", "YELLOW", "GREEN"};

    public static void openUI(Player player, GameHandler gameHandler) {
        Inventory inventory = Bukkit.createInventory(new TeamSelectUI.Holder(), 27, Component.text("Select your team!"));
        for (int i = 0; i < 10; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.displayName(Component.text(""));
            glass.setItemMeta(glassMeta);

            inventory.setItem(i, glass);
            inventory.setItem(26 - i, new ItemStack(glass));
        }
        for (int i = 0; i < wools.length; i++) {
            ItemStack itemStack = new ItemStack(wools[i]);
            ItemMeta woolMeta = itemStack.getItemMeta();
            woolMeta.displayName(Component.text(teamColors[i]).append(Component.text(" TEAM")).decoration(TextDecoration.ITALIC, false));
            MineralTeam team = gameHandler.getTeam(i);
            if (team.playerInTeam(player)) {
                woolMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                woolMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(woolMeta);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to join the ", Style.style(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(teamColors[i], Style.style(textColors[i], TextDecoration.BOLD)))
                    .append(Component.text(" team!", Style.style(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
            lore.add(Component.text("Players in this team:").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            for (UUID uuid : team.getPlayerUUID()) {
                Player teamPlayer = Bukkit.getPlayer(uuid);
                if (teamPlayer != null) {
                    lore.add(Component.text("- ").append(teamPlayer.displayName())
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            itemStack.lore(lore);
            inventory.setItem(10 + i*2, itemStack);
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
