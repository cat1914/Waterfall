package com.waterfall.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import com.waterfall.WaterfallMod;
import com.waterfall.api.WaterfallPhysicsApi;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.dimension.StructureCopier;
import com.waterfall.dimension.StructureCopier.StructureCopyResult;
import com.waterfall.entity.PhysicsBlockEntity;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.physics.MaterialPhysics;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import com.waterfall.physics.rotation.RotationalBody;
import com.waterfall.physics.rotation.RotationalBodyManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * WaterfallPhysicsApi 实现 - 基于物理维度映射系统
 * 
 * 创建物理结构的新流程：
 * 1. 收集方块数据
 * 2. 将方块复制到物理维度（同时从主世界移除）
 * 3. 在物理维度创建 RigidBody 进行物理计算
 * 4. 在主世界创建 PhysicsBlockEntity 作为代理显示
 * 5. 注册代理实体到 InteractionMapper
 */
public class WaterfallPhysicsApiImpl implements WaterfallPhysicsApi {

    @Override
    public PhysicsBlockEntity createPhysicsStructure(Level level, Vec3 position, Map<BlockPos, BlockState> blocks) {
        if (!(level instanceof ServerLevel mainLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        // 获取物理维度
        ServerLevel physicsLevel = PhysicsDimension.getLevel(mainLevel.getServer());
        if (physicsLevel == null) {
            WaterfallMod.LOGGER.error("Physics dimension not available! Make sure it's registered properly.");
            return null;
        }

        try {
            // 步骤1：将方块复制到物理维度
            StructureCopyResult copyResult = StructureCopier.copyStructureToPhysicsDimension(
                mainLevel, physicsLevel, position, blocks
            );
            
            if (copyResult == null || copyResult.blockCount == 0) {
                WaterfallMod.LOGGER.error("Failed to copy structure to physics dimension");
                return null;
            }

            // 步骤2：在物理维度创建刚体
            RigidBody body = RigidBodyManager.getInstance().createRigidBody(physicsLevel);
            
            // 收集轻/重质方块数量
            int lightBlocks = 0;
            int heavyBlocks = 0;
            for (BlockPos localPos : copyResult.localToWorldMap.keySet()) {
                BlockState state = blocks.get(localPos);
                if (state != null) {
                    if (MaterialPhysics.isLightMaterial(state)) {
                        lightBlocks++;
                        body.addBlock(localPos, state);
                    } else if (MaterialPhysics.isHeavyMaterial(state)) {
                        heavyBlocks++;
                        body.addBlock(localPos, state);
                    } else {
                        body.addBlock(localPos, state);
                    }
                }
            }
            
            // 设置刚体初始位置
            body.getPhysicsBody().setPosition(new com.waterfall.physics.Vector3(
                (float) position.x, (float) position.y, (float) position.z
            ));

            // 步骤3：创建旋转刚体
            RotationalBody rotBody = RotationalBodyManager.getInstance().createRotationalBody(
                physicsLevel, body.getMass()
            );

            // 步骤4：在主世界创建代理实体
            PhysicsBlockEntity entity = new PhysicsBlockEntity(
                PhysicsEntityType.PHYSICS_BLOCK.get(), mainLevel
            );
            
            entity.setPos(position.x, position.y, position.z);
            
            Set<BlockPos> localPositions = copyResult.localToWorldMap.keySet();
            entity.initializeFromPhysicsDimension(
                copyResult.structureId,
                copyResult.physicsOrigin,
                new HashSet<>(localPositions),
                copyResult.localToWorldMap,
                body.getId(),
                lightBlocks,
                heavyBlocks
            );
            entity.setRotationalBodyId(rotBody.getId());

            // 步骤5：生成到主世界
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
    public PhysicsBlockEntity createFromWorldArea(Level level, Vec3 center, BlockPos min, BlockPos max, boolean consumeBlocks) {
        if (!(level instanceof ServerLevel mainLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        // 收集范围内的方块
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos centerPos = new BlockPos((int) center.x, (int) center.y, (int) center.z);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(worldPos);
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
        if (entity != null && entity.level() instanceof ServerLevel) {
            entity.getEntityData().set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, true);
            RigidBodyId id = entity.getRigidBodyId();
            if (id != null) {
                RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
                if (body != null) {
                    body.setActive(true);
                }
            }
        }
    }

    @Override
    public void deactivatePhysics(PhysicsBlockEntity entity) {
        if (entity != null) {
            entity.getEntityData().set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, false);
            RigidBodyId id = entity.getRigidBodyId();
            if (id != null) {
                RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
                if (body != null) {
                    body.setActive(false);
                }
            }
        }
    }

    @Override
    public void applyImpulse(PhysicsBlockEntity entity, Vec3 force) {
        if (entity != null) {
            RigidBodyId id = entity.getRigidBodyId();
            if (id != null) {
                RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
                if (body != null && body.isActive()) {
                    com.waterfall.physics.Vector3 impulse = new com.waterfall.physics.Vector3(
                        (float) force.x, (float) force.y, (float) force.z
                    );
                    body.getPhysicsBody().applyImpulse(impulse);
                }
            } else {
                entity.setDeltaMovement(entity.getDeltaMovement().add(force));
            }
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

        ServerLevel mainLevel = entity.level() instanceof ServerLevel ? (ServerLevel) entity.level() : null;
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
        
        // 清理物理维度中的方块
        if (physicsLevel != null && entity.getPhysicsOrigin() != null) {
            for (BlockPos localPos : entity.getLocalBlockPositions()) {
                BlockPos physicsPos = entity.getPhysicsOrigin().offset(localPos);
                BlockState currentState = physicsLevel.getBlockState(physicsPos);
                
                // 可选：恢复到主世界
                if (restoreBlocks && mainLevel != null) {
                    BlockPos worldPos = entity.getLocalToWorldMap().get(localPos);
                    if (worldPos != null && !currentState.isAir()) {
                        mainLevel.setBlock(worldPos, currentState, 3);
                    }
                }
                
                // 从物理维度清除
                physicsLevel.setBlock(physicsPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        // 清理刚体
        RigidBodyId id = entity.getRigidBodyId();
        if (id != null) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
            if (body != null) {
                RigidBodyManager.getInstance().destroyRigidBody(id);
            }
        }

        // 移除实体
        entity.discard();

        WaterfallMod.LOGGER.info("Destroyed physics structure #{}", entity.getStructureId());
    }

    @Override
    public int getLightBlockCount(PhysicsBlockEntity entity) {
        if (entity != null) {
            return entity.getEntityData().get(PhysicsBlockEntity.DATA_LIGHT_BLOCKS);
        }
        return 0;
    }

    @Override
    public int getHeavyBlockCount(PhysicsBlockEntity entity) {
        if (entity != null) {
            return entity.getEntityData().get(PhysicsBlockEntity.DATA_HEAVY_BLOCKS);
        }
        return 0;
    }

    @Override
    public boolean isUnderwater(PhysicsBlockEntity entity) {
        if (entity == null) return false;
        
        RigidBodyId id = entity.getRigidBodyId();
        if (id != null) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
            if (body != null) {
                return body.isUnderwater();
            }
        }
        
        // Fallback: check in physics dimension
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
        if (physicsLevel != null) {
            Vec3 pos = entity.position();
            BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
            FluidState fluid = physicsLevel.getFluidState(blockPos);
            return fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER);
        }
        return false;
    }

    @Override
    public float calculateNetBuoyancy(PhysicsBlockEntity entity) {
        if (entity == null) return 0;
        
        RigidBodyId id = entity.getRigidBodyId();
        if (id != null) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
            if (body != null) {
                return body.calculateNetBuoyancy();
            }
        }
        
        int light = getLightBlockCount(entity);
        int heavy = getHeavyBlockCount(entity);
        return light - (heavy * 0.25f);
    }

    // ============ Rotation API ============

    @Override
    public void applyTorque(PhysicsBlockEntity entity, float torqueX, float torqueY, float torqueZ) {
        if (entity == null) return;
        
        UUID rotBodyId = entity.getRotationalBodyId();
        if (rotBodyId != null) {
            RotationalBody rotBody = RotationalBodyManager.getInstance().getRotationalBody(rotBodyId);
            if (rotBody != null && !rotBody.isStatic()) {
                com.waterfall.physics.Vector3 torque = new com.waterfall.physics.Vector3(torqueX, torqueY, torqueZ);
                rotBody.applyTorque(torque);
            }
        }
    }

    @Override
    public void applyImpulseTorque(PhysicsBlockEntity entity, float impulseX, float impulseY, float impulseZ) {
        if (entity == null) return;
        
        UUID rotBodyId = entity.getRotationalBodyId();
        if (rotBodyId != null) {
            RotationalBody rotBody = RotationalBodyManager.getInstance().getRotationalBody(rotBodyId);
            if (rotBody != null && !rotBody.isStatic()) {
                com.waterfall.physics.Vector3 impulse = new com.waterfall.physics.Vector3(impulseX, impulseY, impulseZ);
                rotBody.applyImpulseTorque(impulse);
            }
        }
    }

    @Override
    public float[] getRotation(PhysicsBlockEntity entity) {
        if (entity == null) return new float[]{0, 0, 0};
        
        UUID rotBodyId = entity.getRotationalBodyId();
        if (rotBodyId != null) {
            RotationalBody rotBody = RotationalBodyManager.getInstance().getRotationalBody(rotBodyId);
            if (rotBody != null) {
                return rotBody.getDirection().getRotation();
            }
        }
        return new float[]{0, 0, 0};
    }

    @Override
    public void setRotation(PhysicsBlockEntity entity, float pitch, float yaw, float roll) {
        if (entity == null) return;
        
        UUID rotBodyId = entity.getRotationalBodyId();
        if (rotBodyId != null) {
            RotationalBody rotBody = RotationalBodyManager.getInstance().getRotationalBody(rotBodyId);
            if (rotBody != null) {
                rotBody.getDirection().set(pitch, yaw, roll);
            }
        }
    }
}
