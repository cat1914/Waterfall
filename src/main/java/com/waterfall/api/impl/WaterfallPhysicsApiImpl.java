package com.waterfall.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.waterfall.WaterfallMod;
import com.waterfall.api.WaterfallPhysicsApi;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.dimension.StructureCopier;
import com.waterfall.dimension.StructureCopier.StructureCopyResult;
import com.waterfall.entity.PhysicsBlockEntity;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.physics.MaterialPhysics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * WaterfallPhysicsApi 实现（基于物理维度 + 主世界实体双轨架构）
 *
 * 创建流程：
 *   1. 收集方块 map（BlockPos -> BlockState）
 *   2. 通过 StructureCopier 把方块复制到物理维度，从主世界删除原方块
 *   3. 在主世界创建 PhysicsBlockEntity 作为代理（做物理+碰撞+渲染+交互）
 *   4. PhysicsBlockEntity.tick 自己负责主世界物理计算
 *
 * 销毁流程：
 *   1. 从物理维度读取当前方块状态
 *   2. 放回主世界对应位置
 *   3. 清除物理维度中的方块
 *   4. 移除主世界实体
 */
public class WaterfallPhysicsApiImpl implements WaterfallPhysicsApi {

    @Override
    public PhysicsBlockEntity createPhysicsStructure(Level level, Vec3 position,
                                                     Map<BlockPos, BlockState> blocks) {
        if (!(level instanceof ServerLevel mainLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        // 获取物理维度（只用来存放方块，不做任何物理计算）
        ServerLevel physicsLevel = PhysicsDimension.getLevel(mainLevel.getServer());
        if (physicsLevel == null) {
            WaterfallMod.LOGGER.error("Physics dimension not available!");
            return null;
        }

        try {
            // 步骤 1：复制方块到物理维度，同时从主世界删除原方块
            StructureCopyResult copyResult = StructureCopier.copyStructureToPhysicsDimension(
                mainLevel, physicsLevel, position, blocks
            );
            if (copyResult == null || copyResult.blockCount == 0) {
                WaterfallMod.LOGGER.error("Failed to copy structure to physics dimension");
                return null;
            }

            // 步骤 2：统计轻/重质方块
            int lightBlocks = 0;
            int heavyBlocks = 0;
            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                BlockState state = entry.getValue();
                if (state == null || state.isAir()) continue;
                if (MaterialPhysics.isLightMaterial(state)) lightBlocks++;
                else if (MaterialPhysics.isHeavyMaterial(state)) heavyBlocks++;
            }

            // 步骤 3：在主世界创建 PhysicsBlockEntity 实体（它自己负责物理）
            PhysicsBlockEntity entity = new PhysicsBlockEntity(
                PhysicsEntityType.PHYSICS_BLOCK.get(), mainLevel
            );
            entity.setPos(position.x, position.y, position.z);
            entity.initializeFromPhysicsDimension(
                copyResult.physicsOrigin,
                new HashSet<>(copyResult.localToWorldMap.keySet()),
                copyResult.localToWorldMap,
                lightBlocks,
                heavyBlocks
            );

            // 步骤 4：生成到主世界
            mainLevel.addFreshEntity(entity);

            WaterfallMod.LOGGER.info(
                "Created physics structure #{} at {} with {} blocks (light:{}, heavy:{})",
                copyResult.structureId, position, copyResult.blockCount, lightBlocks, heavyBlocks
            );
            return entity;

        } catch (Exception e) {
            WaterfallMod.LOGGER.error("Failed to create physics structure", e);
            return null;
        }
    }

    @Override
    public PhysicsBlockEntity createFromWorldArea(Level level, Vec3 center, BlockPos min,
                                                   BlockPos max, boolean consumeBlocks) {
        if (!(level instanceof ServerLevel mainLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        // 收集范围内的非空气方块
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = mainLevel.getBlockState(worldPos);
                    if (!state.isAir()) {
                        BlockPos localPos = new BlockPos(
                            x - centerPos.getX(),
                            y - centerPos.getY(),
                            z - centerPos.getZ()
                        );
                        blocks.put(localPos, state);
                    }
                }
            }
        }

        return createPhysicsStructure(level, center, blocks);
    }

