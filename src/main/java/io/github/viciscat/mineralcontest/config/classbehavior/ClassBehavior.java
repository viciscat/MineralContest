package io.github.viciscat.mineralcontest.config.classbehavior;

import com.mojang.serialization.Codec;
import io.github.viciscat.mineralcontest.MineralContest;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.plasmid.api.util.TinyRegistry;

import java.util.function.Predicate;

/**
 * Unused. Might use it one day
 */
public interface ClassBehavior {
    TinyRegistry<ClassBehavior.Type<?>> TYPE_REGISTRY = TinyRegistry.create();

    void registerEvents(@NotNull GameActivity activity, Predicate<PlayerRef> hasCorrectClass);

    @NotNull Type<?> getType();

    record Type<T extends ClassBehavior>(Codec<T> codec) {

        public static final Type<AttributesBehavior> ATTRIBUTES = register(MineralContest.id("attributes"), AttributesBehavior.CODEC);

        private static <T extends ClassBehavior> Type<T> register(Identifier identifier, Codec<T> codec) {
            Type<T> type = new Type<>(codec);
            TYPE_REGISTRY.register(identifier, type);
            return type;
        }
    }
}
