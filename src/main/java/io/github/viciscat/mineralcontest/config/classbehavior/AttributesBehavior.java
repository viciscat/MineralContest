package io.github.viciscat.mineralcontest.config.classbehavior;

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.viciscat.mineralcontest.event.PlayerRespawnEvent;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

import java.util.List;
import java.util.function.Predicate;


public record AttributesBehavior(List<@NotNull Modifier> attributes) implements ClassBehavior {
    public static final Codec<AttributesBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Modifier.CODEC.listOf().fieldOf("attribute_modifiers").forGetter(AttributesBehavior::attributes)
    ).apply(instance, AttributesBehavior::new));

    public Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> getModifiersMap() {
        return attributes.stream().collect(
                ImmutableSetMultimap.toImmutableSetMultimap(AttributesBehavior.Modifier::attribute, AttributesBehavior.Modifier::modifier));
    }

    @Override
    public void registerEvents(@NotNull GameActivity activity, Predicate<PlayerRef> hasCorrectClass) {
        activity.listen(PlayerRespawnEvent.EVENT, player -> {
            if (!hasCorrectClass.test(PlayerRef.of(player))) return;
            player.getAttributes().addTemporaryModifiers(getModifiersMap());
        });
    }

    @Override
    public @NotNull Type<?> getType() {
        return Type.ATTRIBUTES;
    }

    public record Modifier(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) {
        public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityAttribute.CODEC.fieldOf("attribute").forGetter(Modifier::attribute),
                EntityAttributeModifier.CODEC.fieldOf("modifier").forGetter(Modifier::modifier)
        ).apply(instance, Modifier::new));
    }
}
