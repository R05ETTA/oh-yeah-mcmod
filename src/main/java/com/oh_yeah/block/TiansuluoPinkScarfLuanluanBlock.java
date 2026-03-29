package com.oh_yeah.block;

import com.mojang.serialization.MapCodec;
import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.entity.TiansuluoPinkScarfEntity;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class TiansuluoPinkScarfLuanluanBlock extends Block {
    public static final MapCodec<TiansuluoPinkScarfLuanluanBlock> CODEC = createCodec(TiansuluoPinkScarfLuanluanBlock::new);
    public static final IntProperty HATCH = IntProperty.of("hatch", 0, 2);
    public static final IntProperty BLOCKS = IntProperty.of("blocks", 1, 4);
    private static final int LUANLUAN_BLOCK_HINT_COLOR_STAGE_0 = 0xFFB6DE;
    private static final int LUANLUAN_BLOCK_HINT_COLOR_STAGE_1 = 0xFF92D1;
    private static final int LUANLUAN_BLOCK_HINT_COLOR_STAGE_2 = 0xFF6FC5;
    private static final int HATCH_STAGE_TICKS = 20 * 10;
    private static final VoxelShape SMALL_SHAPE = createCuboidShape(3.0, 0.0, 3.0, 12.0, 7.0, 12.0);
    private static final VoxelShape LARGE_SHAPE = createCuboidShape(1.0, 0.0, 1.0, 15.0, 7.0, 15.0);

    @Override
    public MapCodec<TiansuluoPinkScarfLuanluanBlock> getCodec() {
        return CODEC;
    }

    public TiansuluoPinkScarfLuanluanBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(HATCH, 0).with(BLOCKS, 1));
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.bypassesSteppingEffects()) {
            int chance = Math.max(1, tuning().luanluanBlockBreakChanceStepOn());
            if (entity instanceof PlayerEntity) {
                // Keep "step" behavior probabilistic, but make it much more noticeable for players.
                chance = Math.min(chance, 12);
            }
            this.tryBreakLuanluanBlock(world, state, pos, entity, chance);
        }
        super.onSteppedOn(world, pos, state, entity);
    }

    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof ZombieEntity) && !entity.bypassesSteppingEffects()) {
            int chance = Math.max(1, tuning().luanluanBlockBreakChanceLanded());
            if (entity instanceof PlayerEntity) {
                // Player stomp should feel deterministic.
                chance = 1;
            }
            this.tryBreakLuanluanBlock(world, state, pos, entity, chance);
        }
        super.onLandedUpon(world, state, pos, entity, fallDistance);
    }

    private void tryBreakLuanluanBlock(World world, BlockState state, BlockPos pos, Entity entity, int inverseChance) {
        if (!this.breaksLuanluanBlock(world, entity)) {
            return;
        }

        if (!world.isClient && world.random.nextInt(inverseChance) == 0 && state.isOf(ModBlocks.TIANSULUO_PINK_SCARF_LUANLUAN_BLOCK)) {
            this.breakLuanluanBlock(world, pos, state);
            if (entity instanceof PlayerEntity player) {
                player.sendMessage(Text.translatable("message.oh-yeah.tiansuluo_pink_scarf.luanluan_block_broken"), true);
            }
        }
    }

    private void breakLuanluanBlock(World world, BlockPos pos, BlockState state) {
        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_CALCITE_BREAK,
                SoundCategory.BLOCKS,
                0.7F,
                0.9F + world.random.nextFloat() * 0.2F
        );
        int blocks = state.get(BLOCKS);
        if (blocks <= 1) {
            world.breakBlock(pos, false);
            return;
        }

        BlockState updated = state.with(BLOCKS, blocks - 1);
        world.setBlockState(pos, updated, 2);
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
        world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!isValidLuanluanBlockBase(world, pos) || !state.isOf(ModBlocks.TIANSULUO_PINK_SCARF_LUANLUAN_BLOCK)) {
            return;
        }
        this.scheduleNextHatchStage(world, pos);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!isValidLuanluanBlockBase(world, pos) || !state.isOf(ModBlocks.TIANSULUO_PINK_SCARF_LUANLUAN_BLOCK)) {
            return;
        }
        int hatch = state.get(HATCH);
        if (hatch < 2) {
            world.playSound(null, pos, SoundEvents.BLOCK_CALCITE_HIT, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            BlockState nextState = state.with(HATCH, hatch + 1);
            world.setBlockState(pos, nextState, 2);
            world.emitGameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Emitter.of(state));
            this.notifyNearbyPlayers(world, pos, "message.oh-yeah.tiansuluo_pink_scarf.luanluan_block_hatch_progress", hatch + 1, 2);
            this.scheduleNextHatchStage(world, pos);
            return;
        }

        world.playSound(null, pos, SoundEvents.BLOCK_CALCITE_PLACE, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
        world.removeBlock(pos, false);
        world.emitGameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Emitter.of(state));
        int blocks = state.get(BLOCKS);
        int babyAge = tuning().babyGrowthAgeTicks();
        for (int i = 0; i < blocks; i++) {
            world.syncWorldEvent(2001, pos, Block.getRawIdFromState(state));
            TiansuluoPinkScarfEntity child = ModEntityTypes.TIANSULUO_PINK_SCARF.create(world);
            if (child == null) {
                continue;
            }

            child.setBaby(true);
            child.setBreedingAge(babyAge);
            child.refreshPositionAndAngles(
                    pos.getX() + 0.3D + i * 0.2D,
                    pos.getY(),
                    pos.getZ() + 0.3D,
                    0.0F,
                    0.0F
            );
            world.spawnEntity(child);
        }
        this.notifyNearbyPlayers(world, pos, "message.oh-yeah.tiansuluo_pink_scarf.luanluan_block_hatched", blocks);
    }

    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        int hatch = state.get(HATCH);
        if (random.nextInt(8 - hatch * 2) != 0) {
            return;
        }

        ParticleEffect effect = EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, switch (hatch) {
            case 0 -> LUANLUAN_BLOCK_HINT_COLOR_STAGE_0;
            case 1 -> LUANLUAN_BLOCK_HINT_COLOR_STAGE_1;
            default -> LUANLUAN_BLOCK_HINT_COLOR_STAGE_2;
        });

        int particles = 1 + hatch;
        for (int i = 0; i < particles; i++) {
            double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
            double y = pos.getY() + 0.18D + random.nextDouble() * 0.28D;
            double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
            double vx = (random.nextDouble() - 0.5D) * 0.01D;
            double vy = 0.01D + random.nextDouble() * 0.02D;
            double vz = (random.nextDouble() - 0.5D) * 0.01D;
            world.addParticle(effect, x, y, z, vx, vy, vz);
        }
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (isValidLuanluanBlockBase(world, pos) && !world.isClient) {
            world.syncWorldEvent(2012, pos, 15);
            this.scheduleNextHatchStage((ServerWorld) world, pos);
        }
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);
        this.breakLuanluanBlock(world, pos, state);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        if (!context.shouldCancelInteraction()
                && context.getStack().isOf(this.asItem())
                && state.get(BLOCKS) < 4) {
            return true;
        }
        return super.canReplace(state, context);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        BlockState existing = context.getWorld().getBlockState(context.getBlockPos());
        if (existing.isOf(this)) {
            return existing.with(BLOCKS, Math.min(4, existing.get(BLOCKS) + 1));
        }
        return super.getPlacementState(context);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(BLOCKS) > 1 ? LARGE_SHAPE : SMALL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.getOutlineShape(state, world, pos, context);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HATCH, BLOCKS);
    }

    private boolean breaksLuanluanBlock(World world, Entity entity) {
        if (entity instanceof TiansuluoPinkScarfEntity || entity instanceof BatEntity) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        return entity instanceof PlayerEntity || world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
    }

    public static boolean isValidLuanluanBlockBase(BlockView world, BlockPos pos) {
        BlockPos basePos = pos.down();
        BlockState baseState = world.getBlockState(basePos);
        return baseState.isSideSolidFullSquare(world, basePos, Direction.UP);
    }

    private static TiansuluoTuningConfig tuning() {
        return OhYeahConfigManager.getTiansuluoPinkScarfConfig().tuning();
    }

    private void scheduleNextHatchStage(ServerWorld world, BlockPos pos) {
        world.scheduleBlockTick(pos, this, HATCH_STAGE_TICKS);
    }

    private void notifyNearbyPlayers(ServerWorld world, BlockPos pos, String translationKey, Object... args) {
        Text message = Text.translatable(translationKey, args);
        double maxDistanceSq = 24.0D * 24.0D;
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= maxDistanceSq) {
                player.sendMessage(message, true);
            }
        }
    }
}
