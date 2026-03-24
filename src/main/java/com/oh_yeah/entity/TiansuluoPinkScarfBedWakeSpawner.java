package com.oh_yeah.entity;

import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.sound.TiansuluoVoiceType;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TiansuluoPinkScarfBedWakeSpawner implements SleepWakeSpeciesHandler {
    public static final TiansuluoPinkScarfBedWakeSpawner INSTANCE = new TiansuluoPinkScarfBedWakeSpawner();

    private TiansuluoPinkScarfBedWakeSpawner() {
    }

    @Override
    public String speciesId() {
        return "tiansuluo_pink_scarf";
    }

    @Override
    public boolean shouldQueueSpawn(ServerPlayerEntity player) {
        if (player == null || !(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoPinkScarfConfig();
        if (!config.enableBedWakeSpawn()) {
            return false;
        }
        if (!player.canResetTimeBySleeping()) {
            return false;
        }
        Optional<BlockPos> sleepingPos = player.getSleepingPosition();
        if (sleepingPos.isEmpty()) {
            return false;
        }
        return isBed(world, sleepingPos.get());
    }

    @Override
    public boolean canSpawnAt(ServerPlayerEntity player, BlockPos bedPos) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoPinkScarfConfig();
        return config.enableBedWakeSpawn() && isBed(world, bedPos);
    }

    @Override
    public void trySpawn(ServerPlayerEntity player, BlockPos bedPos) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoPinkScarfConfig();
        if (!config.enableBedWakeSpawn() || !isBed(world, bedPos)) {
            return;
        }

        List<BlockPos> spawnPositions = findSpawnPositions(world, bedPos, config.bedWakeSpawnRadius(), 2);
        if (spawnPositions.size() < 2) {
            return;
        }

        TiansuluoPinkScarfEntity adult = createEntity(world, spawnPositions.get(0), false);
        TiansuluoPinkScarfEntity baby = createEntity(world, spawnPositions.get(1), true);
        if (adult == null || baby == null) {
            return;
        }

        if (!world.spawnEntity(adult)) {
            return;
        }
        if (!world.spawnEntity(baby)) {
            adult.discard();
        }
    }

    private static @org.jetbrains.annotations.Nullable TiansuluoPinkScarfEntity createEntity(ServerWorld world, BlockPos pos, boolean baby) {
        TiansuluoPinkScarfEntity entity = ModEntityTypes.TIANSULUO_PINK_SCARF.create(world);
        if (entity == null) {
            return null;
        }

        Vec3d spawnPos = Vec3d.ofBottomCenter(pos);
        entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.getRandom().nextFloat() * 360.0F, 0.0F);
        entity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
        entity.markOneShotVoiceAsPlayed(TiansuluoVoiceType.SPAWN);
        if (baby) {
            entity.setBaby(true);
            entity.setBreedingAge(OhYeahConfigManager.getTiansuluoPinkScarfConfig().tuning().babyGrowthAgeTicks());
        } else {
            entity.setBreedingAge(0);
        }
        return entity;
    }

    private static List<BlockPos> findSpawnPositions(ServerWorld world, BlockPos bedPos, int radius, int needed) {
        List<BlockPos> positions = new ArrayList<>();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int distance = 1; distance <= radius; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) {
                        continue;
                    }
                    for (int dy = 0; dy >= -2; dy--) {
                        mutable.set(bedPos.getX() + dx, bedPos.getY() + dy, bedPos.getZ() + dz);
                        if (!isSafeSpawnPos(world, mutable, bedPos)) {
                            continue;
                        }
                        BlockPos candidate = mutable.toImmutable();
                        if (!positions.contains(candidate)) {
                            positions.add(candidate);
                            if (positions.size() >= needed) {
                                return positions;
                            }
                        }
                    }
                }
            }
        }
        return positions;
    }

    private static boolean isSafeSpawnPos(ServerWorld world, BlockPos pos, BlockPos bedPos) {
        if (pos.equals(bedPos)) {
            return false;
        }
        BlockState floor = world.getBlockState(pos.down());
        if (floor.isAir() || floor.getCollisionShape(world, pos.down()).isEmpty()) {
            return false;
        }
        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
            return false;
        }
        if (!world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()) {
            return false;
        }
        if (!world.getFluidState(pos).isEmpty() || !world.getFluidState(pos.up()).isEmpty()) {
            return false;
        }
        if (isBed(world, pos) || isBed(world, pos.up())) {
            return false;
        }

        Vec3d spawnPos = Vec3d.ofBottomCenter(pos);
        Box box = ModEntityTypes.TIANSULUO_PINK_SCARF.getDimensions().getBoxAt(spawnPos);
        return world.isSpaceEmpty(box);
    }

    private static boolean isBed(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BedBlock) {
            return true;
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos adjacent = pos.offset(direction);
            if (world.getBlockState(adjacent).getBlock() instanceof BedBlock) {
                return true;
            }
        }
        return false;
    }
}
