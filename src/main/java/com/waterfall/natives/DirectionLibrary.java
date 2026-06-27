package com.waterfall.natives;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA binding to direction's exported C API (extern "C").
 * <p>
 * Objects are opaque handles (void*) created by {@code *_create} and destroyed
 * by {@code *_destroy}. No C++ name mangling involved.
 */
public interface DirectionLibrary extends Library {

    DirectionLibrary INSTANCE = Native.load("direction-0.0.1", DirectionLibrary.class);

    // =========================================================================
    // Vector3
    // =========================================================================
    Pointer direction_Vector3_create();

    Pointer direction_Vector3_create_with_params(float x, float y, float z);

    void direction_Vector3_destroy(Pointer vec);

    void direction_Vector3_set(Pointer vec, float x, float y, float z);

    float direction_Vector3_getX(Pointer vec);

    float direction_Vector3_getY(Pointer vec);

    float direction_Vector3_getZ(Pointer vec);

    // =========================================================================
    // Rotation
    // =========================================================================
    Pointer direction_Rotation_create();

    Pointer direction_Rotation_create_with_params(float pitch, float yaw, float roll);

    void direction_Rotation_destroy(Pointer rot);

    void direction_Rotation_set(Pointer rot, float pitch, float yaw, float roll);

    float direction_Rotation_getPitch(Pointer rot);

    float direction_Rotation_getYaw(Pointer rot);

    float direction_Rotation_getRoll(Pointer rot);

    void direction_Rotation_reset(Pointer rot);

    // =========================================================================
    // RotationalBody
    // =========================================================================
    Pointer direction_RotationalBody_create();

    Pointer direction_RotationalBody_create_with_params(float momentOfInertia);

    void direction_RotationalBody_destroy(Pointer body);

    void direction_RotationalBody_applyTorque(Pointer body, Pointer torque);

    void direction_RotationalBody_applyImpulseTorque(Pointer body, Pointer impulseTorque);

    void direction_RotationalBody_applyOscillation(Pointer body, Pointer axis, float initialMagnitude);

    void direction_RotationalBody_update(Pointer body, float deltaTime);

    void direction_RotationalBody_setStatic(Pointer body, int value);

    int direction_RotationalBody_getStatic(Pointer body);

    void direction_RotationalBody_reset(Pointer body);

    float direction_RotationalBody_getPitch(Pointer body);

    float direction_RotationalBody_getYaw(Pointer body);

    float direction_RotationalBody_getRoll(Pointer body);

    // =========================================================================
    // RotationalForce
    // =========================================================================
    Pointer direction_RotationalForce_create();

    void direction_RotationalForce_destroy(Pointer force);

    void direction_RotationalForce_setTorque(Pointer force, Pointer torque);

    void direction_RotationalForce_setTorque_with_params(Pointer force, float x, float y, float z);

    void direction_RotationalForce_reset(Pointer force);

    // =========================================================================
    // RotationalWorld
    // =========================================================================
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
