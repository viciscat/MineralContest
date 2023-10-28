package me.viciscat.mineralcontest.commands.sub;

import me.viciscat.mineralcontest.MineralUtils;
import me.viciscat.mineralcontest.game.GameHandler;
import me.viciscat.mineralcontest.MineralContest;
import me.viciscat.mineralcontest.game.GameParameters;
import me.viciscat.mineralcontest.game.MineralChunkGen;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.noise.SimplexNoiseGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class GameCreator {

    World world;
    List<Integer> heights = new ArrayList<>();
    int highest = 0;
    private SimplexNoiseGenerator noiseGenerator;

    private String worldName;

    private int getHighestNonBullshitBlockYAt(int x, int z) {
        int highestY = world.getHighestBlockYAt(x, z);
        Block block = world.getBlockAt(x, highestY, z);
        while (
                Tag.LOGS.isTagged(block.getType()) || Tag.LEAVES.isTagged(block.getType()) || Tag.REPLACEABLE.isTagged(block.getType()) ||
                Tag.ICE.isTagged(block.getType()) || block.getType().equals(Material.MANGROVE_ROOTS) || block.getType().equals(Material.BAMBOO) || Tag.FLOWERS.isTagged(block.getType())) {
            highestY--;
            block = world.getBlockAt(x, highestY, z);
        }
        return highestY;
    }

    private Material getSurfaceMaterial(Biome biome) {

        String key = biome.getKey().getKey();
        if (key.contains("badlands")) {
            return Material.RED_SAND;
        } else if (key.contains("desert")) {
            return Material.SAND;
        } else if (key.contains("old_growth") && key.contains("taiga")) {
            return Material.PODZOL;
        } else if (key.contains("cave")) {
            return Material.STONE;
        } else {
            return Material.GRASS_BLOCK;
        }
    }

    private Material getSubSurfaceMaterial(Biome biome) {

        String key = biome.getKey().getKey();
        if (key.contains("badlands")) {
            return Material.RED_SANDSTONE;
        } else if (key.contains("desert")) {
            return Material.SANDSTONE;
        } else if (key.contains("cave")) {
            return Material.STONE;
        } else {
            return Material.DIRT;
        }
    }

    private Material getSubWaterMaterial(Biome biome) {

        String key = biome.getKey().getKey();
        if (key.contains("badlands")) {
            return Material.RED_SAND;
        } else if (key.contains("desert")) {
            return Material.SAND;
        } else if (key.contains("old_growth") && biome.getKey().getKey().contains("taiga")) {
            return Material.PODZOL;
        } else if (key.contains("river") || biome.getKey().getKey().contains("ocean")) {
            return Material.GRAVEL;
        } else if (key.contains("mangrove")) {
            return Material.MUD;
        } else if (key.contains("cave")) {
            return Material.STONE;
        } else {
            return Material.DIRT;
        }
    }

    private boolean findWorld() {
        String prefix = plugin.config.getString("worldNamePrefix", "mineral-contest_");
        Random random = new Random();
        WorldCreator creator = new WorldCreator(prefix + worldName);
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


            world1 = creator.seed(random.nextLong()).generator(new MineralChunkGen()).createWorld();
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

        this.worldName = worldName;
        boolean result = findWorld();

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
        while (MineralUtils.getVariance(heights, -1) > 20 && !heights.isEmpty()) {
            // double prev_variance = getVariance(heights, -1);
            if (MineralUtils.getVariance(heights.subList(0, heights.size() - 2), -1) < MineralUtils.getVariance(heights.subList(1, heights.size() - 1), -1)) {
                heights.remove(heights.size() - 1);
            }   else { heights.remove(0); }
        }

        // The height that will be used for all platforms
        int final_height = (int) MineralUtils.getAverage(heights);

        // Place all the blocks of the platform and place air up until the max height
        for (int i = -96; i < 96; i++) {
            for (int j = -96; j < 96; j++) {
                int highestY = getHighestNonBullshitBlockYAt(i, j);
                double falloff = MineralUtils.getFallOff((double) i/96, (double) j/96, 0.73, 0.98, 0.875);
                int surfaceHeight = (int) (highestY + (final_height - highestY) * falloff);
                // MineralContest.getInstance().getLogger().info("x:" + i + " z:" + j + " " + surfaceHeight + " " + highestY);

                Biome biome = world.getComputedBiome(i, surfaceHeight, j);
                Material subSurface = getSubSurfaceMaterial(biome);
                Material surface = getSurfaceMaterial(biome);
                Material surfaceWater = getSubWaterMaterial(biome);

                if (noiseGenerator.noise(i/50d, j/50d) > 0.50 && falloff < 0.998d) {
                    if (highestY > final_height) {
                        surfaceHeight--;
                    } else if (highestY < final_height) {
                        surfaceHeight++;
                    }
                }

                MineralUtils.setBlockInNativeChunk(world, i, surfaceHeight, j, surface);
                for (int k = Math.min(surfaceHeight, highestY) - 3; k <= Math.max(surfaceHeight, highestY); k++) {
                    if (k < 61) {
                        MineralUtils.setBlockInNativeChunk(world, i, k, j, surfaceWater);
                    }
                    else if (k > surfaceHeight) {
                        MineralUtils.setBlockInNativeChunk(world, i, k, j, Material.AIR);
                    } else if (k < surfaceHeight && k > surfaceHeight - 3) {
                        MineralUtils.setBlockInNativeChunk(world, i, k, j, subSurface);
                    } else if (k <= surfaceHeight - 3) {
                        MineralUtils.setBlockInNativeChunk(world, i, k, j, Material.STONE);
                    }
                }


            }
            // Log the progress in console
            float percentage = ((float) i + 80)/160;
            logger.info(percentage + " progress");
        }

        plugin.structureMap.get("arene_cleaner").place(new Location(world, -21, final_height-14, -21), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("arene").place(new Location(world, -21, final_height-14, -21), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_red").place(new Location(world, -68, final_height-1, -10), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_blue").place(new Location(world, 22, final_height-1, -10), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_yellow").place(new Location(world, -10, final_height-1, -68), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));
        plugin.structureMap.get("castle_green").place(new Location(world, -10, final_height-1, 22), false, StructureRotation.NONE, Mirror.NONE, 0, 1, new Random(0));


        playerSender.sendPlainMessage("Doing final things");
        // playerSender.teleport(new Location(world, 0, final_height+2, 0));
        Bukkit.getScheduler().runTask(plugin, () -> stage3(playerSender, final_height));
    }

    private void stage3(Player playerSender, int final_height) {


        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(80);
        world.setSpawnLocation(0, final_height, 0);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setKeepSpawnInMemory(true);
        for (Player player : playerSender.getWorld().getPlayers()) {
            player.sendMessage(playerSender.displayName().color(NamedTextColor.DARK_AQUA).append(
                    Component.text(" created a mineral contest! Join here: ")
            ).append(
                    Component.text("/mineralcontest join " + worldName).clickEvent(ClickEvent.suggestCommand("/mineralcontest join " + worldName)).decoration(TextDecoration.BOLD, true).color(NamedTextColor.YELLOW)
            ));
        }
        GameParameters gameParameters = GameParameters.fromConfig();
        plugin.gameHandlerMap.put(world, new GameHandler(world, final_height, gameParameters));
        playerSender.sendPlainMessage("Game created with settings: " + gameParameters.DURATION + " " + gameParameters.MIN_SPAWN_DELAY + " " + gameParameters.MAX_SPAWN_DELAY);
    }
}
