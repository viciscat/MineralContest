package me.viciscat.mineralcontest;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.util.CraftMagicNumbers;

import java.util.List;
import java.util.Random;

/**
Because every java project needs a util class
 **/
public class MineralUtils {
    static private final Random random = new Random();


    static public <T> T weightedRandom(Pair<Double, T>[] values) {
        double totalWeight = 0.0;
        for (Pair<Double, T> value : values) {
            totalWeight += value.left();
        }
        double randomValue = random.nextDouble(totalWeight);
        double temp = 0.0;
        T returnedValue = null;
        for (Pair<Double, T> value : values) {
            temp += value.left();
            if (temp >= randomValue) {
                returnedValue = value.right();
                break;
            }
        }
        return returnedValue;
    }
    /**
     * Calculates the average, nothing crazy
     * @param integers List of integers
     * @return Double
     */
    static public double getAverage(List<Integer> integers) {
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
    static public double getVariance(List<Integer> integers, double average) {
        if (average < 0) {
            average = getAverage(integers);
        }
        double variance = 0;
        for (Integer integer : integers) {
            variance += Math.pow(integer - average, 2);
        }
        return variance / integers.size();
    }

    public static double getFallOff(double x, double y, double highRadius, double lowGradientRadius, double cornerRadius) {
        double magnitudeCenter = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double magnitudeCorner = Math.sqrt(Math.pow(x - ((0>x)?-1:1), 2) + Math.pow(y - ((0>y)?-1:1), 2)); // Could be neat if it was more like an oval
        double out;
        if (magnitudeCenter < highRadius) {
            out = 1;
        } else if (magnitudeCenter > lowGradientRadius) {
            out = 0;
        } else {
            double i = (magnitudeCenter - highRadius)/(highRadius - lowGradientRadius);
            out = (Math.cos(i * Math.PI)+1)/2d;
        }
        double j = Math.min(magnitudeCorner/cornerRadius, 1);
        return Math.max(0, out - (Math.cos(j * Math.PI - Math.sin(j * Math.PI))+1)/2d);
    }

    public static void setBlockInNativeChunk(World world, int x, int y, int z, Material material) {
        net.minecraft.server.level.ServerLevel nmsLevel = ((CraftWorld) world).getHandle();
        net.minecraft.world.level.chunk.LevelChunk nmsChunk = nmsLevel.getChunk(x >> 4, z >> 4);
        BlockPos bp = new BlockPos(x, y, z);

        BlockState state = CraftMagicNumbers.getBlock(material).defaultBlockState();
        nmsChunk.setBlockState(bp, state, false);
    }
}
