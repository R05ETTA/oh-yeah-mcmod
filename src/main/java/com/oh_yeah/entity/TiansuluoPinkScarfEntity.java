package com.oh_yeah.entity;

import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.entity.projectile.TiansuluoPinkScarfProjectileEntity;
import com.oh_yeah.entity.species.ModSpeciesProfiles;
import com.oh_yeah.sound.VoiceType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TiansuluoPinkScarfEntity extends AbstractTiansuluoEntity implements RangedAttackMob {
    private enum RetaliationState { IDLE, PENDING_RETALIATION_DECLARE, RETALIATING }

    private RetaliationState retaliationState = RetaliationState.IDLE;
    private @Nullable LivingEntity retaliationTarget;
    private int retaliationTicksRemaining;
    private int retaliationBurstShotsFired;
    private int retaliationBurstCooldownTicks;
    private int retaliationDeclareTicksRemaining;

    public TiansuluoPinkScarfEntity(EntityType<? extends AnimalEntity> entityType, World world) { super(entityType, world); }

    public static DefaultAttributeContainer.Builder createAttributes() {
        TiansuluoTuningConfig tuning = OhYeahConfigManager.getTiansuluoPinkScarfConfig().tuning();
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, tuning.maxHealth())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, tuning.movementSpeed())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, tuning.followRange());
    }

    @Override
    protected void initGoals() {
        TiansuluoTuningConfig tuning = this.tuning();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ProjectileAttackGoal(this, tuning.projectileAttackGoalSpeed(), tuning.retaliationBurstIntervalTicks(), (float) tuning.retaliationRange()));
        this.goalSelector.add(2, this.createMateForEggBlockGoal(tuning.mateGoalSpeed()));
        this.goalSelector.add(3, this.createLayEggBlockGoal());
        this.goalSelector.add(4, this.createTemptWhileAvailableGoal(tuning.temptGoalSpeed()));
        this.goalSelector.add(5, new FollowParentGoal(this, tuning.followParentGoalSpeed()));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, tuning.wanderGoalSpeed()));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
    }

    @Override
    protected void tickSpeciesMovement() {
        this.updateRetaliationTarget();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && !this.getWorld().isClient) {
            EntityType<?> attackerType = source.getAttacker() != null ? source.getAttacker().getType() : null;
            if (source.getAttacker() instanceof LivingEntity attacker && attacker != this && this.canTarget(attackerType)) this.rememberRetaliationTarget(attacker);
        }
        return damaged;
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        if (this.retaliationState != RetaliationState.RETALIATING || this.retaliationBurstCooldownTicks > 0) return;
        this.faceRetaliationTarget(target);
        Vec3d muzzlePos = this.getProjectileMuzzlePos();
        TiansuluoPinkScarfProjectileEntity projectile = new TiansuluoPinkScarfProjectileEntity(this.getWorld(), muzzlePos.x, muzzlePos.y, muzzlePos.z);
        projectile.setOwner(this);
        projectile.setDamage(this.tuning().retaliationProjectileDamage());
        double targetY = target.getEyeY() - this.tuning().retaliationTargetEyeOffset();
        double deltaX = target.getX() - muzzlePos.x;
        double deltaY = targetY - projectile.getY();
        double deltaZ = target.getZ() - muzzlePos.z;
        double arcBoost = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 0.2F;
        projectile.setVelocity(deltaX, deltaY + arcBoost, deltaZ, this.tuning().retaliationProjectileSpeed(), this.tuning().retaliationProjectileDivergence());
        this.tryPlayVoice(VoiceType.ATTACK_SHOT, this.sounds().get(VoiceType.ATTACK_SHOT), 1.0F, 1.0F);
        this.playSpeciesSound(SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.getWorld().spawnEntity(projectile);
        this.retaliationBurstShotsFired++;
        if (this.retaliationBurstShotsFired >= this.tuning().retaliationBurstShots()) {
            this.retaliationBurstShotsFired = 0;
            this.retaliationBurstCooldownTicks = this.tuning().retaliationBurstCooldownTicks();
        }
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) { return null; }
    @Override protected SpeciesConfig speciesConfig() { return OhYeahConfigManager.getTiansuluoPinkScarfConfig(); }
    @Override protected String soundProfileKey() { return ModSpeciesProfiles.TIANSULUO_PINK_SCARF_SOUND_PROFILE; }
    @Override protected String speciesKey() { return "tiansuluo_pink_scarf"; }
    @Override public float getSoundPitch() { return 1.0F; }

    private Vec3d getProjectileMuzzlePos() {
        Vec3d forward = Vec3d.fromPolar(0.0F, this.getHeadYaw()).normalize();
        double horizontalOffset = this.getWidth() * 0.5D + this.tuning().projectileFrontOffset();
        return new Vec3d(this.getX() + forward.x * horizontalOffset, this.getY() + this.getHeight() * this.tuning().projectileMuzzleHeightRatio(), this.getZ() + forward.z * horizontalOffset);
    }

    private void rememberRetaliationTarget(LivingEntity attacker) {
        this.retaliationTarget = attacker;
        this.retaliationTicksRemaining = this.tuning().retaliationMemoryTicks();
        this.setTarget(attacker);
        if (this.retaliationState == RetaliationState.IDLE) {
            this.retaliationState = RetaliationState.PENDING_RETALIATION_DECLARE;
            this.retaliationDeclareTicksRemaining = 0;
        }
    }

    private void updateRetaliationTarget() {
        if (this.retaliationBurstCooldownTicks > 0) this.retaliationBurstCooldownTicks--;
        if (this.retaliationTicksRemaining > 0) this.retaliationTicksRemaining--;
        LivingEntity currentTarget = this.retaliationTarget;
        if (this.isValidRetaliationTarget(currentTarget)) {
            this.setTarget(currentTarget);
            this.faceRetaliationTarget(currentTarget);
            this.updateRetaliationState(currentTarget);
            return;
        }
        this.clearRetaliationState(true);
    }

    private void updateRetaliationState(LivingEntity target) {
        if (this.retaliationState == RetaliationState.IDLE) return;
        if (this.retaliationState == RetaliationState.PENDING_RETALIATION_DECLARE) {
            if (this.retaliationDeclareTicksRemaining > 0) {
                this.retaliationDeclareTicksRemaining--;
                if (this.retaliationDeclareTicksRemaining <= 0) this.retaliationState = RetaliationState.RETALIATING;
                return;
            }
            if (this.isVoiceActive(VoiceType.HURT)) return;
            this.faceRetaliationTarget(target);
            this.tryPlayVoice(VoiceType.ATTACK_DECLARE, this.sounds().get(VoiceType.ATTACK_DECLARE), 1.0F, 1.0F);
            this.retaliationDeclareTicksRemaining = this.tuning().attackDeclareDurationTicks();
            if (this.retaliationDeclareTicksRemaining <= 0) this.retaliationState = RetaliationState.RETALIATING;
        }
    }

    private void clearRetaliationState(boolean playEndVoice) {
        boolean shouldPlayEndVoice = playEndVoice && this.retaliationState == RetaliationState.RETALIATING;
        this.retaliationTarget = null;
        this.retaliationState = RetaliationState.IDLE;
        this.retaliationBurstShotsFired = 0;
        this.retaliationBurstCooldownTicks = 0;
        this.retaliationDeclareTicksRemaining = 0;
        if (this.getTarget() != null) this.setTarget(null);
        if (shouldPlayEndVoice) this.tryPlayVoice(VoiceType.ATTACK_END, this.sounds().get(VoiceType.ATTACK_END), 1.0F, 1.0F);
    }

    private void faceRetaliationTarget(LivingEntity target) {
        float turnSpeed = (float) this.tuning().retaliationFaceTargetTurnSpeed();
        this.getLookControl().lookAt(target, turnSpeed, turnSpeed);
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        if (Math.abs(deltaX) < 1.0E-6 && Math.abs(deltaZ) < 1.0E-6) return;
        float targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0F / Math.PI) - 90.0F;
        float bodyYaw = MathHelper.stepUnwrappedAngleTowards(this.getYaw(), targetYaw, turnSpeed);
        float headYaw = MathHelper.stepUnwrappedAngleTowards(this.getHeadYaw(), targetYaw, turnSpeed);
        this.setYaw(bodyYaw);
        this.bodyYaw = bodyYaw;
        this.prevBodyYaw = bodyYaw;
        this.setHeadYaw(headYaw);
    }

    private boolean isValidRetaliationTarget(@Nullable LivingEntity candidate) {
        if (candidate == null || this.retaliationTicksRemaining <= 0) return false;
        if (!candidate.isAlive() || candidate.isRemoved()) return false;
        if (candidate.getWorld() != this.getWorld()) return false;
        if (!this.canTarget(candidate.getType())) return false;
        if (this.squaredDistanceTo(candidate) > MathHelper.square(this.tuning().retaliationRange())) return false;
        return this.canSee(candidate);
    }
}
