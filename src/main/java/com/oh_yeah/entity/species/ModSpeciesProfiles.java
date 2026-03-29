package com.oh_yeah.entity.species;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.ModSoundEvents;
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

    private static final Map<String, SpeciesSoundProfile> TIANSULUO_SOUNDS = new HashMap<>();

    static {
        SpeciesSoundProfile pinkScarf = new SpeciesSoundProfile(
                ModSoundEvents.TIANSULUO_AMBIENT,
                ModSoundEvents.TIANSULUO_RARE_CALL,
                ModSoundEvents.TIANSULUO_HURT,
                ModSoundEvents.TIANSULUO_DEATH,
                ModSoundEvents.TIANSULUO_STEP_SOFT,
                ModSoundEvents.TIANSULUO_EAT,
                ModSoundEvents.TIANSULUO_EAT_FAVORITE,
                ModSoundEvents.TIANSULUO_TEMPTED,
                ModSoundEvents.TIANSULUO_NOTICE_PLAYER,
                ModSoundEvents.TIANSULUO_BREED_SUCCESS,
                ModSoundEvents.TIANSULUO_CARRY_EGG,
                ModSoundEvents.TIANSULUO_ATTACK_SHOT,
                ModSoundEvents.TIANSULUO_ATTACK_END,
                ModSoundEvents.TIANSULUO_ATTACK_DECLARE,
                ModSoundEvents.TIANSULUO_GROW_UP,
                ModSoundEvents.TIANSULUO_SHEAR_REACT
        );
        SpeciesSoundProfile battleFace = new SpeciesSoundProfile(
                ModSoundEvents.TIANSULUO_AMBIENT,
                ModSoundEvents.TIANSULUO_RARE_CALL,
                ModSoundEvents.TIANSULUO_HURT,
                ModSoundEvents.TIANSULUO_DEATH,
                ModSoundEvents.TIANSULUO_STEP_SOFT,
                ModSoundEvents.TIANSULUO_EAT,
                ModSoundEvents.TIANSULUO_EAT_FAVORITE,
                ModSoundEvents.TIANSULUO_TEMPTED,
                ModSoundEvents.TIANSULUO_NOTICE_PLAYER,
                ModSoundEvents.TIANSULUO_BREED_SUCCESS,
                ModSoundEvents.TIANSULUO_CARRY_EGG,
                ModSoundEvents.TIANSULUO_ATTACK_SHOT,
                ModSoundEvents.TIANSULUO_ATTACK_END,
                ModSoundEvents.TIANSULUO_ATTACK_DECLARE,
                ModSoundEvents.TIANSULUO_GROW_UP,
                ModSoundEvents.TIANSULUO_SHEAR_REACT
        );
        TIANSULUO_SOUNDS.put(TIANSULUO_PINK_SCARF_SOUND_PROFILE, pinkScarf);
        TIANSULUO_SOUNDS.put(TIANSULUO_BATTLE_FACE_SOUND_PROFILE, battleFace);
    }

    private ModSpeciesProfiles() {
    }

    public static SpeciesSoundProfile soundsFor(String soundProfileKey) {
        SpeciesSoundProfile profile = TIANSULUO_SOUNDS.get(soundProfileKey);
        return profile != null ? profile : TIANSULUO_SOUNDS.get(TIANSULUO_PINK_SCARF_SOUND_PROFILE);
    }

    private static boolean matchesChips(ItemStack stack) {
        return stack.isOf(ModItems.CHIPS);
    }
}
