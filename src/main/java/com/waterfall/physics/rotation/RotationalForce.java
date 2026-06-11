package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * 旋转力类 - 用于计算扭矩
 */
public class RotationalForce implements AutoCloseable {
    private final Pointer nativeForce;
    private boolean ownsNative;
    
    public RotationalForce() {
        this.nativeForce = DirectionLibrary.INSTANCE.direction_RotationalForce_create();
        this.ownsNative = true;
    }
    
    RotationalForce(Pointer nativeForce) {
        this.nativeForce = nativeForce;
        this.ownsNative = false;
    }
    
    Pointer getNativeForce() {
        return nativeForce;
    }
    
    /**
     * 设置扭矩
     */
    public void setTorque(Vector3 torque) {
        DirectionLibrary.INSTANCE.direction_RotationalForce_setTorque(nativeForce, torque.getNativeVector());
    }
    
    /**
     * 设置扭矩（直接参数）
     */
    public void setTorque(float x, float y, float z) {
        DirectionLibrary.INSTANCE.direction_RotationalForce_setTorque_with_params(nativeForce, x, y, z);
    }
    
    /**
     * 从力和作用点计算扭矩
     * torque = r × F
     * r = pointOfApplication - centerOfRotation
     */
    public void addTorqueFromForce(Vector3 force, Vector3 pointOfApplication, Vector3 centerOfRotation) {
        // This would need native implementation
        // For now, we calculate manually
        Vector3 r = new Vector3(
            pointOfApplication.getX() - centerOfRotation.getX(),
            pointOfApplication.getY() - centerOfRotation.getY(),
            pointOfApplication.getZ() - centerOfRotation.getZ()
        );
        Vector3 torque = r.cross(force);
        setTorque(torque);
    }
    
    public void reset() {
        DirectionLibrary.INSTANCE.direction_RotationalForce_reset(nativeForce);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeForce != null) {
            DirectionLibrary.INSTANCE.direction_RotationalForce_destroy(nativeForce);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