    @Override
    public void activatePhysics(PhysicsBlockEntity entity) {
        if (entity != null) {
            entity.getEntityData().set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, true);
        }
    }

    @Override
    public void deactivatePhysics(PhysicsBlockEntity entity) {
        if (entity != null) {
            entity.getEntityData().set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, false);
        }
    }

    @Override
    public void applyImpulse(PhysicsBlockEntity entity, Vec3 force) {
        if (entity != null) {
            entity.applyImpulse(force);
        }
    }

    @Override
    public void setVelocity(PhysicsBlockEntity entity, Vec3 velocity) {
        if (entity != null) {
            entity.setDeltaMovement(velocity);
        }
    }

    @Override
    public void destroyPhysicsStructure(PhysicsBlockEntity entity, boolean restoreBlocks) {
        if (entity == null) return;

        ServerLevel mainLevel = entity.level() instanceof ServerLevel
            ? (ServerLevel) entity.level() : null;
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();

        BlockPos origin = entity.getPhysicsOrigin();
        Map<BlockPos, BlockPos> localToWorld = entity.getLocalToWorldMap();

        // 清理物理维度方块 + 可选还原到主世界
        if (physicsLevel != null && origin != null) {
            for (Map.Entry<BlockPos, BlockPos> entry : localToWorld.entrySet()) {
                BlockPos localPos = entry.getKey();
                BlockPos physicsPos = origin.offset(localPos);
                BlockState currentState = physicsLevel.getBlockState(physicsPos);

                if (restoreBlocks && mainLevel != null) {
                    BlockPos worldPos = entry.getValue();
                    if (worldPos != null && !currentState.isAir()) {
                        mainLevel.setBlock(worldPos, currentState, 3);
                    }
                }

                if (!currentState.isAir()) {
                    physicsLevel.setBlock(physicsPos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // 移除主世界实体
        entity.discard();
        WaterfallMod.LOGGER.info("Destroyed physics structure at {}", entity.position());
    }

    @Override
    public int getLightBlockCount(PhysicsBlockEntity entity) {
        return entity != null ? entity.getLightBlockCount() : 0;
    }

    @Override
    public int getHeavyBlockCount(PhysicsBlockEntity entity) {
        return entity != null ? entity.getHeavyBlockCount() : 0;
    }

    @Override
    public boolean isUnderwater(PhysicsBlockEntity entity) {
        // 主世界检查：实体自己在主世界 tick 里检测
        if (entity == null) return false;
        BlockPos center = new BlockPos(
            (int) entity.position().x,
            (int) entity.position().y,
            (int) entity.position().z
        );
        return entity.level().getFluidState(center).is(net.minecraft.world.level.material.Fluids.WATER)
            || entity.level().getFluidState(center).is(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
    }

    @Override
    public float calculateNetBuoyancy(PhysicsBlockEntity entity) {
        // 直接使用实体预计算的浮力
        return entity != null ? entity.getNetBuoyancy() : 0.0f;
    }

    // ============ Rotation API（可选，仍由主世界实体处理） ============

    @Override
    public void applyTorque(PhysicsBlockEntity entity, float torqueX, float torqueY, float torqueZ) {
        // 当前实现：简化为冲量（真旋转由额外的 rotation system 处理，这里不做）
        if (entity == null) return;
        entity.applyImpulse(new Vec3(torqueX * 0.01, torqueY * 0.01, torqueZ * 0.01));
    }

    @Override
    public void applyImpulseTorque(PhysicsBlockEntity entity, float impulseX, float impulseY, float impulseZ) {
        if (entity == null) return;
        entity.applyImpulse(new Vec3(impulseX, impulseY, impulseZ));
    }

    @Override
    public float[] getRotation(PhysicsBlockEntity entity) {
        if (entity == null) return new float[]{0, 0, 0};
        return new float[]{
            entity.getXRot(),
            entity.getYRot(),
            0
        };
    }

    @Override
    public void setRotation(PhysicsBlockEntity entity, float pitch, float yaw, float roll) {
        if (entity == null) return;
        entity.setYRot(yaw);
        entity.setXRot(pitch);
    }
}
