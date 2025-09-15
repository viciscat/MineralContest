package io.github.viciscat.mineralcontest.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record StructureConfig(
    Identifier structure,
    BlockPos position,
    BlockRotation rotation,
    BlockMirror mirror,
    boolean positionAbsolute
) {
    public static final Codec<StructureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("structure_template").forGetter(StructureConfig::structure),
            BlockPos.CODEC.fieldOf("position").forGetter(StructureConfig::position),
            BlockRotation.CODEC.optionalFieldOf("rotation", BlockRotation.NONE).forGetter(StructureConfig::rotation),
            BlockMirror.CODEC.optionalFieldOf("mirror", BlockMirror.NONE).forGetter(StructureConfig::mirror),
            Codec.BOOL.optionalFieldOf("position_absolute", Boolean.FALSE).forGetter(StructureConfig::positionAbsolute)
    ).apply(instance, StructureConfig::new));

    public StructureConfig(Identifier structure, BlockPos position) {
        this(structure, position, BlockRotation.NONE, BlockMirror.NONE, Boolean.FALSE);
    }
}
