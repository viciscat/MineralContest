package me.viciscat.mineralcontest;

import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.entity.Player;

public class MineralTeam {

    private final Player[] players = new Player[4];

    public int playerCount = 0;

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

    public boolean playerInTeam(Player player) {
        return ArrayUtils.contains(players, player);
    }

}
