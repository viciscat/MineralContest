package me.viciscat.mineralcontest.commands;

import me.viciscat.mineralcontest.MineralContest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TabCompletion implements TabCompleter {
    /**
     * Requests a list of possible completions for a command argument.
     *
     * @param sender  Source of the command.  For players tab-completing a
     *                command inside a command block, this will be the player, not
     *                the command block.
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    The arguments passed to the command, including final
     *                partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     * to default to the command executor
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equals("mineralcontest")) return null;
        List<String> returnList = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("mineral-contest.admin")) {
                returnList.add("create");
                returnList.add("debug");
                returnList.add("start");
                returnList.add("reload");
                returnList.add("delete_force");
            }
            returnList.add("join");
            return returnList;
        } else if (args.length == 2) {
            if (args[0].equals("config")) {
                returnList.addAll(MineralContest.instance.config.getKeys(false));
            }
            return returnList;
        }
        return null;
    }
}
