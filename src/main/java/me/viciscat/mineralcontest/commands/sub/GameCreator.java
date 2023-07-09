package me.viciscat.mineralcontest.commands.sub;

import com.google.common.io.Files;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.MineralContest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.noise.SimplexNoiseGenerator;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class GameCreator {

    World world;
    List<Integer> heights = new ArrayList<>();
    int highest = 0;
    private final int SMOOTH_DISTANCE = 6;
    private final float EXTRA_SMOOTH_DISTANCE = 3;
    private SimplexNoiseGenerator noiseGenerator;

    /**
     * Calculates the average, nothing crazy
     * @param integers List of integers
     * @return Double
     */
    private double getAverage(List<Integer> integers) {
        double average = 0;
        for (Integer integer: integers) {
            average += integer;
        } return average / integers.size();
    }

    /**
     * Calculates the variance
     * @param integers List of integers
     * @param average The already computed average, put -1 to compute the average with {@link #getAverage(List)}
     * @return The variance as a double
     */
    private double getVariance(List<Integer> integers, double average) {
        if (average < 0) {
            average = getAverage(integers);
        }
        double variance = 0;
        for (Integer integer : integers) {
            variance += Math.pow(integer - average, 2);
        }
        return variance / integers.size();
    }

    private int getHighestNonTreeBlockYAt(int x, int z) {
        Block highestBlock = world.getHighestBlockAt(x, z);
        Material material = highestBlock.getType();
        boolean isPlant = material.equals(Material.KELP_PLANT) || material.equals(Material.KELP) || Tag.CORAL_BLOCKS.isTagged(material);
        while (Tag.LOGS.isTagged(highestBlock.getType()) || Tag.LEAVES.isTagged(highestBlock.getType()) || highestBlock.getType().equals(Material.WATER) || isPlant) {
            highestBlock.setType(Material.AIR);
            highestBlock = world.getHighestBlockAt(x, z);
        }
        return world.getHighestBlockYAt(x, z);
    }

    private boolean findWorld(String worldName) {
        Random random = new Random();
        WorldCreator creator = new WorldCreator(worldName);
        creator.generateStructures(false);
        World world1 = null;
        List<Integer> heights1 = new ArrayList<>();
        int oceanBiomeNumber;
        int totalBiomeNumber = 0;
        int tries = 0;

        while (totalBiomeNumber < 256 && tries < 3) {
            totalBiomeNumber = 0;
            oceanBiomeNumber = 0;
            highest = 0;
            heights1.clear();

            try {
                String path = Bukkit.getServer().getWorldContainer().getCanonicalPath() + "/" + worldName;
                logger.info(path);
                logger.info("Create dirs: " + new File(path).mkdirs());

                path += "/level.dat";
                File targetFile = new File(path);

                InputStream inputStream = plugin.levelDat;
                byte[] buffer = new byte[inputStream.available()];
                logger.info("read: " + inputStream.read(buffer));
                Files.write(buffer, targetFile);
            } catch (IOException e) {
                logger.warning(e.toString());
                return false;
            }

            world1 = creator.seed(random.nextLong()).createWorld();
            logger.info("Try number: " + tries);
            assert world1 != null;
            loop: for (int i = -80; i < 80; i+=10) {
                for (int j = -80; j < 80; j += 10) {

                    int height = world1.getHighestBlockYAt(i, j);
                    oceanBiomeNumber += world1.getBiome(i, height, j).name().toLowerCase().contains("ocean") ? 1: 0;
                    if (oceanBiomeNumber > 128) {
                        tries += 1;
                        Bukkit.getServer().unloadWorld(world1, false);
                        boolean deleteResult = world1.getWorldFolder().delete();
                        logger.info("World discarded! Try number: " + tries + " ;Seed: " + world1.getSeed() + " ;Ocean/Total: " + oceanBiomeNumber + "/" + totalBiomeNumber + " " + deleteResult);
                        break loop;
                    }
                    heights1.add(height);
                    highest = Math.max(height, highest);
                    totalBiomeNumber += 1;
                }
            }
        }
        if (tries >= 3) {
            return false;
        } else {
            world = world1;
            heights = heights1;
            return true;
        }
    }

    private final MineralContest plugin = MineralContest.getInstance();
    Logger logger = plugin.getLogger();
    public GameCreator() {}

    public boolean createGame(String worldName, CommandSender sender) {
        if (!(sender instanceof Player playerSender)) return false;
        // Create and generate the world

        boolean result = findWorld(worldName);

        if (!result) {
            sender.sendPlainMessage("Couldn't find good enough spawn location... Please try again!");
            return false;
        }
        noiseGenerator = new SimplexNoiseGenerator(world);
        sender.sendPlainMessage("Found a world!");
        Bukkit.getScheduler().runTask(plugin, () -> stage2(playerSender));

        return true;
    }

    private void stage2(Player playerSender) {
        Collections.sort(heights);
        // While the terrain varies too much, remove highest or lowest point depending on how it affects the variance
        // Doing so to find the best place to place the platform thing
        while (getVariance(heights, -1) > 20 && heights.size() > 0) {
            // double prev_variance = getVariance(heights, -1);
            if (getVariance(heights.subList(0, heights.size() - 2), -1) < getVariance(heights.subList(1, heights.size() - 1), -1)) {
                heights.remove(heights.size() - 1);
            }   else { heights.remove(0); }
        }

        // The height that will be used for all platforms
        int final_height = (int) getAverage(heights);

        // Place all the blocks of the platform and place air up until the max height
        for (int i = -80; i < 80; i++) {
            for (int j = -80; j < 80; j++) {
                int x_min = -80 + SMOOTH_DISTANCE;
                int x_max = 80 - SMOOTH_DISTANCE;
                int z_min = -80 + SMOOTH_DISTANCE;
                int z_max = 80 - SMOOTH_DISTANCE;
                // logger.info(i + " " + j + " " + x_min + " " + x_max + " " + z_min + " " + z_max);
                for (int k = highest + 5; k >= final_height; k--) {
                    if (k == final_height && i > x_min && i < x_max && j > z_min && j < z_max) {
                        world.getBlockAt(i, k, j).setType(Material.GRASS_BLOCK); // TODO: Adapt this depending on biome, example: sand in desert
                    } else {
                        world.getBlockAt(i, k, j).setType(Material.AIR);
                    }
                }

            }
            // Log the progress in console
            float percentage = ((float) i + 80)/160;
            logger.info(percentage + " progress");
        }
        playerSender.sendPlainMessage("Doing final things");
        Bukkit.getScheduler().runTask(plugin, () -> stage3(playerSender, final_height));
    }

    private void stage3(Player playerSender, int final_height) {
        // Now it's getting spicy
        smooth_side(final_height, false, false);
        smooth_side(final_height, false, true);
        smooth_side(final_height, true, false);
        smooth_side(final_height, true, true);

        plugin.structureMap.get("arene_cleaner").place(new Location(world, -21, final_height-14, -21), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("arene").place(new Location(world, -21, final_height-14, -21), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_red").place(new Location(world, -68, final_height-1, -10), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_blue").place(new Location(world, 22, final_height-1, -10), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_yellow").place(new Location(world, -10, final_height-1, -68), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_green").place(new Location(world, -10, final_height-1, 22), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(80);
        world.setSpawnLocation(0, final_height, 0);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setKeepSpawnInMemory(true);
        for (Player player : playerSender.getWorld().getPlayers()) {
            player.sendMessage(playerSender.displayName().color(NamedTextColor.DARK_AQUA).append(
                    Component.text(" created a mineral contest! Join here: ")
            ).append(
                    Component.text("/mineralcontest join " + world.getName()).clickEvent(ClickEvent.suggestCommand("/mineralcontest join " + world.getName())).decoration(TextDecoration.BOLD, true).color(NamedTextColor.YELLOW)
            ));
        }
        int gameDuration = plugin.config.getInt("gameDuration", 3600);
        int firstChestDelay = plugin.config.getInt("firstChestDelay", 900);
        int chestPeriod = plugin.config.getInt("chestPeriod", 1200);
        plugin.gameHandlerMap.put(world, new GameHandler(world, gameDuration, firstChestDelay, chestPeriod, final_height));
        playerSender.sendPlainMessage("Game created with settings: " + gameDuration + " " + firstChestDelay + " " + chestPeriod);
    }


    private void smooth_side(int final_height, boolean isXAxis, boolean isPositive) {
        for (int i = -80; i < 80; i++) {
            int smoothing = (int) (((noiseGenerator.noise(i/8d) + 1)/2) * EXTRA_SMOOTH_DISTANCE);

            int multiplier = isPositive ? 1: -1; // -1 if on negative side of axis
            int side_height = getHighestNonTreeBlockYAt(
                    isXAxis ? 81*multiplier: i,
                    isXAxis ? i: 81*multiplier); // Get the height of the outside to smooth between the platform and normal terrain

            int blocks_to_do = SMOOTH_DISTANCE + smoothing;
            for (int j = 0; j < blocks_to_do + 1; j++) {

                int x = isXAxis ? (80-j) * multiplier : i;
                int z = isXAxis ? i : (80-j) * multiplier;

                // if height of platform higher than the normal terrain
                if (side_height < final_height) {

                    // Go from the lowest block to the highest
                    for (int k = side_height; k <= final_height; k++) {
                        int target_height = side_height + (int) (((float) final_height - side_height)/blocks_to_do * j);

                        if (k == final_height && Math.abs(i) < 80 - SMOOTH_DISTANCE + 1) {
                            world.getBlockAt(x , k, z).setType(Material.AIR);
                        }
                        if (k <= target_height && Math.abs(i) < 80 - SMOOTH_DISTANCE + 1 /* Ugly stuff happens when 2 corners meet up, lazy fix */) {
                            world.getBlockAt(x, k, z).setType(getMaterial(k, target_height));
                        }


                    }
                } else {

                    for (int k = final_height; k <= side_height; k++) {
                        int target_height = final_height + (int) (((float) side_height - final_height)/blocks_to_do * (blocks_to_do-j));

                        if (k == final_height) {world.getBlockAt(x , k, z).setType(Material.AIR);}
                        if (k <= target_height) {world.getBlockAt(x, k, z).setType(getMaterial(k, target_height));}
                    }
                }
            }
        }
    }

    private Material getMaterial(int height, int target_height) {
        int difference = Math.abs(target_height - height);

        if (difference == 0) {
            return Material.GRASS_BLOCK;
        } else if (difference < 3) {
            return Material.DIRT;
        } else return Material.STONE;
    }
}
