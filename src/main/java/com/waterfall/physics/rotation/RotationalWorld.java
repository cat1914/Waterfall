package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;

/**
 * Wrapper around {@code direction::RotationalWorld}.
 * <p>
 * Owns an internal {@code std::vector<RotationalBody*>}; the native handle MUST
 * be destroyed via {@link #destroy()} when no longer needed.
 */
public class RotationalWorld {

    private Pointer nativePtr;
    private boolean destroyed = false;

    public RotationalWorld() {
        nativePtr = DirectionLibrary.INSTANCE.direction_RotationalWorld_create();
    }

    public Pointer getPointer() {
        return nativePtr;
    }

    public synchronized void destroy() {
        if (destroyed) return;
        if (nativePtr != null) {
            DirectionLibrary.INSTANCE.direction_RotationalWorld_destroy(nativePtr);
            nativePtr = null;
        }
        destroyed = true;
    }

    public void close() {
        destroy();
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    // ---- Body registration ----
    public void addBody(RotationalBody body) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_addBody(nativePtr, body.getPointer());
    }

    public void removeBody(RotationalBody body) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_removeBody(nativePtr, body.getPointer());
    }

    public void clearBodies() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_clearBodies(nativePtr);
    }

    public long getBodyCount() {
        return DirectionLibrary.INSTANCE.direction_RotationalWorld_getBodyCount(nativePtr);
    }

    // ---- Simulation ----
    public void setGlobalForce(RotationalForce force) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_setGlobalForce(nativePtr, force.getPointer());
    }

    public void setDeltaTime(float dt) {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_setDeltaTime(nativePtr, dt);
    }

    public float getDeltaTime() {
        return DirectionLibrary.INSTANCE.direction_RotationalWorld_getDeltaTime(nativePtr);
    }

    public void applyGlobalForces() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_applyGlobalForces(nativePtr);
    }

    public void update() {
        DirectionLibrary.INSTANCE.direction_RotationalWorld_update(nativePtr);
    }
}
