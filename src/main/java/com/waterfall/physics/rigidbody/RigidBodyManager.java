package com.waterfall.physics.rigidbody;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 刚体管理器，负责管理子维度中的所有刚体
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
                body.getPhysicsBody().update(0.016f);
            }
        }
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
