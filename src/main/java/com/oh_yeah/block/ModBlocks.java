package com.oh_yeah.block;

import com.oh_yeah.OhYeah;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static final Block TIANSULUO_PINK_SCARF_LUANLUAN_BLOCK = register(
            "tiansuluo_pink_scarf_luanluan_block",
            new LuanluanEggBlock(
                    "tiansuluo_pink_scarf",
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(0.2F)
                            .sounds(BlockSoundGroup.CALCITE)
                            .ticksRandomly()
                            .nonOpaque()
            )
    );
    public static final Block TIANSULUO_BATTLE_FACE_LUANLUAN_BLOCK = register(
            "tiansuluo_battle_face_luanluan_block",
            new LuanluanEggBlock(
                    "tiansuluo_battle_face",
                    AbstractBlock.Settings.create()
                            .mapColor(MapColor.PALE_YELLOW)
                            .strength(0.2F)
                            .sounds(BlockSoundGroup.CALCITE)
                            .ticksRandomly()
                            .nonOpaque()
            )
    );

    private ModBlocks() {
    }

    public static void initialize() {
    }

    private static Block register(String id, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(OhYeah.MOD_ID, id), block);
    }
}
