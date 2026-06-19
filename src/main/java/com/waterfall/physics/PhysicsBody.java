package com.waterfall.physics;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

/**
 * Wrapper around {@code heavy::PhysicsBody}.
 * <p>
 * Memory layout (72 bytes, fixed offsets verified with C {@code sizeof}):
 * <pre>
 *   0 .. 11  Vector3 position
 *  12 .. 23  Vector3 velocity
 *  24 .. 35  Vector3 acceleration
 *  36 .. 39  float   mass
 *  40 .. 43  bool    isStatic  (padded to 4 bytes)
 *  44 .. 55  Vector3 oscillationForce
 *  56 .. 67  Vector3 lastOscillationDirection
 *  68 .. 71  bool    isOscillating  (padded to 4 bytes)
 * </pre>
 */
public class PhysicsBody {

    public static final int SIZE = 72; // sizeof(heavy::PhysicsBody)

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_VELOCITY = 12;
    private static final int OFFSET_ACCEL    = 24;
    private static final int OFFSET_MASS     = 36;
    private static final int OFFSET_STATIC   = 40;

    private final Memory nativeMem;

    public PhysicsBody() {
        nativeMem = new Memory(SIZE);
        HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBodyC1Ev(nativeMem);
    }

    public PhysicsBody(Vector3 position, float mass) {
        nativeMem = new Memory(SIZE);
        try (Memory posMem = position.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBodyC1ERKNS_7Vector3Ef(
                    nativeMem, posMem, mass);
        }
    }

    /** Convenience 4-arg constructor mirroring the older API. */
    public PhysicsBody(float x, float y, float z, float mass) {
        this(new Vector3(x, y, z), mass);
    }

    /** No-op placeholder - native memory is released by GC via Memory finalizer.
     *  Kept for API compatibility; heavy::PhysicsBody has no heap resources to free. */
    public void close() {
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    // ---- Mutations delegated to heavy ----
    public void applyForce(Vector3 force) {
        try (Memory f = force.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody10applyForceERKNS_7Vector3E(nativeMem, f);
        }
    }

    public void applyImpulse(Vector3 impulse) {
        try (Memory i = impulse.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody12applyImpulseERKNS_7Vector3E(nativeMem, i);
        }
    }

    public void applyOscillation(Vector3 direction, float initialMagnitude) {
        try (Memory d = direction.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody16applyOscillationERKNS_7Vector3Ef(
                    nativeMem, d, initialMagnitude);
        }
    }

    public void update(float dt) {
        HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody6updateEf(nativeMem, dt);
    }

    public void setStatic(boolean value) {
        HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody9setStaticEb(nativeMem, (byte)(value ? 1 : 0));
    }

    public boolean isStatic() {
        return HeavyLibrary.INSTANCE._ZNK5heavy11PhysicsBody9getStaticEv(nativeMem) != 0;
    }

    public void reset() {
        HeavyLibrary.INSTANCE._ZN5heavy11PhysicsBody5resetEv(nativeMem);
    }

    // ---- Reads: direct memory access at known offsets ----
    public Vector3 getPosition() {
        return Vector3.fromNative(nativeMem.share(OFFSET_POSITION));
    }

    public Vector3 getVelocity() {
        return Vector3.fromNative(nativeMem.share(OFFSET_VELOCITY));
    }

    public Vector3 getAcceleration() {
        return Vector3.fromNative(nativeMem.share(OFFSET_ACCEL));
    }

    public float getMass() {
        return nativeMem.getFloat(OFFSET_MASS);
    }

    /** Directly overwrite position (used to sync entity position into heavy). */
    public void setPosition(Vector3 p) {
        Pointer slot = nativeMem.share(OFFSET_POSITION);
        slot.setFloat(0, p.x);
        slot.setFloat(4, p.y);
        slot.setFloat(8, p.z);
    }

    /** Float-tuple convenience overload (matches older API). */
    public void setPosition(float x, float y, float z) {
        setPosition(new Vector3(x, y, z));
    }

    /** Directly overwrite velocity (useful after a collision). */
    public void setVelocity(Vector3 v) {
        Pointer slot = nativeMem.share(OFFSET_VELOCITY);
        slot.setFloat(0, v.x);
        slot.setFloat(4, v.y);
        slot.setFloat(8, v.z);
    }

    /** Float-tuple convenience overload (matches older API). */
    public void setVelocity(float x, float y, float z) {
        setVelocity(new Vector3(x, y, z));
    }
}
