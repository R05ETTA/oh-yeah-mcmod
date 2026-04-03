package com.oh_yeah.entity;

import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.ModSoundEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SuxiaEntity extends WaterCreatureEntity {
    private static final byte STATUS_RESET_THRUST_TIMER = 67;
    private static final byte STATUS_SUXIA_HURT_BURST = 68;
    private static final byte STATUS_SUXIA_DEATH_BURST = 69;
    private static final int SUXIA_INK_COLOR_RGB = 0xF5D142;
    private static final float LAND_HOP_VERTICAL_SPEED = 0.34F;
    private static final float LAND_HOP_HORIZONTAL_SPEED = 0.18F;
    private static final float LAND_IDLE_DAMPING = 0.85F;
    private static final int LAND_HOP_COOLDOWN_MIN = 10;
    private static final int LAND_HOP_COOLDOWN_MAX = 18;
    private static final float LAND_YAW_JITTER_DEGREES = 70.0F;
    private static final double TEMPTED_PLAYER_RANGE = 14.0D;
    private static final float TEMPT_SWIM_SPEED = 0.26F;
    private static final float TEMPT_SWIM_VERTICAL_MIN = -0.08F;
    private static final float TEMPT_SWIM_VERTICAL_MAX = 0.22F;
    private static final double TEMPT_STOP_DISTANCE = 1.6D;
    private static final double FACE_PLAYER_RANGE = 8.0D;
    private static final float FACE_PLAYER_BIAS_RISE_SPEED = 0.1F;
    private static final float FACE_PLAYER_BIAS_FALL_SPEED = 0.08F;
    private static final float FACE_PLAYER_MAX_TURN_SPEED = 6.0F;
    private static final double FACE_PLAYER_MAX_HORIZONTAL_SPEED_SQ = 0.03D * 0.03D;
    private static final double FACE_PLAYER_MAX_VERTICAL_SPEED = 0.08D;

    public float tiltAngle;
    public float prevTiltAngle;
    public float rollAngle;
    public float prevRollAngle;
    public float thrustTimer;
    public float prevThrustTimer;
    public float tentacleAngle;
    public float prevTentacleAngle;
    private float swimVelocityScale;
    private float thrustTimerSpeed;
    private float turningSpeed;
    private float swimX;
    private float swimY;
    private float swimZ;
    private float facePlayerBias;
    private float prevFacePlayerBias;

    public SuxiaEntity(EntityType<? extends SuxiaEntity> entityType, World world) {
        super(entityType, world);
        this.random.setSeed(this.getId());
        this.thrustTimerSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
        this.tiltAngle = 0.0F;
        this.prevTiltAngle = 0.0F;
        this.rollAngle = 0.0F;
        this.prevRollAngle = 0.0F;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new LandHopGoal());
        this.goalSelector.add(1, new EscapeAttackerGoal());
        this.goalSelector.add(2, new TemptByChipsGoal());
        this.goalSelector.add(3, new SwimGoal(this));
    }

    public static DefaultAttributeContainer.Builder createSuxiaAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSoundEvents.Suxia.AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSoundEvents.Suxia.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSoundEvents.Suxia.DEATH;
    }

    protected SoundEvent getSquirtSound() {
        return ModSoundEvents.Suxia.SQUIRT;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.EVENTS;
    }

    @Override
    protected double getGravity() {
        return 0.08;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        this.prevFacePlayerBias = this.facePlayerBias;
        this.prevTiltAngle = this.tiltAngle;
        this.prevRollAngle = this.rollAngle;
        this.prevThrustTimer = this.thrustTimer;
        this.prevTentacleAngle = this.tentacleAngle;
        this.thrustTimer += this.thrustTimerSpeed;
        if (this.thrustTimer > Math.PI * 2) {
            if (this.getWorld().isClient) {
                this.thrustTimer = (float) (Math.PI * 2);
            } else {
                this.thrustTimer -= (float) (Math.PI * 2);
                if (this.random.nextInt(10) == 0) {
                    this.thrustTimerSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
                }
                this.getWorld().sendEntityStatus(this, STATUS_RESET_THRUST_TIMER);
            }
        }

        if (this.isInsideWaterOrBubbleColumn()) {
            if (this.thrustTimer < (float) Math.PI) {
                float f = this.thrustTimer / (float) Math.PI;
                this.tentacleAngle = MathHelper.sin(f * f * (float) Math.PI) * (float) Math.PI * 0.25F;
                if (f > 0.75F) {
                    this.swimVelocityScale = 1.0F;
                    this.turningSpeed = 1.0F;
                } else {
                    this.turningSpeed *= 0.8F;
                }
            } else {
                this.tentacleAngle = 0.0F;
                this.swimVelocityScale *= 0.9F;
                this.turningSpeed *= 0.99F;
            }

            if (!this.getWorld().isClient) {
                this.setVelocity(this.swimX * this.swimVelocityScale, this.swimY * this.swimVelocityScale, this.swimZ * this.swimVelocityScale);
            }

            Vec3d vec3d = this.getVelocity();
            double horizontalLength = vec3d.horizontalLength();
            this.bodyYaw = this.bodyYaw + (-((float) MathHelper.atan2(vec3d.x, vec3d.z)) * (180.0F / (float) Math.PI) - this.bodyYaw) * 0.1F;
            this.setYaw(this.bodyYaw);
            this.rollAngle += (float) Math.PI * this.turningSpeed * 1.5F;
            this.tiltAngle = this.tiltAngle + (-((float) MathHelper.atan2(horizontalLength, vec3d.y)) * (180.0F / (float) Math.PI) - this.tiltAngle) * 0.1F;
        } else {
            this.tentacleAngle = MathHelper.abs(MathHelper.sin(this.thrustTimer)) * (float) Math.PI * 0.25F;
            if (!this.getWorld().isClient) {
                Vec3d velocity = this.getVelocity();
                double velocityY = this.getVelocity().y;
                if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
                    velocityY = 0.05 * (this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1);
                } else {
                    velocityY -= this.getFinalGravity();
                }
                double horizontalDamping = this.isOnGround() ? LAND_IDLE_DAMPING : 0.98F;
                this.setVelocity(velocity.x * horizontalDamping, velocityY * 0.98F, velocity.z * horizontalDamping);
            }

            this.tiltAngle = this.tiltAngle + (0.0F - this.tiltAngle) * 0.2F;
            this.rollAngle = this.rollAngle + (0.0F - this.rollAngle) * 0.2F;
            this.bodyYaw = this.getYaw();
        }

        this.updateFacePlayerBias();
        this.applyFacePlayerYawBias();

    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (super.damage(source, amount)) {
            if (!this.getWorld().isClient) {
                this.getWorld().sendEntityStatus(this, STATUS_SUXIA_HURT_BURST);
            }
            if (this.getAttacker() != null && !this.getWorld().isClient) {
                this.squirt();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, STATUS_SUXIA_DEATH_BURST);
        }
        super.onDeath(damageSource);
        if (!this.getWorld().isClient) {
            this.dropItem(ModItems.XIAMI_HUHU);
        }
    }

    private Vec3d applyBodyRotations(Vec3d shootVector) {
        Vec3d vec3d = shootVector.rotateX(this.prevTiltAngle * (float) (Math.PI / 180.0));
        return vec3d.rotateY(-this.prevBodyYaw * (float) (Math.PI / 180.0));
    }

    private void squirt() {
        this.playSound(this.getSquirtSound());
        Vec3d vec3d = this.applyBodyRotations(new Vec3d(0.0, -1.0, 0.0)).add(this.getX(), this.getY(), this.getZ());

        for (int i = 0; i < 30; i++) {
            Vec3d direction = this.applyBodyRotations(new Vec3d(this.random.nextFloat() * 0.6 - 0.3, -1.0, this.random.nextFloat() * 0.6 - 0.3));
            Vec3d velocity = direction.multiply(0.3 + this.random.nextFloat() * 2.0F);
            ((ServerWorld) this.getWorld()).spawnParticles(this.getInkParticle(), vec3d.x, vec3d.y + 0.5, vec3d.z, 0, velocity.x, velocity.y, velocity.z, 0.1F);
        }
    }

    protected ParticleEffect getInkParticle() {
        return EntityEffectParticleEffect.create(ParticleTypes.ENTITY_EFFECT, SUXIA_INK_COLOR_RGB);
    }

    @Override
    public void travel(Vec3d movementInput) {
        this.move(MovementType.SELF, this.getVelocity());
    }

    @Override
    public void handleStatus(byte status) {
        if (status == STATUS_RESET_THRUST_TIMER) {
            this.thrustTimer = 0.0F;
        } else if (status == STATUS_SUXIA_HURT_BURST) {
            this.spawnHurtBurstParticles();
        } else if (status == STATUS_SUXIA_DEATH_BURST) {
            this.spawnDeathBurstParticles();
        } else {
            super.handleStatus(status);
        }
    }

    private void spawnHurtBurstParticles() {
        ParticleEffect effect = this.getInkParticle();
        for (int i = 0; i < 12; i++) {
            double offsetX = (this.random.nextDouble() - 0.5D) * 0.55D;
            double offsetY = this.random.nextDouble() * 0.45D + 0.2D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.55D;
            double velocityX = (this.random.nextDouble() - 0.5D) * 0.06D;
            double velocityY = this.random.nextDouble() * 0.08D + 0.03D;
            double velocityZ = (this.random.nextDouble() - 0.5D) * 0.06D;
            this.getWorld().addParticle(effect, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, velocityX, velocityY, velocityZ);
        }
    }

    private void spawnDeathBurstParticles() {
        ParticleEffect effect = this.getInkParticle();
        for (int i = 0; i < 44; i++) {
            double offsetX = (this.random.nextDouble() - 0.5D) * 0.9D;
            double offsetY = this.random.nextDouble() * 0.65D + 0.1D;
            double offsetZ = (this.random.nextDouble() - 0.5D) * 0.9D;
            double velocityX = (this.random.nextDouble() - 0.5D) * 0.18D;
            double velocityY = this.random.nextDouble() * 0.18D + 0.04D;
            double velocityZ = (this.random.nextDouble() - 0.5D) * 0.18D;
            this.getWorld().addParticle(effect, this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ, velocityX, velocityY, velocityZ);
        }
    }

    public void setSwimmingVector(float x, float y, float z) {
        this.swimX = x;
        this.swimY = y;
        this.swimZ = z;
    }

    public boolean hasSwimmingVector() {
        return this.swimX != 0.0F || this.swimY != 0.0F || this.swimZ != 0.0F;
    }

    public float getFacePlayerBias(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.prevFacePlayerBias, this.facePlayerBias);
    }

    private boolean isFavoriteFood(ItemStack stack) {
        return stack.isOf(ModItems.CHIPS);
    }

    private boolean isHoldingFavoriteFood(PlayerEntity player) {
        return this.isFavoriteFood(player.getMainHandStack()) || this.isFavoriteFood(player.getOffHandStack());
    }

    private PlayerEntity getTemptingPlayer() {
        return this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), TEMPTED_PLAYER_RANGE, entity ->
                entity instanceof PlayerEntity player
                        && player.isAlive()
                        && this.isHoldingFavoriteFood(player)
        );
    }

    private PlayerEntity getNearbyPlayerForFacing() {
        return this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), FACE_PLAYER_RANGE, entity ->
                entity instanceof PlayerEntity player
                        && player.isAlive()
                        && !player.isSpectator()
        );
    }

    private boolean shouldPreferFacingPlayer(PlayerEntity targetPlayer) {
        LivingEntity attacker = this.getAttacker();
        boolean escaping = attacker != null
                && attacker.isAlive()
                && this.isTouchingWater()
                && this.squaredDistanceTo(attacker) < 100.0;
        if (escaping) {
            return false;
        }

        if (this.getTemptingPlayer() != null) {
            return false;
        }

        Vec3d velocity = this.getVelocity();
        return velocity.horizontalLengthSquared() <= FACE_PLAYER_MAX_HORIZONTAL_SPEED_SQ
                && Math.abs(velocity.y) <= FACE_PLAYER_MAX_VERTICAL_SPEED
                && this.squaredDistanceTo(targetPlayer) <= FACE_PLAYER_RANGE * FACE_PLAYER_RANGE;
    }

    private void updateFacePlayerBias() {
        PlayerEntity nearbyPlayer = this.getNearbyPlayerForFacing();
        boolean shouldBias = nearbyPlayer != null && this.shouldPreferFacingPlayer(nearbyPlayer);
        float step = shouldBias ? FACE_PLAYER_BIAS_RISE_SPEED : FACE_PLAYER_BIAS_FALL_SPEED;
        this.facePlayerBias = MathHelper.clamp(
                MathHelper.stepTowards(this.facePlayerBias, shouldBias ? 1.0F : 0.0F, step),
                0.0F,
                1.0F
        );
    }

    private void applyFacePlayerYawBias() {
        if (this.facePlayerBias <= 0.0F) {
            return;
        }

        PlayerEntity targetPlayer = this.getNearbyPlayerForFacing();
        if (targetPlayer == null) {
            return;
        }

        double deltaX = targetPlayer.getX() - this.getX();
        double deltaZ = targetPlayer.getZ() - this.getZ();
        if (Math.abs(deltaX) < 1.0E-6D && Math.abs(deltaZ) < 1.0E-6D) {
            return;
        }

        float targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / Math.PI) - 90.0F;
        float turnSpeed = FACE_PLAYER_MAX_TURN_SPEED * this.facePlayerBias;
        float adjustedYaw = MathHelper.stepUnwrappedAngleTowards(this.bodyYaw, targetYaw, turnSpeed);
        this.setYaw(adjustedYaw);
        this.setBodyYaw(adjustedYaw);
        this.prevBodyYaw = adjustedYaw;
    }

    class EscapeAttackerGoal extends Goal {
        private int timer;

        @Override
        public boolean canStart() {
            LivingEntity attacker = SuxiaEntity.this.getAttacker();
            return SuxiaEntity.this.isTouchingWater() && attacker != null ? SuxiaEntity.this.squaredDistanceTo(attacker) < 100.0 : false;
        }

        @Override
        public void start() {
            this.timer = 0;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            this.timer++;
            LivingEntity attacker = SuxiaEntity.this.getAttacker();
            if (attacker != null) {
                Vec3d offset = new Vec3d(
                        SuxiaEntity.this.getX() - attacker.getX(),
                        SuxiaEntity.this.getY() - attacker.getY(),
                        SuxiaEntity.this.getZ() - attacker.getZ()
                );
                BlockState blockState = SuxiaEntity.this.getWorld().getBlockState(
                        BlockPos.ofFloored(SuxiaEntity.this.getX() + offset.x, SuxiaEntity.this.getY() + offset.y, SuxiaEntity.this.getZ() + offset.z)
                );
                FluidState fluidState = SuxiaEntity.this.getWorld().getFluidState(
                        BlockPos.ofFloored(SuxiaEntity.this.getX() + offset.x, SuxiaEntity.this.getY() + offset.y, SuxiaEntity.this.getZ() + offset.z)
                );
                if (fluidState.isIn(FluidTags.WATER) || blockState.isAir()) {
                    double length = offset.length();
                    if (length > 0.0) {
                        offset = offset.normalize();
                        double speed = 3.0;
                        if (length > 5.0) {
                            speed -= (length - 5.0) / 5.0;
                        }
                        if (speed > 0.0) {
                            offset = offset.multiply(speed);
                        }
                    }

                    if (blockState.isAir()) {
                        offset = offset.subtract(0.0, offset.y, 0.0);
                    }

                    SuxiaEntity.this.setSwimmingVector((float) offset.x / 20.0F, (float) offset.y / 20.0F, (float) offset.z / 20.0F);
                }

                if (this.timer % 10 == 5) {
                    SuxiaEntity.this.getWorld().addParticle(ParticleTypes.BUBBLE, SuxiaEntity.this.getX(), SuxiaEntity.this.getY(), SuxiaEntity.this.getZ(), 0.0, 0.0, 0.0);
                }
            }
        }
    }

    class SwimGoal extends Goal {
        private final SuxiaEntity suxia;

        public SwimGoal(SuxiaEntity suxia) {
            this.suxia = suxia;
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public void tick() {
            int despawnCounter = this.suxia.getDespawnCounter();
            if (despawnCounter > 100) {
                this.suxia.setSwimmingVector(0.0F, 0.0F, 0.0F);
            } else if (this.suxia.getRandom().nextInt(toGoalTicks(50)) == 0 || !this.suxia.touchingWater || !this.suxia.hasSwimmingVector()) {
                float angle = this.suxia.getRandom().nextFloat() * (float) (Math.PI * 2);
                float x = MathHelper.cos(angle) * 0.2F;
                float y = -0.1F + this.suxia.getRandom().nextFloat() * 0.2F;
                float z = MathHelper.sin(angle) * 0.2F;
                this.suxia.setSwimmingVector(x, y, z);
            }
        }
    }

    class TemptByChipsGoal extends Goal {
        private PlayerEntity targetPlayer;

        @Override
        public boolean canStart() {
            this.targetPlayer = SuxiaEntity.this.getTemptingPlayer();
            return this.targetPlayer != null;
        }

        @Override
        public boolean shouldContinue() {
            if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
                return false;
            }
            if (!SuxiaEntity.this.isHoldingFavoriteFood(this.targetPlayer)) {
                return false;
            }
            return SuxiaEntity.this.squaredDistanceTo(this.targetPlayer) <= TEMPTED_PLAYER_RANGE * TEMPTED_PLAYER_RANGE;
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            this.targetPlayer = null;
            if (!SuxiaEntity.this.isTouchingWater()) {
                SuxiaEntity.this.setSwimmingVector(0.0F, 0.0F, 0.0F);
            }
        }

        @Override
        public void tick() {
            if (this.targetPlayer == null) {
                this.targetPlayer = SuxiaEntity.this.getTemptingPlayer();
                if (this.targetPlayer == null) {
                    return;
                }
            }

            Vec3d toTarget = new Vec3d(
                    this.targetPlayer.getX() - SuxiaEntity.this.getX(),
                    this.targetPlayer.getY() - SuxiaEntity.this.getY(),
                    this.targetPlayer.getZ() - SuxiaEntity.this.getZ()
            );
            double distance = toTarget.length();
            if (distance < TEMPT_STOP_DISTANCE) {
                SuxiaEntity.this.setSwimmingVector(0.0F, 0.0F, 0.0F);
                return;
            }

            Vec3d direction = toTarget.normalize();
            float swimX = (float) (direction.x * TEMPT_SWIM_SPEED);
            float swimY = (float) MathHelper.clamp(direction.y * 0.7D, TEMPT_SWIM_VERTICAL_MIN, TEMPT_SWIM_VERTICAL_MAX);
            float swimZ = (float) (direction.z * TEMPT_SWIM_SPEED);

            // Keep buoyant while tempting a player on shore so Suxia can reach land and hop forward.
            if (!this.targetPlayer.isTouchingWater() && SuxiaEntity.this.isTouchingWater()) {
                swimY = Math.max(swimY, 0.04F);
            }

            SuxiaEntity.this.setSwimmingVector(swimX, swimY, swimZ);
        }
    }

    class LandHopGoal extends Goal {
        private int cooldown;

        @Override
        public boolean canStart() {
            return !SuxiaEntity.this.isTouchingWater();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.cooldown = 0;
        }

        @Override
        public void tick() {
            if (SuxiaEntity.this.isTouchingWater()) {
                return;
            }

            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }

            if (!SuxiaEntity.this.isOnGround()) {
                return;
            }

            float yaw = SuxiaEntity.this.bodyYaw;
            PlayerEntity temptingPlayer = SuxiaEntity.this.getTemptingPlayer();
            LivingEntity attacker = SuxiaEntity.this.getAttacker();
            if (temptingPlayer != null) {
                yaw = (float) (MathHelper.atan2(
                        temptingPlayer.getX() - SuxiaEntity.this.getX(),
                        temptingPlayer.getZ() - SuxiaEntity.this.getZ()
                ) * (180.0F / (float) Math.PI));
            } else if (attacker != null) {
                yaw = (float) (MathHelper.atan2(
                        SuxiaEntity.this.getX() - attacker.getX(),
                        SuxiaEntity.this.getZ() - attacker.getZ()
                ) * (180.0F / (float) Math.PI));
            } else if (SuxiaEntity.this.horizontalCollision) {
                yaw += 120.0F + SuxiaEntity.this.random.nextFloat() * 120.0F;
            } else {
                yaw += (SuxiaEntity.this.random.nextFloat() - 0.5F) * LAND_YAW_JITTER_DEGREES;
            }

            SuxiaEntity.this.setYaw(yaw);
            SuxiaEntity.this.setBodyYaw(yaw);

            float rad = yaw * MathHelper.RADIANS_PER_DEGREE;
            double velocityX = -MathHelper.sin(rad) * LAND_HOP_HORIZONTAL_SPEED;
            double velocityZ = MathHelper.cos(rad) * LAND_HOP_HORIZONTAL_SPEED;
            SuxiaEntity.this.setVelocity(velocityX, LAND_HOP_VERTICAL_SPEED, velocityZ);

            this.cooldown = LAND_HOP_COOLDOWN_MIN
                    + SuxiaEntity.this.random.nextInt(LAND_HOP_COOLDOWN_MAX - LAND_HOP_COOLDOWN_MIN + 1);
        }
    }
}
