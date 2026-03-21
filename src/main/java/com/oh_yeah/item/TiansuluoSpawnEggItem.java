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

    public TiansuluoSpawnEggItem(EntityType<? extends MobEntity> type, int primaryColor, int secondaryColor, Item.Settings settings) {
        super(type, primaryColor, secondaryColor, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(Text.translatable("item.oh-yeah.tiansuluo_egg.desc").setStyle(TOOLTIP_STYLE));
        tooltip.add(Text.translatable("item.oh-yeah.tiansuluo_egg.desc_2").setStyle(TOOLTIP_STYLE));
    }
}
