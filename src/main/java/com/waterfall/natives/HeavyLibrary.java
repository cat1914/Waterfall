package com.waterfall.natives;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.FloatByReference;

public interface HeavyLibrary extends Library {
    static {
        NativeLoader.loadHeavy();
    }

    HeavyLibrary INSTANCE = Native.load("heavy", HeavyLibrary.class);
    
    Pointer heavy_PhysicsWorld_create();
    void heavy_PhysicsWorld_destroy(Pointer world);
    void heavy_PhysicsWorld_addBody(Pointer world, Pointer body);
    void heavy_PhysicsWorld_removeBody(Pointer world, Pointer body);
    void heavy_PhysicsWorld_clearBodies(Pointer world);
    void heavy_PhysicsWorld_setGlobalForce(Pointer world, Pointer force);
    Pointer heavy_PhysicsWorld_getGlobalForce(Pointer world);
    void heavy_PhysicsWorld_setDeltaTime(Pointer world, float dt);
    float heavy_PhysicsWorld_getDeltaTime(Pointer world);
    void heavy_PhysicsWorld_applyGlobalForces(Pointer world);
    void heavy_PhysicsWorld_update(Pointer world);
    long heavy_PhysicsWorld_getBodyCount(Pointer world);
    Pointer heavy_PhysicsWorld_getBody(Pointer world, long index);
    
    Pointer heavy_PhysicsBody_create();
    Pointer heavy_PhysicsBody_create_with_params(float x, float y, float z, float mass);
    void heavy_PhysicsBody_destroy(Pointer body);
    void heavy_PhysicsBody_applyForce(Pointer body, Pointer force);
    void heavy_PhysicsBody_applyImpulse(Pointer body, Pointer impulse);
    void heavy_PhysicsBody_applyOscillation(Pointer body, Pointer direction, float initialMagnitude);
    void heavy_PhysicsBody_update(Pointer body, float deltaTime);
    void heavy_PhysicsBody_setStatic(Pointer body, boolean value);
    boolean heavy_PhysicsBody_getStatic(Pointer body);
    void heavy_PhysicsBody_reset(Pointer body);
    
    Pointer heavy_Vector3_create();
    Pointer heavy_Vector3_create_with_params(float x, float y, float z);
    void heavy_Vector3_destroy(Pointer vec);
    void heavy_Vector3_set(Pointer vec, float x, float y, float z);
    float heavy_Vector3_getX(Pointer vec);
    float heavy_Vector3_getY(Pointer vec);
    float heavy_Vector3_getZ(Pointer vec);
    
    Pointer heavy_Force_create();
    void heavy_Force_destroy(Pointer force);
    Pointer heavy_Force_calculateNetForce(Pointer force);
    void heavy_Force_setGravity(Pointer force, Pointer vec);
    void heavy_Force_setGravity_with_params(Pointer force, float x, float y, float z);
    void heavy_Force_setLift(Pointer force, Pointer vec);
    void heavy_Force_setLift_with_params(Pointer force, float x, float y, float z);
    void heavy_Force_setThrust(Pointer force, Pointer vec);
    void heavy_Force_setThrust_with_params(Pointer force, float x, float y, float z);
    void heavy_Force_addThrustForward(Pointer force, float magnitude);
    void heavy_Force_addThrustBackward(Pointer force, float magnitude);
    void heavy_Force_addThrustLeft(Pointer force, float magnitude);
    void heavy_Force_addThrustRight(Pointer force, float magnitude);
    void heavy_Force_addThrustUp(Pointer force, float magnitude);
    void heavy_Force_addThrustDown(Pointer force, float magnitude);
    void heavy_Force_reset(Pointer force);
}
