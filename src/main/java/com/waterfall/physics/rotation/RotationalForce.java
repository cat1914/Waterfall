package com.waterfall.physics.rotation;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * Wrapper around {@code direction::RotationalForce}.
 * <p>
 * Memory layout: 24 bytes — two Vector3 fields (torque, damping) at offsets 0 and 12.
 */
public class RotationalForce {

    public static final int SIZE = 24; // sizeof(direction::RotationalForce)

    private static final int OFFSET_TORQUE  = 0;
    private static final int OFFSET_DAMPING = 12;

    private final Memory nativeMem;

    public RotationalForce() {
        nativeMem = new Memory(SIZE);
        DirectionLibrary.INSTANCE._ZN9direction15RotationalForceC1Ev(nativeMem);
    }

    public Pointer getPointer() {
        return nativeMem;
    }

    public void setTorque(Vector3 torque) {
        try (Memory v = torque.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction15RotationalForce9setTorqueERKNS_7Vector3E(nativeMem, v);
        }
    }

    public void setTorque(float x, float y, float z) {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalForce9setTorqueEfff(nativeMem, x, y, z);
    }

    public void setDamping(Vector3 damping) {
        try (Memory v = damping.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction15RotationalForce10setDampingERKNS_7Vector3E(nativeMem, v);
        }
    }

    public void setDamping(float x, float y, float z) {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalForce10setDampingEfff(nativeMem, x, y, z);
    }

    public Vector3 getTorque() {
        return Vector3.fromNative(nativeMem.share(OFFSET_TORQUE));
    }

    public Vector3 getDamping() {
        return Vector3.fromNative(nativeMem.share(OFFSET_DAMPING));
    }

    /**
     * Add torque derived from an applied force at an offset from the rotation center.
     * torque = (point - center) x force
     */
    public void addTorqueFromForce(Vector3 force, Vector3 pointOfApplication, Vector3 centerOfRotation) {
        Vector3 r = new Vector3(
                pointOfApplication.getX() - centerOfRotation.getX(),
                pointOfApplication.getY() - centerOfRotation.getY(),
                pointOfApplication.getZ() - centerOfRotation.getZ());
        Vector3 torque = r.cross(force);
        // Add to existing torque (sum via native call)
        try (Memory fv = force.toNative();
             Memory pv = pointOfApplication.toNative();
             Memory cv = centerOfRotation.toNative()) {
            DirectionLibrary.INSTANCE._ZN9direction15RotationalForce18addTorqueFromForceERKNS_7Vector3ES3_S3_(nativeMem, fv, pv, cv);
        }
    }

    public void reset() {
        DirectionLibrary.INSTANCE._ZN9direction15RotationalForce5resetEv(nativeMem);
    }
}
