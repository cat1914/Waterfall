package com.waterfall.dimension;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import com.waterfall.WaterfallMod;

/**
 * 物理维度类型常量
 */
public class PhysicsDimensions {
    public static final ResourceKey<DimensionType> PHYSICS_LAB_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation(WaterfallMod.MODID, "physics_lab")
    );
    
    public static final ResourceKey<Level> PHYSICS_LAB = ResourceKey.create(
        Registries.LEVEL,
        new ResourceLocation(WaterfallMod.MODID, "physics_lab")
    );
    
    private static final ResourceKey<?>[] DIMENSION_KEYS = {
        PHYSICS_LAB_TYPE,
        PHYSICS_LAB
    };
}
