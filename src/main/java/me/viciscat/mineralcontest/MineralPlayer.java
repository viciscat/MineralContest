package me.viciscat.mineralcontest;

import com.destroystokyo.paper.ClientOption;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public class MineralPlayer {

    private final UUID playerUUID;
    public UUID PlayerUUID() {
        return playerUUID;
    }
    public Player Player() {
        return Bukkit.getPlayer(playerUUID);
    }


    private MineralTeam mineralTeam = null;
    public void MineralTeam(MineralTeam mineralTeam) {
        this.mineralTeam = mineralTeam;
    }
    public @Nullable MineralTeam MineralTeam() {
        return mineralTeam;
    }


    public MineralPlayer(UUID playerUUID) {
        this.playerUUID = playerUUID;
        Scoreboard newScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective newObjective = newScoreboard.registerNewObjective("mineral_contest_gui", Criteria.DUMMY, Component.text("Mineral Contest").decoration(TextDecoration.BOLD, true).color(NamedTextColor.DARK_AQUA));
        newObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        playerScoreboard = newScoreboard;
        locale = Bukkit.getPlayer(playerUUID).locale();
    }


    private String classString = "none";

    /**
     * The class string will be {@code "None"} if the player doesn't have a class (for some reason)
     * @return the class string
     */
    public String ClassString() {
        return classString;
    }
    public void ClassString(String classString) {
        this.classString = classString;
    }


    private Scoreboard playerScoreboard;
    public Scoreboard PlayerScoreboard() {
        return playerScoreboard;
    }
    public void PlayerScoreboard(Scoreboard playerScoreboard) {
        this.playerScoreboard = playerScoreboard;
    }


    private Locale locale;

    public Locale Locale() {
        return locale;
    }

    public void Locale(Locale locale) {
        this.locale = locale;
    }
}
