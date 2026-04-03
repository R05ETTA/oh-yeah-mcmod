package com.oh_yeah.sound;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 通用的语音调度器，管理优先级排队、冷却计时、一次性语音和打断逻辑。
 * 通过组合方式嵌入实体类中使用。
 *
 * <p>冷却时序：冷却从语音播放结束时刻开始计时，而非激活时刻。
 * 如果语音被高优先级打断，冷却从打断时刻开始。</p>
 */
public final class VoiceScheduler {
    private static final String TAG_PLAYED_VOICES = "PlayedVoices";
    private static final String TAG_VOICE_COOLDOWNS = "VoiceCooldowns";

    private final Set<VoiceType> playedOneShotVoices = new HashSet<>();
    private final Map<VoiceType, Long> lastVoiceEndTicks = new EnumMap<>(VoiceType.class);
    private @Nullable VoiceType currentVoiceType;
    private long currentVoiceEndTick;

    /**
     * 尝试开始播放一个语音类型。
     *
     * @param requested       请求的语音类型
     * @param worldTime       当前世界 tick
     * @param intervalTicks   同类型语音的冷却间隔 tick 数
     * @param durationTicks   该语音的持续 tick 数（占用插槽的时长）
     * @return true 表示可以播放，false 表示被拒绝（冷却中/低优先级/一次性已播放）
     */
    public boolean beginVoice(VoiceType requested, long worldTime, int intervalTicks, int durationTicks) {
        this.clearFinishedVoice(worldTime);

        // 一次性语音检查：已播放过则拒绝
        if (requested.isOneShot() && this.playedOneShotVoices.contains(requested)) {
            return false;
        }

        // 冷却检查：lastVoiceEndTicks 存的是语音结束时刻，冷却从那时起算
        if (!requested.isOneShot()) {
            long lastEndTick = this.lastVoiceEndTicks.getOrDefault(requested, Long.MIN_VALUE / 4);
            if (worldTime - lastEndTick < intervalTicks) {
                return false;
            }
        }

        // 优先级检查：当前有正在播放的语音且尚未结束
        if (this.currentVoiceType != null && this.currentVoiceEndTick > worldTime) {
            if (this.currentVoiceType.priority() >= requested.priority()) {
                return false;
            }
            // 高优先级打断：被打断的语音冷却从此刻开始
            this.lastVoiceEndTicks.put(this.currentVoiceType, worldTime);
            this.clearCurrentVoiceState();
        }

        // 全局限流检查：防止多实体同时说话
        if (!GlobalVoiceLimiter.canPlay(worldTime, requested)) {
            return false;
        }

        boolean activated = this.activateVoice(requested, worldTime, durationTicks);
        if (activated) {
            GlobalVoiceLimiter.markPlayed(worldTime);
        }
        return activated;
    }

    /**
     * 检查指定语音类型是否正在活跃播放中。
     */
    public boolean isVoiceActive(VoiceType type, long worldTime) {
        this.clearFinishedVoice(worldTime);
        return this.currentVoiceType == type;
    }

    /**
     * 清理已结束的语音插槽。应在每个 tick 调用。
     */
    public void clearFinishedVoice(long worldTime) {
        if (this.currentVoiceType != null && this.currentVoiceEndTick <= worldTime) {
            // 语音自然结束：冷却起点 = 语音结束时刻（已在 activateVoice 中预设）
            this.clearCurrentVoiceState();
        }
    }

    /**
     * 标记一个一次性语音为已播放。
     */
    public void markOneShotAsPlayed(VoiceType type) {
        if (type.isOneShot()) {
            this.playedOneShotVoices.add(type);
        }
    }

    /**
     * 将语音调度状态序列化到 NBT。
     */
    public void writeToNbt(NbtCompound parentNbt) {
        NbtCompound played = new NbtCompound();
        for (VoiceType type : this.playedOneShotVoices) {
            played.putBoolean(type.configKey(), true);
        }
        parentNbt.put(TAG_PLAYED_VOICES, played);

        NbtCompound cooldowns = new NbtCompound();
        for (Map.Entry<VoiceType, Long> entry : this.lastVoiceEndTicks.entrySet()) {
            cooldowns.putLong(entry.getKey().configKey(), entry.getValue());
        }
        parentNbt.put(TAG_VOICE_COOLDOWNS, cooldowns);
    }

    /**
     * 从 NBT 恢复语音调度状态。
     */
    public void readFromNbt(NbtCompound parentNbt) {
        this.playedOneShotVoices.clear();
        NbtCompound played = parentNbt.getCompound(TAG_PLAYED_VOICES);
        for (VoiceType type : VoiceType.values()) {
            if (played.getBoolean(type.configKey())) {
                this.playedOneShotVoices.add(type);
            }
        }

        this.lastVoiceEndTicks.clear();
        NbtCompound cooldowns = parentNbt.getCompound(TAG_VOICE_COOLDOWNS);
        for (VoiceType type : VoiceType.values()) {
            if (cooldowns.contains(type.configKey())) {
                this.lastVoiceEndTicks.put(type, cooldowns.getLong(type.configKey()));
            }
        }
    }

    private boolean activateVoice(VoiceType requested, long worldTime, int durationTicks) {
        this.currentVoiceType = requested;
        long endTick = worldTime + durationTicks;
        this.currentVoiceEndTick = endTick;
        // 冷却起点预设为语音结束时刻（如果自然结束，此值即为冷却起点）
        this.lastVoiceEndTicks.put(requested, endTick);
        if (requested.isOneShot()) {
            this.playedOneShotVoices.add(requested);
        }
        return true;
    }

    private void clearCurrentVoiceState() {
        this.currentVoiceType = null;
        this.currentVoiceEndTick = 0L;
    }
}
