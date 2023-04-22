package me.viciscat.mineralcontest;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MineralTeam {

    private final List<UUID> players = new ArrayList<>();

    public int getScore() {
        return score;
    }
    public void setScore(int score) {
        this.score = score;
    }
    public void addScore(int score) {
        this.score += score;
    }
    public int score = 0;

    public boolean addPlayer(Player player) {
        if (players.size() >= 4) return false;
        if (!player.isOnline()) return false;
        if (playerInTeam(player.getUniqueId())) return false;
        players.add(player.getUniqueId());
        return true;
    }

    public boolean removePlayer(Player player) {
        return players.remove(player.getUniqueId());
    }

    public List<UUID> getPlayerUUID() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean playerInTeam(Player player) {
        return players.contains(player.getUniqueId());
    }
    public boolean playerInTeam(UUID playerUUID) {
        return players.contains(playerUUID);
    }

}
