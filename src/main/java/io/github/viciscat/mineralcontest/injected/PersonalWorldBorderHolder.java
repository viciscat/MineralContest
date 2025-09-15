package io.github.viciscat.mineralcontest.injected;

import net.minecraft.world.border.WorldBorder;
import org.jetbrains.annotations.NotNull;

public interface PersonalWorldBorderHolder {

    void mineral_contest$setWorldBorder(@NotNull WorldBorder worldBorder);
    WorldBorder mineral_contest$getWorldBorder();
    void mineral_contest$removeWorldBorder();
    boolean mineral_contest$hasWorldBorder();

    default void setWorldBorder(@NotNull WorldBorder worldBorder) {
        mineral_contest$setWorldBorder(worldBorder);
    }
    default WorldBorder getWorldBorder() {
        return mineral_contest$getWorldBorder();
    }
    default void removeWorldBorder() {
        mineral_contest$removeWorldBorder();
    }
    default boolean hasWorldBorder() {
        return mineral_contest$hasWorldBorder();
    }
}
