package com.waterfall.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Optional;

public class PhysicsDimensionType {
    public static void register(IEventBus eventBus) {
    }
    
    public static void bootstrapType(BootstapContext<DimensionType> context) {
        context.register(
            PhysicsDimension.PHYSICS_DIMENSION_TYPE,
            DimensionType.builder()
                .fixedTime(Optional.of(6000L))
                .hasSkyLight(false)
                .hasCeiling(false)
                .ultraWarm(false)
                .natural(false)
                .coordinateScale(1.0)
                .bedWorks(false)
                .respawnAnchorWorks(false)
                .hasRaids(false)
                .minY(-64)
                .height(384)
                .logicalHeight(384)
                .build()
        );
    }
}
