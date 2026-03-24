package com.oh_yeah.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

public final class TiansuluoSpawnEggItem extends SpawnEggItem {
    private static final Style TOOLTIP_STYLE = Style.EMPTY.withColor(0x7A5C2E);
    private final String descKey;
    private final String descKey2;

    public TiansuluoSpawnEggItem(EntityType<? extends MobEntity> type, int primaryColor, int secondaryColor, Item.Settings settings, String descKey, String descKey2) {
        super(type, primaryColor, secondaryColor, settings);
        this.descKey = descKey;
        this.descKey2 = descKey2;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable(this.descKey).setStyle(TOOLTIP_STYLE));
        tooltip.add(Text.translatable(this.descKey2).setStyle(TOOLTIP_STYLE));
    }
}
