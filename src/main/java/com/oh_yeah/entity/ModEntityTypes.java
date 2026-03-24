package com.oh_yeah.entity;

import com.oh_yeah.OhYeah;
import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.VariantConfig;
import com.oh_yeah.entity.projectile.TiansuluoPinkScarfProjectileEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public final class ModEntityTypes {
    public static final Identifier TIANSULUO_PINK_SCARF_ID = Identifier.of(OhYeah.MOD_ID, "tiansuluo_pink_scarf");
    public static final EntityType<TiansuluoPinkScarfEntity> TIANSULUO_PINK_SCARF = Registry.register(
            Registries.ENTITY_TYPE,
            TIANSULUO_PINK_SCARF_ID,
            EntityType.Builder.create(TiansuluoPinkScarfEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.8F)
                    .maxTrackingRange(8)
                    .build(TIANSULUO_PINK_SCARF_ID.toString())
    );
    public static final Identifier TIANSULUO_PINK_SCARF_PROJECTILE_ID = Identifier.of(OhYeah.MOD_ID, "tiansuluo_pink_scarf_projectile");
    public static final EntityType<TiansuluoPinkScarfProjectileEntity> TIANSULUO_PINK_SCARF_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            TIANSULUO_PINK_SCARF_PROJECTILE_ID,
            EntityType.Builder.<TiansuluoPinkScarfProjectileEntity>create(TiansuluoPinkScarfProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build(TIANSULUO_PINK_SCARF_PROJECTILE_ID.toString())
    );
    public static final Identifier TIANSULUO_BATTLE_FACE_ID = Identifier.of(OhYeah.MOD_ID, "tiansuluo_battle_face");
    public static final EntityType<TiansuluoBattleFaceEntity> TIANSULUO_BATTLE_FACE = Registry.register(
            Registries.ENTITY_TYPE,
            TIANSULUO_BATTLE_FACE_ID,
            EntityType.Builder.create(TiansuluoBattleFaceEntity::new, SpawnGroup.CREATURE)
                    .dimensions(TiansuluoBattleFaceEntity.TARGET_ADULT_WIDTH, TiansuluoBattleFaceEntity.TARGET_ADULT_HEIGHT)
                    .maxTrackingRange(8)
                    .build(TIANSULUO_BATTLE_FACE_ID.toString())
    );

    private ModEntityTypes() {
    }

    public static void initialize() {
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoPinkScarfConfig();
        SpeciesConfig battleFaceConfig = OhYeahConfigManager.getTiansuluoBattleFaceConfig();
        SpawnRestriction.register(TIANSULUO_PINK_SCARF, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntityTypes::canSpawnTiansuluoPinkScarf);
        SpawnRestriction.register(TIANSULUO_BATTLE_FACE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntityTypes::canSpawnTiansuluoBattleFace);
        FabricDefaultAttributeRegistry.register(TIANSULUO_PINK_SCARF, TiansuluoPinkScarfEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TIANSULUO_BATTLE_FACE, TiansuluoBattleFaceEntity.createAttributes());

        if (config.enableNaturalSpawn()) {
            List<RegistryKey<Biome>> biomes = resolveBiomes(config);
            if (!biomes.isEmpty()) {
                BiomeModifications.addSpawn(
                        BiomeSelectors.includeByKey(biomes.toArray(RegistryKey[]::new)),
                        SpawnGroup.CREATURE,
                        TIANSULUO_PINK_SCARF,
                        config.spawnWeight(),
                        config.spawnMinGroup(),
                        config.spawnMaxGroup()
                );
            }
        }

        if (battleFaceConfig.enableNaturalSpawn()) {
            List<RegistryKey<Biome>> biomes = resolveBiomes(battleFaceConfig);
            if (!biomes.isEmpty()) {
                BiomeModifications.addSpawn(
                        BiomeSelectors.includeByKey(biomes.toArray(RegistryKey[]::new)),
                        SpawnGroup.CREATURE,
                        TIANSULUO_BATTLE_FACE,
                        battleFaceConfig.spawnWeight(),
                        battleFaceConfig.spawnMinGroup(),
                        battleFaceConfig.spawnMaxGroup()
                );
            }
        }
    }

    private static boolean canSpawnTiansuluoPinkScarf(EntityType<TiansuluoPinkScarfEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        if (!AnimalEntity.isValidNaturalSpawn(type, world, spawnReason, pos, random)) {
            return false;
        }
        SpeciesConfig cfg = OhYeahConfigManager.getTiansuluoPinkScarfConfig();
        VariantConfig variantCfg = cfg.defaultVariantConfig();
        if (variantCfg == null) {
            return false;
        }
        return pos.getY() >= variantCfg.minY()
                && pos.getY() <= variantCfg.maxY()
                && (spawnReason != SpawnReason.NATURAL || world.getBaseLightLevel(pos, 0) >= variantCfg.minLight());
    }

    private static boolean canSpawnTiansuluoBattleFace(EntityType<TiansuluoBattleFaceEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        if (!AnimalEntity.isValidNaturalSpawn(type, world, spawnReason, pos, random)) {
            return false;
        }
        SpeciesConfig cfg = OhYeahConfigManager.getTiansuluoBattleFaceConfig();
        VariantConfig variantCfg = cfg.defaultVariantConfig();
        if (variantCfg == null) {
            return false;
        }
        return pos.getY() >= variantCfg.minY()
                && pos.getY() <= variantCfg.maxY()
                && (spawnReason != SpawnReason.NATURAL || world.getBaseLightLevel(pos, 0) >= variantCfg.minLight());
    }

    private static List<RegistryKey<Biome>> resolveBiomes(SpeciesConfig config) {
        List<RegistryKey<Biome>> keys = new ArrayList<>();
        for (String biome : config.allAllowedBiomes()) {
            Identifier id = Identifier.tryParse(biome);
            if (id != null) {
                keys.add(RegistryKey.of(RegistryKeys.BIOME, id));
            }
        }
        return keys;
    }
}
