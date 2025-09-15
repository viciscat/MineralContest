package io.github.viciscat.mineralcontest.phases.play.gate;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.util.InvisibleBox;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalConnectingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ProjectileDeflection;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class GateEntity extends Entity implements PolymerEntity {

    public static final float SPEED = 0.1f;

    public static final RegistryKey<EntityType<?>> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, MineralContest.id("gate"));
    public static final EntityType<GateEntity> TYPE = EntityType.Builder.create(GateEntity::new, SpawnGroup.MISC).maxTrackingRange(0).build(REGISTRY_KEY);

    private final List<DisplayEntity.BlockDisplayEntity> blocks = new ArrayList<>();
    public int width, height;
    public float openedHeight = -1;
    public Direction.Axis axis;

    private float currentHeight = 0, previousHeight = 0;

    public @NotNull Predicate<ServerPlayerEntity> opensGate = player -> true;
    public Box openTrigger;
    private BlockState state = Blocks.AIR.getDefaultState();

    private final InvisibleBox hitbox;

    public GateEntity(EntityType<?> type, World world) {
        super(type, world);
        hitbox = new InvisibleBox((ServerWorld) world, getBoundingBox());
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityType.MARKER;
    }

    private void initialize() {
        if (width == 0 || height == 0 || axis == null || axis == Direction.Axis.Y) {
            throw new IllegalStateException("GateEntity has invalid size or direction.");
        }
        if (openedHeight < 0) openedHeight = height - 0.5f;
        if (openTrigger == null) openTrigger = new Box(
                new Vec3d(axis == Direction.Axis.X ? -3 : -width/2d, 0, axis == Direction.Axis.Z ? -3 : -width/2d),
                new Vec3d(axis == Direction.Axis.X ? 3 : width/2d, height, axis == Direction.Axis.Z ? 3 : width/2d)
        );
        for (int i = 0; i < width * height; i++) {
            DisplayEntity.BlockDisplayEntity blockDisplay = new TemporaryBlockDisplay(EntityType.BLOCK_DISPLAY, getWorld());
            blocks.add(blockDisplay);
            this.state = Blocks.OAK_FENCE.getDefaultState()
                    .with(HorizontalConnectingBlock.NORTH, !axis.test(Direction.NORTH))
                    .with(HorizontalConnectingBlock.EAST, !axis.test(Direction.EAST))
                    .with(HorizontalConnectingBlock.WEST, !axis.test(Direction.WEST))
                    .with(HorizontalConnectingBlock.SOUTH, !axis.test(Direction.SOUTH));
            blockDisplay.setBlockState(state);
            blockDisplay.setInterpolationDuration(1);
            blockDisplay.setTeleportDuration(1);
            blockDisplay.setDisplayWidth(1);
            blockDisplay.setDisplayHeight(1);
            blockDisplay.setTransformation(new AffineTransformation(new Vector3f(-0.5f, 0, -0.5f),null, null, null));
            getWorld().spawnEntity(blockDisplay);
        }
        positionEntities();
    }

    private boolean isLookingAt(ServerPlayerEntity player) {
        Vec3d vec3d = player.getRotationVec(1.0F).normalize();

        for (double y = getY(); y < getY() + height; y += 0.5) {
            Vec3d lookVector = new Vec3d(this.getX() - player.getX(), y - player.getEyeY(), this.getZ() - player.getZ());
            lookVector = lookVector.normalize();
            double dot = vec3d.dotProduct(lookVector);
            if (dot > 0.5
                    && player.canSee(this, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, y)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void tick() {
        if (firstUpdate) initialize();
        List<ServerPlayerEntity> entities = getWorld().getEntitiesByType(TypeFilter.instanceOf(ServerPlayerEntity.class), openTrigger.offset(getPos()), opensGate.and(this::isLookingAt).and(player -> !player.isSpectator()));
        if (entities.isEmpty()) {
            currentHeight -= SPEED * 1.5f;
        } else {
            currentHeight += SPEED;
        }
        currentHeight = Math.clamp(currentHeight, 0, openedHeight);
        if (currentHeight != previousHeight) {
            previousHeight = currentHeight;
            positionEntities();
        }
        hitbox.tick();
        super.tick();
    }

    private void positionEntities() {
        for (int i = 0; i < blocks.size(); i++) {
            int x = i % width;
            int y = i / width;
            float v = width / 2f - 0.5f;
            DisplayEntity.BlockDisplayEntity display = blocks.get(i);
            double displayY = y + currentHeight;
            display.setPosition(
                    getX() + (axis == Direction.Axis.X ? 0 : (x - v)),
                    getY() + displayY,
                    getZ() + (axis == Direction.Axis.Z ? 0 : (x - v))
            );
            display.setBlockState(displayY >= height ? Blocks.AIR.getDefaultState() : state);
        }
        Box boundingBox = calculateDefaultBoundingBox(getPos());
        setBoundingBox(boundingBox);
        hitbox.box = boundingBox;
    }

    @Override
    protected Box calculateDefaultBoundingBox(Vec3d pos) {
        return new Box(
                new Vec3d(axis == Direction.Axis.X ? -0.2 : -width/2d, currentHeight, axis == Direction.Axis.Z ? -0.2 : -width/2d),
                new Vec3d(axis == Direction.Axis.X ? 0.2 : width/2d, height, axis == Direction.Axis.Z ? 0.2 : width/2d)
        ).offset(pos);
    }

    @Override
    public void onRemove(RemovalReason reason) {
        blocks.forEach(block -> block.remove(reason));
        hitbox.close();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public ProjectileDeflection getProjectileDeflection(ProjectileEntity projectile) {
        return ProjectileDeflection.SIMPLE;
    }

    @Override
    public boolean canHit() {
        return true;
    }

    @Override
    protected void readCustomData(ReadView view) {
        width = view.getInt("width", 3);
        height = view.getInt("height", 5);
        axis = view.read("axis", Direction.Axis.CODEC).orElse(Direction.Axis.X);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putInt("width", width);
        view.putInt("height", height);
        view.put("axis", Direction.Axis.CODEC, axis);
    }

    @Override
    public boolean isCollidable(@Nullable Entity entity) {
        return true;
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
