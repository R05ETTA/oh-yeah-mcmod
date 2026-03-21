package com.oh_yeah.entity.species;

public enum FoodPreferenceResult {
    NONE,
    LIKED,
    FAVORITE;

    public boolean isFood() {
        return this != NONE;
    }
}
