package com.waterfall.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PhysicsEngineManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Map<UUID, PhysicsWorld> worldMap;
    private final Map<UUID, PhysicsBody> entityBodyMap;
    private final Map<UUID, Vec3> entityPositionMap;
    private final Map<UUID, Vec3> entityVelocityMap;
    
    private static final float GRAVITY = 9.81f;
    private static final float WATER_GRAVITY = 1.5f;
    private static final float WATER_DENSITY = 1.025f;
    private static final float AIR_DRAG = 0.01f;
    private static final float WATER_DRAG = 0.05f;
    private static final float BUOYANCY_FORCE = 2.0f;
    
    private static PhysicsEngineManager instance;
    
    private PhysicsEngineManager() {
        this.worldMap = new ConcurrentHashMap<>();
        this.entityBodyMap = new ConcurrentHashMap<>();
        this.entityPositionMap = new ConcurrentHashMap<>();
        this.entityVelocityMap = new ConcurrentHashMap<>();
        LOGGER.info("PhysicsEngineManager initialized");
    }
    
    public static synchronized PhysicsEngineManager getInstance() {
        if (instance == null) {
            instance = new PhysicsEngineManager();
        }
        return instance;
    }
    
    public PhysicsWorld createWorld() {
        PhysicsWorld world = new PhysicsWorld();
        UUID worldId = UUID.randomUUID();
        worldMap.put(worldId, world);
        LOGGER.info("Created new physics world: {}", worldId);
        return world;
    }
    
    public void destroyWorld(PhysicsWorld world) {
        worldMap.values().removeIf(w -> w == world);
        world.close();
    }
    
    public void registerEntity(UUID entityId, Vec3 initialPosition, float mass) {
        if (entityBodyMap.containsKey(entityId)) {
            LOGGER.warn("Entity {} already registered", entityId);
            return;
        }
        
        PhysicsBody body = new PhysicsBody(
            (float) initialPosition.x,
            (float) initialPosition.y,
            (float) initialPosition.z,
            mass
        );
        
        entityBodyMap.put(entityId, body);
        entityPositionMap.put(entityId, initialPosition);
        entityVelocityMap.put(entityId, Vec3.ZERO);
        
        LOGGER.debug("Registered entity {} with mass {}", entityId, mass);
    }
    
    public void unregisterEntity(UUID entityId) {
        PhysicsBody body = entityBodyMap.remove(entityId);
        if (body != null) {
            body.close();
        }
        entityPositionMap.remove(entityId);
        entityVelocityMap.remove(entityId);
        LOGGER.debug("Unregistered entity {}", entityId);
    }
    
    public void updateEntity(UUID entityId, Level level) {
        PhysicsBody body = entityBodyMap.get(entityId);
        if (body == null) {
            return;
        }
        
        Vec3 position = entityPositionMap.get(entityId);
        if (position == null) {
            return;
        }
        
        boolean inWater = isInWater(level, new BlockPos((int) position.x, (int) position.y, (int) position.z));
        
        if (inWater) {
            updateWaterPhysics(body, level, position);
        } else {
            updateAirPhysics(body, level, position);
        }
        
        body.update(0.016f);
        
        Vec3 newPosition = new Vec3(body.getPosition().getX(), body.getPosition().getY(), body.getPosition().getZ());
        entityPositionMap.put(entityId, newPosition);
    }
    
    private void updateAirPhysics(PhysicsBody body, Level level, Vec3 position) {
        Force force = new Force();
        force.setGravity(0, -GRAVITY, 0);
        
        FluidState fluidState = level.getFluidState(new BlockPos((int) position.x, (int) position.y, (int) position.z));
        if (!fluidState.isEmpty()) {
            BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
            float height = (float) fluidState.getHeight(level, blockPos);
            if (position.y < height) {
                float drag = AIR_DRAG;
                Vec3 velocity = entityVelocityMap.getOrDefault(UUID.randomUUID(), Vec3.ZERO);
                force.addThrustDown((float)(drag * velocity.length()));
            }
        }
        
        body.applyForce(force.calculateNetForce());
    }

    private void updateWaterPhysics(PhysicsBody body, Level level, Vec3 position) {
        Force force = new Force();
        force.setGravity(0, -WATER_GRAVITY, 0);
        
        float depth = calculateWaterDepth(level, new BlockPos((int) position.x, (int) position.y, (int) position.z));
        float submergedRatio = Math.min(1.0f, depth / 2.0f);
        
        force.setLift(0, BUOYANCY_FORCE * submergedRatio, 0);
        
        Vec3 velocity = entityVelocityMap.getOrDefault(UUID.randomUUID(), Vec3.ZERO);
        float drag = (float)(WATER_DRAG * velocity.length());
        force.addThrustDown(drag);
        
        body.applyForce(force.calculateNetForce());
        force.close();
    }
    
    private boolean isInWater(Level level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(Fluids.WATER) || fluidState.is(Fluids.FLOWING_WATER);
    }
    
    private float calculateWaterDepth(Level level, BlockPos pos) {
        int waterLevel = level.getSeaLevel();
        int depth = waterLevel - pos.getY();
        return Math.max(0, depth);
    }
    
    public Vec3 getEntityPosition(UUID entityId) {
        return entityPositionMap.get(entityId);
    }
    
    public void setEntityPosition(UUID entityId, Vec3 position) {
        PhysicsBody body = entityBodyMap.get(entityId);
        if (body != null) {
            body.setPosition((float) position.x, (float) position.y, (float) position.z);
            entityPositionMap.put(entityId, position);
        }
    }
    
    public void applyImpulse(UUID entityId, Vec3 impulse) {
        PhysicsBody body = entityBodyMap.get(entityId);
        if (body != null) {
            body.applyImpulse(new Vector3((float) impulse.x, (float) impulse.y, (float) impulse.z));
        }
    }
    
    public void setEntityVelocity(UUID entityId, Vec3 velocity) {
        entityVelocityMap.put(entityId, velocity);
    }
    
    public Vec3 getEntityVelocity(UUID entityId) {
        return entityVelocityMap.getOrDefault(entityId, Vec3.ZERO);
    }
    
    public void tick(Level level) {
        for (UUID entityId : entityBodyMap.keySet()) {
            updateEntity(entityId, level);
        }
    }
    
    public int getRegisteredEntityCount() {
        return entityBodyMap.size();
    }
    
    public void clear() {
        for (PhysicsBody body : entityBodyMap.values()) {
            body.close();
        }
        entityBodyMap.clear();
        entityPositionMap.clear();
        entityVelocityMap.clear();
        
        for (PhysicsWorld world : worldMap.values()) {
            world.close();
        }
        worldMap.clear();
        
        LOGGER.info("PhysicsEngineManager cleared");
    }
}
