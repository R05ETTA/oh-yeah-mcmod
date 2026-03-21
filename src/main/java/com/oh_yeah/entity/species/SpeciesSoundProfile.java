package com.oh_yeah.entity.species;

import net.minecraft.sound.SoundEvent;

public record SpeciesSoundProfile(
        SoundEvent ambient,
        SoundEvent rareCall,
        SoundEvent hurt,
        SoundEvent death,
        SoundEvent step,
        SoundEvent eat,
        SoundEvent eatFavorite,
        SoundEvent tempted,
        SoundEvent noticePlayer,
        SoundEvent breedSuccess,
        SoundEvent attackShot,
        SoundEvent attackEnd,
        SoundEvent attackDeclare,
        SoundEvent growUp,
        SoundEvent spawn
) {
}
