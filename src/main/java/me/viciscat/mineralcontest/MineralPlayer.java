package me.viciscat.mineralcontest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public class MineralPlayer {

    private final UUID playerUUID;
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    public Player getPlayer() {
        return Bukkit.getPlayer(playerUUID);
    }


    private MineralTeam mineralTeam = null;
    public void setMineralTeam(MineralTeam mineralTeam) {
        this.mineralTeam = mineralTeam;
    }
    public @Nullable MineralTeam getMineralTeam() {
        return mineralTeam;
    }


    public MineralPlayer(UUID playerUUID) {
        this.playerUUID = playerUUID;
        Scoreboard newScoreboard = Bukkit.getScoreboardManager().getNewScoreboard();

        Team redTeam = newScoreboard.registerNewTeam("red");
        Team blueTeam = newScoreboard.registerNewTeam("blue");
        Team greenTeam = newScoreboard.registerNewTeam("green");
        Team yellowTeam = newScoreboard.registerNewTeam("yellow");


        // Adding teams for each player "locally" so it can be displayed in their player list
        redTeam.prefix(Component.text("R ").style(Style.style().color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true)));
        blueTeam.prefix(Component.text("B ").style(Style.style().color(NamedTextColor.DARK_BLUE).decoration(TextDecoration.BOLD, true)));
        greenTeam.prefix(Component.text("G ").style(Style.style().color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true)));
        yellowTeam.prefix(Component.text("Y ").style(Style.style().color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true)));

        Objective newObjective = newScoreboard.registerNewObjective("mineral_contest_gui", Criteria.DUMMY, Component.text("Mineral Contest").decoration(TextDecoration.BOLD, true).color(NamedTextColor.DARK_AQUA));
        Objective playerHealthObjective = newScoreboard.registerNewObjective("player_health", Criteria.HEALTH, Component.text("HP"));
        playerHealthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
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
        getPlayer().sendMessage(Component.text("--> ").append(
                Component.translatable("mineral-contest.ui.class_select." + classString))
        );
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
