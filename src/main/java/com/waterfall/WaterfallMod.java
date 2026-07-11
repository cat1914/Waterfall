package com.waterfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.block.PhysicsBlocks;
import com.waterfall.physics.PhysicsBody;
import com.waterfall.physics.PhysicsWorld;
import com.waterfall.natives.NativeLoader;

/**
 * Waterfall Mod 主入口
 *
 * 架构：
 *   PhysicsWorld（heavy） ← 每 tick 由服务端 PhysicsBlockEntity 各自推进
 *   ↑
 *   PhysicsBlockEntity（主世界）：持有 PhysicsBody + Force，tick 时交给 heavy 计算
 *   ↑
 *   玩家交互 → 映射到物理维度的原版方块 → BlockState.use()
 */
@Mod(WaterfallMod.MODID)
public class WaterfallMod {
    public static final String MODID = "waterfall";
    public static final Logger LOGGER = LogUtils.getLogger();

    // heavy 原生库的全局 PhysicsWorld（所有 PhysicsBlockEntity 的 body 都挂在这里）
    private static PhysicsWorld HEAVY_WORLD = null;

    // DimensionType 注册
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
        DeferredRegister.create(Registries.DIMENSION_TYPE, MODID);

    public WaterfallMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Waterfall Physics Mod");

        // 加载原生库 (heavy / direction)
        try {
            NativeLoader.loadHeavy();
            NativeLoader.loadDirection();
            LOGGER.info("Native libraries loaded successfully");
        } catch (Throwable e) {
            LOGGER.error("Failed to load native libraries", e);
        }

        // 配置
        PhysicsConfig.register();

        // 注册内容
        DIMENSION_TYPES.register(modEventBus);
        PhysicsEntityType.register(modEventBus);
        PhysicsBlocks.register(modEventBus);

        // 服务器启动时：缓存物理维度引用 + 创建 heavy PhysicsWorld
        // 注意：ServerStartingEvent 不是 IModBusEvent，必须注册到 NeoForge.EVENT_BUS
        // （游戏事件总线），不能注册到 modEventBus，否则启动时抛 IllegalArgumentException。
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        LOGGER.info("Waterfall Physics Mod initialized");
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Server starting - caching physics dimension and creating heavy world");

        // 1) 缓存物理维度
        ServerLevel physicsLevel = event.getServer().getLevel(PhysicsDimension.LEVEL_KEY);
        if (physicsLevel != null) {
            PhysicsDimension.cacheLevel(physicsLevel);
            LOGGER.info("Physics dimension cached at {}", physicsLevel.dimension().location());
        } else {
            LOGGER.warn("Physics dimension not found - ensure dimension json files exist!");
        }

        // 2) 建立 heavy 的 PhysicsWorld
        if (HEAVY_WORLD != null) {
            try {
                HEAVY_WORLD.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing previous heavy world: {}", e.getMessage());
            }
            HEAVY_WORLD = null;
        }
        try {
            HEAVY_WORLD = new PhysicsWorld();
            LOGGER.info("heavy PhysicsWorld created");
        } catch (Throwable e) {
            LOGGER.error("Failed to create heavy PhysicsWorld - native library may be unavailable", e);
        }
    }

    public static PhysicsWorld getHeavyWorld() {
        return HEAVY_WORLD;
    }

    public static void addPhysicsBodyToWorld(PhysicsBody body) {
        if (body == null) return;
        if (HEAVY_WORLD != null) {
            try {
                HEAVY_WORLD.addBody(body);
            } catch (Throwable e) {
                LOGGER.warn("addPhysicsBodyToWorld failed: {}", e.getMessage());
            }
        }
    }

    public static void removePhysicsBodyFromWorld(PhysicsBody body) {
        if (body == null) return;
        if (HEAVY_WORLD != null) {
            try {
                HEAVY_WORLD.removeBody(body);
            } catch (Throwable e) {
                LOGGER.warn("removePhysicsBodyFromWorld failed: {}", e.getMessage());
            }
        }
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
