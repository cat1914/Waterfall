package com.waterfall.physics;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

/**
 * Wrapper around {@code heavy::PhysicsWorld}.
 * <p>
 * This object owns an {@code std::vector<PhysicsBody*>} internally, so its
 * destructor MUST be called when it is no longer needed.
 *
 * @implNote Size: 64 bytes (verified with C {@code sizeof}).
 */
public class PhysicsWorld {

    public static final int SIZE = 64; // sizeof(heavy::PhysicsWorld)

    private final Memory nativeMem;
    private boolean destroyed = false;

    public PhysicsWorld() {
        nativeMem = new Memory(SIZE);
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorldC1Ev(nativeMem);
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    public synchronized void destroy() {
        if (destroyed) return;
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorldD1Ev(nativeMem);
        destroyed = true;
    }

    /** Alias for {@link #destroy()}; matches caller expectations (WaterfallMod). */
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

    // ---- Registration ----
    public void addBody(PhysicsBody body) {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld7addBodyEPNS_11PhysicsBodyE(
                nativeMem, body.getPointer());
    }

    public void removeBody(PhysicsBody body) {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld10removeBodyEPNS_11PhysicsBodyE(
                nativeMem, body.getPointer());
    }

    public void clearBodies() {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld11clearBodiesEv(nativeMem);
    }

    // ---- Simulation ----
    public void setGlobalForce(Force force) {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld14setGlobalForceERKNS_5ForceE(
                nativeMem, force.getPointer());
    }

    public void setDeltaTime(float dt) {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld12setDeltaTimeEf(nativeMem, dt);
    }

    public float getDeltaTime() {
        return HeavyLibrary.INSTANCE._ZNK5heavy12PhysicsWorld12getDeltaTimeEv(nativeMem);
    }

    public void applyGlobalForces() {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld17applyGlobalForcesEv(nativeMem);
    }

    public void update() {
        HeavyLibrary.INSTANCE._ZN5heavy12PhysicsWorld6updateEv(nativeMem);
    }

    public long getBodyCount() {
        return HeavyLibrary.INSTANCE._ZNK5heavy12PhysicsWorld12getBodyCountEv(nativeMem);
    }
}
