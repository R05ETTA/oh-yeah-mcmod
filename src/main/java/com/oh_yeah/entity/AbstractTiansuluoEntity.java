package com.oh_yeah.entity;

import com.oh_yeah.OhYeah;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.TiansuluoTuningConfig;
import com.oh_yeah.config.VariantConfig;
import com.oh_yeah.entity.species.FoodPreferenceResult;
import com.oh_yeah.entity.species.ModSpeciesProfiles;
import com.oh_yeah.entity.species.SpeciesSoundProfile;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.TiansuluoVoiceType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractTiansuluoEntity extends AnimalEntity {
    private static final String TAG_PLAYED_VOICES = "PlayedVoices";
    private static final String TAG_VOICE_COOLDOWNS = "VoiceCooldowns";
    private static final String TAG_SILENCED_BY_SHEARS = "SilencedByShears";

    private final Set<TiansuluoVoiceType> playedOneShotVoices = new HashSet<>();
    private final Map<TiansuluoVoiceType, Long> lastVoiceTicks = new EnumMap<>(TiansuluoVoiceType.class);
    private @Nullable TiansuluoVoiceType currentVoiceType;
    private long currentVoiceEndTick;
    private boolean ambientRareCallNext;
    private boolean wasBabyLastTick;
    private boolean wasTemptedByPlayer;
    private boolean wasNoticingPlayer;
    private boolean silencedByShears;

    protected AbstractTiansuluoEntity(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    protected abstract SpeciesConfig speciesConfig();

    protected abstract String soundProfileKey();

    protected abstract String speciesKey();

    protected abstract Item eggDropItem();

    protected void onSpeciesInitialize(ServerWorldAccess world, LocalDifficulty difficulty, net.minecraft.entity.SpawnReason spawnReason) {
    }

    protected void tickSpeciesMovement() {
    }

    @Override
    public @Nullable EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, net.minecraft.entity.SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        this.onSpeciesInitialize(world, difficulty, spawnReason);

        if (spawnReason == net.minecraft.entity.SpawnReason.SPAWN_EGG || spawnReason == net.minecraft.entity.SpawnReason.BREEDING) {
            this.setBaby(true);
            this.setBreedingAge(this.tuning().babyGrowthAgeTicks());
        }

        if (spawnReason != net.minecraft.entity.SpawnReason.BREEDING) {
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
        this.tickSpeciesMovement();
        this.clearFinishedVoice();
        this.updateGrowthVoice();
        this.updateStateVoices();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return this.getFoodPreference(stack).isFood();
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.isShearMuteInteractionEnabledForSpecies() && stack.isOf(Items.SHEARS)) {
            if (this.getWorld().isClient) {
                return !this.silencedByShears ? ActionResult.CONSUME : ActionResult.PASS;
            }
            return this.tryUseShears(player, hand, stack) ? ActionResult.SUCCESS : ActionResult.PASS;
        }

        FoodPreferenceResult preference = this.getFoodPreference(stack);
        ActionResult result = super.interactMob(player, hand);

        if (!this.getWorld().isClient && result.isAccepted() && preference.isFood()) {
            this.applyGrowthFromFood(preference);
            if (this.silencedByShears) {
                this.setSilencedByShears(false);
            }
            TiansuluoVoiceType voiceType = preference == FoodPreferenceResult.FAVORITE ? TiansuluoVoiceType.EAT_FAVORITE : TiansuluoVoiceType.EAT;
            SoundEvent eatSound = preference == FoodPreferenceResult.FAVORITE ? this.sounds().eatFavorite() : this.sounds().eat();
            this.tryPlayVoice(voiceType, eatSound, 1.0F, preference == FoodPreferenceResult.FAVORITE ? 1.08F : 1.0F);
        }
        return result;
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        super.onDeath(damageSource);
        if (!this.getWorld().isClient && !this.isBaby()) {
            this.dropItem(ModItems.CHIPS);
            this.dropItem(this.eggDropItem());
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        TiansuluoVoiceType type = this.ambientRareCallNext ? TiansuluoVoiceType.RARE_CALL : TiansuluoVoiceType.AMBIENT;
        SoundEvent event = this.ambientRareCallNext ? this.sounds().rareCall() : this.sounds().ambient();
        this.ambientRareCallNext = this.random.nextInt(this.tuning().rareCallChanceDivisor()) == 0;
        if (!this.beginVoice(type)) {
            return null;
        }
        return event;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        SoundEvent event = this.sounds().hurt();
        if (!this.beginVoice(TiansuluoVoiceType.HURT)) {
            return null;
        }
        return event;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        SoundEvent event = this.sounds().death();
        if (!this.beginVoice(TiansuluoVoiceType.DEATH)) {
            return null;
        }
        return event;
    }

    @Override
    public float getSoundVolume() {
        return this.resolveSoundVolume(super.getSoundVolume());
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // `tiansuluo.step_soft` is intentionally muted for now.
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

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
        nbt.putBoolean(TAG_SILENCED_BY_SHEARS, this.silencedByShears);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

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
        this.silencedByShears = nbt.getBoolean(TAG_SILENCED_BY_SHEARS);
    }

    public void markOneShotVoiceAsPlayed(TiansuluoVoiceType type) {
        if (type.isOneShot()) {
            this.playedOneShotVoices.add(type);
        }
    }

    public Identifier getTextureId() {
        VariantConfig cfg = this.speciesConfig().defaultVariantConfig();
        String texture = cfg != null ? cfg.texture() : "textures/entity/tiansuluo.png";
        if (texture.contains(":")) {
            Identifier parsed = Identifier.tryParse(texture);
            return parsed != null ? parsed : Identifier.of(OhYeah.MOD_ID, "textures/entity/tiansuluo.png");
        }
        return Identifier.of(OhYeah.MOD_ID, texture);
    }

    protected TiansuluoTuningConfig tuning() {
        return this.speciesConfig().tuning();
    }

    protected SpeciesSoundProfile sounds() {
        return ModSpeciesProfiles.soundsFor(this.soundProfileKey());
    }

    protected boolean tryPlayVoice(TiansuluoVoiceType type, SoundEvent sound, float volume, float pitch) {
        if (!this.beginVoice(type)) {
            return false;
        }
        this.playSpeciesSound(sound, volume, pitch);
        return true;
    }

    public boolean isSilencedByShears() {
        return this.silencedByShears;
    }

    public void setSilencedByShears(boolean silencedByShears) {
        this.silencedByShears = silencedByShears;
    }

    protected float resolveSoundVolume(float requestedVolume) {
        return this.silencedByShears ? 0.0F : requestedVolume;
    }

    protected void playSpeciesSound(SoundEvent sound, float volume, float pitch) {
        this.playSound(sound, this.resolveSoundVolume(volume), pitch);
    }

    protected boolean isVoiceActive(TiansuluoVoiceType type) {
        this.clearFinishedVoice();
        return this.currentVoiceType == type;
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

    private boolean beginVoice(TiansuluoVoiceType requested) {
        this.clearFinishedVoice();
        long now = this.getWorld().getTime();

        if (requested.isOneShot() && this.playedOneShotVoices.contains(requested)) {
            return false;
        }

        int intervalTicks = requested.isOneShot() ? 0 : this.speciesConfig().intervalTicks(requested.configKey());
        long lastTick = this.lastVoiceTicks.getOrDefault(requested, Long.MIN_VALUE / 4);
        if (!requested.isOneShot() && now - lastTick < intervalTicks) {
            return false;
        }

        if (this.currentVoiceType != null && this.currentVoiceEndTick > now) {
            if (this.currentVoiceType.priority() >= requested.priority()) {
                return false;
            }
            this.clearCurrentVoiceState();
        }

        return this.activateVoice(requested, now);
    }

    private boolean activateVoice(TiansuluoVoiceType requested, long now) {
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
            this.clearCurrentVoiceState();
        }
    }

    private void clearCurrentVoiceState() {
        this.currentVoiceType = null;
        this.currentVoiceEndTick = 0L;
    }

    private boolean isShearMuteInteractionEnabledForSpecies() {
        var config = com.oh_yeah.config.OhYeahConfigManager.getShearMuteGameplayConfig();
        return config.enabled() && config.speciesIds() != null && config.speciesIds().contains(this.speciesKey());
    }

    private boolean tryUseShears(PlayerEntity player, Hand hand, ItemStack stack) {
        if (this.silencedByShears) {
            return false;
        }

        this.dropItem(Items.RED_WOOL);
        this.getWorld().playSoundFromEntity(null, this, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.PLAYERS, 1.0F, 1.0F);
        // Play the high-priority protest line before the post-shear mute flag suppresses later voices.
        this.tryPlayVoice(TiansuluoVoiceType.SHEAR_REACT, this.sounds().shearReact(), 1.0F, 1.0F);
        this.setSilencedByShears(true);
        stack.damage(1, player, getSlotForHand(hand));
        return true;
    }
}
