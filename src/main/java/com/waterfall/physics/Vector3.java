package com.waterfall.physics;

import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

public class Vector3 implements AutoCloseable {
    private final Pointer nativeVector;
    private boolean ownsNative;
    
    public Vector3(float x, float y, float z) {
        this.nativeVector = HeavyLibrary.INSTANCE.heavy_Vector3_create_with_params(x, y, z);
        this.ownsNative = true;
    }
    
    Vector3(Pointer nativeVector) {
        this.nativeVector = nativeVector;
        this.ownsNative = false;
    }
    
    public Pointer getNativeVector() {
        return nativeVector;
    }
    
    public float getX() {
        return HeavyLibrary.INSTANCE.heavy_Vector3_getX(nativeVector);
    }
    
    public float getY() {
        return HeavyLibrary.INSTANCE.heavy_Vector3_getY(nativeVector);
    }
    
    public float getZ() {
        return HeavyLibrary.INSTANCE.heavy_Vector3_getZ(nativeVector);
    }
    
    public void set(float x, float y, float z) {
        HeavyLibrary.INSTANCE.heavy_Vector3_set(nativeVector, x, y, z);
    }
    
    public void setX(float x) {
        set(x, getY(), getZ());
    }
    
    public void setY(float y) {
        set(getX(), y, getZ());
    }
    
    public void setZ(float z) {
        set(getX(), getY(), z);
    }
    
    @Override
    public void close() {
        if (ownsNative && nativeVector != null) {
            HeavyLibrary.INSTANCE.heavy_Vector3_destroy(nativeVector);
            ownsNative = false;
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
    
    public static Vector3 zero() {
        return new Vector3(0, 0, 0);
    }
    
    public static Vector3 up() {
        return new Vector3(0, 1, 0);
    }
    
    public static Vector3 down() {
        return new Vector3(0, -1, 0);
    }
    
    public static Vector3 right() {
        return new Vector3(1, 0, 0);
    }
    
    public static Vector3 left() {
        return new Vector3(-1, 0, 0);
    }
    
    public static Vector3 forward() {
        return new Vector3(0, 0, -1);
    }
    
    public static Vector3 backward() {
        return new Vector3(0, 0, 1);
    }
    
    /**
     * 计算叉积 (cross product)
     * this × other
     */
    public Vector3 cross(Vector3 other) {
        return new Vector3(
            getY() * other.getZ() - getZ() * other.getY(),
            getZ() * other.getX() - getX() * other.getZ(),
            getX() * other.getY() - getY() * other.getX()
        );
    }
    
    /**
     * 计算点积 (dot product)
     */
    public float dot(Vector3 other) {
        return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
    }
    
    /**
     * 计算长度 (magnitude)
     */
    public float length() {
        return (float) Math.sqrt(getX() * getX() + getY() * getY() + getZ() * getZ());
    }
    
    /**
     * 归一化
     */
    public Vector3 normalized() {
        float len = length();
        if (len > 0.0001f) {
            return new Vector3(getX() / len, getY() / len, getZ() / len);
        }
        return zero();
    }
}
