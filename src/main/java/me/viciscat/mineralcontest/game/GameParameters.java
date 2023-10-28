package me.viciscat.mineralcontest.game;

import me.viciscat.mineralcontest.MineralContest;

public class GameParameters {

    /**
     * Game duration. (seconds)
     */
    public final int DURATION;
    /**
     * Minimum delay between chest spawns. (seconds)
     */
    public final int MIN_SPAWN_DELAY;
    /**
     * Maximum delay between chest spawns. (seconds)
     */
    public final int MAX_SPAWN_DELAY;
    /**
     * Should a chest always spawn 3 minutes before the end. (seconds)
     */
    public final boolean SPAWN_FINAL_CHEST;

    /**
     * Initializes a new instance of the GameParameters class.
     *
     * @param durationSec The duration of the game in seconds.
     * @param minChestDelay The minimum delay between spawning chests in seconds.
     * @param maxChestDelay The maximum delay between spawning chests in seconds.
     * @param finalChest Determines if a final chest will be spawned at the end of the game.
     */
    public GameParameters(int durationSec, int minChestDelay, int maxChestDelay, boolean finalChest) {
        this.DURATION = durationSec;
        this.MIN_SPAWN_DELAY = minChestDelay;
        this.MAX_SPAWN_DELAY = maxChestDelay;
        this.SPAWN_FINAL_CHEST = finalChest;
    }

    /**
     * Retrieves the game parameters from the configuration file.
     *
     * @return The game parameters read from the configuration file.
     */
    public static GameParameters fromConfig() {
        MineralContest plugin = MineralContest.getInstance();

        int gameDuration = plugin.config.getInt("gameDuration", 3600);
        int minChestDelay = plugin.config.getInt("minChestDelay", 450);
        int maxChestDelay = plugin.config.getInt("maxChestDelay", 1200);
        boolean shouldSpawnFinalChest = plugin.config.getBoolean("spawnFinalChest", true);

        return new GameParameters(
                gameDuration,
                minChestDelay,
                maxChestDelay,
                shouldSpawnFinalChest
        );
    }
}
