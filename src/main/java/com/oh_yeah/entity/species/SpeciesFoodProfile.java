package com.oh_yeah.entity.species;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class SpeciesFoodProfile {
    private final Map<Item, FoodPreferenceResult> explicitItems;
    private final Map<String, Predicate<ItemStack>> reservedRules;

    private SpeciesFoodProfile(Map<Item, FoodPreferenceResult> explicitItems, Map<String, Predicate<ItemStack>> reservedRules) {
        this.explicitItems = Map.copyOf(explicitItems);
        this.reservedRules = Map.copyOf(reservedRules);
    }

    public FoodPreferenceResult getPreference(ItemStack stack) {
        FoodPreferenceResult itemResult = this.explicitItems.get(stack.getItem());
        if (itemResult != null) {
            return itemResult;
        }
        for (Predicate<ItemStack> rule : this.reservedRules.values()) {
            if (rule.test(stack)) {
                return FoodPreferenceResult.FAVORITE;
            }
        }
        return FoodPreferenceResult.NONE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Item, FoodPreferenceResult> explicitItems = new LinkedHashMap<>();
        private final Map<String, Predicate<ItemStack>> reservedRules = new LinkedHashMap<>();

        public Builder item(Item item, FoodPreferenceResult preference) {
            this.explicitItems.put(item, preference);
            return this;
        }

        public Builder reservedFavoriteRule(String id, Predicate<ItemStack> predicate) {
            this.reservedRules.put(id, predicate);
            return this;
        }

        public SpeciesFoodProfile build() {
            return new SpeciesFoodProfile(this.explicitItems, this.reservedRules);
        }
    }
}
