package com.waterfall.physics;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.waterfall.natives.HeavyLibrary;

/**
 * 3D vector - used for position, velocity, acceleration, force.
 * <p>
 * {@code heavy::Vector3} is a 12-byte POD with fields {@code x}, {@code y},
 * {@code z} at offsets 0, 4, 8. When we need to pass a vector into a heavy
 * method that takes {@code const Vector3&} we just point it at a 12-byte
 * native memory block laid out the same way.
 */
public class Vector3 {

    public static final int SIZE = 12; // sizeof(heavy::Vector3)

    public float x;
    public float y;
    public float z;

    public Vector3() {
        this(0, 0, 0);
    }

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ---- Field-like getters (used by the rest of the project) ----
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }

    /** Write this vector to a 12-byte native memory block (for passing into heavy). */
    public Memory toNative() {
        Memory mem = new Memory(SIZE);
        mem.setFloat(0, x);
        mem.setFloat(4, y);
        mem.setFloat(8, z);
        return mem;
    }

    /** Alias for {@link #toNative()} used by direction/RotationalBody callers. */
    public Memory getNativeVector() {
        return toNative();
    }

    /** Read a Vector3 back from a native 12-byte memory block. */
    public static Vector3 fromNative(Pointer ptr) {
        return new Vector3(ptr.getFloat(0), ptr.getFloat(4), ptr.getFloat(8));
    }

    // Convenience math - all computed on the Java side. heavy is for physics
    // integration only, we do small vector ops ourselves.
    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 add(float dx, float dy, float dz) {
        return new Vector3(x + dx, y + dy, z + dz);
    }

    public Vector3 sub(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(float s) {
        return new Vector3(x * s, y * s, z * s);
    }

    public float length() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3 normalize() {
        float len = length();
        if (len == 0) return new Vector3(0, 0, 0);
        return scale(1.0f / len);
    }

    public float dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    // Small helpers we pass to heavy (e.g. to apply up vector as force direction)
    public static Memory nativeZero() {
        Memory m = new Memory(SIZE);
        m.setFloat(0, 0); m.setFloat(4, 0); m.setFloat(8, 0);
        return m;
    }

    public static Memory nativeUp() {
        Memory m = new Memory(SIZE);
        m.setFloat(0, 0); m.setFloat(4, 1); m.setFloat(8, 0);
        return m;
    }

    public static Memory nativeDown() {
        Memory m = new Memory(SIZE);
        m.setFloat(0, 0); m.setFloat(4, -1); m.setFloat(8, 0);
        return m;
    }
}
