package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;
import com.waterfall.physics.Vector3;

/**
 * 旋转刚体 - 用于计算3D旋转物理
 * 包含方向、角速度、角加速度和转动惯量
 */
public class RotationalBody implements AutoCloseable {
    private final Pointer nativeBody;
    private boolean ownsNative;
    private final Direction direction;
    
    public RotationalBody() {
        this.nativeBody = DirectionLibrary.INSTANCE.direction_RotationalBody_create();
        this.ownsNative = true;
        this.direction = new Direction(nativeBody);
    }
    
    RotationalBody(Pointer nativeBody) {
        this.nativeBody = nativeBody;
        this.ownsNative = false;
        this.direction = new Direction(nativeBody);
    }
    
    public RotationalBody(float momentOfInertia) {
        this.nativeBody = DirectionLibrary.INSTANCE.direction_RotationalBody_create_with_params(momentOfInertia);
        this.ownsNative = true;
        this.direction = new Direction(nativeBody);
    }
    
    Pointer getNativeBody() {
        return nativeBody;
    }
    
    /**
     * 获取方向（pitch, yaw, roll）
     */
    public Direction getDirection() {
        return direction;
    }
    
    /**
     * 应用扭矩
     */
    public void applyTorque(Vector3 torque) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyTorque(nativeBody, torque.getNativeVector());
    }
    
    /**
     * 应用脉冲扭矩
     */
    public void applyImpulseTorque(Vector3 impulseTorque) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyImpulseTorque(nativeBody, impulseTorque.getNativeVector());
    }
    
    /**
     * 应用振荡力
     */
    public void applyOscillation(Vector3 axis, float initialMagnitude) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_applyOscillation(nativeBody, axis.getNativeVector(), initialMagnitude);
    }
    
    /**
     * 更新旋转状态
     */
    public void update(float deltaTime) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_update(nativeBody, deltaTime);
    }
    
    /**
     * 设置是否为静态
     */
    public void setStatic(boolean value) {
        DirectionLibrary.INSTANCE.direction_RotationalBody_setStatic(nativeBody, value);
    }
    
    public boolean isStatic() {
        return DirectionLibrary.INSTANCE.direction_RotationalBody_getStatic(nativeBody);
    }
    
    public void reset() {
        DirectionLibrary.INSTANCE.direction_RotationalBody_reset(nativeBody);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeBody != null) {
            DirectionLibrary.INSTANCE.direction_RotationalBody_destroy(nativeBody);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
