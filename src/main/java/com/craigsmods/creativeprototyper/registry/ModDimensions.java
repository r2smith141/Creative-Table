package com.craigsmods.creativeprototyper.registry;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import com.craigsmods.creativeprototyper.CreativePrototyper;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class ModDimensions {
    public static final ResourceKey<LevelStem> CREATIVE_DIMENSION_KEY = ResourceKey.create(Registries.LEVEL_STEM,
            new ResourceLocation(CreativePrototyper.MOD_ID, "creative_dimension")
    );
    public static final ResourceKey<Level> CREATIVE_DIMENSION_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(CreativePrototyper.MOD_ID, "creative_dimension")
    );

    // Resource key for our dimension type
    public static final ResourceKey<DimensionType> CREATIVE_DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            new ResourceLocation(CreativePrototyper.MOD_ID, "creative_dimension_type")
    );
    public static void bootstrapType(BootstapContext<DimensionType> context) {
        context.register(CREATIVE_DIMENSION_TYPE, new DimensionType(
                OptionalLong.of(6000), // fixedTime - noon
                true,  // hasSkylight - enable skylight
                false, // hasCeiling
                false, // ultraWarm
                false, // natural
                1.0,   // coordinateScale
                true,  // bedWorks
                false, // respawnAnchorWorks
                -64,   // minY
                384,   // height
                384,   // logicalHeight
                BlockTags.INFINIBURN_OVERWORLD, // infiniburn
                new ResourceLocation(CreativePrototyper.MOD_ID, "creative_sky"), // Reference to our custom sky
                1.0f,  // ambientLight - full brightness 
                new DimensionType.MonsterSettings(
                        false, // piglinSafe
                        false, // hasRaids
                        ConstantInt.of(0), // monsterSpawnLightLevel
                        0      // monsterSpawnBlockLightLimit
                    )));
    }
    

        public static void bootstrapStem(BootstapContext<LevelStem> context) {
    HolderGetter<Biome> biomeRegistry = context.lookup(Registries.BIOME);
    HolderGetter<DimensionType> dimTypes = context.lookup(Registries.DIMENSION_TYPE);

        Optional<HolderSet<StructureSet>> noStructureOverrides = Optional.of(HolderSet.direct(List.of()));
    FlatLevelGeneratorSettings flatSettings = new FlatLevelGeneratorSettings(noStructureOverrides, biomeRegistry.getOrThrow(Biomes.DESERT), null); // plains biome
    flatSettings.getLayersInfo().clear();
    flatSettings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR)); 

    

    FlatLevelSource flatGenerator = new FlatLevelSource(flatSettings);
    
    LevelStem stem = new LevelStem(
            dimTypes.getOrThrow(ModDimensions.CREATIVE_DIMENSION_TYPE), 
            flatGenerator);
    
    context.register(CREATIVE_DIMENSION_KEY, stem);
}
}