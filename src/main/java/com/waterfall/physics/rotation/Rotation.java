package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;

/**
 * Wrapper around {@code direction::Rotation}.
 * <p>
 * Represents a 3D rotation as Euler angles (pitch, yaw, roll).
 * Backed by a native handle allocated via the C API.
 */
public class Rotation {

    private Pointer nativePtr;
    private boolean owned;

    public Rotation() {
        nativePtr = DirectionLibrary.INSTANCE.direction_Rotation_create();
        owned = true;
    }

    public Rotation(float pitch, float yaw, float roll) {
        nativePtr = DirectionLibrary.INSTANCE.direction_Rotation_create_with_params(pitch, yaw, roll);
        owned = true;
    }

    /** Wrap an existing native pointer without taking ownership. */
    Rotation(Pointer ptr) {
        nativePtr = ptr;
        owned = false;
    }

    public Pointer getPointer() {
        return nativePtr;
    }

    public void set(float pitch, float yaw, float roll) {
        DirectionLibrary.INSTANCE.direction_Rotation_set(nativePtr, pitch, yaw, roll);
    }

    public float getPitch() {
        return DirectionLibrary.INSTANCE.direction_Rotation_getPitch(nativePtr);
    }

    public float getYaw() {
        return DirectionLibrary.INSTANCE.direction_Rotation_getYaw(nativePtr);
    }

    public float getRoll() {
        return DirectionLibrary.INSTANCE.direction_Rotation_getRoll(nativePtr);
    }

    public float[] getRotation() {
        return new float[]{getPitch(), getYaw(), getRoll()};
    }

    public void reset() {
        DirectionLibrary.INSTANCE.direction_Rotation_reset(nativePtr);
    }

    public synchronized void destroy() {
        if (owned && nativePtr != null) {
            DirectionLibrary.INSTANCE.direction_Rotation_destroy(nativePtr);
            nativePtr = null;
            owned = false;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            destroy();
        } finally {
            super.finalize();
        }
    }

    public static Rotation zero() {
        return new Rotation(0, 0, 0);
    }
}
