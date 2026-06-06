package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 旋转世界 - 管理所有旋转刚体
 */
public class RotationalWorld implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Pointer nativeWorld;
    private boolean ownsNative;
    private final List<RotationalBody> managedBodies;
    private RotationalForce globalForce;
    
    public RotationalWorld() {
        this.nativeWorld = DirectionLibrary.INSTANCE.direction_RotationalWorld_create();
        this.ownsNative = true;
        this.managedBodies = new ArrayList<>();
        this.globalForce = new RotationalForce();
        DirectionLibrary.INSTANCE.direction_RotationalWorld_setGlobalForce(nativeWorld, globalForce.getNativeForce());
        LOGGER.info("RotationalWorld created successfully");
    }
    
    RotationalWorld(Pointer nativeWorld) {
        this.nativeWorld = nativeWorld;
        this.ownsNative = false;
        this.managedBodies = new ArrayList<>();
        this.globalForce = new RotationalForce(DirectionLibrary.INSTANCE.direction_RotationalWorld_getGlobalForce(nativeWorld));
    }
    
    Pointer getNativeWorld() {
        return nativeWorld;
    }
    
    public void addBody(RotationalBody body) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_addBody(nativeWorld, body.getNativeBody());
        managedBodies.add(body);
        LOGGER.debug("Added rotational body to world, total bodies: {}", managedBodies.size());
    }
    
    public void removeBody(RotationalBody body) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_removeBody(nativeWorld, body.getNativeBody());
        managedBodies.remove(body);
    }
    
    public void clearBodies() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_clearBodies(nativeWorld);
        managedBodies.clear();
    }
    
    public RotationalForce getGlobalForce() {
        return globalForce;
    }
    
    public void setDeltaTime(float dt) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_setDeltaTime(nativeWorld, dt);
    }
    
    public float getDeltaTime() {
        return DirectionLibrary.INSTANCE.direction_RotationalWorld_getDeltaTime(nativeWorld);
    }
    
    public void applyGlobalForces() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_applyGlobalForces(nativeWorld);
    }
    
    public void update() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_update(nativeWorld);
    }
    
    public long getBodyCount() {
        return DirectionLibrary.INSTANCE.direction_RotationalWorld_getBodyCount(nativeWorld);
    }
    
    public List<RotationalBody> getManagedBodies() {
        return new ArrayList<>(managedBodies);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeWorld != null) {
            DirectionLibrary.INSTANCE.direction_RotationalWorld_destroy(nativeWorld);
            ownsNative = false;
            LOGGER.info("RotationalWorld destroyed");
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
