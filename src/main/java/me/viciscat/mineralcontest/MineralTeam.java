package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MineralTeam implements Comparable<MineralTeam>{

    private final List<UUID> players = new ArrayList<>();

    public TranslatableComponent getTeamName() {
        return teamName;
    }

    private final BoundingBox boundingBox;

    public BoundingBox BoundingBox() {
        return boundingBox;
    }

    private final Location spawnLocation;
    public Location getSpawnLocation() {
        return spawnLocation;
    }

    private final Location enderChestLocation;
    public Location getEnderChestLocation() {
        return enderChestLocation;
    }

    private final Team team;
    public Team getTeam() {
        return team;
    }

    private final TranslatableComponent teamName;

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


    public float getScoreMultiplier() {
        return scoreMultiplier;
    }
    public void setScoreMultiplier(float scoreMultiplier) {
        this.scoreMultiplier = scoreMultiplier;
    }
    public void addScoreMultiplier(float scoreMultiplier) {
        this.scoreMultiplier += scoreMultiplier;
    }
    public float scoreMultiplier = 1;

    private Location arenaLocation;


    public MineralTeam(TranslatableComponent translatableName, BoundingBox territory, Location spawn, Location enderChest, Location arenaSpawn, Team team) {
        teamName = translatableName;
        boundingBox = territory;
        spawnLocation = spawn;
        enderChestLocation = enderChest;
        arenaLocation = arenaSpawn;
        this.team = team;
    }


    public void addPlayer(MineralPlayer player) {
        if (players.size() >= 4) return;
        if (!player.Player().isOnline()) return;
        if (playerInTeam(player.PlayerUUID())) return;
        player.MineralTeam(this);
        players.add(player.PlayerUUID());
    }

    public void removePlayer(MineralPlayer player) {
        players.remove(player.PlayerUUID());
    }

    public List<UUID> getPlayerUUID() {
        return players;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean playerInTeam(MineralPlayer player) {
        return players.contains(player.PlayerUUID());
    }
    public boolean playerInTeam(UUID playerUUID) {
        return players.contains(playerUUID);
    }
    public void teleportToArena(){
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(arenaLocation);
                player.sendMessage(Component.text("Whoosh!"));
            }
        }
    }

    public void sendMessage(ComponentLike componentLike) {
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(componentLike);
            }
        }
    }


    @Override
    public int compareTo(@NotNull MineralTeam o) {
        return Integer.compare(this.score, o.getScore());
    }
}
