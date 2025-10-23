package io.github.viciscat.mineralcontest.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.codecs.MoreCodecs;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeamKey;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record TeamConfig(GameTeamKey id,
                         Text name,
                         Text prefix,
                         DyeColor color,
                         List<StructureConfig> structureConfigs,
                         Optional<Gate> gate,
                         PositionOrientation spawn,
                         PositionOrientation arenaSpawn
) {
    public static final Codec<TeamConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GameTeamKey.CODEC.fieldOf("id").forGetter(TeamConfig::id),
            TextCodecs.CODEC.fieldOf("name").forGetter(TeamConfig::name),
            TextCodecs.CODEC.optionalFieldOf("prefix", Text.empty()).forGetter(TeamConfig::prefix),
            DyeColor.CODEC.fieldOf("color").forGetter(TeamConfig::color),
            StructureConfig.CODEC.listOf().optionalFieldOf("structures", List.of()).forGetter(TeamConfig::structureConfigs),
            Gate.CODEC.optionalFieldOf("gate").forGetter(TeamConfig::gate),
            PositionOrientation.LENIENT_CODEC.fieldOf("spawn").forGetter(TeamConfig::spawn),
            PositionOrientation.LENIENT_CODEC.fieldOf("arena_spawn").forGetter(TeamConfig::arenaSpawn)
    ).apply(instance, TeamConfig::new));

    public record PositionOrientation(Vec3d pos, float yaw, float pitch) {
        public static final Codec<PositionOrientation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3d.CODEC.fieldOf("position").forGetter(PositionOrientation::pos),
                Codec.FLOAT.fieldOf("yaw").forGetter(PositionOrientation::yaw),
                Codec.FLOAT.fieldOf("pitch").forGetter(PositionOrientation::pitch)
        ).apply(instance, PositionOrientation::new));

        public static final Codec<PositionOrientation> LENIENT_CODEC = Codec.withAlternative(CODEC, Vec3d.CODEC, PositionOrientation::new);

        public PositionOrientation(Vec3d pos) {
            this(pos, 0f, 0f);
        }

        public void teleport(ServerPlayerEntity player, Vec3d offset) {
            Vec3d pos = pos().add(offset);
            player.teleport(player.getEntityWorld(), pos.x, pos.y, pos.z, Set.of(), yaw(), pitch(), true);
        }
    }

    public GameTeam toNewGameTeam() {
        return new GameTeam(
                id,
                new GameTeamConfig(
                        name,
                        GameTeamConfig.Colors.from(color),
                        false,
                        AbstractTeam.CollisionRule.PUSH_OTHER_TEAMS,
                        AbstractTeam.VisibilityRule.ALWAYS,
                        prefix,
                        Text.empty()
                )
        );
    }

    public record Gate(Vec3d pos,
                       int width,
                       int height,
                       Direction.Axis axis,
                       Box openTrigger) {
        public static final Codec<Gate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3d.CODEC.fieldOf("position").forGetter(Gate::pos),
                Codec.INT.fieldOf("width").forGetter(Gate::width),
                Codec.INT.fieldOf("height").forGetter(Gate::height),
                Direction.Axis.CODEC
                        .validate(axis1 -> axis1 == Direction.Axis.Y ? DataResult.error(() -> "Y axis is not allowed"): DataResult.success(axis1))
                        .fieldOf("axis").forGetter(Gate::axis),
                MoreCodecs.BOX.fieldOf("open_trigger").forGetter(Gate::openTrigger)
        ).apply(instance, Gate::new));
    }
}
