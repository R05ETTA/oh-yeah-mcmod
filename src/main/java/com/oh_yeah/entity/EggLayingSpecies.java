package com.oh_yeah.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface EggLayingSpecies {
    boolean hasCarriedEggBlock();

    void setHasCarriedEggBlock(boolean hasCarriedEggBlock);

    @Nullable BlockPos getCarriedEggBlockTargetPos();

    void setCarriedEggBlockTargetPos(@Nullable BlockPos pos);

    int getEggBlockPlacingCounter();

    void setEggBlockPlacingCounter(int counter);

    @Nullable UUID getEggBlockAttractedPlayerUuid();

    void setEggBlockAttractedPlayerUuid(@Nullable UUID uuid);

    default void setEggBlockAttractedPlayer(@Nullable PlayerEntity player) {
        this.setEggBlockAttractedPlayerUuid(player == null ? null : player.getUuid());
    }
}
