package com.oh_yeah.entity.species;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.ModSoundEvents;
import com.oh_yeah.sound.SoundProfile;
import com.oh_yeah.sound.VoiceType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.HashMap;
import java.util.Map;

public final class ModSpeciesProfiles {
    private static final String CHIPS_RULE = "chips_item";
    public static final String TIANSULUO_PINK_SCARF_SOUND_PROFILE = "tiansuluo_pink_scarf";
    public static final String TIANSULUO_BATTLE_FACE_SOUND_PROFILE = "tiansuluo_battle_face";
    public static final SpeciesFoodProfile TIANSULUO_FOOD = SpeciesFoodProfile.builder()
            .item(Items.CAKE, FoodPreferenceResult.FAVORITE)
            .item(Items.WHEAT, FoodPreferenceResult.LIKED)
            .item(Items.CARROT, FoodPreferenceResult.LIKED)
            .item(Items.BEETROOT, FoodPreferenceResult.LIKED)
            .item(Items.POTATO, FoodPreferenceResult.LIKED)
            .reservedFavoriteRule(CHIPS_RULE, ModSpeciesProfiles::matchesChips)
            .build();

    private static final Map<String, SoundProfile> TIANSULUO_SOUNDS = new HashMap<>();

    static {
        SoundProfile pinkScarf = SoundProfile.builder()
                .sound(VoiceType.AMBIENT, ModSoundEvents.TiansuluoPinkScarf.AMBIENT)
                .sound(VoiceType.RARE_CALL, ModSoundEvents.TiansuluoPinkScarf.RARE_CALL)
                .sound(VoiceType.HURT, ModSoundEvents.TiansuluoPinkScarf.HURT)
                .sound(VoiceType.DEATH, ModSoundEvents.TiansuluoPinkScarf.DEATH)
                .sound(VoiceType.EAT, ModSoundEvents.TiansuluoPinkScarf.EAT)
                .sound(VoiceType.EAT_FAVORITE, ModSoundEvents.TiansuluoPinkScarf.EAT_FAVORITE)
                .sound(VoiceType.TEMPTED, ModSoundEvents.TiansuluoPinkScarf.TEMPTED)
                .sound(VoiceType.NOTICE_PLAYER, ModSoundEvents.TiansuluoPinkScarf.NOTICE_PLAYER)
                .sound(VoiceType.BREED_SUCCESS, ModSoundEvents.TiansuluoPinkScarf.BREED_SUCCESS)
                .sound(VoiceType.CARRY_EGG, ModSoundEvents.TiansuluoPinkScarf.CARRY_EGG)
                .sound(VoiceType.ATTACK_SHOT, ModSoundEvents.TiansuluoPinkScarf.ATTACK_SHOT)
                .sound(VoiceType.ATTACK_END, ModSoundEvents.TiansuluoPinkScarf.ATTACK_END)
                .sound(VoiceType.ATTACK_DECLARE, ModSoundEvents.TiansuluoPinkScarf.ATTACK_DECLARE)
                .sound(VoiceType.GROW_UP, ModSoundEvents.TiansuluoPinkScarf.GROW_UP)
                .sound(VoiceType.SHEAR_REACT, ModSoundEvents.TiansuluoPinkScarf.SHEAR_REACT)
                .build();

        // Battle Face 保留独立实例，目前暂时复用相同音源，将来可替换为独立音频
        SoundProfile battleFace = SoundProfile.builder()
                .sound(VoiceType.AMBIENT, ModSoundEvents.Tiansuluo.AMBIENT)
                .sound(VoiceType.RARE_CALL, ModSoundEvents.Tiansuluo.RARE_CALL)
                .sound(VoiceType.HURT, ModSoundEvents.Tiansuluo.HURT)
                .sound(VoiceType.DEATH, ModSoundEvents.Tiansuluo.DEATH)
                .sound(VoiceType.EAT, ModSoundEvents.Tiansuluo.EAT)
                .sound(VoiceType.EAT_FAVORITE, ModSoundEvents.Tiansuluo.EAT_FAVORITE)
                .sound(VoiceType.TEMPTED, ModSoundEvents.Tiansuluo.TEMPTED)
                .sound(VoiceType.NOTICE_PLAYER, ModSoundEvents.Tiansuluo.NOTICE_PLAYER)
                .sound(VoiceType.BREED_SUCCESS, ModSoundEvents.Tiansuluo.BREED_SUCCESS)
                .sound(VoiceType.CARRY_EGG, ModSoundEvents.Tiansuluo.CARRY_EGG)
                .sound(VoiceType.ATTACK_SHOT, ModSoundEvents.Tiansuluo.ATTACK_SHOT)
                .sound(VoiceType.ATTACK_END, ModSoundEvents.Tiansuluo.ATTACK_END)
                .sound(VoiceType.ATTACK_DECLARE, ModSoundEvents.Tiansuluo.ATTACK_DECLARE)
                .sound(VoiceType.GROW_UP, ModSoundEvents.Tiansuluo.GROW_UP)
                .sound(VoiceType.SHEAR_REACT, ModSoundEvents.Tiansuluo.SHEAR_REACT)
                .build();

        TIANSULUO_SOUNDS.put(TIANSULUO_PINK_SCARF_SOUND_PROFILE, pinkScarf);
        TIANSULUO_SOUNDS.put(TIANSULUO_BATTLE_FACE_SOUND_PROFILE, battleFace);
    }

    private ModSpeciesProfiles() {
    }

    public static SoundProfile soundsFor(String soundProfileKey) {
        SoundProfile profile = TIANSULUO_SOUNDS.get(soundProfileKey);
        return profile != null ? profile : TIANSULUO_SOUNDS.get(TIANSULUO_PINK_SCARF_SOUND_PROFILE);
    }

    private static boolean matchesChips(ItemStack stack) {
        return stack.isOf(ModItems.CHIPS);
    }
}
