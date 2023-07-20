package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralTeam;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.commands.sub.GameCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.FileUtil;
import org.codehaus.plexus.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

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
        if (!(sender instanceof Player player)) return false;
        if (args.length == 0) {
            sender.sendPlainMessage("Command Format: /mineralcontest <create|...>");
            return false;
        }
        switch (args[0]) {
            case "create" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                String name;
                if (args.length == 1) {
                    SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                    name = sdf1.format(new Date().getTime());
                    sender.sendPlainMessage("Using current time as name: " + name);
                } else {
                    name = args[1];
                }
                GameCreator gameCreator = new GameCreator();
                return gameCreator.createGame(name, sender);
            }
            case "debug" -> {
                if (!plugin.gameHandlerMap.containsKey(player.getWorld())) return false;
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                GameHandler gameHandler = plugin.gameHandlerMap.get(player.getWorld());
                switch (args[1]) {
                    case "timer" -> {
                        sender.sendPlainMessage(String.valueOf(gameHandler.getSecondsLeft()));
                        return true;
                    }
                    case "team" -> {
                        MineralTeam mineralTeam = gameHandler.playerManager.getPlayer(player).MineralTeam();
                        Component teamName = mineralTeam == null ? Component.text("None") : mineralTeam.getTeamName();
                        sender.sendMessage(teamName);
                        sender.sendPlainMessage(Objects.requireNonNull(gameHandler.gameScoreboard.getPlayerTeam(player)).getName());
                        return true;
                    }
                    case "score" -> {
                        MineralTeam mineralTeam = gameHandler.playerManager.getPlayer(player).MineralTeam();
                        sender.sendPlainMessage(mineralTeam == null ? "-1" : String.valueOf(mineralTeam.getScore()));
                        return true;
                    }
                    default -> {
                        sender.sendPlainMessage("L");
                        return false;
                    }
                }
            }
            case "start" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                if (!plugin.gameHandlerMap.containsKey(player.getWorld())) return false;
                GameHandler handler = plugin.gameHandlerMap.get(player.getWorld());
                handler.startClassSelection();
                return true;
            }
            case "join" -> {
                if (!sender.hasPermission("mineral-contest.join")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest join <Game Name>");
                    return false;
                }
                String name = args[1];
                String prefix = plugin.config.getString("worldNamePrefix", "mineral-contest_");
                World requestedWorld = Bukkit.getWorld(prefix + name);
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
                player.setBedSpawnLocation(tpLocation, true);
                return true;


            }
            case "reload" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                MineralContest.instance.actuallyReloadConfig();
                return true;
            }
            case "config" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest config <config thing> <new value>");
                    return false;
                }
                FileConfiguration configuration = plugin.config;
                if (args.length == 2) {
                    Object object =  configuration.get(args[1]);
                    for (String comment : configuration.getComments(args[1])) {
                        sender.sendPlainMessage(comment);
                    }
                    sender.sendPlainMessage("value: " + (object == null ? "null": object.toString()));
                    return true;
                }
                Object object =  configuration.get(args[1]);
                if (object instanceof Integer) {
                    try {
                        configuration.set(args[1], Integer.valueOf(args[2]));
                    } catch (NumberFormatException e) {
                        sender.sendPlainMessage("THIS SHIT AIN'T AN INT");
                        return true;
                    }
                }
                sender.sendPlainMessage("THINGY HAS BEEN SET!");
                plugin.saveConfig();
                return true;

            }
            case "delete_force" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest delete_force <world_name>");
                    return false;
                }
                File worldFolder = Bukkit.getServer().getWorldContainer();
                File[] worldFiles = worldFolder.listFiles(File::isDirectory);
                assert worldFiles != null;

                for (File file : worldFiles) {
                    if (file.getName().equals(args[1])) {
                        try {
                            FileUtils.deleteDirectory(file);
                            sender.sendPlainMessage("deleted!");
                        } catch (IOException e) {
                            sender.sendPlainMessage("An error occured :(");
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            }
            case "unload" -> {
                if (!sender.hasPermission("mineral-contest.admin")) {
                    sender.sendPlainMessage("No permission :(");
                    return false;
                }
                if (args.length == 1) {
                    sender.sendPlainMessage("Format: /mineralcontest unload <world_name>");
                    return false;
                }
                World world = Bukkit.getServer().getWorld(args[1]);
                if (world == null) {
                    sender.sendPlainMessage("world no exist");
                    return false;
                }
                Bukkit.getServer().unloadWorld(world, false);
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
