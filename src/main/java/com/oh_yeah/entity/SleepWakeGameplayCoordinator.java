package com.oh_yeah.entity;

import com.oh_yeah.config.OhYeahConfigManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SleepWakeGameplayCoordinator {
    private static final Map<String, SleepWakeSpeciesHandler> HANDLERS = new LinkedHashMap<>();

    static {
        register(TiansuluoPinkScarfBedWakeSpawner.INSTANCE);
    }

    private SleepWakeGameplayCoordinator() {
    }

    private static void register(SleepWakeSpeciesHandler handler) {
        HANDLERS.put(handler.speciesId(), handler);
    }

    public static boolean shouldQueueSpawn(ServerPlayerEntity player) {
        if (player == null || !OhYeahConfigManager.getSleepWakeConfig().enabled()) {
            return false;
        }
        for (SleepWakeSpeciesHandler handler : getConfiguredHandlers()) {
            if (handler.shouldQueueSpawn(player)) {
                return true;
            }
        }
        return false;
    }

    public static void trySpawnAfterWake(ServerPlayerEntity player, BlockPos bedPos) {
        if (player == null || bedPos == null || !OhYeahConfigManager.getSleepWakeConfig().enabled()) {
            return;
        }

        List<SleepWakeSpeciesHandler> candidates = new ArrayList<>();
        for (SleepWakeSpeciesHandler handler : getConfiguredHandlers()) {
            if (handler.canSpawnAt(player, bedPos)) {
                candidates.add(handler);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        SleepWakeSpeciesHandler selected = candidates.get(player.getRandom().nextInt(candidates.size()));
        selected.trySpawn(player, bedPos);
    }

    private static List<SleepWakeSpeciesHandler> getConfiguredHandlers() {
        List<SleepWakeSpeciesHandler> handlers = new ArrayList<>();
        for (String speciesId : OhYeahConfigManager.getSleepWakeConfig().speciesIds()) {
            SleepWakeSpeciesHandler handler = HANDLERS.get(speciesId);
            if (handler != null) {
                handlers.add(handler);
            }
        }
        return handlers;
    }
}
