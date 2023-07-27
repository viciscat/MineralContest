package me.viciscat.mineralcontest;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.World;

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

    public static double getFallOff(double x, double y, double highRadius, double lowGradientRadius) {
        double magnitude = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        if (magnitude < highRadius) {
            return 1;
        } else if (magnitude > lowGradientRadius) {
            return 0;
        } else {
            double i = (magnitude - highRadius)/(highRadius - lowGradientRadius);
            return (Math.cos(i * Math.PI)+1)/2d;
        }
    }
}
