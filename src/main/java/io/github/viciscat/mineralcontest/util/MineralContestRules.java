package io.github.viciscat.mineralcontest.util;

import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;

public class MineralContestRules {

    public static final GameRuleType ALL_DAMAGE = GameRuleType.create().enforces(PlayerDamageEvent.EVENT, result -> (player, source, amount) -> result);
}
