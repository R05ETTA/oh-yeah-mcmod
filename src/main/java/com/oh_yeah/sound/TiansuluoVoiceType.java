package com.oh_yeah.sound;

public enum TiansuluoVoiceType {
    AMBIENT("ambient", 0, 30, false),
    RARE_CALL("rare_call", 5, 30, false),
    TEMPTED("tempted", 10, 30, false),
    EAT("eat", 20, 24, false),
    EAT_FAVORITE("eat_favorite", 30, 28, false),
    BREED_SUCCESS("breed_success", 40, 36, false),
    ATTACK_SHOT("attack_shot", 50, 8, false),
    ATTACK_END("attack_end", 60, 16, false),
    HURT("hurt", 80, 22, false),
    ATTACK_DECLARE("attack_declare", 85, 40, false),
    DEATH("death", 90, 36, false),
    NOTICE_PLAYER("notice_player", 100, 30, true),
    GROW_UP("grow_up", 100, 36, true),
    SPAWN("spawn", 100, 30, true),
    // Shear-triggered protest line that should interrupt every regular Tiansuluo voice.
    SHEAR_REACT("shear_react", 101, 30, false);

    private final String configKey;
    private final int priority;
    private final int durationTicks;
    private final boolean oneShot;

    TiansuluoVoiceType(String configKey, int priority, int durationTicks, boolean oneShot) {
        this.configKey = configKey;
        this.priority = priority;
        this.durationTicks = durationTicks;
        this.oneShot = oneShot;
    }

    public String configKey() {
        return this.configKey;
    }

    public int priority() {
        return this.priority;
    }

    public int durationTicks() {
        return this.durationTicks;
    }

    public boolean isOneShot() {
        return this.oneShot;
    }
}
