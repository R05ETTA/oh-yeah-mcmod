package com.oh_yeah.sound;

import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class SoundProfile {
    private final Map<VoiceType, SoundEvent> sounds;

    private SoundProfile(Map<VoiceType, SoundEvent> sounds) {
        this.sounds = Map.copyOf(sounds);
    }

    public @Nullable SoundEvent get(VoiceType type) {
        return this.sounds.get(type);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<VoiceType, SoundEvent> map = new EnumMap<>(VoiceType.class);

        private Builder() {
        }

        public Builder sound(VoiceType type, SoundEvent event) {
            this.map.put(type, event);
            return this;
        }

        public SoundProfile build() {
            return new SoundProfile(this.map);
        }
    }
}
