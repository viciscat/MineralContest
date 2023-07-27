package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.game.MineralChunkGen;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class testing3Command implements CommandExecutor {
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
        WorldCreator creator = new WorldCreator(String.valueOf(new Random().nextLong()));
        World world = creator.generator(new MineralChunkGen()).createWorld();
        assert world != null;
        player.teleport(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 2, 0));
        return true;
    }
}
