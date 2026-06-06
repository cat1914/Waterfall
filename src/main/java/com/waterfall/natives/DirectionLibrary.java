package com.waterfall.natives;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA interface for the direction rotation calculation library
 */
public interface DirectionLibrary extends Library {
    static {
        NativeLoader.loadDirection();
    }
    
    DirectionLibrary INSTANCE = Native.load("direction", DirectionLibrary.class);

    // Vector3 operations
    Pointer direction_Vector3_create();
    Pointer direction_Vector3_create_with_params(float x, float y, float z);
    void direction_Vector3_destroy(Pointer vec);
    void direction_Vector3_set(Pointer vec, float x, float y, float z);
    float direction_Vector3_getX(Pointer vec);
    float direction_Vector3_getY(Pointer vec);
    float direction_Vector3_getZ(Pointer vec);

    // Direction operations
    Pointer direction_Direction_create();
    Pointer direction_Direction_create_with_params(float pitch, float yaw, float roll);
    void direction_Direction_destroy(Pointer dir);
    void direction_Direction_set(Pointer dir, float pitch, float yaw, float roll);
    float direction_Direction_getPitch(Pointer dir);
    float direction_Direction_getYaw(Pointer dir);
    float direction_Direction_getRoll(Pointer dir);
    void direction_Direction_reset(Pointer dir);

    // RotationalBody operations
    Pointer direction_RotationalBody_create();
    Pointer direction_RotationalBody_create_with_params(float momentOfInertia);
    void direction_RotationalBody_destroy(Pointer body);
    void direction_RotationalBody_applyTorque(Pointer body, Pointer torque);
    void direction_RotationalBody_applyImpulseTorque(Pointer body, Pointer impulseTorque);
    void direction_RotationalBody_applyOscillation(Pointer body, Pointer axis, float initialMagnitude);
    void direction_RotationalBody_update(Pointer body, float deltaTime);
    void direction_RotationalBody_setStatic(Pointer body, boolean value);
    boolean direction_RotationalBody_getStatic(Pointer body);
    void direction_RotationalBody_reset(Pointer body);

    // Direction getters for RotationalBody
    float direction_RotationalBody_getPitch(Pointer body);
    float direction_RotationalBody_getYaw(Pointer body);
    float direction_RotationalBody_getRoll(Pointer body);

    // RotationalForce operations
    Pointer direction_RotationalForce_create();
    void direction_RotationalForce_destroy(Pointer force);
    void direction_RotationalForce_setTorque(Pointer force, Pointer torque);
    void direction_RotationalForce_setTorque_with_params(Pointer force, float x, float y, float z);
    void direction_RotationalForce_reset(Pointer force);

    // RotationalWorld operations
    Pointer direction_RotationalWorld_create();
    void direction_RotationalWorld_destroy(Pointer world);
    void direction_RotationalWorld_addBody(Pointer world, Pointer body);
    void direction_RotationalWorld_removeBody(Pointer world, Pointer body);
    void direction_RotationalWorld_clearBodies(Pointer world);
    void direction_RotationalWorld_setGlobalForce(Pointer world, Pointer force);
    Pointer direction_RotationalWorld_getGlobalForce(Pointer world);
    void direction_RotationalWorld_setDeltaTime(Pointer world, float dt);
    float direction_RotationalWorld_getDeltaTime(Pointer world);
    void direction_RotationalWorld_applyGlobalForces(Pointer world);
    void direction_RotationalWorld_update(Pointer world);
    long direction_RotationalWorld_getBodyCount(Pointer world);
    Pointer direction_RotationalWorld_getBody(Pointer world, long index);
}
