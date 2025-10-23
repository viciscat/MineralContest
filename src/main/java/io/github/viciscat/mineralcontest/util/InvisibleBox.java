package io.github.viciscat.mineralcontest.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.io.Closeable;
import java.util.*;

public class InvisibleBox implements Closeable {
    private static final float SHULKER_SIZE = 2;
    private static final Collection<EntityAttributeInstance> COLLECTION = Collections.singleton(Util.make(
            new EntityAttributeInstance(EntityAttributes.SCALE, ignore -> {}),
            instance -> instance.setBaseValue(SHULKER_SIZE)
    ));

    public Box box;
    private final ServerWorld world;
    private final ShulkerEntity hitbox;
    private final InteractionEntity marker;

    private final Set<PlayerRef> players = new ObjectOpenHashSet<>();

    public InvisibleBox(ServerWorld world, Box box) {
        this.box = box;
        this.world = world;
        this.hitbox = EntityType.SHULKER.create(world, SpawnReason.COMMAND);
        this.marker = EntityType.INTERACTION.create(world, SpawnReason.COMMAND);
        assert hitbox != null;
        assert marker != null;
        hitbox.setInvisible(true);
        hitbox.setNoGravity(true);
        hitbox.setAiDisabled(true);
        hitbox.setSilent(true);
        hitbox.getAttributes().getCustomInstance(EntityAttributes.SCALE).setBaseValue(SHULKER_SIZE);
        marker.setInteractionWidth(0);
        marker.setInteractionHeight(0);
    }

    public void tick() {
        List<ServerPlayerEntity> worldPlayers = world.getPlayers();
        players.retainAll(worldPlayers.stream().map(PlayerRef::of).toList());
        for (ServerPlayerEntity worldPlayer : worldPlayers) {
            if (!players.contains(PlayerRef.of(worldPlayer))) {
                if (box.squaredMagnitude(worldPlayer.getEntityPos()) < 5 * 5) {
                    sendHitbox(worldPlayer);
                } else continue;
            } else {
                if (box.squaredMagnitude(worldPlayer.getEntityPos()) > 5 * 5) {
                    removeHitbox(worldPlayer);
                    continue;
                }
            }
            Box playerBox = box.contract(0);
            Vec3d center = playerBox.getCenter();
            if (playerBox.getLengthX() < SHULKER_SIZE) {
                if (worldPlayer.getX() > center.x) {
                    playerBox = playerBox.withMinX(playerBox.getMax(Direction.Axis.X) - SHULKER_SIZE);
                } else {
                    playerBox = playerBox.withMaxX(playerBox.getMin(Direction.Axis.X) + SHULKER_SIZE);
                }
            }
            if (playerBox.getLengthY() < SHULKER_SIZE) {
                if (worldPlayer.getY() > center.y) {
                    playerBox = playerBox.withMinY(playerBox.getMax(Direction.Axis.Y) - SHULKER_SIZE);
                } else {
                    playerBox = playerBox.withMaxY(playerBox.getMin(Direction.Axis.Y) + SHULKER_SIZE);
                }
            }
            if (playerBox.getLengthZ() < SHULKER_SIZE) {
                if (worldPlayer.getZ() > center.z) {
                    playerBox = playerBox.withMinZ(playerBox.getMax(Direction.Axis.Z) - SHULKER_SIZE);
                } else {
                    playerBox = playerBox.withMaxZ(playerBox.getMin(Direction.Axis.Z) + SHULKER_SIZE);
                }
            }
            playerBox = playerBox.contract(SHULKER_SIZE / 2);
            Vec3d vec3d = getClosestPointToPlayer(worldPlayer, playerBox);
            // TODO do not send that if the coordinates are the same
            worldPlayer.networkHandler.sendPacket(new EntityPositionSyncS2CPacket(marker.getId(), new EntityPosition(vec3d, Vec3d.ZERO, 0, 0), false));

        }
    }

    @Override
    public void close() {
        players.stream().map(playerRef -> playerRef.getEntity(world)).filter(Objects::nonNull).forEach(this::removeHitbox);
        players.clear();
    }

    // calculate closest point to player that is inside the box
    private Vec3d getClosestPointToPlayer(ServerPlayerEntity player, Box box) {
        Vec3d playerPos = player.getEntityPos().add(0, SHULKER_SIZE / 2, 0).add(player.getVelocity().multiply(0.5));
        double x = Math.max(box.minX, Math.min(playerPos.x, box.maxX));
        double y = Math.max(box.minY, Math.min(playerPos.y, box.maxY));
        double z = Math.max(box.minZ, Math.min(playerPos.z, box.maxZ));
        return new Vec3d(x, y - SHULKER_SIZE / 2, z);
    }

    private void sendHitbox(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new EntitySpawnS2CPacket(hitbox, 0, BlockPos.ofFloored(box.getCenter())));
        player.networkHandler.sendPacket(new EntitySpawnS2CPacket(marker, 0, BlockPos.ofFloored(box.getCenter())));
        hitbox.startRiding(marker, true, true);
        player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(hitbox.getId(), hitbox.getDataTracker().getChangedEntries()));
        player.networkHandler.sendPacket(new EntityTrackerUpdateS2CPacket(marker.getId(), marker.getDataTracker().getChangedEntries()));
        player.networkHandler.sendPacket(new EntityAttributesS2CPacket(hitbox.getId(), COLLECTION));
        player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(marker));
        players.add(PlayerRef.of(player));
    }

    private void removeHitbox(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new EntitiesDestroyS2CPacket(hitbox.getId(), marker.getId()));
        players.remove(PlayerRef.of(player));
    }
}
