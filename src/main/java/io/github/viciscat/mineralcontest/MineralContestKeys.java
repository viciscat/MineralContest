package io.github.viciscat.mineralcontest;

import io.github.viciscat.mineralcontest.config.Kit;
import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.PlayerClass;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;
import xyz.nucleoid.plasmid.api.registry.PlasmidRegistryKeys;

import static io.github.viciscat.mineralcontest.MineralContest.id;

public final class MineralContestKeys {

    public static final RegistryKey<GameConfig<?>> DEFAULT_CONFIG = RegistryKey.of(PlasmidRegistryKeys.GAME_CONFIG, id("mineral_contest_default"));

    public static final RegistryKey<PlayerClass> WARRIOR_CLASS = RegistryKey.of(PlayerClass.REGISTRY_KEY, MineralContest.WARRIOR_CLASS);
    public static final RegistryKey<PlayerClass> MINER_CLASS = RegistryKey.of(PlayerClass.REGISTRY_KEY, MineralContest.MINER_CLASS);
    public static final RegistryKey<PlayerClass> AGILE_CLASS = RegistryKey.of(PlayerClass.REGISTRY_KEY, MineralContest.AGILE_CLASS);
    public static final RegistryKey<PlayerClass> WORKER_CLASS = RegistryKey.of(PlayerClass.REGISTRY_KEY, MineralContest.WORKER_CLASS);
    public static final RegistryKey<PlayerClass> ROBUST_CLASS = RegistryKey.of(PlayerClass.REGISTRY_KEY, MineralContest.ROBUST_CLASS);

    public static final RegistryKey<Kit> BASIC_KIT = RegistryKey.of(Kit.REGISTRY_KEY, id("basic"));

    public static final RegistryKey<MapConfig> DEFAULT_MAP = RegistryKey.of(MapConfig.REGISTRY_KEY, id("default_map"));

    public static final String MOVABLE_KEY = id("movable").toString();
    public static final String DROPS_ON_DEATH_KEY = id("drops_on_death").toString();
    public static final RegistryKey<LootTable> CHEST_LOOT_TABLE = RegistryKey.of(RegistryKeys.LOOT_TABLE, id("arena_chest"));

    private static final Identifier SCORE_STATIC_IDENTIFIER = MineralContest.id("score");
    public static final StatisticKey<Float> SCORE_STATISTIC_KEY = StatisticKey.floatKey(SCORE_STATIC_IDENTIFIER);
    public static StatisticKey<Float> scoreStatisticKeyForTeam(GameTeamKey team) {
        return StatisticKey.floatKey(SCORE_STATIC_IDENTIFIER.withSuffixedPath("/" + team.id()));
    }
}
