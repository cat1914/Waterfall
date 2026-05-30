package com.waterfall.physics.rigidbody;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import com.waterfall.physics.Vector3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刚体管理器，负责管理子维度中的所有刚体
 * 具有精确的水下物理检测和浮力应用功能
 */
public class RigidBodyManager implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static RigidBodyManager instance;
    
    private final Map<RigidBodyId, RigidBody> rigidBodies;
    private final Map<RigidBodyId, ServerLevel> rigidBodyDimensions;
    private final Map<ResourceKey<Level>, Set<RigidBodyId>> dimensionRigidBodies;
    
    private RigidBodyManager() {
        this.rigidBodies = new ConcurrentHashMap<>();
        this.rigidBodyDimensions = new ConcurrentHashMap<>();
        this.dimensionRigidBodies = new ConcurrentHashMap<>();
    }
    
    public static synchronized RigidBodyManager getInstance() {
        if (instance == null) {
            instance = new RigidBodyManager();
        }
        return instance;
    }
    
    public RigidBody createRigidBody(ServerLevel dimension) {
        RigidBodyId id = new RigidBodyId();
        RigidBody body = new RigidBody(id);
        
        rigidBodies.put(id, body);
        rigidBodyDimensions.put(id, dimension);
        
        dimensionRigidBodies.computeIfAbsent(dimension.dimension(), k -> new HashSet<>()).add(id);
        
        LOGGER.debug("Created rigid body {} in dimension {}", id, dimension.dimension().location());
        return body;
    }
    
    public void destroyRigidBody(RigidBodyId id) {
        RigidBody body = rigidBodies.remove(id);
        if (body != null) {
            ServerLevel dimension = rigidBodyDimensions.remove(id);
            if (dimension != null) {
                Set<RigidBodyId> bodies = dimensionRigidBodies.get(dimension.dimension());
                if (bodies != null) {
                    bodies.remove(id);
                }
            }
            body.close();
            LOGGER.debug("Destroyed rigid body {}", id);
        }
    }
    
    public RigidBody getRigidBody(RigidBodyId id) {
        return rigidBodies.get(id);
    }
    
    public Set<RigidBodyId> getRigidBodiesInDimension(ResourceKey<Level> dimension) {
        return Collections.unmodifiableSet(
            dimensionRigidBodies.getOrDefault(dimension, Collections.emptySet())
        );
    }
    
    public ServerLevel getDimensionForRigidBody(RigidBodyId id) {
        return rigidBodyDimensions.get(id);
    }
    
    public void tick(ServerLevel dimension) {
        Set<RigidBodyId> ids = dimensionRigidBodies.get(dimension.dimension());
        if (ids == null) return;
        
        for (RigidBodyId id : ids) {
            RigidBody body = rigidBodies.get(id);
            if (body != null && body.isActive() && !body.isStatic()) {
                // 检测是否在水下
                boolean wasUnderwater = body.isUnderwater();
                boolean isNowUnderwater = checkIfUnderwater(body, dimension);
                body.setUnderwater(isNowUnderwater);
                
                // 如果在水下，应用浮力
                if (isNowUnderwater) {
                    body.applyUnderwaterForces();
                    
                    // 第一次进入水下时记录日志
                    if (!wasUnderwater) {
                        LOGGER.debug("Rigid body {} entered water - Light: {}, Heavy: {}, Net buoyancy: {}", 
                            id, body.getLightBlockCount(), body.getHeavyBlockCount(), 
                            body.calculateNetBuoyancy());
                    }
                }
                
                // 更新物理
                body.getPhysicsBody().update(0.016f);
            }
        }
    }
    
    /**
     * 检查刚体是否在水下
     * 检查刚体的中心位置和几个关键点
     */
    private boolean checkIfUnderwater(RigidBody body, ServerLevel dimension) {
        // 获取物理主体的位置
        Vector3 position = body.getPhysicsBody().getPosition();
        BlockPos centerPos = new BlockPos(
            (int) Math.floor(position.getX()), 
            (int) Math.floor(position.getY()), 
            (int) Math.floor(position.getZ())
        );
        
        // 检查中心位置
        if (isWaterAt(dimension, centerPos)) {
            return true;
        }
        
        // 检查几个关键点（边界）
        BlockPos[] checkPositions = new BlockPos[] {
            centerPos.above(),
            centerPos.below(),
            centerPos.north(),
            centerPos.south(),
            centerPos.east(),
            centerPos.west()
        };
        
        // 只要有一个关键点在水下就认为在水下
        for (BlockPos pos : checkPositions) {
            if (isWaterAt(dimension, pos)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查指定位置是否是水
     */
    private boolean isWaterAt(ServerLevel level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
    }
    
    public void applyForce(RigidBodyId id, Vector3 force, BlockPos localPosition) {
        RigidBody body = rigidBodies.get(id);
        if (body != null && !body.isStatic()) {
            // 简化的力施加，暂不考虑力矩
            body.getPhysicsBody().applyForce(force);
        }
    }
    
    public void applyImpulse(RigidBodyId id, Vector3 impulse, BlockPos localPosition) {
        RigidBody body = rigidBodies.get(id);
        if (body != null && !body.isStatic()) {
            // 简化的冲量施加，暂不考虑力矩
            body.getPhysicsBody().applyImpulse(impulse);
        }
    }
    
    @Override
    public void close() {
        for (RigidBody body : rigidBodies.values()) {
            body.close();
        }
        rigidBodies.clear();
        rigidBodyDimensions.clear();
        dimensionRigidBodies.clear();
    }
}
