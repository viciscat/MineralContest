package me.viciscat.mineralcontest;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
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
}
