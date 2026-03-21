package com.oh_yeah.entity.projectile;

import com.oh_yeah.entity.ModEntityTypes;
import com.oh_yeah.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class TiansuluoProjectileEntity extends ThrownItemEntity {
    private float damage = 1.0F;

    public TiansuluoProjectileEntity(World world, LivingEntity owner) {
        super(ModEntityTypes.TIANSULUO_PROJECTILE, owner, world);
    }

    public TiansuluoProjectileEntity(World world, double x, double y, double z) {
        super(ModEntityTypes.TIANSULUO_PROJECTILE, x, y, z, world);
    }

    public TiansuluoProjectileEntity(net.minecraft.entity.EntityType<? extends TiansuluoProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TIANSULUO_EGG;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0.0F, damage);
    }

    private ParticleEffect getParticleParameters() {
        ItemStack stack = this.getStack();
        return !stack.isEmpty() && !stack.isOf(this.getDefaultItem())
                ? new ItemStackParticleEffect(ParticleTypes.ITEM, stack)
                : new ItemStackParticleEffect(ParticleTypes.ITEM, new ItemStack(this.getDefaultItem()));
    }

    @Override
    public void handleStatus(byte status) {
        if (status == EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) {
            ParticleEffect particle = this.getParticleParameters();
            for (int i = 0; i < 8; i++) {
                this.getWorld().addParticle(particle, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        entity.damage(this.getDamageSources().thrown(this, this.getOwner()), this.damage);
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES);
            this.discard();
        }
    }
}
