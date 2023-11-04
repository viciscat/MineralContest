package me.viciscat.mineralcontest.listeners;

import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.game.ArenaChestHandler;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.ui.ChestUnlockingUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;
import java.util.Objects;

public class ArenaChestListener implements Listener {

    Map<World, GameHandler> map = MineralContest.instance.gameHandlerMap;

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return; // Is it a player?
        if (!map.containsKey(player.getWorld())) return;
        if (event.getReason().equals(InventoryCloseEvent.Reason.OPEN_NEW)) return;

        Inventory inventory = event.getInventory();
        GameHandler gameHandler = map.get(player.getWorld());

        if (Objects.equals(inventory.getLocation(), gameHandler.arenaChestHandler.getBlock().getLocation())) {
            if (inventory.isEmpty()) {
                gameHandler.arenaChestHandler.end();
            }
            gameHandler.arenaChestHandler.resetCurrentOpener();
        }


    }

    @EventHandler
    public void onOpenChest(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) return;
        GameHandler gameHandler = map.get(player.getWorld());
        ArenaChestHandler arenaChestHandler = gameHandler.arenaChestHandler;
        if (!Objects.equals(event.getClickedBlock(), arenaChestHandler.getBlock())) return;
        if (arenaChestHandler.getCurrentChestOpener() == null) {
            arenaChestHandler.changeCurrentOpener(gameHandler.playerManager.getPlayer(player));
        } else {
            TextComponent unlockWarning = Component.text(" is already unlocking the chest! Attack them to stop them!");
            player.sendMessage(arenaChestHandler.getCurrentChestOpener().getPlayer().displayName().append(unlockWarning));
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerAttacked(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!map.containsKey(player.getWorld())) return;
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof ChestUnlockingUI.Holder) {
            player.closeInventory(InventoryCloseEvent.Reason.PLAYER);
        }
    }
}
