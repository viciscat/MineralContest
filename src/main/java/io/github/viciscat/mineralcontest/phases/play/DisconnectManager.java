package io.github.viciscat.mineralcontest.phases.play;

import io.github.viciscat.mineralcontest.config.MapConfig;
import io.github.viciscat.mineralcontest.config.TeamConfig;
import io.github.viciscat.mineralcontest.util.Attachments;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.datafixer.Schemas;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.NbtReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ErrorReporter;
import net.minecraft.util.path.SymlinkValidationException;
import net.minecraft.world.GameMode;
import net.minecraft.world.PlayerSaveHandler;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.*;
import xyz.nucleoid.plasmid.api.util.PlayerPos;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.io.IOException;
import java.nio.file.Path;

public class DisconnectManager {

    private final PlayerSaveHandler playerSaveHandler;
    private final GameSpace space;

    private final Object2LongMap<PlayerRef> disconnectedPlayers = new Object2LongOpenHashMap<>();

    public static void addTo(GameActivity activity) {
        new DisconnectManager(activity);
    }

    private DisconnectManager(GameActivity activity) {
        space = activity.getGameSpace();
        RegistryKey<World> registryKey = space.getAttachmentOrThrow(Attachments.MAIN_WORLD).getRegistryKey();
        Path path = ((MinecraftServerAccess) space.getServer()).getSession().getWorldDirectory(registryKey);
        LevelStorage.Session session;
        try {
            session = LevelStorage.create(path).createSession(".");
        } catch (IOException | SymlinkValidationException e) {
            throw new RuntimeException(e);
        }
        activity.addResource(session);
        playerSaveHandler = new PlayerSaveHandler(session, Schemas.getFixer());

        activity.listen(GamePlayerEvents.LEAVE, this::onLeave);
        activity.listen(GamePlayerEvents.JOIN, this::onJoin);
        activity.listen(GamePlayerEvents.ACCEPT, this::onAcceptPlayers);
        activity.listen(GamePlayerEvents.OFFER, this::onOfferPlayers);
    }

    private void onLeave(ServerPlayerEntity player) {
        if (space.getPlayers().participants().contains(player)) {
            playerSaveHandler.savePlayerData(player);
            disconnectedPlayers.put(PlayerRef.of(player), System.currentTimeMillis());
        }
    }

    private void onJoin(ServerPlayerEntity player) {
        long l = disconnectedPlayers.removeLong(PlayerRef.of(player));
        if (l != 0) {
            MinecraftServer server = space.getServer();
            PlayerManager playerManager = server.getPlayerManager();
            var userData = playerManager.getUserData();
            if (userData == null) {
                userData = server.getSaveProperties().getPlayerData();
            }

            NbtCompound playerData;
            PlayerConfigEntry playerConfigEntry = new PlayerConfigEntry(player.getGameProfile());
            if (server.isHost(playerConfigEntry) && userData != null) {
                playerData = userData;
                player.readData(NbtReadView.create(ErrorReporter.EMPTY, player.getRegistryManager(), playerData));
            } else {
                playerData = this.playerSaveHandler.loadPlayerData(playerConfigEntry).orElse(null);
            }

            var dimension = playerData != null ? playerData.get("Dimension", World.CODEC).orElse(World.OVERWORLD) : null;

            var world = server.getWorld(dimension);
            if (world == null) {
                world = server.getOverworld();
            }

            player.setServerWorld(world);

            player.changeGameMode(playerData.get("playerGameType", GameMode.INDEX_CODEC).orElse(GameMode.SURVIVAL));
            ServerPlayNetworkHandler networkHandler = player.networkHandler;
            networkHandler.requestTeleport(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
            networkHandler.syncWithPlayerPosition();
            if (System.currentTimeMillis() - l > 15_000) player.kill(world);
        }
    }

    JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
        if (acceptor.intent() == JoinIntent.SPECTATE) return JoinAcceptorResult.PASS;
        ServerWorld world = space.getAttachmentOrThrow(Attachments.MAIN_WORLD);
        TeamManager teamManager = space.getAttachmentOrThrow(Attachments.TEAM_MANAGER);
        MapConfig config = space.getAttachmentOrThrow(Attachments.CONFIG).mapConfig().value();
        Integer i = space.getAttachmentOrThrow(Attachments.FLOOR_Y);
        final PlayerPos defaultPos = new PlayerPos(world, 20, i + 1, 0, 0, 0);
        return acceptor.teleport(profile -> {
            PlayerRef player = PlayerRef.of(profile);
            GameTeamKey teamKey = teamManager.teamFor(player);
            if (teamKey == null) return defaultPos;
            TeamConfig teamConfig = config.teamConfig(teamKey).orElseThrow();
            return new PlayerPos(world, teamConfig.spawn().pos().add(0, i + 1, 0), teamConfig.spawn().yaw(), teamConfig.spawn().pitch());
        });
    }

    private JoinOfferResult onOfferPlayers(JoinOffer offer) {
        if (offer.intent() == JoinIntent.SPECTATE) return JoinOfferResult.PASS;
        return offer.playerIds().stream().map(PlayerRef::new).allMatch(disconnectedPlayers::containsKey) ?
                offer.accept() :
                offer.reject(Text.literal("Some players aren't disconnected participants!"));
    }
}
