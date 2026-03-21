package com.oh_yeah.item;

import com.oh_yeah.OhYeah;
import com.oh_yeah.entity.ModEntityTypes;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item TIANSULUO_EGG = register(
            "tiansuluo_egg",
            new TiansuluoSpawnEggItem(ModEntityTypes.TIANSULUO, 0xF3D7A6, 0x9D6E52, new Item.Settings())
    );
    public static final Item CHIPS = register("chips", new Item(new Item.Settings().food(FoodComponents.COOKIE)));

    private ModItems() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(TIANSULUO_EGG));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(CHIPS));
    }

    private static Item register(String id, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(OhYeah.MOD_ID, id), item);
    }
}
