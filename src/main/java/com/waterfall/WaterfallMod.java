package com.waterfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.level.dimension.DimensionType;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.block.PhysicsBlocks;
import com.waterfall.block.PhysicsBlockEntities;
import com.waterfall.network.PhysicsPacketHandler;
import com.waterfall.natives.NativeLoader;
import com.waterfall.physics.PhysicsEngineManager;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import com.waterfall.physics.rotation.RotationalBodyManager;

@Mod(WaterfallMod.MODID)
public class WaterfallMod {
    public static final String MODID = "waterfall";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 维度类型注册
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES = 
        DeferredRegister.create(Registries.DIMENSION_TYPE, MODID);
    
    public WaterfallMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Waterfall Physics Mod");
        
        // 初始化原生库
        try {
            NativeLoader.loadHeavy();
            NativeLoader.loadDirection();
            LOGGER.info("Native libraries loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load native libraries", e);
        }
        
        // 注册配置
        PhysicsConfig.register();
        
        // 注册维度类型
        DIMENSION_TYPES.register(modEventBus);
        
        // 注册实体类型
        PhysicsEntityType.register(modEventBus);
        
        // 注册方块相关
        PhysicsBlocks.register(modEventBus);
        PhysicsBlockEntities.register(modEventBus);
        com.waterfall.item.PhysicsItems.register(modEventBus);
        
        // 注册网络
        PhysicsPacketHandler.register();
        
        // 事件监听
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
        LOGGER.info("Server starting - preparing physics dimension");
        
        // 缓存物理维度引用
        ServerLevel physicsLevel = event.getServer().getLevel(PhysicsDimension.LEVEL_KEY);
        if (physicsLevel != null) {
            PhysicsDimension.cacheLevel(physicsLevel);
            LOGGER.info("Physics dimension cached successfully");
        } else {
            LOGGER.warn("Physics dimension not found! Make sure dimension JSON data is present.");
        }
    }
    
    private void onServerTick(final ServerTickEvent.Post event) {
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
        
        // 确保物理维度已缓存
        if (physicsLevel == null) {
            ServerLevel level = event.getServer().getLevel(PhysicsDimension.LEVEL_KEY);
            if (level != null) {
                PhysicsDimension.cacheLevel(level);
                physicsLevel = level;
            }
        }
        
        // Tick 物理引擎（在物理维度中执行）
        if (physicsLevel != null) {
            RigidBodyManager.getInstance().tick(physicsLevel);
            RotationalBodyManager.getInstance().tick(physicsLevel);
        }
    }
    
    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
