package com.waterfall.physics;

import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

public class PhysicsBody implements AutoCloseable {
    private final Pointer nativeBody;
    private boolean ownsNative;
    
    public PhysicsBody(float x, float y, float z, float mass) {
        this.nativeBody = HeavyLibrary.INSTANCE.heavy_PhysicsBody_create_with_params(x, y, z, mass);
        this.ownsNative = true;
    }
    
    PhysicsBody(Pointer nativeBody) {
        this.nativeBody = nativeBody;
        this.ownsNative = false;
    }
    
    Pointer getNativeBody() {
        return nativeBody;
    }
    
    public Vector3 getPosition() {
        return new Vector3(
            HeavyLibrary.INSTANCE.heavy_Vector3_getX(nativeBody),
            HeavyLibrary.INSTANCE.heavy_Vector3_getY(nativeBody),
            HeavyLibrary.INSTANCE.heavy_Vector3_getZ(nativeBody)
        );
    }
    
    public void setPosition(Vector3 position) {
        HeavyLibrary.INSTANCE.heavy_Vector3_set(nativeBody, position.getX(), position.getY(), position.getZ());
    }
    
    public void setPosition(float x, float y, float z) {
        HeavyLibrary.INSTANCE.heavy_Vector3_set(nativeBody, x, y, z);
    }
    
    public void applyForce(Vector3 force) {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_applyForce(nativeBody, force.getNativeVector());
    }
    
    public void applyImpulse(Vector3 impulse) {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_applyImpulse(nativeBody, impulse.getNativeVector());
    }
    
    public void applyOscillation(Vector3 direction, float initialMagnitude) {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_applyOscillation(nativeBody, direction.getNativeVector(), initialMagnitude);
    }
    
    public void update(float deltaTime) {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_update(nativeBody, deltaTime);
    }
    
    public void setStatic(boolean value) {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_setStatic(nativeBody, value);
    }
    
    public boolean isStatic() {
        return HeavyLibrary.INSTANCE.heavy_PhysicsBody_getStatic(nativeBody);
    }
    
    public void reset() {
        HeavyLibrary.INSTANCE.heavy_PhysicsBody_reset(nativeBody);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeBody != null) {
            HeavyLibrary.INSTANCE.heavy_PhysicsBody_destroy(nativeBody);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
