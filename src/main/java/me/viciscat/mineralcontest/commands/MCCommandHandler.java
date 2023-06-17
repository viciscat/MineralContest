package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralListener;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.commands.sub.GameCreator;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MCCommandHandler implements CommandExecutor {

    MineralContest plugin = MineralContest.getInstance();
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
        if (!(sender instanceof Player)) return false;
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendPlainMessage("Command Format: /mineralcontest <create|...>");
            return false;
        }
        switch (args[0]) {
            case "create" -> {
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest create <World Name>");
                    return false;
                }
                GameCreator gameCreator = new GameCreator();
                return gameCreator.createGame(args[1], sender);
            }
            case "debug" -> {
                if (!plugin.gameHandlerMap.containsKey(player.getWorld())) return false;
                GameHandler gameHandler = plugin.gameHandlerMap.get(player.getWorld());
                switch (args[1]) {
                    case "timer" -> {
                        sender.sendPlainMessage(String.valueOf(gameHandler.getSecondsLeft()));
                        return true;
                    }
                    case "team" -> {
                        sender.sendPlainMessage(String.valueOf(gameHandler.getTeamID(player)));
                        return true;
                    }
                    case "score" -> {
                        int teamID = gameHandler.getTeamID(player);
                        sender.sendPlainMessage(String.valueOf(gameHandler.getTeam(teamID).getScore()));
                        return true;
                    }
                    default -> {
                        sender.sendPlainMessage("L");
                        return false;
                    }
                }
            }
            case "start" -> {
                if (!plugin.gameHandlerMap.containsKey(player.getWorld())) return false;
                GameHandler handler = plugin.gameHandlerMap.get(player.getWorld());
                handler.startClassSelection();
                return true;
            }
            case "join" -> {
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest join <Game Name>");
                    return false;
                }
                World requestedWorld = Bukkit.getWorld(args[1]);
                if (requestedWorld == null || !MineralContest.getInstance().gameHandlerMap.containsKey(requestedWorld)) {
                    sender.sendPlainMessage("This game doesn't exist!!");
                    return false;
                }
                GameHandler gameHandler = MineralContest.getInstance().gameHandlerMap.get(requestedWorld);
                Location tpLocation = new Location(requestedWorld, -25, gameHandler.groundHeight + 1, 0);
                if (gameHandler.gamePhase != GameHandler.Phase.PREGAME) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
                player.teleport(tpLocation);
                return true;


            }
            default -> {
                return false;
            }
        }
    }
}
