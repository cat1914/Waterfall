package com.waterfall;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.block.PhysicsBlocks;
import com.waterfall.block.PhysicsBlockEntities;
import com.waterfall.network.PhysicsPacketHandler;
import com.waterfall.physics.PhysicsEngineManager;
import com.waterfall.physics.rigidbody.RigidBodyManager;

@Mod(WaterfallMod.MODID)
public class WaterfallMod {
    public static final String MODID = "waterfall";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public WaterfallMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Waterfall Physics Mod");
        
        PhysicsConfig.register();
        PhysicsEntityType.register(modEventBus);
        PhysicsBlocks.register(modEventBus);
        PhysicsBlockEntities.register(modEventBus);
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
            // 在所有世界中运行物理模拟
            PhysicsEngineManager.getInstance().tick(level);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                RigidBodyManager.getInstance().tick(serverLevel);
            }
        });
    }
    
    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
