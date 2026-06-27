package com.waterfall.physics.rotation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;

/**
 * Wrapper around {@code direction::RotationalWorld}.
 * <p>
 * This object owns an internal {@code std::vector<RotationalBody*>} so its
 * destructor MUST be invoked when no longer needed.
 */
public class RotationalWorld {

    public static final int SIZE = 64; // sizeof(direction::RotationalWorld)

    private final Memory nativeMem;
    private boolean destroyed = false;

    public RotationalWorld() {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorldC1Ev(nativeMem);
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    public synchronized void destroy() {
        if (destroyed) return;
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorldD1Ev(nativeMem);
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
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld7addBodyEPNS_14RotationalBodyE(
                nativeMem, body.getPointer());
    }

    public void removeBody(RotationalBody body) {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld10removeBodyEPNS_14RotationalBodyE(
                nativeMem, body.getPointer());
    }

    public void clearBodies() {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld11clearBodiesEv(nativeMem);
    }

    public long getBodyCount() {
        return DirectionLibrary.INSTANCE._ZNK9direction15RotationalWorld12getBodyCountEv(nativeMem);
    }

    // ---- Simulation ----
    public void setGlobalForce(RotationalForce force) {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld14setGlobalForceERKNS_15RotationalForceE(
                nativeMem, force.getPointer());
    }

    public void setDeltaTime(float dt) {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld12setDeltaTimeEf(nativeMem, dt);
    }

    public float getDeltaTime() {
        return DirectionLibrary.INSTANCE._ZNK9direction15RotationalWorld12getDeltaTimeEv(nativeMem);
    }

    public void applyGlobalForces() {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld17applyGlobalForcesEv(nativeMem);
    }

    public void update() {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalWorld6updateEv(nativeMem);
    }
}
