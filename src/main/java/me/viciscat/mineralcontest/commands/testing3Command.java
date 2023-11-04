package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralContest;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

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
        World world = player.getWorld();
        Block block = world.getBlockAt(0, 80, 0);
        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            chest.getPersistentDataContainer().set(NamespacedKey.fromString("testy", MineralContest.getInstance()), PersistentDataType.STRING, "I love Tacos!");
            chest.update();
        }
        return true;
    }
}
