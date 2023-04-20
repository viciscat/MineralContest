package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.BoundingBox;

public class MineralListener implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        BoundingBox boundingBox = new BoundingBox(-80, 0, -80, 80, 255, 80);
        LivingEntity entity = event.getEntity();
        CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();
        if (spawnReason.equals(CreatureSpawnEvent.SpawnReason.NATURAL) && boundingBox.contains(entity.getLocation().toVector())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!MineralContest.instance.gameHandlerMap.containsKey(player.getWorld())) return;
        Inventory inventory = event.getInventory();
        if (!inventory.getType().equals(InventoryType.ENDER_CHEST)) return;

        player.sendMessage(Component.text("You closed an enderchest! Is it one of the castle's one? Who knows cuz this hasn't been implemented yet!!"));


    }
}
