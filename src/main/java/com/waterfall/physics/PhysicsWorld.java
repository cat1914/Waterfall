package com.waterfall.physics;

import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class PhysicsWorld implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Pointer nativeWorld;
    private boolean ownsNative;
    private final List<PhysicsBody> managedBodies;
    private Force globalForce;
    
    public PhysicsWorld() {
        this.nativeWorld = HeavyLibrary.INSTANCE.heavy_PhysicsWorld_create();
        this.ownsNative = true;
        this.managedBodies = new ArrayList<>();
        this.globalForce = new Force();
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_setGlobalForce(nativeWorld, globalForce.getNativeForce());
        LOGGER.info("PhysicsWorld created successfully");
    }
    
    PhysicsWorld(Pointer nativeWorld) {
        this.nativeWorld = nativeWorld;
        this.ownsNative = false;
        this.managedBodies = new ArrayList<>();
        this.globalForce = new Force(HeavyLibrary.INSTANCE.heavy_PhysicsWorld_getGlobalForce(nativeWorld));
    }
    
    Pointer getNativeWorld() {
        return nativeWorld;
    }
    
    public void addBody(PhysicsBody body) {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_addBody(nativeWorld, body.getNativeBody());
        managedBodies.add(body);
        LOGGER.debug("Added physics body to world, total bodies: {}", managedBodies.size());
    }
    
    public void removeBody(PhysicsBody body) {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_removeBody(nativeWorld, body.getNativeBody());
        managedBodies.remove(body);
    }
    
    public void clearBodies() {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_clearBodies(nativeWorld);
        managedBodies.clear();
    }
    
    public Force getGlobalForce() {
        return globalForce;
    }
    
    public void setDeltaTime(float dt) {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_setDeltaTime(nativeWorld, dt);
    }
    
    public float getDeltaTime() {
        return HeavyLibrary.INSTANCE.heavy_PhysicsWorld_getDeltaTime(nativeWorld);
    }
    
    public void applyGlobalForces() {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_applyGlobalForces(nativeWorld);
    }
    
    public void update() {
        HeavyLibrary.INSTANCE.heavy_PhysicsWorld_update(nativeWorld);
    }
    
    public long getBodyCount() {
        return HeavyLibrary.INSTANCE.heavy_PhysicsWorld_getBodyCount(nativeWorld);
    }
    
    public List<PhysicsBody> getManagedBodies() {
        return new ArrayList<>(managedBodies);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeWorld != null) {
            HeavyLibrary.INSTANCE.heavy_PhysicsWorld_destroy(nativeWorld);
            ownsNative = false;
            LOGGER.info("PhysicsWorld destroyed");
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
