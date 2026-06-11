package com.waterfall.physics.rotation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 旋转刚体管理器 - 管理所有旋转物理实体
 */
public class RotationalBodyManager implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static RotationalBodyManager instance;
    
    private final Map<UUID, RotationalBody> rotationalBodies;
    private final Map<UUID, ServerLevel> bodyDimensions;
    private final Map<ResourceKey<Level>, Set<UUID>> dimensionBodies;
    
    private RotationalBodyManager() {
        this.rotationalBodies = new ConcurrentHashMap<>();
        this.bodyDimensions = new ConcurrentHashMap<>();
        this.dimensionBodies = new ConcurrentHashMap<>();
    }
    
    public static synchronized RotationalBodyManager getInstance() {
        if (instance == null) {
            instance = new RotationalBodyManager();
        }
        return instance;
    }
    
    /**
     * 创建旋转刚体
     * @param dimension 所属维度
     * @param momentOfInertia 转动惯量
     * @return 创建的旋转刚体
     */
    public RotationalBody createRotationalBody(ServerLevel dimension, float momentOfInertia) {
        UUID id = UUID.randomUUID();
        RotationalBody body = new RotationalBody(momentOfInertia);
        
        rotationalBodies.put(id, body);
        bodyDimensions.put(id, dimension);
        
        dimensionBodies.computeIfAbsent(dimension.dimension(), k -> new HashSet<>()).add(id);
        
        LOGGER.debug("Created rotational body {} in dimension {}", id, dimension.dimension().location());
        return body;
    }
    
    /**
     * 创建旋转刚体（默认转动惯量）
     */
    public RotationalBody createRotationalBody(ServerLevel dimension) {
        return createRotationalBody(dimension, 1.0f);
    }
    
    /**
     * 销毁旋转刚体
     */
    public void destroyRotationalBody(UUID id) {
        RotationalBody body = rotationalBodies.remove(id);
        if (body != null) {
            ServerLevel dimension = bodyDimensions.remove(id);
            if (dimension != null) {
                Set<UUID> bodies = dimensionBodies.get(dimension.dimension());
                if (bodies != null) {
                    bodies.remove(id);
                }
            }
            body.close();
            LOGGER.debug("Destroyed rotational body {}", id);
        }
    }
    
    /**
     * 获取旋转刚体
     */
    public RotationalBody getRotationalBody(UUID id) {
        return rotationalBodies.get(id);
    }
    
    /**
     * 获取维度中的所有旋转刚体
     */
    public Set<UUID> getRotationalBodiesInDimension(ResourceKey<Level> dimension) {
        return Collections.unmodifiableSet(
            dimensionBodies.getOrDefault(dimension, Collections.emptySet())
        );
    }
    
    /**
     * 每tick更新
     */
    public void tick(ServerLevel dimension) {
        Set<UUID> ids = dimensionBodies.get(dimension.dimension());
        if (ids == null) return;
        
        for (UUID id : ids) {
            RotationalBody body = rotationalBodies.get(id);
            if (body != null && !body.isStatic()) {
                // 更新旋转
                body.update(0.016f);
            }
        }
    }
    
    /**
     * 应用扭矩到指定刚体
     */
    public void applyTorque(UUID id, com.waterfall.physics.Vector3 torque) {
        RotationalBody body = rotationalBodies.get(id);
        if (body != null && !body.isStatic()) {
            body.applyTorque(torque);
        }
    }
    
    /**
     * 应用脉冲扭矩
     */
    public void applyImpulseTorque(UUID id, com.waterfall.physics.Vector3 impulseTorque) {
        RotationalBody body = rotationalBodies.get(id);
        if (body != null && !body.isStatic()) {
            body.applyImpulseTorque(impulseTorque);
        }
    }
    
    /**
     * 应用振荡力
     */
    public void applyOscillation(UUID id, com.waterfall.physics.Vector3 axis, float magnitude) {
        RotationalBody body = rotationalBodies.get(id);
        if (body != null && !body.isStatic()) {
            body.applyOscillation(axis, magnitude);
        }
    }
    
    @Override
    public void close() {
        for (RotationalBody body : rotationalBodies.values()) {
            body.close();
        }
        rotationalBodies.clear();
        bodyDimensions.clear();
        dimensionBodies.clear();
    }
}
