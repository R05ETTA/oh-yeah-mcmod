package com.oh_yeah.sound;

import com.oh_yeah.OhYeah;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSoundEvents {

    public static final class Tiansuluo {
        public static final SoundEvent AMBIENT = register("tiansuluo.ambient");
        public static final SoundEvent RARE_CALL = register("tiansuluo.rare_call");
        public static final SoundEvent HURT = register("tiansuluo.hurt");
        public static final SoundEvent DEATH = register("tiansuluo.death");
        public static final SoundEvent STEP_SOFT = register("tiansuluo.step_soft");
        public static final SoundEvent EAT = register("tiansuluo.eat");
        public static final SoundEvent EAT_FAVORITE = register("tiansuluo.eat_favorite");
        public static final SoundEvent TEMPTED = register("tiansuluo.tempted");
        public static final SoundEvent NOTICE_PLAYER = register("tiansuluo.notice_player");
        public static final SoundEvent BREED_SUCCESS = register("tiansuluo.breed_success");
        public static final SoundEvent CARRY_EGG = register("tiansuluo.carry_egg");
        public static final SoundEvent ATTACK_SHOT = register("tiansuluo.attack_shot");
        public static final SoundEvent ATTACK_END = register("tiansuluo.attack_end");
        public static final SoundEvent ATTACK_DECLARE = register("tiansuluo.attack_declare");
        public static final SoundEvent GROW_UP = register("tiansuluo.grow_up");
        public static final SoundEvent SHEAR_REACT = register("tiansuluo.shear_react");

        private Tiansuluo() {
        }
    }

    public static final class TiansuluoPinkScarf {
        public static final SoundEvent AMBIENT = register("tiansuluo_ps.ambient");
        public static final SoundEvent RARE_CALL = register("tiansuluo_ps.rare_call");
        public static final SoundEvent HURT = register("tiansuluo_ps.hurt");
        public static final SoundEvent DEATH = register("tiansuluo_ps.death");
        public static final SoundEvent STEP_SOFT = register("tiansuluo_ps.step_soft");
        public static final SoundEvent EAT = register("tiansuluo_ps.eat");
        public static final SoundEvent EAT_FAVORITE = register("tiansuluo_ps.eat_favorite");
        public static final SoundEvent TEMPTED = register("tiansuluo_ps.tempted");
        public static final SoundEvent NOTICE_PLAYER = register("tiansuluo_ps.notice_player");
        public static final SoundEvent BREED_SUCCESS = register("tiansuluo_ps.breed_success");
        public static final SoundEvent CARRY_EGG = register("tiansuluo_ps.carry_egg");
        public static final SoundEvent ATTACK_SHOT = register("tiansuluo_ps.attack_shot");
        public static final SoundEvent ATTACK_END = register("tiansuluo_ps.attack_end");
        public static final SoundEvent ATTACK_DECLARE = register("tiansuluo_ps.attack_declare");
        public static final SoundEvent GROW_UP = register("tiansuluo_ps.grow_up");
        public static final SoundEvent SHEAR_REACT = register("tiansuluo_ps.shear_react");

        private TiansuluoPinkScarf() {
        }
    }

    public static final class Suxia {
        public static final SoundEvent AMBIENT = register("suxia.ambient");
        public static final SoundEvent HURT = register("suxia.hurt");
        public static final SoundEvent DEATH = register("suxia.death");
        public static final SoundEvent SQUIRT = register("suxia.squirt");

        private Suxia() {
        }
    }

    private ModSoundEvents() {
    }

    private static SoundEvent register(String path) {
        Identifier id = Identifier.of(OhYeah.MOD_ID, path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void initialize() {
        // 触发内部类的静态初始化以注册所有 SoundEvent
        @SuppressWarnings("unused") SoundEvent t = Tiansuluo.AMBIENT;
        @SuppressWarnings("unused") SoundEvent tp = TiansuluoPinkScarf.AMBIENT;
        @SuppressWarnings("unused") SoundEvent s = Suxia.AMBIENT;
        OhYeah.LOGGER.info("Registered sound events for {}", OhYeah.MOD_ID);
    }
}
