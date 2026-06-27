package com.waterfall.physics.rotation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * Wrapper around {@code direction::RotationalBody}.
 * <p>
 * Memory layout (approximate):
 * <pre>
 *    0 .. 11  Direction (pitch, yaw, roll)
 *   12 .. 23  Vector3 angularVelocity
 *   24 .. 35  Vector3 angularAcceleration
 *   36 .. 39  float momentOfInertia
 *   40 .. 43  bool isStatic (padded to 4 bytes)
 *   44 .. 55  Vector3 oscillationAxis
 *   56 .. 59  float oscillationMagnitude
 *   60 .. 63  bool isOscillating (padded to 4 bytes)
 *   64 .. 71  padding to 72
 * </pre>
 */
public class RotationalBody {

    public static final int SIZE = 72; // sizeof(direction::RotationalBody)

    private static final int OFFSET_DIR   = 0;   // Direction at start
    private static final int OFFSET_MOI   = 36;  // momentOfInertia

    private final Memory nativeMem;

    public RotationalBody() {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction14RotationalBodyC1Ev(nativeMem);
    }

    public RotationalBody(float momentOfInertia) {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction14RotationalBodyC1Ef(nativeMem, momentOfInertia);
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    public Direction getDirection() {
        // View the first 12 bytes as a Direction. We construct a Java-side copy
        // by reading the memory contents directly.
        Pointer dirView = nativeMem.share(OFFSET_DIR);
        Direction d = new Direction();
        // Copy from our memory into d. The Direction getters read via member
        // functions, which operate on d's internal memory. So we re-initialize
        // d using C++ constructor semantics by reading offsets and calling set.
        float pitch = dirView.getFloat(0);
        float yaw   = dirView.getFloat(4);
        float roll  = dirView.getFloat(8);
        d.set(pitch, yaw, roll);
        return d;
    }

    public float getPitch() {
        return nativeMem.getFloat(0);
    }

    public float getYaw() {
        return nativeMem.getFloat(4);
    }

    public float getRoll() {
        return nativeMem.getFloat(8);
    }

    public float getMomentOfInertia() {
        return nativeMem.getFloat(OFFSET_MOI);
    }

    public void applyTorque(Vector3 torque) {
        try (Memory v = torque.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction14RotationalBody11applyTorqueERKNS_7Vector3E(nativeMem, v);
        }
    }

    public void applyImpulseTorque(Vector3 impulseTorque) {
        try (Memory v = impulseTorque.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction14RotationalBody18applyImpulseTorqueERKNS_7Vector3E(nativeMem, v);
        }
    }

    public void applyOscillation(Vector3 axis, float initialMagnitude) {
        try (Memory v = axis.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction14RotationalBody16applyOscillationERKNS_7Vector3Ef(nativeMem, v, initialMagnitude);
        }
    }

    public void update(float dt) {
        DirectionLibrary.INSTANCE._ZN9direction14RotationalBody6updateEf(nativeMem, dt);
    }

    public void setStatic(boolean value) {
        DirectionLibrary.INSTANCE._ZN9direction14RotationalBody9setStaticEb(nativeMem, (byte)(value ? 1 : 0));
    }

    public boolean isStatic() {
        return DirectionLibrary.INSTANCE._ZNK9direction14RotationalBody9getStaticEv(nativeMem) != 0;
    }

    public void reset() {
        DirectionLibrary.INSTANCE._ZN9direction14RotationalBody5resetEv(nativeMem);
    }

    /** No-op placeholder — kept for API compatibility. */
    public void close() {
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
