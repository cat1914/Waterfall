package com.waterfall.physics;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

/**
 * Wrapper around {@code heavy::Force}.
 * <p>
 * Memory layout (36 bytes, verified with C {@code sizeof}):
 * <pre>
 *   0 .. 11  Vector3 gravity
 *  12 .. 23  Vector3 lift
 *  24 .. 35  Vector3 thrust
 * </pre>
 * The constructor initialises gravity to {@code (0, -9.8, 0)}.
 */
public class Force {

    public static final int SIZE = 36; // sizeof(heavy::Force)

    private static final int OFFSET_GRAVITY = 0;
    private static final int OFFSET_LIFT    = 12;
    private static final int OFFSET_THRUST  = 24;

    private final Memory nativeMem;

    public Force() {
        nativeMem = new Memory(SIZE);
        HeavyLibrary.INSTANCE._ZN5heavy5ForceC1Ev(nativeMem);
    }

    /** No-op; native memory is released by GC. Kept for API compat. */
    public void close() {
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    // ---- Mutations delegated to heavy ----
    public void setGravity(Vector3 g) {
        try (Memory m = g.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy5Force10setGravityERKNS_7Vector3E(nativeMem, m);
        }
    }

    public void setGravity(float x, float y, float z) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force10setGravityEfff(nativeMem, x, y, z);
    }

    public void setLift(Vector3 g) {
        try (Memory m = g.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy5Force7setLiftERKNS_7Vector3E(nativeMem, m);
        }
    }

    public void setLift(float x, float y, float z) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force7setLiftEfff(nativeMem, x, y, z);
    }

    public void setThrust(Vector3 g) {
        try (Memory m = g.toNative()) {
            HeavyLibrary.INSTANCE._ZN5heavy5Force9setThrustERKNS_7Vector3E(nativeMem, m);
        }
    }

    public void setThrust(float x, float y, float z) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force9setThrustEfff(nativeMem, x, y, z);
    }

    public void addThrustForward(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force16addThrustForwardEf(nativeMem, magnitude);
    }

    public void addThrustBackward(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force17addThrustBackwardEf(nativeMem, magnitude);
    }

    public void addThrustLeft(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force13addThrustLeftEf(nativeMem, magnitude);
    }

    public void addThrustRight(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force14addThrustRightEf(nativeMem, magnitude);
    }

    public void addThrustUp(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force11addThrustUpEf(nativeMem, magnitude);
    }

    public void addThrustDown(float magnitude) {
        HeavyLibrary.INSTANCE._ZN5heavy5Force13addThrustDownEf(nativeMem, magnitude);
    }

    public void reset() {
        HeavyLibrary.INSTANCE._ZN5heavy5Force5resetEv(nativeMem);
    }

    // ---- Reads (direct memory access, offsets match heavy::Force) ----
    public Vector3 getGravity() {
        return Vector3.fromNative(nativeMem.share(OFFSET_GRAVITY));
    }

    public Vector3 getLift() {
        return Vector3.fromNative(nativeMem.share(OFFSET_LIFT));
    }

    public Vector3 getThrust() {
        return Vector3.fromNative(nativeMem.share(OFFSET_THRUST));
    }

    /**
     * Mirrors {@code heavy::Force::calculateNetForce()} - sum of gravity +
     * lift + thrust. Computed here to avoid a by-value return through JNA.
     */
    public Vector3 calculateNetForce() {
        Vector3 g = getGravity();
        Vector3 l = getLift();
        Vector3 t = getThrust();
        return new Vector3(g.x + l.x + t.x, g.y + l.y + t.y, g.z + l.z + t.z);
    }
}
