package com.waterfall.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.waterfall.WaterfallMod;

/**
 * 交互映射器（Interaction Mapper）
 * 
 * 负责将主世界代理实体上的交互映射到物理维度中的实际方块上。
 * 
 * 工作流程：
 * 1. 玩家右键/左键点击主世界的代理实体
 * 2. 计算点击命中的具体局部方块位置
 * 3. 将局部位置映射到物理维度中的实际坐标
 * 4. 在物理维度执行方块交互
 * 5. 将结果同步回主世界代理实体
 */
public class InteractionMapper {
    
    private static final Map<UUID, StructureMapping> MAPPINGS = new HashMap<>();
    
    /**
     * 注册一个新的结构映射
     * 
     * @param entityId 主世界代理实体的UUID
     * @param originInPhysicsDim 物理维度中的结构原点
     * @param localToWorldMap 局部位置到原始世界位置的映射（用于恢复）
     */
    public static void registerMapping(UUID entityId, BlockPos originInPhysicsDim, 
                                        Map<BlockPos, BlockPos> localToWorldMap) {
        StructureMapping mapping = new StructureMapping(originInPhysicsDim, localToWorldMap);
        MAPPINGS.put(entityId, mapping);
        WaterfallMod.LOGGER.info("Registered physics structure mapping: {} -> origin {}", entityId, originInPhysicsDim);
    }
    
    /**
     * 移除结构映射
     */
    public static void removeMapping(UUID entityId) {
        StructureMapping mapping = MAPPINGS.remove(entityId);
        if (mapping != null) {
            WaterfallMod.LOGGER.info("Removed physics structure mapping: {}", entityId);
        }
    }
    
    /**
     * 获取结构映射
     */
    public static StructureMapping getMapping(UUID entityId) {
        return MAPPINGS.get(entityId);
    }
    
    /**
     * 将局部位置转换为物理维度中的实际位置
     */
    public static BlockPos localToPhysicsPos(UUID entityId, BlockPos localPos) {
        StructureMapping mapping = MAPPINGS.get(entityId);
        if (mapping == null) return null;
        return mapping.origin.offset(localPos);
    }
    
    /**
     * 处理右键交互
     * 
     * @param entityId 代理实体UUID
     * @param player 玩家
     * @param hand 交互的手
     * @param localPos 点击到的局部方块位置
     * @param hitPos 精确的碰撞点位置
     * @param physicsLevel 物理维度
     * @return 交互结果
     */
    public static InteractionResult handleRightClick(UUID entityId, Player player, 
                                                      InteractionHand hand,
                                                      BlockPos localPos, Vec3 hitPos,
                                                      ServerLevel physicsLevel) {
        BlockPos physicsPos = localToPhysicsPos(entityId, localPos);
        if (physicsPos == null) {
            return InteractionResult.PASS;
        }
        
        BlockState state = physicsLevel.getBlockState(physicsPos);
        if (state.isAir()) {
            return InteractionResult.PASS;
        }
        
        // 在物理维度执行方块交互
        Vec3 physicsHitPos = new Vec3(
            physicsPos.getX() + (hitPos.x - Math.floor(hitPos.x)),
            physicsPos.getY() + (hitPos.y - Math.floor(hitPos.y)),
            physicsPos.getZ() + (hitPos.z - Math.floor(hitPos.z))
        );
        
        BlockHitResult hitResult = new BlockHitResult(
            physicsHitPos,
            player.getDirection(),
            physicsPos,
            false
        );
        
        InteractionResult result = state.use(
            player.level() instanceof ServerLevel ? player : player,
            player,
            hand,
            hitResult
        );
        
        return result;
    }
    
    /**
     * 处理左键攻击（破坏方块）
     */
    public static boolean handleLeftClick(UUID entityId, BlockPos localPos, 
                                          ServerLevel physicsLevel) {
        BlockPos physicsPos = localToPhysicsPos(entityId, localPos);
        if (physicsPos == null) return false;
        
        BlockState state = physicsLevel.getBlockState(physicsPos);
        if (state.isAir()) return false;
        
        // 在物理维度执行方块破坏逻辑
        return physicsLevel.destroyBlock(physicsPos, false);
    }
    
    /**
     * 获取物理维度中指定局部位置的方块状态
     */
    public static BlockState getBlockState(UUID entityId, BlockPos localPos, 
                                            ServerLevel physicsLevel) {
        BlockPos physicsPos = localToPhysicsPos(entityId, localPos);
        if (physicsPos == null) return null;
        return physicsLevel.getBlockState(physicsPos);
    }
    
    /**
     * 设置物理维度中指定局部位置的方块状态
     */
    public static boolean setBlockState(UUID entityId, BlockPos localPos, 
                                        BlockState state, ServerLevel physicsLevel, 
                                        int flags) {
        BlockPos physicsPos = localToPhysicsPos(entityId, localPos);
        if (physicsPos == null) return false;
        return physicsLevel.setBlock(physicsPos, state, flags);
    }
    
    /**
     * 清理物理维度中的结构方块（结构销毁时调用）
     */
    public static void clearStructureInPhysicsDimension(UUID entityId, ServerLevel physicsLevel) {
        StructureMapping mapping = MAPPINGS.get(entityId);
        if (mapping == null || physicsLevel == null) return;
        
        for (BlockPos localPos : mapping.localToWorldMap.keySet()) {
            BlockPos physicsPos = mapping.origin.offset(localPos);
            physicsLevel.setBlock(physicsPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }
    
    /**
     * 获取结构在物理维度的原点
     */
    public static BlockPos getPhysicsOrigin(UUID entityId) {
        StructureMapping mapping = MAPPINGS.get(entityId);
        return mapping != null ? mapping.origin : null;
    }
    
    /**
     * 结构映射数据
     */
    public static class StructureMapping {
        public final BlockPos origin;
        public final Map<BlockPos, BlockPos> localToWorldMap;
        
        public StructureMapping(BlockPos origin, Map<BlockPos, BlockPos> localToWorldMap) {
            this.origin = origin;
            this.localToWorldMap = localToWorldMap;
        }
    }
}
