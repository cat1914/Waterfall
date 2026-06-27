package com.waterfall.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;

/**
 * 物理维度（Physics Dimension）
 *
 * 角色：只存放物理化结构的方块（作为原版方块交互的数据源）
 * 不做：任何物理计算（重力、碰撞、浮力全部在主世界实体中完成）
 *
 * 当结构被物理化时：
 * 1. 方块从主世界复制到物理维度（位置由 structureId 映射）
 * 2. 主世界的原方块被删除
 * 3. 主世界生成 PhysicsBlockEntity 代理实体（负责物理+碰撞+渲染+交互）
 * 4. 玩家交互映射到物理维度的方块 → BlockState.use() 在物理维度执行
 * 5. 物理维度的方块状态变化会被代理实体读取以进行渲染
 *
 * 物理维度是一个空的虚空维度，用于存放所有物理化的结构。
 */
public class PhysicsDimension {
    
    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(WaterfallMod.MODID, "physics_dimension")
    );
    
    public static final ResourceKey<DimensionType> DIMENSION_TYPE_KEY = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        ResourceLocation.fromNamespaceAndPath(WaterfallMod.MODID, "physics_dimension")
    );
    
    private static ServerLevel cachedLevel;
    
    /**
     * 获取物理维度的 ServerLevel
     */
    public static ServerLevel getLevel(MinecraftServer server) {
        if (server == null) return null;
        return server.getLevel(LEVEL_KEY);
    }
    
    /**
     * 缓存物理维度的引用（启动时设置）
     */
    public static void cacheLevel(ServerLevel level) {
        cachedLevel = level;
    }
    
    /**
     * 获取缓存的物理维度
     */
    public static ServerLevel getCachedLevel() {
        return cachedLevel;
    }
    
    /**
     * 根据主世界的位置计算在物理维度中的映射位置
     * 
     * 映射规则：使用结构ID作为Z区块坐标偏移，避免冲突
     * 每个物理化结构占用一个 16x256x16 的区块区域
     */
    public static BlockPos mapToPhysicsDimension(BlockPos mainWorldPos, long structureId) {
        // 使用结构ID生成唯一的区块偏移
        int regionX = ((int) (structureId % 10000)) * 2;
        int regionZ = ((int) ((structureId / 10000) % 10000)) * 2;
        
        // 映射到物理维度
        int x = regionX * 16 + mainWorldPos.getX();
        int y = 64 + mainWorldPos.getY(); // 放到空中
        int z = regionZ * 16 + mainWorldPos.getZ();
        
        return new BlockPos(x, y, z);
    }
    
    /**
     * 获取结构在物理维度中的原点位置
     */
    public static BlockPos getStructureOrigin(long structureId) {
        int regionX = ((int) (structureId % 10000)) * 2;
        int regionZ = ((int) ((structureId / 10000) % 10000)) * 2;
        
        return new BlockPos(regionX * 16, 64, regionZ * 16);
    }
    
}
