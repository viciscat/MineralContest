package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;

import java.util.Map;

public class MineralListener implements Listener {

    Map<World, GameHandler> map = MineralContest.instance.gameHandlerMap;

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!map.containsKey(entity.getWorld())) return;
        BoundingBox boundingBox = new BoundingBox(-80, 0, -80, 80, 255, 80);
        CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
        if (spawnReason.equals(CreatureSpawnEvent.SpawnReason.NATURAL) && boundingBox.contains(entity.getLocation().toVector())) {
            event.setCancelled(true);

        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return; // Is it a player?
        if (!map.containsKey(player.getWorld())) return; // Is it in a Mineral Contest instance?

        Inventory inventory = event.getInventory();
        GameHandler gameHandler = map.get(player.getWorld());

        if (!inventory.getType().equals(InventoryType.ENDER_CHEST)) return; // Is it an EC ?
        if (inventory.getLocation() == null) return;
        if (gameHandler.getTeamID(inventory.getLocation()) == -1) return;
        int chestTeamID = gameHandler.getTeamID(inventory.getLocation());
        int playerTeamID = gameHandler.getTeamID(player);
        Inventory playerInventory = player.getInventory();
        if (playerTeamID == -1 || chestTeamID != playerTeamID) {
            for (ItemStack item : inventory.getStorageContents()) {
                if (item != null) { playerInventory.addItem(item); }
            }

            inventory.clear();
            player.sendMessage(Component.text("Wrong team chest >:( How did you get in here?"));
            return;
        }
        int finalScore = 0;
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null) { finalScore += 0; } else {
                finalScore += gameHandler.scoreMap.getOrDefault(itemStack.getType(), 0) * itemStack.getAmount();
                inventory.remove(itemStack);
            }
        }
        gameHandler.getTeam(playerTeamID).addScore(finalScore);

        Location location = inventory.getLocation();
        player.sendMessage(Component.text("You closed an enderchest! Is it one of the castle's one? Who knows cuz this hasn't been implemented yet!!"));
        player.sendMessage(Component.text(location.getX() + " " + location.getY() + " " + location.getZ()));


    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!map.containsKey(player.getWorld())) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) return;
        ItemStack item = new ItemStack(Material.COMPASS);
        if (!player.getInventory().getItemInMainHand().equals(item)) return;
        event.setCancelled(true);
        GameHandler gameHandler = map.get(player.getWorld());
        TeamSelectUI.openUI(player, gameHandler);

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return; // Is it a player?
        if (!map.containsKey(player.getWorld())) return;
        if (event.getClickedInventory().getHolder() == null) return;
        if (event.getClickedInventory().getHolder() instanceof TeamSelectUI.Holder) {
            GameHandler gameHandler = map.get(player.getWorld());
            event.setCancelled(true);
            if (event.getRawSlot() == 10) {
                gameHandler.getTeam(0).addPlayer(player);
            }
        }
    }

}
