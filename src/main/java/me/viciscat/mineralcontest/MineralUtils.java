package me.viciscat.mineralcontest;

import it.unimi.dsi.fastutil.Pair;

import java.util.Random;

/*
Because every java project needs a util class
 */
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
}
