package com.waterfall.physics.rotation;

import com.sun.jna.Pointer;
import com.waterfall.natives.DirectionLibrary;

/**
 * 方向类 - 表示3D旋转（pitch, yaw, roll）
 * pitch: X轴旋转 (-90 to 90)
 * yaw: Y轴旋转 (0 to 360)
 * roll: Z轴旋转 (0 to 360)
 */
public class Direction implements AutoCloseable {
    private final Pointer nativeDirection;
    private boolean ownsNative;
    
    public Direction() {
        this.nativeDirection = DirectionLibrary.INSTANCE.direction_Direction_create();
        this.ownsNative = true;
    }
    
    Direction(Pointer nativeDirection) {
        this.nativeDirection = nativeDirection;
        this.ownsNative = false;
    }
    
    public Direction(float pitch, float yaw, float roll) {
        this.nativeDirection = DirectionLibrary.INSTANCE.direction_Direction_create_with_params(pitch, yaw, roll);
        this.ownsNative = true;
    }
    
    Pointer getNativeDirection() {
        return nativeDirection;
    }
    
    public void set(float pitch, float yaw, float roll) {
        DirectionLibrary.INSTANCE.direction_Direction_set(nativeDirection, pitch, yaw, roll);
    }
    
    public void setPitch(float pitch) {
        set(pitch, getYaw(), getRoll());
    }
    
    public void setYaw(float yaw) {
        set(getPitch(), yaw, getRoll());
    }
    
    public void setRoll(float roll) {
        set(getPitch(), getYaw(), roll);
    }
    
    public float getPitch() {
        return DirectionLibrary.INSTANCE.direction_Direction_getPitch(nativeDirection);
    }
    
    public float getYaw() {
        return DirectionLibrary.INSTANCE.direction_Direction_getYaw(nativeDirection);
    }
    
    public float getRoll() {
        return DirectionLibrary.INSTANCE.direction_Direction_getRoll(nativeDirection);
    }
    
    /**
     * 获取所有旋转值
     */
    public float[] getRotation() {
        return new float[]{getPitch(), getYaw(), getRoll()};
    }
    
    public void reset() {
        DirectionLibrary.INSTANCE.direction_Direction_reset(nativeDirection);
    }
    
    public static Direction zero() {
        return new Direction(0, 0, 0);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeDirection != null) {
            DirectionLibrary.INSTANCE.direction_Direction_destroy(nativeDirection);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
