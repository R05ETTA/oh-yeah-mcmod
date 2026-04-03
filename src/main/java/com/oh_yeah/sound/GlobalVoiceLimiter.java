package com.oh_yeah.sound;

/**
 * 全局语音限流器。
 * <p>防止多个天素罗实体同时播放语音导致噪音过大。
 * 使用静态字段追踪全局最近一次语音播放时刻。
 * 高优先级语音（HURT、DEATH、SHEAR_REACT）可绕过限流。</p>
 */
public final class GlobalVoiceLimiter {
    /** 被视为"高优先级可旁路"的最低优先级阈值 */
    private static final int BYPASS_PRIORITY_THRESHOLD = 88;
    /** 全局最小间隔（tick），两个不同实体的语音之间至少间隔此值 */
    private static int globalMinIntervalTicks = 40;

    private static long lastGlobalVoiceTick = Long.MIN_VALUE / 4;

    private GlobalVoiceLimiter() {
    }

    /**
     * 检查当前时刻是否允许播放语音。
     *
     * @param worldTime 当前世界 tick
     * @param voiceType 请求播放的语音类型
     * @return true 表示允许播放
     */
    public static boolean canPlay(long worldTime, VoiceType voiceType) {
        if (voiceType.priority() >= BYPASS_PRIORITY_THRESHOLD) {
            return true;
        }
        return worldTime - lastGlobalVoiceTick >= globalMinIntervalTicks;
    }

    /**
     * 标记当前时刻已播放了一个语音。
     */
    public static void markPlayed(long worldTime) {
        lastGlobalVoiceTick = worldTime;
    }

    /**
     * 设置全局最小间隔。供配置系统调用。
     */
    public static void setGlobalMinIntervalTicks(int ticks) {
        globalMinIntervalTicks = Math.max(0, ticks);
    }

    /**
     * 获取当前全局最小间隔。
     */
    public static int getGlobalMinIntervalTicks() {
        return globalMinIntervalTicks;
    }
}
