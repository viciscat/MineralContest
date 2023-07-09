package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralContest;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;


public class testing2Command implements CommandExecutor {

    MineralContest plugin = JavaPlugin.getPlugin(MineralContest.class);

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
        if (args.length < 3) return false;
        World world = player.getWorld();
        Block startBlock = world.getBlockAt(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        if (Tag.LOGS.isTagged(startBlock.getType())) {
            recursiveBusiness(startBlock, world, -1);
        } else if (Tag.LEAVES.isTagged(startBlock.getType())) {
            if (!(startBlock.getBlockData() instanceof Leaves leafData)) return false;
            recursiveBusiness(startBlock, world, leafData.getDistance());
        }
        return true;
    }

    private void recursiveBusiness(Block block, World world, int distance) {
        Vector[] vectors = new Vector[]{
                new Vector(1, 0, 0),
                new Vector(-1, 0, 0),
                new Vector(0, 1, 0),
                new Vector(0, -1, 0),
                new Vector(0, 0, 1),
                new Vector(0, 0, -1),
        };
        Location location = block.getLocation();
        for (Vector vector : vectors) {
            Block newBlock = world.getBlockAt(location.clone().add(vector));
            if (Tag.LOGS.isTagged(newBlock.getType())) {
                block.setType(Material.AIR);
                recursiveBusiness(newBlock, world, 99);
            } else if (Tag.LEAVES.isTagged(newBlock.getType())) {
                if (!(newBlock.getBlockData() instanceof Leaves leafData)) continue;
                block.setType(Material.AIR);
                if (leafData.getDistance() <= distance) {
                    recursiveBusiness(newBlock, world, leafData.getDistance());
                }
            }
        }
    }
}
