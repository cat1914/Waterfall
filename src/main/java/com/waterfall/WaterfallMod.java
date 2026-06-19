package com.waterfall;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.level.dimension.DimensionType;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.block.PhysicsBlocks;
import com.waterfall.network.PhysicsPacketHandler;
import com.waterfall.natives.NativeLoader;

/**
 * Waterfall Mod 主入口
 *
 * 架构说明：
 * - 主世界：PhysicsBlockEntity 负责真实物理计算（重力、浮力、碰撞）、渲染、玩家交互
 * - 物理维度：只存放原版方块（作为交互映射的数据源），不做任何物理 tick
 *
 * 物理流程：
 *   Minecraft 主 tick → PhysicsBlockEntity.tick() → 在主世界做重力/浮力/碰撞
 *   玩家交互 → PhysicsBlockEntity.interact() → 映射到物理维度坐标 → BlockState.use()
 */
@Mod(WaterfallMod.MODID)
public class WaterfallMod {
    public static final String MODID = "waterfall";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 维度类型注册（物理维度的类型定义）
    public static final DeferredRegister<DimensionType> DIMENSION_TYPES =
        DeferredRegister.create(Registries.DIMENSION_TYPE, MODID);

    public WaterfallMod(IEventBus modEventBus) {
        LOGGER.info("Initializing Waterfall Physics Mod");

        // 加载原生库
        try {
            NativeLoader.loadHeavy();
            NativeLoader.loadDirection();
            LOGGER.info("Native libraries loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to load native libraries", e);
        }

        // 注册配置
        PhysicsConfig.register();

        // 注册物理维度类型（资源文件在 data/waterfall/dimension_type/...）
        DIMENSION_TYPES.register(modEventBus);

        // 注册实体类型
        PhysicsEntityType.register(modEventBus);

        // 注册方块
        PhysicsBlocks.register(modEventBus);

        // 网络
        PhysicsPacketHandler.register();

        // 服务器启动时缓存物理维度引用
        modEventBus.addListener(this::onServerStarting);

        LOGGER.info("Waterfall Physics Mod initialized successfully");
    }

    private void onServerStarting(final ServerStartingEvent event) {
        LOGGER.info("Server starting - caching physics dimension reference");

        ServerLevel physicsLevel = event.getServer().getLevel(PhysicsDimension.LEVEL_KEY);
        if (physicsLevel != null) {
            PhysicsDimension.cacheLevel(physicsLevel);
            LOGGER.info("Physics dimension cached at {}", physicsLevel.dimension().location());
        } else {
            LOGGER.warn("Physics dimension not found - ensure dimension json files exist!");
        }
    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
