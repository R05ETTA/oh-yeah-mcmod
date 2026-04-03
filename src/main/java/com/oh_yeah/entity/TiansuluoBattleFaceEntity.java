package com.oh_yeah.entity;

import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.TiansuluoFeatureSetConfig;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.entity.ai.TiansuluoBattleFacePounceGoal;
import com.oh_yeah.entity.species.ModSpeciesProfiles;
import com.oh_yeah.sound.VoiceType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class TiansuluoBattleFaceEntity extends AbstractTiansuluoEntity {
    public static final float TARGET_ADULT_HEIGHT = 1.2F;
    public static final float BABY_SCALE_FACTOR = 0.5F;
    private static final float MODEL_PIXEL_HEIGHT = 50.0F;
    private static final float MODEL_MAX_HORIZONTAL_SPAN_PIXELS = 26.0F;
    private static final float BOX_MARGIN_BLOCKS = 0.04F;
    public static final float ADULT_RENDER_SCALE = (TARGET_ADULT_HEIGHT * 16.0F) / MODEL_PIXEL_HEIGHT;
    public static final float TARGET_ADULT_WIDTH = ((MODEL_MAX_HORIZONTAL_SPAN_PIXELS / 16.0F) * ADULT_RENDER_SCALE) + BOX_MARGIN_BLOCKS;
    private static final double POUNCE_TRIGGER_DISTANCE = 3.0D;
    private static final double POUNCE_TRIGGER_TOLERANCE = 0.4D;
    private static final int POUNCE_COOLDOWN_TICKS = 100;
    private static final double KNOCKBACK_HORIZONTAL_SPEED = 1.15D;
    private static final double KNOCKBACK_VERTICAL_SPEED = 0.35D;

    public enum RetaliationState {
        IDLE,
        PENDING_DECLARE,
        READY_TO_POUNCE,
        COOLDOWN
    }

    private @Nullable LivingEntity retaliationTarget;
    private RetaliationState retaliationState = RetaliationState.IDLE;
    private int retaliationTicksRemaining;
    private int retaliationDeclareTicksRemaining;
    private int attackCooldownTicksRemaining;

    public TiansuluoBattleFaceEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoBattleFaceConfig();
        TiansuluoTuningConfig tuning = config.tuning();
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, tuning.maxHealth())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, tuning.movementSpeed())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, tuning.followRange())
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, config.archetype().features().attack().meleeDamage());
    }

    @Override
    protected void initGoals() {
        TiansuluoTuningConfig tuning = this.tuning();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new TiansuluoBattleFacePounceGoal(this));
        this.goalSelector.add(2, this.createMateForEggBlockGoal(tuning.mateGoalSpeed()));
        this.goalSelector.add(3, this.createLayEggBlockGoal());
        this.goalSelector.add(4, this.createTemptWhileAvailableGoal(tuning.temptGoalSpeed()));
        this.goalSelector.add(5, new FollowParentGoal(this, tuning.followParentGoalSpeed()));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, tuning.wanderGoalSpeed()));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    protected SpeciesConfig speciesConfig() {
        return OhYeahConfigManager.getTiansuluoBattleFaceConfig();
    }

    @Override
    protected String soundProfileKey() {
        return ModSpeciesProfiles.TIANSULUO_BATTLE_FACE_SOUND_PROFILE;
    }

    @Override
    protected String speciesKey() {
        return "tiansuluo_battle_face";
    }

    @Override
    protected void tickSpeciesMovement() {
        if (this.attackCooldownTicksRemaining > 0) {
            this.attackCooldownTicksRemaining--;
        }
        this.updateRetaliationTarget();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && !this.getWorld().isClient && source.getAttacker() instanceof LivingEntity attacker && attacker != this) {
            this.rememberRetaliationTarget(attacker);
        }
        return damaged;
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (!super.tryAttack(target)) {
            return false;
        }

        if (target instanceof LivingEntity livingTarget) {
            Vec3d direction = this.getHorizontalDirectionTo(livingTarget);
            livingTarget.addVelocity(direction.x * KNOCKBACK_HORIZONTAL_SPEED, KNOCKBACK_VERTICAL_SPEED, direction.z * KNOCKBACK_HORIZONTAL_SPEED);
            livingTarget.velocityModified = true;
            this.addVelocity(-direction.x * KNOCKBACK_HORIZONTAL_SPEED, KNOCKBACK_VERTICAL_SPEED, -direction.z * KNOCKBACK_HORIZONTAL_SPEED);
            this.velocityModified = true;

            if (livingTarget instanceof PlayerEntity player) {
                this.applyHungerPenalty(player);
            }
        }

        return true;
    }

    public TiansuluoFeatureSetConfig.AttackFeatureConfig attackConfig() {
        return this.speciesConfig().archetype().features().attack();
    }

    public RetaliationState retaliationState() {
        return this.retaliationState;
    }

    public boolean isReadyToPounce() {
        return this.retaliationState == RetaliationState.READY_TO_POUNCE;
    }

    public boolean isCoolingDown() {
        return this.retaliationState == RetaliationState.COOLDOWN;
    }

    public @Nullable LivingEntity getRetaliationTarget() {
        return this.retaliationTarget;
    }

    public boolean hasUsableRetaliationTarget() {
        return this.isValidRetaliationTarget(this.retaliationTarget);
    }

    public double getApproachSpeed() {
        return this.tuning().wanderGoalSpeed() + 0.25D;
    }

    public boolean isAttackCooldownReady() {
        return this.attackCooldownTicksRemaining <= 0;
    }

    public int getAttackCooldownTicksRemaining() {
        return this.attackCooldownTicksRemaining;
    }

    public void setAttackCooldownTicks(int ticks) {
        this.attackCooldownTicksRemaining = Math.max(0, ticks);
    }

    public boolean isWithinPounceWindow(LivingEntity target) {
        double distance = this.distanceTo(target);
        return distance >= POUNCE_TRIGGER_DISTANCE - POUNCE_TRIGGER_TOLERANCE
                && distance <= POUNCE_TRIGGER_DISTANCE + POUNCE_TRIGGER_TOLERANCE;
    }

    public Vec3d getPounceAimPoint(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    public void beginCharge() {
        this.getNavigation().stop();
    }

    public void finishSuccessfulRetaliation() {
        this.clearRetaliationState(true);
    }

    public void startCooldown() {
        this.attackCooldownTicksRemaining = POUNCE_COOLDOWN_TICKS;
        this.retaliationState = RetaliationState.COOLDOWN;
        this.getNavigation().stop();
        this.setAttacking(false);
    }

    public void syncNavigationTarget() {
        if (this.retaliationTarget != null && this.getTarget() != this.retaliationTarget) {
            this.setTarget(this.retaliationTarget);
        }
    }

    public void faceRetaliationTarget(LivingEntity target) {
        float turnSpeed = (float) this.tuning().retaliationFaceTargetTurnSpeed();
        this.getLookControl().lookAt(target, turnSpeed, turnSpeed);

        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        if (Math.abs(deltaX) < 1.0E-6 && Math.abs(deltaZ) < 1.0E-6) {
            return;
        }

        float targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / Math.PI) - 90.0F;
        float bodyYaw = MathHelper.stepUnwrappedAngleTowards(this.getYaw(), targetYaw, turnSpeed);
        float headYaw = MathHelper.stepUnwrappedAngleTowards(this.getHeadYaw(), targetYaw, turnSpeed);
        this.setYaw(bodyYaw);
        this.bodyYaw = bodyYaw;
        this.prevBodyYaw = bodyYaw;
        this.setHeadYaw(headYaw);
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return null;
    }

    private void rememberRetaliationTarget(LivingEntity attacker) {
        if (!this.isValidRetaliationSource(attacker)) {
            return;
        }

        this.retaliationTarget = attacker;
        this.retaliationTicksRemaining = this.tuning().retaliationMemoryTicks();
        this.retaliationDeclareTicksRemaining = 0;
        this.attackCooldownTicksRemaining = 0;
        this.retaliationState = RetaliationState.PENDING_DECLARE;
        this.syncNavigationTarget();
    }

    private void updateRetaliationTarget() {
        if (this.retaliationTicksRemaining > 0) {
            this.retaliationTicksRemaining--;
        }

        LivingEntity currentTarget = this.retaliationTarget;
        if (!this.isValidRetaliationTarget(currentTarget)) {
            this.clearRetaliationState(true);
            return;
        }

        this.syncNavigationTarget();
        this.faceRetaliationTarget(currentTarget);

        if (this.retaliationState == RetaliationState.COOLDOWN) {
            this.getNavigation().stop();
            if (this.attackCooldownTicksRemaining <= 0) {
                this.retaliationState = RetaliationState.READY_TO_POUNCE;
            }
            return;
        }

        if (this.retaliationState == RetaliationState.PENDING_DECLARE) {
            this.getNavigation().stop();
            if (this.isVoiceActive(VoiceType.HURT)) {
                return;
            }

            if (this.isVoiceActive(VoiceType.ATTACK_DECLARE)) {
                return;
            }

            if (this.retaliationDeclareTicksRemaining <= 0) {
                this.tryPlayVoice(VoiceType.ATTACK_DECLARE, this.sounds().get(VoiceType.ATTACK_DECLARE), 1.0F, 1.0F);
                this.retaliationDeclareTicksRemaining = 1;
            } else {
                this.retaliationDeclareTicksRemaining = 0;
                this.retaliationState = RetaliationState.READY_TO_POUNCE;
            }
            return;
        }

        if (this.retaliationState == RetaliationState.READY_TO_POUNCE) {
            if (this.isWithinPounceWindow(currentTarget)) {
                this.getNavigation().stop();
            } else if (this.distanceTo(currentTarget) < POUNCE_TRIGGER_DISTANCE - POUNCE_TRIGGER_TOLERANCE) {
                this.getNavigation().stop();
            } else {
                this.getNavigation().startMovingTo(currentTarget, this.getApproachSpeed());
            }
        }
    }

    private void clearRetaliationState(boolean playEndVoice) {
        boolean shouldPlayEndVoice = playEndVoice && this.retaliationState != RetaliationState.IDLE;
        this.retaliationTarget = null;
        this.retaliationState = RetaliationState.IDLE;
        this.retaliationTicksRemaining = 0;
        this.retaliationDeclareTicksRemaining = 0;
        this.attackCooldownTicksRemaining = 0;
        this.getNavigation().stop();
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        if (shouldPlayEndVoice) {
            this.tryPlayVoice(VoiceType.ATTACK_END, this.sounds().get(VoiceType.ATTACK_END), 1.0F, 1.0F);
        }
    }

    private boolean isValidRetaliationTarget(@Nullable LivingEntity candidate) {
        if (!this.isValidRetaliationSource(candidate)) {
            return false;
        }
        if (this.retaliationTicksRemaining <= 0) {
            return false;
        }

        double maxRange = Math.max(this.tuning().retaliationRange(), this.attackConfig().pounceRange());
        if (this.squaredDistanceTo(candidate) > MathHelper.square(maxRange)) {
            return false;
        }
        return this.canSee(candidate);
    }

    private boolean isValidRetaliationSource(@Nullable LivingEntity candidate) {
        if (candidate == null || candidate == this) {
            return false;
        }
        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }
        return candidate.getWorld() == this.getWorld();
    }

    private Vec3d getHorizontalDirectionTo(LivingEntity target) {
        Vec3d direction = new Vec3d(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
        if (direction.lengthSquared() > 1.0E-7D) {
            return direction.normalize();
        }

        float yawRadians = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        return new Vec3d(-MathHelper.sin(yawRadians), 0.0D, MathHelper.cos(yawRadians));
    }

    private void applyHungerPenalty(PlayerEntity player) {
        TiansuluoFeatureSetConfig.AttackFeatureConfig attack = this.attackConfig();
        int hungerLoss = switch (this.getWorld().getDifficulty()) {
            case PEACEFUL -> 0;
            case EASY -> attack.hungerDamageEasy();
            case NORMAL -> attack.hungerDamageNormal();
            case HARD -> attack.hungerDamageHard();
        };
        if (hungerLoss > 0) {
            player.getHungerManager().setFoodLevel(Math.max(0, player.getHungerManager().getFoodLevel() - hungerLoss));
        }
        if (attack.saturationDrain() > 0.0F) {
            player.getHungerManager().addExhaustion(attack.saturationDrain());
        }
    }
}
