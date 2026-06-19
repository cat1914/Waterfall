package com.waterfall.physics.rotation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * Wrapper around {@code direction::Direction}.
 * <p>
 * Represents a 3D rotation as Euler angles (pitch, yaw, roll).
 * Memory layout: 12 bytes — three floats at offsets 0, 4, 8.
 */
public class Direction {

    public static final int SIZE = 12; // sizeof(direction::Direction)

    private static final int OFFSET_PITCH = 0;
    private static final int OFFSET_YAW   = 4;
    private static final int OFFSET_ROLL  = 8;

    private final Memory nativeMem;

    public Direction() {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction9DirectionC1Ev(nativeMem);
    }

    public Direction(float pitch, float yaw, float roll) {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction9DirectionC1Efff(nativeMem, pitch, yaw, roll);
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    public void set(float pitch, float yaw, float roll) {
        DirectionLibrary.INSTANCE._ZN9direction9Direction3setEfff(nativeMem, pitch, yaw, roll);
    }

    public void setPitch(float pitch) {
        DirectionLibrary.INSTANCE._ZN9direction9Direction8setPitchEf(nativeMem, pitch);
    }

    public void setYaw(float yaw) {
        DirectionLibrary.INSTANCE._ZN9direction9Direction6setYawEf(nativeMem, yaw);
    }

    public void setRoll(float roll) {
        DirectionLibrary.INSTANCE._ZN9direction9Direction7setRollEf(nativeMem, roll);
    }

    public float getPitch() {
        return nativeMem.getFloat(OFFSET_PITCH);
    }

    public float getYaw() {
        return nativeMem.getFloat(OFFSET_YAW);
    }

    public float getRoll() {
        return nativeMem.getFloat(OFFSET_ROLL);
    }

    public float[] getRotation() {
        return new float[]{getPitch(), getYaw(), getRoll()};
    }

    public void applyAngularVelocity(Vector3 angularVec, float dt) {
        try (Memory v = angularVec.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction9Direction20applyAngularVelocityERKS0_f(nativeMem, v, dt);
        }
    }

    public void reset() {
        DirectionLibrary.INSTANCE._ZN9direction9Direction5resetEv(nativeMem);
    }

    public static Direction zero() {
        return new Direction(0, 0, 0);
    }
}
