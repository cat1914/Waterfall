package com.waterfall;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.dimension.PhysicsDimensionType;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.network.PhysicsPacketHandler;
import com.waterfall.physics.PhysicsEngineManager;

import java.util.Optional;

@Mod(WaterfallMod.MODID)
public class WaterfallMod {
    public static final String MODID = "waterfall";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = DeferredRegister.create(Registries.DIMENSION_TYPE, MODID);
    public static final DeferredRegister<LevelStem> DIMENSIONS = DeferredRegister.create(Registries.LEVEL_STEM, MODID);
    
    public static final DeferredHolder<DimensionType, DimensionType> PHYSICS_DIMENSION_TYPE = 
        DIMENSION_TYPES.register(PhysicsDimension.PHYSICS_DIMENSION_ID, () ->
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
    
    public WaterfallMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Waterfall Physics Mod");
        
        DIMENSION_TYPES.register(modEventBus);
        DIMENSIONS.register(modEventBus);
        
        PhysicsDimension.register();
        PhysicsConfig.register();
        PhysicsDimensionType.register(modEventBus);
        PhysicsEntityType.register(modEventBus);
        com.waterfall.block.PhysicsBlocks.register(modEventBus);
        com.waterfall.item.PhysicsItems.register(modEventBus);
        
        PhysicsPacketHandler.register();
        
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onServerStarting);
        modEventBus.addListener(this::onServerTick);
        
        LOGGER.info("Waterfall Physics Mod initialized successfully");
    }
    
    private void onCommonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info("Running common setup");
        event.enqueueWork(() -> {
            LOGGER.info("Physics engine manager initialized");
        });
    }
    
    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Server starting");
    }
    
    private void onServerTick(final ServerTickEvent.Post event) {
        event.getServer().getAllLevels().forEach(level -> {
            if (level.dimension().location().getNamespace().equals(MODID)) {
                PhysicsEngineManager.getInstance().tick(level);
            }
        });
    }
    
    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
