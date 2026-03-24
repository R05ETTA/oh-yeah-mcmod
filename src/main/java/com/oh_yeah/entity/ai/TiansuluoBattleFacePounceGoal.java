package com.oh_yeah.entity.ai;

import com.oh_yeah.entity.TiansuluoBattleFaceEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

public final class TiansuluoBattleFacePounceGoal extends Goal {
    private static final int MAX_FLIGHT_TICKS = 12;
    private static final double HITBOX_PADDING = 0.2D;

    private final TiansuluoBattleFaceEntity entity;
    private LivingEntity committedTarget;
    private int flightTicksRemaining;
    private boolean launched;
    private boolean resolved;

    public TiansuluoBattleFacePounceGoal(TiansuluoBattleFaceEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = this.entity.getRetaliationTarget();
        return this.entity.isReadyToPounce()
                && this.entity.isAttackCooldownReady()
                && this.entity.isOnGround()
                && target != null
                && this.entity.hasUsableRetaliationTarget()
                && this.entity.isWithinPounceWindow(target);
    }

    @Override
    public boolean shouldContinue() {
        return !this.resolved
                && this.entity.isReadyToPounce()
                && this.committedTarget != null
                && this.entity.getRetaliationTarget() == this.committedTarget
                && this.entity.hasUsableRetaliationTarget()
                && this.launched;
    }

    @Override
    public void start() {
        this.committedTarget = this.entity.getRetaliationTarget();
        this.flightTicksRemaining = MAX_FLIGHT_TICKS;
        this.launched = false;
        this.resolved = false;
        this.entity.beginCharge();
    }

    @Override
    public void stop() {
        this.committedTarget = null;
        this.flightTicksRemaining = 0;
        this.launched = false;
    }

    @Override
    public void tick() {
        LivingEntity target = this.entity.getRetaliationTarget();
        if (target == null || !this.entity.hasUsableRetaliationTarget() || target != this.committedTarget) {
            this.resolved = true;
            return;
        }

        this.entity.faceRetaliationTarget(target);

        if (!this.launched) {
            this.launchAt(target);
            return;
        }

        if (this.tryResolveHit(target)) {
            return;
        }

        this.flightTicksRemaining--;
        if (this.entity.isOnGround() || this.flightTicksRemaining <= 0) {
            this.resolved = true;
            this.entity.startCooldown();
        }
    }

    private void launchAt(LivingEntity target) {
        Vec3d aimPoint = this.entity.getPounceAimPoint(target);
        Vec3d origin = this.entity.getBoundingBox().getCenter();
        Vec3d delta = aimPoint.subtract(origin);
        Vec3d horizontal = new Vec3d(delta.x, 0.0D, delta.z);
        Vec3d horizontalDirection = horizontal.lengthSquared() > 1.0E-7D ? horizontal.normalize() : Vec3d.ZERO;

        double horizontalSpeed = Math.max(
                Math.max(this.entity.attackConfig().leapHorizontalSpeed(), 0.9D),
                Math.min(1.3D, horizontal.length() * 0.4D)
        );
        double verticalBase = Math.max(this.entity.attackConfig().leapVerticalSpeed(), 0.42D);
        double verticalOffset = MathHelper.clamp(delta.y * 0.25D, -0.18D, 0.3D);
        double verticalSpeed = MathHelper.clamp(verticalBase + verticalOffset, 0.24D, 0.95D);

        this.entity.setVelocity(horizontalDirection.x * horizontalSpeed, verticalSpeed, horizontalDirection.z * horizontalSpeed);
        this.entity.velocityModified = true;
        this.launched = true;
    }

    private boolean tryResolveHit(LivingEntity target) {
        Box hitBox = this.entity.getBoundingBox().expand(HITBOX_PADDING);
        if (!hitBox.intersects(target.getBoundingBox().expand(HITBOX_PADDING))) {
            return false;
        }

        this.resolved = true;
        if (this.entity.tryAttack(target)) {
            this.entity.finishSuccessfulRetaliation();
        } else {
            this.entity.startCooldown();
        }
        return true;
    }
}
