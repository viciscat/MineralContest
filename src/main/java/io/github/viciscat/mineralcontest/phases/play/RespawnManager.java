package io.github.viciscat.mineralcontest.phases.play;

import io.github.viciscat.mineralcontest.event.PlayerRespawnEvent;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.Stimuli;
import xyz.nucleoid.stimuli.event.DroppedItemsResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.entity.EntityDropItemsEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.ArrayList;
import java.util.List;

public class RespawnManager {
    /**
     * In ticks.
     */
    private static final int DEFAULT_RESPAWN_TIME = 20 * 15;
    private static final Text DEAD_TEXT = Text.translatable("game.mineral_contest.dead").formatted(Formatting.RED, Formatting.BOLD);

    private final Object2IntMap<PlayerRef> respawnTimes = new Object2IntOpenHashMap<>();
    private final int respawnTime;
    private final GameSpace space;

    private RespawnManager(int respawnTime, GameSpace space) {
        this.respawnTime = respawnTime;
        this.space = space;
    }

    public static void addTo(@NotNull GameActivity activity, int respawnTime) {
        RespawnManager manager = new RespawnManager(respawnTime, activity.getGameSpace());
        activity.listen(GameActivityEvents.TICK, manager::tick);
        activity.listen(PlayerDeathEvent.EVENT, manager::onDeath);
        activity.listen(GamePlayerEvents.ADD, manager::playerAdded);
    }

    public static void addTo(@NotNull GameActivity activity) {
        addTo(activity, DEFAULT_RESPAWN_TIME);
    }

    private EventResult onDeath(ServerPlayerEntity player, DamageSource source) {
        respawnTimes.put(PlayerRef.of(player), respawnTime);
        player.changeGameMode(GameMode.SPECTATOR);
        List<ItemStack> drops = new ArrayList<>(player.getInventory().size());
        player.getInventory().forEach(drops::add);
        player.getInventory().clear();
        try (var invokers = Stimuli.select().forEntity(player)) {

            DroppedItemsResult result = invokers.get(EntityDropItemsEvent.EVENT)
                    .onDropItems(player, drops);

            for (ItemStack stack : result.dropStacks()) {
                player.dropItem(stack, true, false);
            }
        }
        return EventResult.DENY;
    }

    private void tick() {
        for (Object2IntMap.Entry<PlayerRef> entry : respawnTimes.object2IntEntrySet()) {
            if (entry.getIntValue() <= 0) continue;
            ServerPlayerEntity player = entry.getKey().getEntity(space);
            if (player == null) continue;
            int i = entry.getIntValue() - 1;
            entry.setValue(i);
            if (i > 0) {
                showTitle(player, respawnTime - i < 30 ? DEAD_TEXT : Text.empty(), Text.translatable("game.mineral_contest.respawn", i / 20 + 1), 0, 2, 2);
                continue;
            }
            space.getBehavior().propagatingInvoker(PlayerRespawnEvent.EVENT).onRespawn(player);
        }
    }

    private void playerAdded(ServerPlayerEntity player) {
        PlayerRef ref = PlayerRef.of(player);
        int i = respawnTimes.getInt(ref);
        if (i > 0) {
            player.changeGameMode(GameMode.SPECTATOR);
            if (i < 20) respawnTimes.put(ref, 20);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void showTitle(ServerPlayerEntity player, Text title, Text subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        sendPacket(player, new TitleFadeS2CPacket(fadeInTicks, stayTicks, fadeOutTicks));
        sendPacket(player, new TitleS2CPacket(title));
        sendPacket(player, new SubtitleS2CPacket(subtitle));
    }

    private static void sendPacket(ServerPlayerEntity player, Packet<?> packet) {
        player.networkHandler.sendPacket(packet);
    }
}
