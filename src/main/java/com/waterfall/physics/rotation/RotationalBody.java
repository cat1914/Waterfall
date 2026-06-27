package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * Wrapper around {@code direction::RotationalBody}.
 * <p>
 * Backed by a native handle allocated via the C API.
 */
public class RotationalBody {

    private Pointer nativePtr;
    private boolean owned;

    public RotationalBody() {
        nativePtr = DirectionLibrary.INSTANCE.direction_RotationalBody_create();
        owned = true;
    }

    public RotationalBody(float momentOfInertia) {
        nativePtr = DirectionLibrary.INSTANCE.direction_RotationalBody_create_with_params(momentOfInertia);
        owned = true;
    }

    /** Wrap an existing native pointer without taking ownership. */
    RotationalBody(Pointer ptr) {
        nativePtr = ptr;
        owned = false;
    }

    public Pointer getPointer() {
        return nativePtr;
    }

    public float getPitch() {
        return DirectionLibrary.INSTANCE.direction_RotationalBody_getPitch(nativePtr);
    }

    public float getYaw() {
        return DirectionLibrary.INSTANCE.direction_RotationalBody_getYaw(nativePtr);
    }

    public float getRoll() {
        return DirectionLibrary.INSTANCE.direction_RotationalBody_getRoll(nativePtr);
    }

    public Rotation getRotation() {
        return new Rotation(getPitch(), getYaw(), getRoll());
    }

    public void applyTorque(Vector3 torque) {
        Pointer v = DirectionLibrary.INSTANCE.direction_Vector3_create_with_params(
                torque.getX(), torque.getY(), torque.getZ());
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyTorque(nativePtr, v);
        DirectionLibrary.INSTANCE.direction_Vector3_destroy(v);
    }

    public void applyImpulseTorque(Vector3 impulseTorque) {
        Pointer v = DirectionLibrary.INSTANCE.direction_Vector3_create_with_params(
                impulseTorque.getX(), impulseTorque.getY(), impulseTorque.getZ());
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyImpulseTorque(nativePtr, v);
        DirectionLibrary.INSTANCE.direction_Vector3_destroy(v);
    }

    public void applyOscillation(Vector3 axis, float initialMagnitude) {
        Pointer v = DirectionLibrary.INSTANCE.direction_Vector3_create_with_params(
                axis.getX(), axis.getY(), axis.getZ());
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyOscillation(nativePtr, v, initialMagnitude);
        DirectionLibrary.INSTANCE.direction_Vector3_destroy(v);
    }

    public void update(float dt) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_update(nativePtr, dt);
    }

    public void setStatic(boolean value) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_setStatic(nativePtr, value ? 1 : 0);
    }

    public boolean isStatic() {
        return DirectionLibrary.INSTANCE.direction_RotationalBody_getStatic(nativePtr) != 0;
    }

    public void reset() {
        DirectionLibrary.INSTANCE.direction_RotationalBody_reset(nativePtr);
    }

    public synchronized void destroy() {
        if (owned && nativePtr != null) {
            DirectionLibrary.INSTANCE.direction_RotationalBody_destroy(nativePtr);
            nativePtr = null;
            owned = false;
        }
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
}
