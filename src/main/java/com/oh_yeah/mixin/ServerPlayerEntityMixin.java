package com.oh_yeah.mixin;

import com.oh_yeah.entity.TiansuluoBedWakeSpawner;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Unique
    private @Nullable BlockPos ohYeah$queuedBedWakePos;

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void ohYeah$captureBedWakeContext(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        this.ohYeah$queuedBedWakePos = TiansuluoBedWakeSpawner.shouldQueueSpawn(player)
                ? player.getSleepingPosition().orElse(null)
                : null;
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("TAIL"))
    private void ohYeah$spawnTiansuluoAfterWake(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (this.ohYeah$queuedBedWakePos == null) {
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        BlockPos bedPos = this.ohYeah$queuedBedWakePos;
        this.ohYeah$queuedBedWakePos = null;
        TiansuluoBedWakeSpawner.trySpawnPair(player, bedPos);
    }
}
