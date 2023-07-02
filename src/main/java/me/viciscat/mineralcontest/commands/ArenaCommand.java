package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.MineralPlayer;
import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.game.GameHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ArenaCommand implements CommandExecutor {
    Map<World, GameHandler> map = MineralContest.getInstance().gameHandlerMap;
    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (!map.containsKey(player.getWorld())) return false;
        GameHandler gameHandler = map.get(player.getWorld());
        MineralPlayer mineralPlayer = gameHandler.playerManager.getPlayer(player);
        MineralTeam mineralTeam = mineralPlayer.MineralTeam();
        if (mineralTeam == null) return false;
        int secBeforeChest = gameHandler.getSecondsLeft() - gameHandler.getNextChest();
        if (secBeforeChest <= 10 && secBeforeChest >= 0) {
            mineralTeam.teleportToArena();
        } else {
            player.sendMessage(Component.text("You can only use this command when a chest is gonna appear!"));
            player.sendMessage(Component.text("Tu peux seulement utiliser cette commande quand un coffre va apparaitre"));
        }
        return true;
    }
}
