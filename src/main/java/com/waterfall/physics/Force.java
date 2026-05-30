package com.waterfall.physics;

import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

public class Force implements AutoCloseable {
    private final Pointer nativeForce;
    private boolean ownsNative;
    
    public Force() {
        this.nativeForce = HeavyLibrary.INSTANCE.heavy_Force_create();
        this.ownsNative = true;
    }
    
    Force(Pointer nativeForce) {
        this.nativeForce = nativeForce;
        this.ownsNative = false;
    }
    
    Pointer getNativeForce() {
        return nativeForce;
    }
    
    public void setGravity(Vector3 gravity) {
        HeavyLibrary.INSTANCE.heavy_Force_setGravity(nativeForce, gravity.getNativeVector());
    }
    
    public void setGravity(float x, float y, float z) {
        HeavyLibrary.INSTANCE.heavy_Force_setGravity_with_params(nativeForce, x, y, z);
    }
    
    public void setLift(Vector3 lift) {
        HeavyLibrary.INSTANCE.heavy_Force_setLift(nativeForce, lift.getNativeVector());
    }
    
    public void setLift(float x, float y, float z) {
        HeavyLibrary.INSTANCE.heavy_Force_setLift_with_params(nativeForce, x, y, z);
    }
    
    public void setThrust(Vector3 thrust) {
        HeavyLibrary.INSTANCE.heavy_Force_setThrust(nativeForce, thrust.getNativeVector());
    }
    
    public void setThrust(float x, float y, float z) {
        HeavyLibrary.INSTANCE.heavy_Force_setThrust_with_params(nativeForce, x, y, z);
    }
    
    public void addThrustForward(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustForward(nativeForce, magnitude);
    }
    
    public void addThrustBackward(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustBackward(nativeForce, magnitude);
    }
    
    public void addThrustLeft(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustLeft(nativeForce, magnitude);
    }
    
    public void addThrustRight(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustRight(nativeForce, magnitude);
    }
    
    public void addThrustUp(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustUp(nativeForce, magnitude);
    }
    
    public void addThrustDown(float magnitude) {
        HeavyLibrary.INSTANCE.heavy_Force_addThrustDown(nativeForce, magnitude);
    }
    
    public void reset() {
        HeavyLibrary.INSTANCE.heavy_Force_reset(nativeForce);
    }
    
    public Vector3 calculateNetForce() {
        Pointer resultPointer = HeavyLibrary.INSTANCE.heavy_Force_calculateNetForce(nativeForce);
        return new Vector3(resultPointer);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeForce != null) {
            HeavyLibrary.INSTANCE.heavy_Force_destroy(nativeForce);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
