package com.oh_yeah.entity;

import com.oh_yeah.OhYeah;
import com.oh_yeah.config.OhYeahConfigManager;
import com.oh_yeah.config.SpeciesConfig;
import com.oh_yeah.config.VariantConfig;
import com.oh_yeah.entity.projectile.TiansuluoProjectileEntity;
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
    public static final Identifier TIANSULUO_ID = Identifier.of(OhYeah.MOD_ID, "tiansuluo");
    public static final EntityType<TiansuluoEntity> TIANSULUO = Registry.register(
            Registries.ENTITY_TYPE,
            TIANSULUO_ID,
            EntityType.Builder.create(TiansuluoEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.6F, 1.8F)
                    .maxTrackingRange(8)
                    .build(TIANSULUO_ID.toString())
    );
    public static final Identifier TIANSULUO_PROJECTILE_ID = Identifier.of(OhYeah.MOD_ID, "tiansuluo_projectile");
    public static final EntityType<TiansuluoProjectileEntity> TIANSULUO_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            TIANSULUO_PROJECTILE_ID,
            EntityType.Builder.<TiansuluoProjectileEntity>create(TiansuluoProjectileEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build(TIANSULUO_PROJECTILE_ID.toString())
    );

    private ModEntityTypes() {
    }

    public static void initialize() {
        SpeciesConfig config = OhYeahConfigManager.getTiansuluoConfig();
        SpawnRestriction.register(TIANSULUO, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ModEntityTypes::canSpawnTiansuluo);
        FabricDefaultAttributeRegistry.register(TIANSULUO, TiansuluoEntity.createAttributes());

        if (config.enableNaturalSpawn()) {
            List<RegistryKey<Biome>> biomes = resolveBiomes(config);
            if (!biomes.isEmpty()) {
                BiomeModifications.addSpawn(
                        BiomeSelectors.includeByKey(biomes.toArray(RegistryKey[]::new)),
                        SpawnGroup.CREATURE,
                        TIANSULUO,
                        config.spawnWeight(),
                        config.spawnMinGroup(),
                        config.spawnMaxGroup()
                );
            }
        }
    }

    private static boolean canSpawnTiansuluo(EntityType<TiansuluoEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        if (!AnimalEntity.isValidNaturalSpawn(type, world, spawnReason, pos, random)) {
            return false;
        }
        SpeciesConfig cfg = OhYeahConfigManager.getTiansuluoConfig();
        TiansuluoVariant variant = TiansuluoEntity.selectSpawnVariant(world, pos, spawnReason, random, cfg);
        VariantConfig variantCfg = cfg.getVariantConfig(variant);
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
