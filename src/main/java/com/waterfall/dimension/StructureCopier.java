package com.waterfall.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.waterfall.WaterfallMod;

/**
 * 结构复制器（Structure Copier）
 * 
 * 将主世界中的方块结构复制到物理维度，并删除主世界中的原方块。
 * 
 * 执行流程：
 * 1. 遍历输入的所有方块位置和状态
 * 2. 为每个物理化结构分配唯一的ID和物理维度中的原点位置
 * 3. 在物理维度的指定位置放置对应的方块和方块实体
 * 4. 从主世界删除原方块
 * 5. 返回结构数据供代理实体使用
 */
public class StructureCopier {
    
    private static final AtomicLong STRUCTURE_ID_COUNTER = new AtomicLong(0);
    
    /**
     * 将主世界中的方块结构复制到物理维度
     * 
     * @param mainLevel 主世界
     * @param physicsLevel 物理维度
     * @param centerPos 结构中心位置（主世界坐标）
     * @param blocks 局部位置 -> 方块状态 映射
     * @param removeFromMainWorld 是否从主世界移除原方块
     * @return 结构复制结果
     */
    public static StructureCopyResult copyStructureToPhysicsDimension(
            ServerLevel mainLevel,
            ServerLevel physicsLevel,
            Vec3 centerPos,
            Map<BlockPos, BlockState> blocks) {
        
        if (physicsLevel == null) {
            WaterfallMod.LOGGER.error("Physics dimension not available!");
            return null;
        }
        
        long structureId = STRUCTURE_ID_COUNTER.incrementAndGet();
        
        // 获取物理维度中的结构原点
        BlockPos physicsOrigin = PhysicsDimension.getStructureOrigin(structureId);
        
        // 存储局部位置 -> 原始世界位置 映射
        Map<BlockPos, BlockPos> localToWorldMap = new HashMap<>();
        BlockPos centerBlockPos = new BlockPos((int) Math.floor(centerPos.x), 
                                                (int) Math.floor(centerPos.y), 
                                                (int) Math.floor(centerPos.z));
        
        int copiedCount = 0;
        int failedCount = 0;
        
        // 复制每个方块到物理维度
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos localPos = entry.getKey();
            BlockState state = entry.getValue();
            
            if (state == null || state.isAir()) {
                continue;
            }
            
            // 计算物理维度中的位置
            BlockPos physicsPos = physicsOrigin.offset(localPos);
            
            // 计算主世界中的原始位置
            BlockPos worldPos = centerBlockPos.offset(localPos);
            
            // 在物理维度放置方块
            boolean placed = physicsLevel.setBlock(physicsPos, state, 3);
            
            if (placed) {
                // 如果有方块实体（如箱子），需要复制其NBT
                BlockEntity mainBlockEntity = mainLevel.getBlockEntity(worldPos);
                if (mainBlockEntity != null) {
                    try {
                        CompoundTag blockEntityNbt = mainBlockEntity.saveWithId(mainLevel.registryAccess());
                        physicsLevel.getBlockEntity(physicsPos).loadWithComponents(blockEntityNbt, mainLevel.registryAccess());
                    } catch (Exception e) {
                        WaterfallMod.LOGGER.warn("Failed to copy block entity at {}: {}", worldPos, e.getMessage());
                    }
                }
                
                localToWorldMap.put(localPos, worldPos);
                copiedCount++;
            } else {
                failedCount++;
            }
        }
        
        // 从主世界删除原方块
        for (Map.Entry<BlockPos, BlockPos> entry : localToWorldMap.entrySet()) {
            BlockPos worldPos = entry.getValue();
            if (!mainLevel.getBlockState(worldPos).isAir()) {
                mainLevel.setBlock(worldPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        
        WaterfallMod.LOGGER.info("Copied structure #{}: {} blocks copied, {} failed, origin {}",
            structureId, copiedCount, failedCount, physicsOrigin);
        
        return new StructureCopyResult(structureId, physicsOrigin, localToWorldMap, copiedCount);
    }
    
    /**
     * 从世界区域收集方块并复制到物理维度
     */
    public static StructureCopyResult copyFromWorldArea(
            ServerLevel mainLevel,
            ServerLevel physicsLevel,
            Vec3 centerPos,
            BlockPos minPos,
            BlockPos maxPos,
            boolean removeFromMainWorld) {
        
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos centerBlockPos = new BlockPos((int) Math.floor(centerPos.x), 
                                                (int) Math.floor(centerPos.y), 
                                                (int) Math.floor(centerPos.z));
        
        // 收集范围内的所有非空气方块
        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = mainLevel.getBlockState(worldPos);
                    
                    if (!state.isAir()) {
                        BlockPos localPos = new BlockPos(
                            x - centerBlockPos.getX(),
                            y - centerBlockPos.getY(),
                            z - centerBlockPos.getZ()
                        );
                        blocks.put(localPos, state);
                    }
                }
            }
        }
        
        return copyStructureToPhysicsDimension(mainLevel, physicsLevel, centerPos, blocks);
    }
    
    /**
     * 销毁物理维度中的结构，并可选地将方块恢复到主世界
     */
    public static void destroyStructure(
            ServerLevel mainLevel,
            ServerLevel physicsLevel,
            long structureId,
            UUID entityId,
            Map<BlockPos, BlockPos> localToWorldMap,
            BlockPos physicsOrigin,
            Vec3 centerPos,
            boolean restoreToMainWorld) {
        
        if (physicsLevel == null) {
            return;
        }
        
        // 遍历所有局部位置
        for (Map.Entry<BlockPos, BlockPos> entry : localToWorldMap.entrySet()) {
            BlockPos localPos = entry.getKey();
            BlockPos physicsPos = physicsOrigin.offset(localPos);
            
            // 获取物理维度中的方块状态
            BlockState currentState = physicsLevel.getBlockState(physicsPos);
            
            // 从物理维度清除
            if (!currentState.isAir()) {
                physicsLevel.setBlock(physicsPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
            
            // 可选地恢复到主世界
            if (restoreToMainWorld && mainLevel != null) {
                BlockPos worldPos = entry.getValue();
                if (!currentState.isAir()) {
                    mainLevel.setBlock(worldPos, currentState, 3);
                }
            }
        }
        
        WaterfallMod.LOGGER.info("Destroyed physics structure #{} with {} blocks", 
            structureId, localToWorldMap.size());
    }
    
    /**
     * 结构复制结果
     */
    public static class StructureCopyResult {
        public final long structureId;
        public final BlockPos physicsOrigin;
        public final Map<BlockPos, BlockPos> localToWorldMap;
        public final int blockCount;
        
        public StructureCopyResult(long structureId, BlockPos physicsOrigin, 
                                   Map<BlockPos, BlockPos> localToWorldMap, int blockCount) {
            this.structureId = structureId;
            this.physicsOrigin = physicsOrigin;
            this.localToWorldMap = localToWorldMap;
            this.blockCount = blockCount;
        }
    }
    
}
