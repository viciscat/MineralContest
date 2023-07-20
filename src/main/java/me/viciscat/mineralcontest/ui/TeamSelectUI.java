package me.viciscat.mineralcontest.ui;

import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.game.GameHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
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
    public final static String[] teamColors = new String[] {"red", "blue", "yellow", "green"};

    public static void openUI(Player player, GameHandler gameHandler) {
        openUI(player, gameHandler, Bukkit.createInventory(new TeamSelectUI.Holder(), 27, Component.translatable("mineral-contest.ui.team_select.title")));
    }

    public static void openUI(Player player, GameHandler gameHandler, Inventory inventory) {
        for (int i = 0; i < 10; i++) {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.displayName(Component.text(""));
            glass.setItemMeta(glassMeta);

            inventory.setItem(i, glass);
            inventory.setItem(26 - i, new ItemStack(glass));
        }
        for (int i = 0; i < wools.length; i++) {

            // Get the translated team color
            TextComponent colorComponent = (TextComponent) GlobalTranslator.render(
                    Component.translatable("mineral-contest.teams." + teamColors[i], Style.style(textColors[i], TextDecoration.BOLD)),
                    player.locale());
            colorComponent = colorComponent.content(colorComponent.content().toUpperCase());

            // Put the item
            ItemStack itemStack = new ItemStack(wools[i]);
            ItemMeta woolMeta = itemStack.getItemMeta();
            woolMeta.displayName(colorComponent.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, false));
            MineralTeam team = gameHandler.getTeam(i);
            assert team != null;
            if (team.playerInTeam(gameHandler.playerManager.getPlayer(player))) { // Glint if same team
                woolMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                woolMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            itemStack.setItemMeta(woolMeta);


            // Get the translated join string
            Component joinText = GlobalTranslator.render(
                    Component.translatable("mineral-contest.ui.team_select.join", Style.style(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
                    player.locale());

            // Replace the thingies
            joinText = joinText.replaceText(TextReplacementConfig.builder().matchLiteral("%%color%%").replacement(colorComponent).build());

            List<Component> lore = new ArrayList<>();
            lore.add(joinText);
            lore.add(GlobalTranslator.render(
                    Component.translatable("mineral-contest.ui.team_select.players").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                    player.locale()));
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
