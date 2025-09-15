package io.github.viciscat.mineralcontest.datagen;

import io.github.viciscat.mineralcontest.MineralContest;
import io.github.viciscat.mineralcontest.MineralContestKeys;
import io.github.viciscat.mineralcontest.config.*;
import io.github.viciscat.mineralcontest.config.classbehavior.AttributesBehavior;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPresets;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;
import xyz.nucleoid.plasmid.api.game.config.CustomValuesConfig;
import xyz.nucleoid.plasmid.api.game.config.GameConfig;
import xyz.nucleoid.plasmid.api.registry.PlasmidRegistryKeys;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("deprecation")
public class MineralContestGameProvider extends FabricDynamicRegistryProvider {

    public static final double TEAM_SPAWN_HORIZONTAL = 57;
    public static final double TEAM_SPAWN_Y = 3.5;
    public static final double TEAM_ARENA_SPAWN_HORIZONTAL = 20;
    public static final double TEAM_ARENA_SPAWN_Y = -8.5;
    public static final double TEAM_GATE_HORIZONTAL = 52;
    public static final double TEAM_GATE_Y = 3;

    public MineralContestGameProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        entries.addAll(registries.getOrThrow(PlasmidRegistryKeys.GAME_CONFIG));
        entries.addAll(registries.getOrThrow(PlayerClass.REGISTRY_KEY));
        entries.addAll(registries.getOrThrow(Kit.REGISTRY_KEY));
        entries.addAll(registries.getOrThrow(MapConfig.REGISTRY_KEY));
    }

    @Override
    public String getName() {
        return "Mineral Contest Game Configurations";
    }

    private static TeamConfig.Gate createGate(Direction direction) {
        Vec3d center = new Vec3d(0.5, 0, 0.5);
        final int width = 3;
        final int height = 4;

        Vec3d min = new Vec3d(
                (direction.getAxis() == Direction.Axis.X ? 2 : -1.5) * direction.getDirection().offset(),
                -2,
                (direction.getAxis() == Direction.Axis.Z ? 2 : -1.5) * direction.getDirection().offset()
        );
        Vec3d max = new Vec3d(
                (direction.getAxis() == Direction.Axis.X ? -7 : 1.5) * direction.getDirection().offset(),
                4,
                (direction.getAxis() == Direction.Axis.Z ? -7 : 1.5) * direction.getDirection().offset()
        );

        return new TeamConfig.Gate(
                center.add(direction.getOffsetX() * TEAM_GATE_HORIZONTAL, TEAM_GATE_Y, direction.getOffsetZ() * TEAM_GATE_HORIZONTAL),
                width,
                height,
                direction.getAxis(),
                new Box(min, max)
        );
    }

    private static TeamConfig createTeam(Direction direction, String id, DyeColor color) {
        Vec3d center = new Vec3d(0.5, 0, 0.5);
        float degrees = direction.getOpposite().getPositiveHorizontalDegrees();
        Formatting formatting = GameTeamConfig.Colors.from(color).chatFormatting();
        return new TeamConfig(
                new GameTeamKey(id),
                Text.translatable("game.mineral_contest.team." + id).formatted(formatting),
                Text.translatable("game.mineral_contest.team." + id + ".prefix").formatted(formatting, Formatting.BOLD),
                color,
                List.of(),
                Optional.of(createGate(direction)),
                new TeamConfig.PositionOrientation(center.add(direction.getOffsetX() * TEAM_SPAWN_HORIZONTAL, TEAM_SPAWN_Y, direction.getOffsetZ() * TEAM_SPAWN_HORIZONTAL), degrees, 0),
                new TeamConfig.PositionOrientation(center.add(direction.getOffsetX() * TEAM_ARENA_SPAWN_HORIZONTAL, TEAM_ARENA_SPAWN_Y, direction.getOffsetZ() * TEAM_ARENA_SPAWN_HORIZONTAL), degrees, 0)
        );
    }

    public static void registerMaps(Registerable<MapConfig> registerable) {

        registerable.register(MineralContestKeys.DEFAULT_MAP, new MapConfig(
                Optional.empty(),
                WorldPresets.DEFAULT,
                DimensionOptions.OVERWORLD,
                true,
                List.of(
                        createTeam(Direction.WEST, "red", DyeColor.RED),
                        createTeam(Direction.EAST, "blue", DyeColor.BLUE),
                        createTeam(Direction.NORTH, "yellow", DyeColor.YELLOW),
                        createTeam(Direction.SOUTH, "green", DyeColor.LIME)
                ),
                List.of(
                        new StructureConfig(MineralContest.id("arena"), new BlockPos(-21, -14, -21)),
                        new StructureConfig(MineralContest.id("red_castle"), new BlockPos(-68, -1, -10)),
                        new StructureConfig(MineralContest.id("blue_castle"), new BlockPos(22, -1, -10)),
                        new StructureConfig(MineralContest.id("yellow_castle"), new BlockPos(-10, -1, -68)),
                        new StructureConfig(MineralContest.id("green_castle"), new BlockPos(-10, -1, 22))
                ),
                75,
                new BlockPos(0, -11, 0),
                MineralContest.SPAWN_BREAKABLE
        ));
    }

    public static void registerKits(Registerable<Kit> registerable) {
        NbtCompound compound = new NbtCompound();
        compound.putBoolean(MineralContestKeys.DROPS_ON_DEATH_KEY, false);
        ComponentChanges.Builder builder = ComponentChanges.builder().add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound));
        registerable.register(MineralContestKeys.BASIC_KIT, new Kit(
                List.of(),
                List.of(
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.IRON_HELMET), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("armor.head")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.IRON_CHESTPLATE), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("armor.chest")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.IRON_LEGGINGS), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("armor.legs")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.IRON_BOOTS), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("armor.feet")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.IRON_SWORD), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("hotbar.*")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.BOW), 1, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("hotbar.*")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.ARROW), 16),
                                Optional.ofNullable(SlotRanges.fromName("hotbar.*")),
                                1
                        ),
                        new Kit.KitItem(
                                new ItemStack(RegistryEntry.of(Items.COOKED_BEEF), 16, builder.build()),
                                Optional.ofNullable(SlotRanges.fromName("hotbar.*")),
                                1
                        )
                )
        ));
    }

    public static void registerPlayerClasses(Registerable<PlayerClass> registerable) {
        RegistryEntry.Reference<Kit> basicKit = registerable.getRegistryLookup(Kit.REGISTRY_KEY).getOrThrow(MineralContestKeys.BASIC_KIT);
        registerable.register(MineralContestKeys.AGILE_CLASS, new PlayerClass(
                MineralContest.AGILE_CLASS,
                Text.translatable("game.mineral_contest.class.agile"),
                Items.FEATHER.getRegistryEntry(),
                List.of(
                        Text.translatable("game.mineral_contest.class.agile.description.1").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.agile.description.2").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.agile.description.3").formatted(Formatting.RED)
                ),
                List.of(
                        new AttributesBehavior.Modifier(EntityAttributes.MOVEMENT_SPEED, new EntityAttributeModifier(
                                MineralContest.AGILE_CLASS,
                                0.2,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        )),
                        new AttributesBehavior.Modifier(EntityAttributes.FALL_DAMAGE_MULTIPLIER, new EntityAttributeModifier(
                                MineralContest.AGILE_CLASS,
                                -1,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ))
                ),
                basicKit
        ));
        NbtCompound compound = new NbtCompound();
        compound.putBoolean(MineralContestKeys.MOVABLE_KEY, false);
        compound.putBoolean(MineralContestKeys.DROPS_ON_DEATH_KEY, false);
        registerable.register(MineralContestKeys.MINER_CLASS, new PlayerClass(
                MineralContest.MINER_CLASS,
                Text.translatable("game.mineral_contest.class.miner"),
                Items.DIAMOND_PICKAXE.getRegistryEntry(),
                List.of(
                        Text.translatable("game.mineral_contest.class.miner.description.1").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.miner.description.2").formatted(Formatting.RED)
                ),
                List.of(),
                RegistryEntry.of(new Kit(List.of(basicKit),
                        List.of(
                                new Kit.KitItem(
                                        new ItemStack(RegistryEntry.of(Items.BARRIER), 1, ComponentChanges.builder()
                                                .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound))
                                                .add(DataComponentTypes.MAX_STACK_SIZE, 1)
                                                .add(DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplayComponent(true, ReferenceSortedSets.emptySet()))
                                                .build()),
                                        Optional.of(Kit.createCustom(IntList.of(9, 10, 11, 12, 13, 14, 15, 16, 17))),
                                        9
                                )
                        )
                        ))
        ));
        registerable.register(MineralContestKeys.ROBUST_CLASS, new PlayerClass(
                MineralContest.ROBUST_CLASS,
                Text.translatable("game.mineral_contest.class.robust"),
                Items.DIAMOND_CHESTPLATE.getRegistryEntry(),
                List.of(
                        Text.translatable("game.mineral_contest.class.robust.description.1").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.robust.description.2").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.robust.description.3").formatted(Formatting.RED)
                ),
                List.of(
                        new AttributesBehavior.Modifier(EntityAttributes.MAX_HEALTH, new EntityAttributeModifier(
                                MineralContest.ROBUST_CLASS,
                                4,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )),
                        new AttributesBehavior.Modifier(EntityAttributes.MOVEMENT_SPEED, new EntityAttributeModifier(
                                MineralContest.ROBUST_CLASS,
                                -0.1,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ))
                ),
                basicKit
        ));
        registerable.register(MineralContestKeys.WARRIOR_CLASS, new PlayerClass(
                MineralContest.WARRIOR_CLASS,
                Text.translatable("game.mineral_contest.class.warrior"),
                Items.DIAMOND_SWORD.getRegistryEntry(),
                List.of(
                        Text.translatable("game.mineral_contest.class.warrior.description.1").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.warrior.description.2").formatted(Formatting.RED)
                ),
                List.of(
                        new AttributesBehavior.Modifier(EntityAttributes.MAX_HEALTH, new EntityAttributeModifier(
                                MineralContest.WARRIOR_CLASS,
                                -4,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        )),
                        new AttributesBehavior.Modifier(EntityAttributes.ATTACK_DAMAGE, new EntityAttributeModifier(
                                MineralContest.WARRIOR_CLASS,
                                0.25,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                        ))
                ),
                basicKit
        ));
        registerable.register(MineralContestKeys.WORKER_CLASS, new PlayerClass(
                MineralContest.WORKER_CLASS,
                Text.translatable("game.mineral_contest.class.worker"),
                Items.GOLD_INGOT.getRegistryEntry(),
                List.of(
                        Text.translatable("game.mineral_contest.class.worker.description.1").formatted(Formatting.GREEN),
                        Text.translatable("game.mineral_contest.class.worker.description.2").formatted(Formatting.RED)
                ),
                List.of(
                        new AttributesBehavior.Modifier(EntityAttributes.MAX_HEALTH, new EntityAttributeModifier(
                                MineralContest.WORKER_CLASS,
                                -8,
                                EntityAttributeModifier.Operation.ADD_VALUE
                        ))
                ),
                basicKit
        ));
    }

    public static void registerGameConfigs(Registerable<GameConfig<?>> registerable) {
        RegistryEntryLookup<PlayerClass> classLookup = registerable.getRegistryLookup(PlayerClass.REGISTRY_KEY);
        RegistryEntryLookup<MapConfig> mapLookup = registerable.getRegistryLookup(MapConfig.REGISTRY_KEY);
        Object2FloatMap<RegistryEntry<Item>> map = new Object2FloatArrayMap<>();
        map.put(Items.COPPER_INGOT.getRegistryEntry(), 2);
        map.put(Items.IRON_INGOT.getRegistryEntry(), 10);
        map.put(Items.AMETHYST_SHARD.getRegistryEntry(), 30);
        map.put(Items.GOLD_INGOT.getRegistryEntry(), 50);
        map.put(Items.DIAMOND.getRegistryEntry(), 150);
        map.put(Items.EMERALD.getRegistryEntry(), 300);
        registerable.register(MineralContestKeys.DEFAULT_CONFIG, new GameConfig<>(
                MineralContest.GAME_TYPE,
                null,
                null,
                null,
                new ItemStack(Items.GOLDEN_PICKAXE),
                CustomValuesConfig.empty(),
                new MainConfig(
                        60 * 60,
                        List.of(
                                classLookup.getOrThrow(MineralContestKeys.AGILE_CLASS),
                                classLookup.getOrThrow(MineralContestKeys.MINER_CLASS),
                                classLookup.getOrThrow(MineralContestKeys.ROBUST_CLASS),
                                classLookup.getOrThrow(MineralContestKeys.WARRIOR_CLASS),
                                classLookup.getOrThrow(MineralContestKeys.WORKER_CLASS)
                        ),
                        mapLookup.getOrThrow(MineralContestKeys.DEFAULT_MAP),
                        map
                )
        ));
    }

}
