package me.viciscat.mineralcontest;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerManager {

    public Map<UUID, MineralPlayer> playerMap = new HashMap<>();

    /**
     * Gets the MineralPlayer of the given player
     * if it doesn't exist, creates it
     * @param player the player
     * @return MineralPlayer of the player
     */
    public MineralPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Gets the MineralPlayer of the given player UUID
     * if it doesn't exist, creates it
     * @param playerUUID player's UUID
     * @return MineralPlayer of the player
     */
    public MineralPlayer getPlayer(UUID playerUUID) {
        return playerMap.computeIfAbsent(playerUUID, MineralPlayer::new);
    }

    public MineralPlayer[] getPlayers() {
        return playerMap.values().toArray(new MineralPlayer[0]);
    }

}
