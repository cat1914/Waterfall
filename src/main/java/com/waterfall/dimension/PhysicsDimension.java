package com.waterfall.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.OptionalLong;

public class PhysicsDimension {
    public static final String PHYSICS_DIMENSION_ID = "physics_lab";
    
    public static final ResourceKey<DimensionType> PHYSICS_DIMENSION_TYPE = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        new ResourceLocation("waterfall", PHYSICS_DIMENSION_ID)
    );
    
    public static final ResourceKey<Level> PHYSICS_LEVEL = ResourceKey.create(
        Registries.LEVEL,
        new ResourceLocation("waterfall", PHYSICS_DIMENSION_ID)
    );
    
    public static void register() {
    }
    
    public static OptionalLong getFixedTime() {
        return OptionalLong.of(6000L);
    }
    
    public static boolean hasSkyLight() {
        return false;
    }
    
    public static boolean hasCeiling() {
        return false;
    }
    
    public static boolean ultraWarm() {
        return false;
    }
    
    public static boolean natural() {
        return false;
    }
    
    public static double coordinateScale() {
        return 1.0;
    }
    
    public static double bedWorks() {
        return false ? 1.0 : 0.0;
    }
    
    public static boolean respawnAnchorWorks() {
        return false;
    }
    
    public static boolean hasRaids() {
        return false;
    }
    
    public static int minY() {
        return -64;
    }
    
    public static int height() {
        return 384;
    }
    
    public static int logicalHeight() {
        return 384;
    }
}
