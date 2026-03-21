package com.oh_yeah.entity.species;

import com.oh_yeah.entity.TiansuluoVariant;
import com.oh_yeah.item.ModItems;
import com.oh_yeah.sound.ModSoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.EnumMap;
import java.util.Map;

public final class ModSpeciesProfiles {
    private static final String CHIPS_RULE = "chips_item";
    public static final SpeciesFoodProfile TIANSULUO_FOOD = SpeciesFoodProfile.builder()
            .item(Items.CAKE, FoodPreferenceResult.FAVORITE)
            .item(Items.WHEAT, FoodPreferenceResult.LIKED)
            .item(Items.CARROT, FoodPreferenceResult.LIKED)
            .item(Items.BEETROOT, FoodPreferenceResult.LIKED)
            .item(Items.POTATO, FoodPreferenceResult.LIKED)
            .reservedFavoriteRule(CHIPS_RULE, ModSpeciesProfiles::matchesChips)
            .build();

    private static final Map<TiansuluoVariant, SpeciesSoundProfile> TIANSULUO_SOUNDS = new EnumMap<>(TiansuluoVariant.class);

    static {
        SpeciesSoundProfile base = new SpeciesSoundProfile(
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
                ModSoundEvents.TIANSULUO_ATTACK_SHOT,
                ModSoundEvents.TIANSULUO_ATTACK_END,
                ModSoundEvents.TIANSULUO_ATTACK_DECLARE,
                ModSoundEvents.TIANSULUO_GROW_UP,
                ModSoundEvents.TIANSULUO_SPAWN
        );
        TIANSULUO_SOUNDS.put(TiansuluoVariant.BASE, base);
        TIANSULUO_SOUNDS.put(TiansuluoVariant.SCARF_PINK, base);
    }

    private ModSpeciesProfiles() {
    }

    public static SpeciesSoundProfile soundsFor(TiansuluoVariant variant) {
        SpeciesSoundProfile profile = TIANSULUO_SOUNDS.get(variant);
        return profile != null ? profile : TIANSULUO_SOUNDS.get(TiansuluoVariant.SCARF_PINK);
    }

    private static boolean matchesChips(ItemStack stack) {
        return stack.isOf(ModItems.CHIPS);
    }
}
