package com.oh_yeah.entity;

import com.oh_yeah.OhYeah;
import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.config.VariantConfig;
import com.oh_yeah.entity.projectile.TiansuluoProjectileEntity;
import com.oh_yeah.entity.species.FoodPreferenceResult;
import com.oh_yeah.entity.species.ModSpeciesProfiles;
import com.oh_yeah.entity.species.SpeciesSoundProfile;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.TiansuluoVoiceType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TiansuluoEntity extends AnimalEntity implements RangedAttackMob {
    private enum RetaliationState {
        IDLE,
        PENDING_RETALIATION_DECLARE,
        RETALIATING
    }

    private static final TrackedData<Integer> VARIANT_INDEX = DataTracker.registerData(TiansuluoEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final String TAG_PLAYED_VOICES = "PlayedVoices";
    private static final String TAG_VOICE_COOLDOWNS = "VoiceCooldowns";

    private final Set<TiansuluoVoiceType> playedOneShotVoices = new HashSet<>();
    private final Map<TiansuluoVoiceType, Long> lastVoiceTicks = new EnumMap<>(TiansuluoVoiceType.class);
    private @Nullable TiansuluoVoiceType currentVoiceType;
    private long currentVoiceEndTick;
    private boolean ambientRareCallNext;
    private boolean wasBabyLastTick;
    private boolean wasTemptedByPlayer;
    private boolean wasNoticingPlayer;
    private RetaliationState retaliationState = RetaliationState.IDLE;
    private @Nullable LivingEntity retaliationTarget;
    private int retaliationTicksRemaining;
    private int retaliationBurstShotsFired;
    private int retaliationBurstCooldownTicks;
    private int retaliationDeclareTicksRemaining;

    public TiansuluoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        TiansuluoTuningConfig tuning = OhYeahConfigManager.getTiansuluoConfig().tuning();
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, tuning.maxHealth())
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, tuning.movementSpeed())
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, tuning.followRange());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT_INDEX, TiansuluoVariant.SCARF_PINK.ordinal());
    }

    @Override
    protected void initGoals() {
        TiansuluoTuningConfig tuning = this.tuning();
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new ProjectileAttackGoal(this, tuning.projectileAttackGoalSpeed(), tuning.retaliationBurstIntervalTicks(), (float) tuning.retaliationRange()));
        this.goalSelector.add(2, new AnimalMateGoal(this, tuning.mateGoalSpeed()));
        this.goalSelector.add(3, new TemptGoal(this, tuning.temptGoalSpeed(), this::isBreedingItem, false));
        this.goalSelector.add(4, new FollowParentGoal(this, tuning.followParentGoalSpeed()));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, tuning.wanderGoalSpeed()));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        SpeciesConfig cfg = OhYeahConfigManager.getTiansuluoConfig();
        TiansuluoVariant variant = selectSpawnVariant(world, this.getBlockPos(), spawnReason, world.getRandom(), cfg);
        this.setVariant(variant);

        if (spawnReason == SpawnReason.SPAWN_EGG || spawnReason == SpawnReason.BREEDING) {
            this.setBaby(true);
            this.setBreedingAge(this.tuning().babyGrowthAgeTicks());
        }

        if (spawnReason != SpawnReason.BREEDING) {
            this.tryPlayVoice(TiansuluoVoiceType.SPAWN, this.sounds().spawn(), 1.0F, 1.0F);
        }
        return data;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            return;
        }
        this.updateRetaliationTarget();
        this.clearFinishedVoice();
        this.updateGrowthVoice();
        this.updateStateVoices();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && !this.getWorld().isClient) {
            EntityType<?> attackerType = null;
            if (source.getAttacker() != null) {
                attackerType = source.getAttacker().getType();
            }
            if (source.getAttacker() instanceof LivingEntity attacker && attacker != this && this.canTarget(attackerType)) {
                this.rememberRetaliationTarget(attacker);
            }
        }
        return damaged;
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        if (this.retaliationState != RetaliationState.RETALIATING || this.retaliationBurstCooldownTicks > 0) {
            return;
        }
        this.faceRetaliationTarget(target);
        Vec3d muzzlePos = this.getProjectileMuzzlePos();
        TiansuluoProjectileEntity projectile = new TiansuluoProjectileEntity(this.getWorld(), muzzlePos.x, muzzlePos.y, muzzlePos.z);
        projectile.setOwner(this);
        projectile.setDamage(this.tuning().retaliationProjectileDamage());
        double targetY = target.getEyeY() - this.tuning().retaliationTargetEyeOffset();
        double deltaX = target.getX() - muzzlePos.x;
        double deltaY = targetY - projectile.getY();
        double deltaZ = target.getZ() - muzzlePos.z;
        double arcBoost = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 0.2F;
        projectile.setVelocity(deltaX, deltaY + arcBoost, deltaZ, this.tuning().retaliationProjectileSpeed(), this.tuning().retaliationProjectileDivergence());
        this.tryPlayVoice(TiansuluoVoiceType.ATTACK_SHOT, this.sounds().attackShot(), 1.0F, 1.0F);
        this.playSound(SoundEvents.ENTITY_SNOW_GOLEM_SHOOT, 1.0F, 0.4F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.getWorld().spawnEntity(projectile);
        this.retaliationBurstShotsFired++;
        if (this.retaliationBurstShotsFired >= this.tuning().retaliationBurstShots()) {
            this.retaliationBurstShotsFired = 0;
            this.retaliationBurstCooldownTicks = this.tuning().retaliationBurstCooldownTicks();
        }
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return this.getFoodPreference(stack).isFood();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        FoodPreferenceResult preference = this.getFoodPreference(stack);
        ActionResult result = super.interactMob(player, hand);

        if (!this.getWorld().isClient && result.isAccepted() && preference.isFood()) {
            this.applyGrowthFromFood(preference);
            TiansuluoVoiceType voiceType = preference == FoodPreferenceResult.FAVORITE ? TiansuluoVoiceType.EAT_FAVORITE : TiansuluoVoiceType.EAT;
            SoundEvent eatSound = preference == FoodPreferenceResult.FAVORITE ? this.sounds().eatFavorite() : this.sounds().eat();
            this.tryPlayVoice(voiceType, eatSound, 1.0F, preference == FoodPreferenceResult.FAVORITE ? 1.08F : 1.0F);
        }
        return result;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        TiansuluoEntity child = ModEntityTypes.TIANSULUO.create(serverWorld);
        if (child != null) {
            child.setBaby(true);
            child.setBreedingAge(this.tuning().babyGrowthAgeTicks());
            child.setVariant(this.random.nextBoolean() ? this.getVariant() : ((TiansuluoEntity) passiveEntity).getVariant());
            this.tryPlayVoice(TiansuluoVoiceType.BREED_SUCCESS, this.sounds().breedSuccess(), 1.0F, 1.0F);
        }
        return child;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.getWorld().isClient && !this.isBaby()) {
            this.dropItem(ModItems.CHIPS);
            this.dropItem(ModItems.TIANSULUO_EGG);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        TiansuluoVoiceType type = this.ambientRareCallNext ? TiansuluoVoiceType.RARE_CALL : TiansuluoVoiceType.AMBIENT;
        SoundEvent event = this.ambientRareCallNext ? this.sounds().rareCall() : this.sounds().ambient();
        this.ambientRareCallNext = this.random.nextInt(this.tuning().rareCallChanceDivisor()) == 0;
        return this.beginVoice(type) ? event : null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return this.beginVoice(TiansuluoVoiceType.HURT) ? this.sounds().hurt() : null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return this.beginVoice(TiansuluoVoiceType.DEATH) ? this.sounds().death() : null;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(this.sounds().step(), this.tuning().stepSoundVolume(), 1.0F);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant().ordinal());

        NbtCompound played = new NbtCompound();
        for (TiansuluoVoiceType type : this.playedOneShotVoices) {
            played.putBoolean(type.configKey(), true);
        }
        nbt.put(TAG_PLAYED_VOICES, played);

        NbtCompound cooldowns = new NbtCompound();
        for (Map.Entry<TiansuluoVoiceType, Long> entry : this.lastVoiceTicks.entrySet()) {
            cooldowns.putLong(entry.getKey().configKey(), entry.getValue());
        }
        nbt.put(TAG_VOICE_COOLDOWNS, cooldowns);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        int variantIndex = nbt.getInt("Variant");
        if (variantIndex < 0 || variantIndex >= TiansuluoVariant.values().length) {
            variantIndex = TiansuluoVariant.SCARF_PINK.ordinal();
        }
        this.dataTracker.set(VARIANT_INDEX, variantIndex);

        this.playedOneShotVoices.clear();
        NbtCompound played = nbt.getCompound(TAG_PLAYED_VOICES);
        for (TiansuluoVoiceType type : TiansuluoVoiceType.values()) {
            if (played.getBoolean(type.configKey())) {
                this.playedOneShotVoices.add(type);
            }
        }

        this.lastVoiceTicks.clear();
        NbtCompound cooldowns = nbt.getCompound(TAG_VOICE_COOLDOWNS);
        for (TiansuluoVoiceType type : TiansuluoVoiceType.values()) {
            if (cooldowns.contains(type.configKey())) {
                this.lastVoiceTicks.put(type, cooldowns.getLong(type.configKey()));
            }
        }
    }

    public TiansuluoVariant getVariant() {
        int index = this.dataTracker.get(VARIANT_INDEX);
        if (index < 0 || index >= TiansuluoVariant.values().length) {
            return TiansuluoVariant.SCARF_PINK;
        }
        return TiansuluoVariant.values()[index];
    }

    public void setVariant(TiansuluoVariant variant) {
        this.dataTracker.set(VARIANT_INDEX, variant.ordinal());
    }

    public Identifier getTextureId() {
        VariantConfig cfg = OhYeahConfigManager.getTiansuluoConfig().getVariantConfig(this.getVariant());
        String texture = cfg != null ? cfg.texture() : "textures/entity/tiansuluo.png";
        if (texture.contains(":")) {
            Identifier parsed = Identifier.tryParse(texture);
            return parsed != null ? parsed : Identifier.of(OhYeah.MOD_ID, "textures/entity/tiansuluo.png");
        }
        return Identifier.of(OhYeah.MOD_ID, texture);
    }

    public static TiansuluoVariant selectSpawnVariant(ServerWorldAccess world, BlockPos pos, SpawnReason spawnReason, Random random, SpeciesConfig cfg) {
        if (spawnReason == SpawnReason.BREEDING || spawnReason == SpawnReason.SPAWN_EGG) {
            return cfg.defaultVariant();
        }

        Optional<RegistryKey<Biome>> biomeKey = world.getBiome(pos).getKey();
        int totalWeight = 0;
        Map<TiansuluoVariant, Integer> candidates = new EnumMap<>(TiansuluoVariant.class);

        for (TiansuluoVariant variant : TiansuluoVariant.values()) {
            VariantConfig vc = cfg.getVariantConfig(variant);
            if (vc == null || !vc.enabled()) {
                continue;
            }
            if (vc.weight() <= 0) {
                continue;
            }
            if (pos.getY() < vc.minY() || pos.getY() > vc.maxY()) {
                continue;
            }
            if (world.getBaseLightLevel(pos, 0) < vc.minLight()) {
                continue;
            }
            if (biomeKey.isPresent()) {
                String currentBiome = biomeKey.get().getValue().toString();
                if (!vc.allowedBiomes().contains(currentBiome)) {
                    continue;
                }
            }
            candidates.put(variant, vc.weight());
            totalWeight += vc.weight();
        }

        if (totalWeight <= 0) {
            return cfg.defaultVariant();
        }
        int pick = random.nextInt(totalWeight);
        for (Map.Entry<TiansuluoVariant, Integer> entry : candidates.entrySet()) {
            pick -= entry.getValue();
            if (pick < 0) {
                return entry.getKey();
            }
        }
        return cfg.defaultVariant();
    }

    private void updateGrowthVoice() {
        boolean babyNow = this.isBaby();
        if (this.wasBabyLastTick && !babyNow) {
            this.tryPlayVoice(TiansuluoVoiceType.GROW_UP, this.sounds().growUp(), 1.0F, 1.05F);
        }
        this.wasBabyLastTick = babyNow;
    }

    private void updateStateVoices() {
        boolean tempted = this.isTemptedByNearbyPlayer();
        if (tempted && !this.wasTemptedByPlayer) {
            this.tryPlayVoice(TiansuluoVoiceType.TEMPTED, this.sounds().tempted(), 1.0F, 1.0F);
        }
        this.wasTemptedByPlayer = tempted;

        boolean noticing = this.isNoticingNearbyPlayer();
        if (noticing && !this.wasNoticingPlayer) {
            this.tryPlayVoice(TiansuluoVoiceType.NOTICE_PLAYER, this.sounds().noticePlayer(), 1.0F, 1.0F);
        }
        this.wasNoticingPlayer = noticing;

    }

    private FoodPreferenceResult getFoodPreference(ItemStack stack) {
        return ModSpeciesProfiles.TIANSULUO_FOOD.getPreference(stack);
    }

    private void applyGrowthFromFood(FoodPreferenceResult preference) {
        if (!this.isBaby()) {
            return;
        }
        if (preference == FoodPreferenceResult.FAVORITE) {
            this.setBreedingAge(0);
            return;
        }
        if (preference == FoodPreferenceResult.LIKED) {
            this.setBreedingAge(Math.min(0, this.getBreedingAge() + this.tuning().likedFoodGrowthStepTicks()));
        }
    }

    private boolean isTemptedByNearbyPlayer() {
        PlayerEntity player = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), this.tuning().temptedPlayerRange(), entity ->
                entity instanceof PlayerEntity candidate
                        && candidate.isAlive()
                        && this.canSee(candidate)
                        && (this.isBreedingItem(candidate.getMainHandStack()) || this.isBreedingItem(candidate.getOffHandStack()))
        );
        return player != null;
    }

    private boolean isNoticingNearbyPlayer() {
        PlayerEntity player = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), this.tuning().noticePlayerRange(), entity ->
                entity instanceof PlayerEntity candidate && candidate.isAlive() && this.canSee(candidate)
        );
        return player != null;
    }

    private Vec3d getProjectileMuzzlePos() {
        Vec3d forward = Vec3d.fromPolar(0.0F, this.getHeadYaw()).normalize();
        double horizontalOffset = this.getWidth() * 0.5D + this.tuning().projectileFrontOffset();
        double x = this.getX() + forward.x * horizontalOffset;
        double y = this.getY() + this.getHeight() * this.tuning().projectileMuzzleHeightRatio();
        double z = this.getZ() + forward.z * horizontalOffset;
        return new Vec3d(x, y, z);
    }

    private TiansuluoTuningConfig tuning() {
        return OhYeahConfigManager.getTiansuluoConfig().tuning();
    }

    private SpeciesSoundProfile sounds() {
        return ModSpeciesProfiles.soundsFor(this.getVariant());
    }

    private boolean tryPlayVoice(TiansuluoVoiceType type, SoundEvent sound, float volume, float pitch) {
        if (!this.beginVoice(type)) {
            return false;
        }
        this.playSound(sound, volume, pitch);
        return true;
    }

    private boolean beginVoice(TiansuluoVoiceType requested) {
        this.clearFinishedVoice();
        long now = this.getWorld().getTime();

        if (requested.isOneShot() && this.playedOneShotVoices.contains(requested)) {
            return false;
        }

        int intervalTicks = requested.isOneShot() ? 0 : OhYeahConfigManager.getTiansuluoConfig().intervalTicks(requested.configKey());
        long lastTick = this.lastVoiceTicks.getOrDefault(requested, Long.MIN_VALUE / 4);
        if (!requested.isOneShot() && now - lastTick < intervalTicks) {
            return false;
        }

        if (this.currentVoiceType != null && this.currentVoiceEndTick > now) {
            if (this.currentVoiceType.priority() >= requested.priority()) {
                return false;
            }
            this.currentVoiceType = null;
            this.currentVoiceEndTick = 0L;
        }

        this.currentVoiceType = requested;
        this.currentVoiceEndTick = now + this.voiceDurationTicks(requested);
        this.lastVoiceTicks.put(requested, now);
        if (requested.isOneShot()) {
            this.playedOneShotVoices.add(requested);
        }
        return true;
    }

    private int voiceDurationTicks(TiansuluoVoiceType type) {
        if (type == TiansuluoVoiceType.ATTACK_DECLARE) {
            return this.tuning().attackDeclareDurationTicks();
        }
        return type.durationTicks();
    }

    private void clearFinishedVoice() {
        long now = this.getWorld().getTime();
        if (this.currentVoiceType != null && this.currentVoiceEndTick <= now) {
            this.currentVoiceType = null;
            this.currentVoiceEndTick = 0L;
        }
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
        if (this.retaliationBurstCooldownTicks > 0) {
            this.retaliationBurstCooldownTicks--;
        }
        if (this.retaliationTicksRemaining > 0) {
            this.retaliationTicksRemaining--;
        }

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
        if (this.retaliationState == RetaliationState.IDLE) {
            return;
        }
        if (this.retaliationState == RetaliationState.PENDING_RETALIATION_DECLARE) {
            if (this.retaliationDeclareTicksRemaining > 0) {
                this.retaliationDeclareTicksRemaining--;
                if (this.retaliationDeclareTicksRemaining <= 0) {
                    this.retaliationState = RetaliationState.RETALIATING;
                }
                return;
            }
            if (this.isVoiceActive(TiansuluoVoiceType.HURT)) {
                return;
            }
            this.faceRetaliationTarget(target);
            this.tryPlayVoice(TiansuluoVoiceType.ATTACK_DECLARE, this.sounds().attackDeclare(), 1.0F, 1.0F);
            this.retaliationDeclareTicksRemaining = this.tuning().attackDeclareDurationTicks();
            if (this.retaliationDeclareTicksRemaining <= 0) {
                this.retaliationState = RetaliationState.RETALIATING;
            }
        }
    }

    private void clearRetaliationState(boolean playEndVoice) {
        boolean shouldPlayEndVoice = playEndVoice && this.retaliationState == RetaliationState.RETALIATING;
        this.retaliationTarget = null;
        this.retaliationState = RetaliationState.IDLE;
        this.retaliationBurstShotsFired = 0;
        this.retaliationBurstCooldownTicks = 0;
        this.retaliationDeclareTicksRemaining = 0;
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
        if (shouldPlayEndVoice) {
            this.tryPlayVoice(TiansuluoVoiceType.ATTACK_END, this.sounds().attackEnd(), 1.0F, 1.0F);
        }
    }

    private boolean isVoiceActive(TiansuluoVoiceType type) {
        this.clearFinishedVoice();
        return this.currentVoiceType == type;
    }

    private void faceRetaliationTarget(LivingEntity target) {
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

    private boolean isValidRetaliationTarget(@Nullable LivingEntity candidate) {
        if (candidate == null || this.retaliationTicksRemaining <= 0) {
            return false;
        }
        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }
        if (candidate.getWorld() != this.getWorld()) {
            return false;
        }
        if (!this.canTarget(candidate.getType())) {
            return false;
        }
        if (this.squaredDistanceTo(candidate) > MathHelper.square(this.tuning().retaliationRange())) {
            return false;
        }
        return this.canSee(candidate);
    }
}
