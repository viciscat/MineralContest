package me.viciscat.mineralcontest;

import org.bukkit.entity.Player;

public class MineralTeam {

    private final Player[] players = new Player[4];

    public int playerCount = 0;

    public boolean addPlayer(Player player) {
        if (playerCount >= 4) return false;
        if (!player.isOnline()) return false;
        players[playerCount] = player;
        return true;
    }

    public Player[] getPlayers() {
        return players;
    }

    public int getPlayerCount() {
        return playerCount;
    }
}
