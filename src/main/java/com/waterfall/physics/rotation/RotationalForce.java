package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * Wrapper around {@code direction::RotationalForce}.
 * <p>
 * Backed by a native handle allocated via the C API.
 */
public class RotationalForce {

    private Pointer nativePtr;
    private boolean owned;

    public RotationalForce() {
        nativePtr = DirectionLibrary.INSTANCE.direction_RotationalForce_create();
        owned = true;
    }

    /** Wrap an existing native pointer without taking ownership. */
    RotationalForce(Pointer ptr) {
        nativePtr = ptr;
        owned = false;
    }

    public Pointer getPointer() {
        return nativePtr;
    }

    public void setTorque(Vector3 torque) {
        Pointer v = DirectionLibrary.INSTANCE.direction_Vector3_create_with_params(
                torque.getX(), torque.getY(), torque.getZ());
        DirectionLibrary.INSTANCE.direction_RotationalForce_setTorque(nativePtr, v);
        DirectionLibrary.INSTANCE.direction_Vector3_destroy(v);
    }

    public void setTorque(float x, float y, float z) {
        DirectionLibrary.INSTANCE.direction_RotationalForce_setTorque_with_params(nativePtr, x, y, z);
    }

    /**
     * Add torque derived from an applied force at an offset from the rotation center.
     * Implemented in Java since the C API does not export this method.
     * torque += (point - center) x force
     */
    public void addTorqueFromForce(Vector3 force, Vector3 pointOfApplication, Vector3 centerOfRotation) {
        Vector3 r = new Vector3(
                pointOfApplication.getX() - centerOfRotation.getX(),
                pointOfApplication.getY() - centerOfRotation.getY(),
                pointOfApplication.getZ() - centerOfRotation.getZ());
        Vector3 torqueFromForce = r.cross(force);
        // Read current torque (offset 0 in the RotationalForce struct: Vector3 torque)
        Vector3 currentTorque = new Vector3(
                nativePtr.getFloat(0),
                nativePtr.getFloat(4),
                nativePtr.getFloat(8));
        Vector3 newTorque = new Vector3(
                currentTorque.getX() + torqueFromForce.getX(),
                currentTorque.getY() + torqueFromForce.getY(),
                currentTorque.getZ() + torqueFromForce.getZ());
        setTorque(newTorque);
    }

    public Vector3 calculateNetTorque() {
        // RotationalForce layout: Vector3 torque at offset 0
        return new Vector3(
                nativePtr.getFloat(0),
                nativePtr.getFloat(4),
                nativePtr.getFloat(8));
    }

    public void reset() {
        DirectionLibrary.INSTANCE.direction_RotationalForce_reset(nativePtr);
    }

    public synchronized void destroy() {
        if (owned && nativePtr != null) {
            DirectionLibrary.INSTANCE.direction_RotationalForce_destroy(nativePtr);
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
