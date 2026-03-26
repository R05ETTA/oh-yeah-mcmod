package com.oh_yeah.sound;

import com.oh_yeah.OhYeah;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSoundEvents {
    public static final SoundEvent TIANSULUO_AMBIENT = register("tiansuluo.ambient");
    public static final SoundEvent TIANSULUO_RARE_CALL = register("tiansuluo.rare_call");
    public static final SoundEvent TIANSULUO_HURT = register("tiansuluo.hurt");
    public static final SoundEvent TIANSULUO_DEATH = register("tiansuluo.death");
    public static final SoundEvent TIANSULUO_STEP_SOFT = register("tiansuluo.step_soft");
    public static final SoundEvent TIANSULUO_EAT = register("tiansuluo.eat");
    public static final SoundEvent TIANSULUO_EAT_FAVORITE = register("tiansuluo.eat_favorite");
    public static final SoundEvent TIANSULUO_TEMPTED = register("tiansuluo.tempted");
    public static final SoundEvent TIANSULUO_NOTICE_PLAYER = register("tiansuluo.notice_player");
    public static final SoundEvent TIANSULUO_BREED_SUCCESS = register("tiansuluo.breed_success");
    public static final SoundEvent TIANSULUO_ATTACK_SHOT = register("tiansuluo.attack_shot");
    public static final SoundEvent TIANSULUO_ATTACK_END = register("tiansuluo.attack_end");
    public static final SoundEvent TIANSULUO_ATTACK_DECLARE = register("tiansuluo.attack_declare");
    public static final SoundEvent TIANSULUO_GROW_UP = register("tiansuluo.grow_up");
    public static final SoundEvent TIANSULUO_SPAWN = register("tiansuluo.spawn");
    public static final SoundEvent TIANSULUO_SHEAR_REACT = register("tiansuluo.shear_react");
    public static final SoundEvent SUXIA_AMBIENT = register("suxia.ambient");
    public static final SoundEvent SUXIA_HURT = register("suxia.hurt");
    public static final SoundEvent SUXIA_DEATH = register("suxia.death");
    public static final SoundEvent SUXIA_SQUIRT = register("suxia.squirt");

    private ModSoundEvents() {
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(OhYeah.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {
        OhYeah.LOGGER.info("Registered sound events for {}", OhYeah.MOD_ID);
    }
}
