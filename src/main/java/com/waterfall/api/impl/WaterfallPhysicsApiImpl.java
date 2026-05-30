package com.waterfall.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import com.waterfall.WaterfallMod;
import com.waterfall.api.WaterfallPhysicsApi;
import com.waterfall.entity.PhysicsBlockEntity;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the Waterfall Physics API
 */
public class WaterfallPhysicsApiImpl implements WaterfallPhysicsApi {

    @Override
    public PhysicsBlockEntity createPhysicsStructure(Level level, Vec3 position, Map<BlockPos, BlockState> blocks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        try {
            // Create RigidBody
            RigidBody body = RigidBodyManager.getInstance().createRigidBody(serverLevel);
            
            // Add all blocks
            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                body.addBlock(entry.getKey(), entry.getValue());
            }

            // Create PhysicsBlockEntity
            PhysicsBlockEntity entity = new PhysicsBlockEntity(
                PhysicsEntityType.PHYSICS_BLOCK.get(),
                serverLevel
            );
            
            // Configure entity
            entity.setPos(position.x, position.y, position.z);
            entity.setRigidBodyId(body.getId());
            entity.entityData.set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, true);
            entity.entityData.set(PhysicsBlockEntity.DATA_LIGHT_BLOCKS, body.getLightBlockCount());
            entity.entityData.set(PhysicsBlockEntity.DATA_HEAVY_BLOCKS, body.getHeavyBlockCount());

            // Spawn in world
            serverLevel.addFreshEntity(entity);
            
            WaterfallMod.LOGGER.info("Created physics structure at {} with {} blocks", 
                position, blocks.size());
                
            return entity;
            
        } catch (Exception e) {
            WaterfallMod.LOGGER.error("Failed to create physics structure", e);
            return null;
        }
    }

    @Override
    public PhysicsBlockEntity createFromWorldArea(Level level, Vec3 center, BlockPos min, BlockPos max, boolean consumeBlocks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            WaterfallMod.LOGGER.error("Cannot create physics structure on client!");
            return null;
        }

        // Collect blocks from world
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        BlockPos centerPos = new BlockPos((int)center.x, (int)center.y, (int)center.z);

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos worldPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(worldPos);
                    
                    if (!state.isAir()) {
                        // Calculate local position relative to center
                        BlockPos localPos = new BlockPos(
                            x - centerPos.getX(),
                            y - centerPos.getY(),
                            z - centerPos.getZ()
                        );
                        blocks.put(localPos, state);
                        
                        // Optionally consume blocks
                        if (consumeBlocks) {
                            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // Create physics structure
        return createPhysicsStructure(level, center, blocks);
    }

    @Override
    public void activatePhysics(PhysicsBlockEntity entity) {
        if (entity != null && entity.level() instanceof ServerLevel) {
            entity.entityData.set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, true);
            
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
            entity.entityData.set(PhysicsBlockEntity.DATA_IS_PHYSICS_ACTIVE, false);
            
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
                        (float)force.x,
                        (float)force.y,
                        (float)force.z
                    );
                    body.getPhysicsBody().applyForce(impulse);
                }
            } else {
                // Fallback to entity motion if no rigid body
                entity.setDeltaMovement(entity.getDeltaMovement().add(force));
            }
        }
    }

    @Override
    public void setVelocity(PhysicsBlockEntity entity, Vec3 velocity) {
        if (entity != null) {
            RigidBodyId id = entity.getRigidBodyId();
            if (id != null) {
                RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
                if (body != null && body.isActive()) {
                    com.waterfall.physics.Vector3 vel = new com.waterfall.physics.Vector3(
                        (float)velocity.x,
                        (float)velocity.y,
                        (float)velocity.z
                    );
                    body.getPhysicsBody().setVelocity(vel);
                }
            }
            entity.setDeltaMovement(velocity);
        }
    }

    @Override
    public void destroyPhysicsStructure(PhysicsBlockEntity entity, boolean restoreBlocks) {
        if (entity == null) return;
        
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            entity.discard();
            return;
        }
        
        // Cleanup rigid body
        RigidBodyId id = entity.getRigidBodyId();
        if (id != null) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(id);
            if (body != null) {
                RigidBodyManager.getInstance().removeRigidBody(id, serverLevel);
            }
        }
        
        // Optionally restore blocks
        if (restoreBlocks) {
            Vec3 entityPos = entity.position();
            BlockPos centerPos = new BlockPos((int)entityPos.x, (int)entityPos.y, (int)entityPos.z);
            
            Map<BlockPos, BlockState> blockStates = entity.getAllBlockStates();
            for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
                BlockPos localPos = entry.getKey();
                BlockState state = entry.getValue();
                
                BlockPos worldPos = new BlockPos(
                    centerPos.getX() + localPos.getX(),
                    centerPos.getY() + localPos.getY(),
                    centerPos.getZ() + localPos.getZ()
                );
                
                serverLevel.setBlock(worldPos, state, 3);
            }
        }
        
        // Remove entity
        entity.discard();
        
        WaterfallMod.LOGGER.info("Destroyed physics structure at {}", entity.position());
    }

    @Override
    public int getLightBlockCount(PhysicsBlockEntity entity) {
        if (entity != null) {
            return entity.entityData.get(PhysicsBlockEntity.DATA_LIGHT_BLOCKS);
        }
        return 0;
    }

    @Override
    public int getHeavyBlockCount(PhysicsBlockEntity entity) {
        if (entity != null) {
            return entity.entityData.get(PhysicsBlockEntity.DATA_HEAVY_BLOCKS);
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
        
        // Fallback check
        Vec3 pos = entity.position();
        BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        FluidState fluidState = entity.level().getFluidState(blockPos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
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
        
        // Manual calculation
        int light = getLightBlockCount(entity);
        int heavy = getHeavyBlockCount(entity);
        return light - (heavy * 0.25f);
    }
}
