package com.oh_yeah.entity;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface SleepWakeSpeciesHandler {
    String speciesId();

    boolean shouldQueueSpawn(ServerPlayerEntity player);

    boolean canSpawnAt(ServerPlayerEntity player, BlockPos bedPos);

    void trySpawn(ServerPlayerEntity player, BlockPos bedPos);
}
