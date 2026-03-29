package com.oh_yeah.entity;

import com.oh_yeah.OhYeah;
import com.oh_yeah.block.LuanluanEggBlock;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.TiansuluoFeatureSetConfig;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.config.VariantConfig;
import com.oh_yeah.entity.species.FoodPreferenceResult;
import com.oh_yeah.entity.species.ModSpeciesProfiles;
import com.oh_yeah.entity.species.SpeciesSoundProfile;
import com.oh_yeah.sound.TiansuluoVoiceType;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractTiansuluoEntity extends AnimalEntity implements EggLayingSpecies {
    private static final TrackedData<Boolean> HAS_CARRIED_EGG_BLOCK = DataTracker.registerData(AbstractTiansuluoEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final String TAG_PLAYED_VOICES = "PlayedVoices";
    private static final String TAG_VOICE_COOLDOWNS = "VoiceCooldowns";
    private static final String TAG_SILENCED_BY_SHEARS = "SilencedByShears";
    private static final String TAG_HAS_CARRIED_EGG_BLOCK = "HasLuanluanBlock";
    private static final String TAG_EGG_BLOCK_TARGET_X = "LuanluanBlockTargetX";
    private static final String TAG_EGG_BLOCK_TARGET_Y = "LuanluanBlockTargetY";
    private static final String TAG_EGG_BLOCK_TARGET_Z = "LuanluanBlockTargetZ";
    private static final String TAG_EGG_BLOCK_PLACING_COUNTER = "LuanluanBlockPlacingCounter";
    private static final String TAG_EGG_BLOCK_PLAYER_UUID = "LuanluanBlockPlayerUuid";

    private final Set<TiansuluoVoiceType> playedOneShotVoices = new HashSet<>();
    private final Map<TiansuluoVoiceType, Long> lastVoiceTicks = new EnumMap<>(TiansuluoVoiceType.class);
    private @Nullable TiansuluoVoiceType currentVoiceType;
    private long currentVoiceEndTick;
    private boolean ambientRareCallNext;
    private boolean wasBabyLastTick;
    private boolean wasTemptedByPlayer;
    private boolean wasNoticingPlayer;
    private boolean silencedByShears;
    private @Nullable BlockPos eggBlockTargetPos;
    private int eggBlockPlacingCounter;
    private @Nullable UUID eggBlockPlayerUuid;

    protected AbstractTiansuluoEntity(EntityType<? extends AnimalEntity> entityType, World world) { super(entityType, world); }
    protected abstract SpeciesConfig speciesConfig();
    protected abstract String soundProfileKey();
    protected abstract String speciesKey();
    protected void onSpeciesInitialize(ServerWorldAccess world, LocalDifficulty difficulty, net.minecraft.entity.SpawnReason spawnReason) {}
    protected void tickSpeciesMovement() {}

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(HAS_CARRIED_EGG_BLOCK, false);
    }

    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, net.minecraft.entity.SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        this.onSpeciesInitialize(world, difficulty, spawnReason);
        if (spawnReason == net.minecraft.entity.SpawnReason.SPAWN_EGG || spawnReason == net.minecraft.entity.SpawnReason.BREEDING) {
            this.setBaby(true);
            this.setBreedingAge(this.tuning().babyGrowthAgeTicks());
        }
        return data;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (this.getWorld().isClient) {
            if (this.shouldDisplayCarriedEggBlockParticles()) this.spawnCarriedEggBlockParticles();
            return;
        }
        this.updateEggBlockAttractedPlayer();
        this.tickSpeciesMovement();
        this.clearFinishedVoice();
        this.updateGrowthVoice();
        this.updateStateVoices();
        this.updateCarryEggVoice();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) { return this.getFoodPreference(stack).isFood(); }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.isShearMuteInteractionEnabledForSpecies() && stack.isOf(Items.SHEARS)) {
            if (this.getWorld().isClient) return !this.silencedByShears ? ActionResult.CONSUME : ActionResult.PASS;
            return this.tryUseShears(player, hand, stack) ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        FoodPreferenceResult preference = this.getFoodPreference(stack);
        ActionResult result = super.interactMob(player, hand);
        if (!this.getWorld().isClient && result.isAccepted() && preference.isFood()) {
            this.applyGrowthFromFood(preference);
            if (this.silencedByShears) this.setSilencedByShears(false);
            TiansuluoVoiceType voiceType = preference == FoodPreferenceResult.FAVORITE ? TiansuluoVoiceType.EAT_FAVORITE : TiansuluoVoiceType.EAT;
            SoundEvent eatSound = preference == FoodPreferenceResult.FAVORITE ? this.sounds().eatFavorite() : this.sounds().eat();
            this.tryPlayVoice(voiceType, eatSound, 1.0F, preference == FoodPreferenceResult.FAVORITE ? 1.08F : 1.0F);
        }
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (this.getWorld().isClient || this.isBaby()) return;
        TiansuluoFeatureSetConfig.LootFeatureConfig loot = this.resolveActiveLootConfig();
        if (loot == null || !loot.enabled()) return;
        for (String itemId : loot.adultDropItemIds()) {
            Item item = resolveItem(itemId);
            if (item != Items.AIR) this.dropItem(item);
        }
    }

    @Override protected @Nullable SoundEvent getAmbientSound() {
        TiansuluoVoiceType type = this.ambientRareCallNext ? TiansuluoVoiceType.RARE_CALL : TiansuluoVoiceType.AMBIENT;
        SoundEvent event = this.ambientRareCallNext ? this.sounds().rareCall() : this.sounds().ambient();
        this.ambientRareCallNext = this.random.nextInt(this.tuning().rareCallChanceDivisor()) == 0;
        return this.beginVoice(type) ? event : null;
    }
    @Override protected @Nullable SoundEvent getHurtSound(DamageSource source) { return this.beginVoice(TiansuluoVoiceType.HURT) ? this.sounds().hurt() : null; }
    @Override protected @Nullable SoundEvent getDeathSound() { return this.beginVoice(TiansuluoVoiceType.DEATH) ? this.sounds().death() : null; }
    @Override public float getSoundVolume() { return this.resolveSoundVolume(super.getSoundVolume()); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {}

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        NbtCompound played = new NbtCompound();
        for (TiansuluoVoiceType type : this.playedOneShotVoices) played.putBoolean(type.configKey(), true);
        nbt.put(TAG_PLAYED_VOICES, played);
        NbtCompound cooldowns = new NbtCompound();
        for (Map.Entry<TiansuluoVoiceType, Long> entry : this.lastVoiceTicks.entrySet()) cooldowns.putLong(entry.getKey().configKey(), entry.getValue());
        nbt.put(TAG_VOICE_COOLDOWNS, cooldowns);
        nbt.putBoolean(TAG_SILENCED_BY_SHEARS, this.silencedByShears);
        nbt.putBoolean(TAG_HAS_CARRIED_EGG_BLOCK, this.hasCarriedEggBlock());
        if (this.eggBlockTargetPos != null) {
            nbt.putInt(TAG_EGG_BLOCK_TARGET_X, this.eggBlockTargetPos.getX());
            nbt.putInt(TAG_EGG_BLOCK_TARGET_Y, this.eggBlockTargetPos.getY());
            nbt.putInt(TAG_EGG_BLOCK_TARGET_Z, this.eggBlockTargetPos.getZ());
        }
        nbt.putInt(TAG_EGG_BLOCK_PLACING_COUNTER, this.eggBlockPlacingCounter);
        if (this.eggBlockPlayerUuid != null) nbt.putUuid(TAG_EGG_BLOCK_PLAYER_UUID, this.eggBlockPlayerUuid);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.playedOneShotVoices.clear();
        NbtCompound played = nbt.getCompound(TAG_PLAYED_VOICES);
        for (TiansuluoVoiceType type : TiansuluoVoiceType.values()) if (played.getBoolean(type.configKey())) this.playedOneShotVoices.add(type);
        this.lastVoiceTicks.clear();
        NbtCompound cooldowns = nbt.getCompound(TAG_VOICE_COOLDOWNS);
        for (TiansuluoVoiceType type : TiansuluoVoiceType.values()) if (cooldowns.contains(type.configKey())) this.lastVoiceTicks.put(type, cooldowns.getLong(type.configKey()));
        this.silencedByShears = nbt.getBoolean(TAG_SILENCED_BY_SHEARS);
        this.setHasCarriedEggBlock(nbt.getBoolean(TAG_HAS_CARRIED_EGG_BLOCK));
        if (nbt.contains(TAG_EGG_BLOCK_TARGET_X) && nbt.contains(TAG_EGG_BLOCK_TARGET_Y) && nbt.contains(TAG_EGG_BLOCK_TARGET_Z)) {
            this.eggBlockTargetPos = new BlockPos(nbt.getInt(TAG_EGG_BLOCK_TARGET_X), nbt.getInt(TAG_EGG_BLOCK_TARGET_Y), nbt.getInt(TAG_EGG_BLOCK_TARGET_Z));
        } else this.eggBlockTargetPos = null;
        this.eggBlockPlacingCounter = Math.max(0, nbt.getInt(TAG_EGG_BLOCK_PLACING_COUNTER));
        this.eggBlockPlayerUuid = nbt.containsUuid(TAG_EGG_BLOCK_PLAYER_UUID) ? nbt.getUuid(TAG_EGG_BLOCK_PLAYER_UUID) : null;
    }

    public void markOneShotVoiceAsPlayed(TiansuluoVoiceType type) { if (type.isOneShot()) this.playedOneShotVoices.add(type); }
    public Identifier getTextureId() {
        VariantConfig cfg = this.speciesConfig().defaultVariantConfig();
        String texture = cfg != null ? cfg.texture() : "textures/entity/tiansuluo.png";
        if (texture.contains(":")) {
            Identifier parsed = Identifier.tryParse(texture);
            return parsed != null ? parsed : Identifier.of(OhYeah.MOD_ID, "textures/entity/tiansuluo.png");
        }
        return Identifier.of(OhYeah.MOD_ID, texture);
    }

    protected TiansuluoTuningConfig tuning() { return this.speciesConfig().tuning(); }
    protected SpeciesSoundProfile sounds() { return ModSpeciesProfiles.soundsFor(this.soundProfileKey()); }
    protected TiansuluoFeatureSetConfig.BreedingFeatureConfig breedingConfig() {
        TiansuluoFeatureSetConfig.BreedingFeatureConfig breeding = this.speciesConfig().archetype().features().breeding();
        return breeding != null ? breeding : TiansuluoFeatureSetConfig.BreedingFeatureConfig.createDefaults();
    }
    protected boolean usesEggBlockBreeding() { return this.breedingConfig().usesEggBlock(); }
    protected AnimalMateGoal createMateForEggBlockGoal(double speed) { return new MateForEggBlockGoal(this, speed); }
    protected Goal createLayEggBlockGoal() { return new LayEggBlockGoal(); }
    protected TemptGoal createTemptWhileAvailableGoal(double speed) { return new TemptWhileAvailableGoal(speed); }
    protected boolean tryPlayVoice(TiansuluoVoiceType type, SoundEvent sound, float volume, float pitch) { if (!this.beginVoice(type)) return false; this.playSpeciesSound(sound, volume, pitch); return true; }
    public boolean isSilencedByShears() { return this.silencedByShears; }
    public void setSilencedByShears(boolean silencedByShears) { this.silencedByShears = silencedByShears; }
    protected float resolveSoundVolume(float requestedVolume) { return this.silencedByShears ? 0.0F : requestedVolume; }
    protected void playSpeciesSound(SoundEvent sound, float volume, float pitch) { this.playSound(sound, this.resolveSoundVolume(volume), pitch); }
    protected boolean isVoiceActive(TiansuluoVoiceType type) { this.clearFinishedVoice(); return this.currentVoiceType == type; }
    protected boolean shouldDisplayCarriedEggBlockParticles() { return this.usesEggBlockBreeding() && this.hasCarriedEggBlock() && !this.isBaby() && this.breedingConfig().showCarriedParticles(); }

    protected void spawnCarriedEggBlockParticles() {
        if (this.age % 10 != 0) return;
        World world = this.getWorld();
        world.addParticle(ParticleTypes.HEART, this.getParticleX(0.6D), this.getBodyY(0.7D), this.getParticleZ(0.6D), (this.random.nextDouble() - 0.5D) * 0.02D, 0.02D + this.random.nextDouble() * 0.02D, (this.random.nextDouble() - 0.5D) * 0.02D);
    }

    protected void updateEggBlockAttractedPlayer() { if (this.usesEggBlockBreeding() && this.hasCarriedEggBlock() && this.eggBlockPlayerUuid != null && this.getEggBlockAttractedPlayer() == null) this.eggBlockPlayerUuid = null; }
    protected @Nullable PlayerEntity getEggBlockAttractedPlayer() {
        if (this.eggBlockPlayerUuid == null) return null;
        PlayerEntity player = this.getWorld().getPlayerByUuid(this.eggBlockPlayerUuid);
        return player == null || !player.isAlive() || player.isSpectator() || player.getWorld() != this.getWorld() ? null : player;
    }
    protected boolean isEggBlockTargetNearPlayer(BlockPos targetPos) {
        PlayerEntity player = this.getEggBlockAttractedPlayer();
        if (player == null) return true;
        int radius = Math.max(2, this.tuning().luanluanBlockSearchRadius());
        int maxDistance = radius + 2;
        return targetPos.getSquaredDistance(player.getBlockPos()) <= (double) (maxDistance * maxDistance);
    }
    protected @Nullable BlockPos findNearbyEggBlockTarget() {
        TiansuluoTuningConfig tuning = this.tuning();
        int radius = Math.max(2, tuning.luanluanBlockSearchRadius());
        BlockPos center = this.getEggBlockAttractedPlayer() != null ? this.getEggBlockAttractedPlayer().getBlockPos() : this.getBlockPos();
        World world = this.getWorld();
        int attempts = Math.max(32, radius * 6);
        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + this.random.nextBetween(-radius, radius);
            int z = center.getZ() + this.random.nextBetween(-radius, radius);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            if (y < world.getBottomY()) continue;
            BlockPos candidate = new BlockPos(x, y, z);
            if (this.canPlaceEggBlockAt(candidate)) return candidate;
        }
        BlockPos fallback = this.getBlockPos().down();
        return this.canPlaceEggBlockAt(fallback) ? fallback : null;
    }
    protected boolean canPlaceEggBlockAt(BlockPos basePos) {
        World world = this.getWorld();
        if (!world.getWorldBorder().contains(basePos)) return false;
        BlockState baseState = world.getBlockState(basePos);
        if (!baseState.isSideSolidFullSquare(world, basePos, Direction.UP)) return false;
        BlockPos eggBlockPos = basePos.up();
        return world.getBlockState(eggBlockPos).isAir() && world.getFluidState(eggBlockPos).isEmpty();
    }
    protected void placeEggBlock(BlockPos basePos) {
        LuanluanEggBlock eggBlock = this.resolveEggBlock();
        if (eggBlock == null) return;
        World world = this.getWorld();
        BlockPos eggBlockPos = basePos.up();
        int blockCount = this.getRandom().nextInt(4) + 1;
        BlockState eggBlockState = eggBlock.getDefaultState().with(LuanluanEggBlock.BLOCKS, blockCount);
        world.setBlockState(eggBlockPos, eggBlockState, 3);
        world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_CALCITE_PLACE, SoundCategory.BLOCKS, 0.3F, 0.9F + world.random.nextFloat() * 0.2F);
        world.emitGameEvent(GameEvent.BLOCK_PLACE, eggBlockPos, GameEvent.Emitter.of(this, eggBlockState));
        PlayerEntity player = this.getEggBlockAttractedPlayer();
        if (player != null) player.sendMessage(Text.translatable(this.breedingConfig().placedMessageKey(), blockCount), true);
    }
    protected @Nullable LuanluanEggBlock resolveEggBlock() {
        Identifier id = Identifier.tryParse(this.breedingConfig().eggBlockId());
        if (id == null) return null;
        Block block = Registries.BLOCK.get(id);
        return block instanceof LuanluanEggBlock eggBlock ? eggBlock : null;
    }
    protected @Nullable TiansuluoFeatureSetConfig.LootFeatureConfig resolveActiveLootConfig() {
        VariantConfig variantConfig = this.speciesConfig().defaultVariantConfig();
        TiansuluoFeatureSetConfig.LootFeatureConfig variantLoot = variantConfig != null && variantConfig.features() != null ? variantConfig.features().loot() : null;
        if (variantLoot != null) return variantLoot;
        return this.speciesConfig().archetype().features().loot();
    }
    protected static Item resolveItem(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return Items.AIR;
        Item item = Registries.ITEM.get(id);
        return item != null ? item : Items.AIR;
    }

    @Override public boolean hasCarriedEggBlock() { return this.dataTracker.get(HAS_CARRIED_EGG_BLOCK); }
    @Override public void setHasCarriedEggBlock(boolean hasCarriedEggBlock) {
        this.dataTracker.set(HAS_CARRIED_EGG_BLOCK, hasCarriedEggBlock);
        if (!hasCarriedEggBlock) { this.eggBlockTargetPos = null; this.eggBlockPlacingCounter = 0; this.eggBlockPlayerUuid = null; } else this.eggBlockPlacingCounter = 0;
    }
    @Override public @Nullable BlockPos getCarriedEggBlockTargetPos() { return this.eggBlockTargetPos; }
    @Override public void setCarriedEggBlockTargetPos(@Nullable BlockPos pos) { this.eggBlockTargetPos = pos; }
    @Override public int getEggBlockPlacingCounter() { return this.eggBlockPlacingCounter; }
    @Override public void setEggBlockPlacingCounter(int counter) { this.eggBlockPlacingCounter = Math.max(0, counter); }
    @Override public @Nullable UUID getEggBlockAttractedPlayerUuid() { return this.eggBlockPlayerUuid; }
    @Override public void setEggBlockAttractedPlayerUuid(@Nullable UUID uuid) { this.eggBlockPlayerUuid = uuid; }

    private void updateGrowthVoice() {
        boolean babyNow = this.isBaby();
        if (this.wasBabyLastTick && !babyNow) this.tryPlayVoice(TiansuluoVoiceType.GROW_UP, this.sounds().growUp(), 1.0F, 1.05F);
        this.wasBabyLastTick = babyNow;
    }

    private void updateStateVoices() {
        boolean tempted = this.isTemptedByNearbyPlayer();
        if (tempted && !this.wasTemptedByPlayer) this.tryPlayVoice(TiansuluoVoiceType.TEMPTED, this.sounds().tempted(), 1.0F, 1.0F);
        this.wasTemptedByPlayer = tempted;
        boolean noticing = this.isNoticingNearbyPlayer();
        if (noticing && !this.wasNoticingPlayer) this.tryPlayVoice(TiansuluoVoiceType.NOTICE_PLAYER, this.sounds().noticePlayer(), 1.0F, 1.0F);
        this.wasNoticingPlayer = noticing;
    }

    private void updateCarryEggVoice() {
        if (!this.usesEggBlockBreeding() || !this.hasCarriedEggBlock() || this.isBaby()) return;
        this.tryPlayVoice(TiansuluoVoiceType.CARRY_EGG, this.sounds().carryEgg(), 1.0F, 1.0F);
    }

    private FoodPreferenceResult getFoodPreference(ItemStack stack) { return ModSpeciesProfiles.TIANSULUO_FOOD.getPreference(stack); }
    private void applyGrowthFromFood(FoodPreferenceResult preference) {
        if (!this.isBaby()) return;
        if (preference == FoodPreferenceResult.FAVORITE) { this.setBreedingAge(0); return; }
        if (preference == FoodPreferenceResult.LIKED) this.setBreedingAge(Math.min(0, this.getBreedingAge() + this.tuning().likedFoodGrowthStepTicks()));
    }
    private boolean isTemptedByNearbyPlayer() {
        PlayerEntity player = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), this.tuning().temptedPlayerRange(), entity ->
                entity instanceof PlayerEntity candidate && candidate.isAlive() && this.canSee(candidate) && (this.isBreedingItem(candidate.getMainHandStack()) || this.isBreedingItem(candidate.getOffHandStack())));
        return player != null;
    }
    private boolean isNoticingNearbyPlayer() {
        PlayerEntity player = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), this.tuning().noticePlayerRange(), entity ->
                entity instanceof PlayerEntity candidate && candidate.isAlive() && this.canSee(candidate));
        return player != null;
    }

    private boolean beginVoice(TiansuluoVoiceType requested) {
        this.clearFinishedVoice();
        long now = this.getWorld().getTime();
        if (requested.isOneShot() && this.playedOneShotVoices.contains(requested)) return false;
        int intervalTicks = requested.isOneShot() ? 0 : this.speciesConfig().intervalTicks(requested.configKey());
        long lastTick = this.lastVoiceTicks.getOrDefault(requested, Long.MIN_VALUE / 4);
        if (!requested.isOneShot() && now - lastTick < intervalTicks) return false;
        if (this.currentVoiceType != null && this.currentVoiceEndTick > now) {
            if (this.currentVoiceType.priority() >= requested.priority()) return false;
            this.clearCurrentVoiceState();
        }
        return this.activateVoice(requested, now);
    }

    private boolean activateVoice(TiansuluoVoiceType requested, long now) {
        this.currentVoiceType = requested;
        this.currentVoiceEndTick = now + this.voiceDurationTicks(requested);
        this.lastVoiceTicks.put(requested, now);
        if (requested.isOneShot()) this.playedOneShotVoices.add(requested);
        return true;
    }

    private int voiceDurationTicks(TiansuluoVoiceType type) { return type == TiansuluoVoiceType.ATTACK_DECLARE ? this.tuning().attackDeclareDurationTicks() : type.durationTicks(); }
    private void clearFinishedVoice() { long now = this.getWorld().getTime(); if (this.currentVoiceType != null && this.currentVoiceEndTick <= now) this.clearCurrentVoiceState(); }
    private void clearCurrentVoiceState() { this.currentVoiceType = null; this.currentVoiceEndTick = 0L; }
    private boolean isShearMuteInteractionEnabledForSpecies() {
        var config = com.oh_yeah.config.OhYeahConfigManager.getShearMuteGameplayConfig();
        return config.enabled() && config.speciesIds() != null && config.speciesIds().contains(this.speciesKey());
    }
    private boolean tryUseShears(PlayerEntity player, Hand hand, ItemStack stack) {
        if (this.silencedByShears) return false;
        this.dropItem(Items.RED_WOOL);
        this.getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.0F, 1.0F);
        this.tryPlayVoice(TiansuluoVoiceType.SHEAR_REACT, this.sounds().shearReact(), 1.0F, 1.0F);
        this.setSilencedByShears(true);
        stack.damage(1, player, getSlotForHand(hand));
        return true;
    }

    protected final class MateForEggBlockGoal extends AnimalMateGoal {
        private final AbstractTiansuluoEntity tiansuluo;
        private MateForEggBlockGoal(AbstractTiansuluoEntity tiansuluo, double speed) { super(tiansuluo, speed); this.tiansuluo = tiansuluo; }
        @Override public boolean canStart() { return this.tiansuluo.usesEggBlockBreeding() && !this.tiansuluo.hasCarriedEggBlock() && super.canStart(); }
        @Override protected void breed() {
            ServerPlayerEntity player = this.animal.getLovingPlayer();
            if (player == null && this.mate.getLovingPlayer() != null) player = this.mate.getLovingPlayer();
            if (player != null) {
                player.incrementStat(Stats.ANIMALS_BRED);
                Criteria.BRED_ANIMALS.trigger(player, this.animal, this.mate, null);
            }
            this.tiansuluo.setHasCarriedEggBlock(true);
            this.tiansuluo.setCarriedEggBlockTargetPos(null);
            this.tiansuluo.setEggBlockPlacingCounter(0);
            this.tiansuluo.setEggBlockAttractedPlayer(player);
            if (player != null) player.sendMessage(Text.translatable(this.tiansuluo.breedingConfig().carriedMessageKey()), true);
            this.tiansuluo.tryPlayVoice(TiansuluoVoiceType.BREED_SUCCESS, this.tiansuluo.sounds().breedSuccess(), 1.0F, 1.0F);
            this.animal.setBreedingAge(6000);
            this.mate.setBreedingAge(6000);
            this.animal.resetLoveTicks();
            this.mate.resetLoveTicks();
            if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
                this.world.spawnEntity(new ExperienceOrbEntity(this.world, this.animal.getX(), this.animal.getY(), this.animal.getZ(), this.animal.getRandom().nextInt(7) + 1));
            }
        }
    }

    protected final class LayEggBlockGoal extends Goal {
        @Override public boolean canStart() { return AbstractTiansuluoEntity.this.usesEggBlockBreeding() && AbstractTiansuluoEntity.this.hasCarriedEggBlock() && !AbstractTiansuluoEntity.this.isBaby(); }
        @Override public boolean shouldContinue() { return this.canStart(); }
        @Override public boolean shouldRunEveryTick() { return true; }
        @Override public void start() { AbstractTiansuluoEntity.this.setEggBlockPlacingCounter(0); }
        @Override public void stop() { AbstractTiansuluoEntity.this.setEggBlockPlacingCounter(0); AbstractTiansuluoEntity.this.getNavigation().stop(); }
        @Override public void tick() {
            if (!AbstractTiansuluoEntity.this.hasCarriedEggBlock()) return;
            BlockPos targetPos = AbstractTiansuluoEntity.this.getCarriedEggBlockTargetPos();
            if (targetPos == null || !AbstractTiansuluoEntity.this.canPlaceEggBlockAt(targetPos) || !AbstractTiansuluoEntity.this.isEggBlockTargetNearPlayer(targetPos)) {
                targetPos = AbstractTiansuluoEntity.this.findNearbyEggBlockTarget();
                AbstractTiansuluoEntity.this.setCarriedEggBlockTargetPos(targetPos);
                AbstractTiansuluoEntity.this.setEggBlockPlacingCounter(0);
            }
            if (targetPos == null) return;
            Vec3d targetCenter = Vec3d.ofBottomCenter(targetPos.up());
            if (AbstractTiansuluoEntity.this.squaredDistanceTo(targetCenter) > 2.25D) {
                AbstractTiansuluoEntity.this.getNavigation().startMovingTo(targetCenter.x, targetCenter.y, targetCenter.z, AbstractTiansuluoEntity.this.tuning().mateGoalSpeed());
                AbstractTiansuluoEntity.this.setEggBlockPlacingCounter(0);
                return;
            }
            AbstractTiansuluoEntity.this.getNavigation().stop();
            if (AbstractTiansuluoEntity.this.isTouchingWater()) return;
            AbstractTiansuluoEntity.this.setEggBlockPlacingCounter(AbstractTiansuluoEntity.this.getEggBlockPlacingCounter() < 1 ? 1 : AbstractTiansuluoEntity.this.getEggBlockPlacingCounter() + 1);
            if (AbstractTiansuluoEntity.this.getEggBlockPlacingCounter() % 5 == 0) {
                BlockState baseState = AbstractTiansuluoEntity.this.getWorld().getBlockState(targetPos);
                AbstractTiansuluoEntity.this.getWorld().syncWorldEvent(2001, targetPos, Block.getRawIdFromState(baseState));
                AbstractTiansuluoEntity.this.emitGameEvent(GameEvent.ENTITY_ACTION);
            }
            if (AbstractTiansuluoEntity.this.getEggBlockPlacingCounter() > AbstractTiansuluoEntity.this.tuning().luanluanBlockPlacingTicks()) {
                AbstractTiansuluoEntity.this.placeEggBlock(targetPos);
                AbstractTiansuluoEntity.this.setHasCarriedEggBlock(false);
                AbstractTiansuluoEntity.this.setLoveTicks(600);
            }
        }
    }

    protected final class TemptWhileAvailableGoal extends TemptGoal {
        private TemptWhileAvailableGoal(double speed) { super(AbstractTiansuluoEntity.this, speed, AbstractTiansuluoEntity.this::isBreedingItem, false); }
        @Override public boolean canStart() { return !AbstractTiansuluoEntity.this.usesEggBlockBreeding() || (!AbstractTiansuluoEntity.this.hasCarriedEggBlock() && super.canStart()); }
        @Override public boolean shouldContinue() { return !AbstractTiansuluoEntity.this.usesEggBlockBreeding() || (!AbstractTiansuluoEntity.this.hasCarriedEggBlock() && super.shouldContinue()); }
    }
}
