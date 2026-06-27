package com.waterfall.natives;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Direct JNA binding to direction's exported C++ symbols.
 * <p>
 * Follows the same convention as {@link HeavyLibrary}: objects are raw blocks
 * of native memory, and each non-static member function receives the {@code this}
 * pointer as its first argument.
 * <p>
 * Memory layouts (must match direction's C++ definitions):
 * <pre>
 *   direction::Direction      - 12 bytes: 3 floats (pitch, yaw, roll)
 *   direction::RotationalBody - 72+ bytes: Direction(12) + Vector3 angularVel(12)
 *                                + Vector3 angularAcc(12) + float momentOfInertia(4)
 *                                + bool isStatic(padded to 4) + Vector3 oscillationAxis(12)
 *                                + float oscillationMagnitude(4) + bool oscillating(padded to 4)
 *   direction::RotationalForce - 24 bytes: Vector3 torque(12) + Vector3 damping(12)
 *   direction::RotationalWorld - 64+ bytes: owns vector&lt;RotationalBody*&gt; + globalForce + dt
 * </pre>
 */
public interface DirectionLibrary extends Library {

    DirectionLibrary INSTANCE = Native.load("direction-0.0.1", DirectionLibrary.class);

    // =========================================================================
    // direction::Direction
    // =========================================================================
    void _ZN9direction9DirectionC1Ev(Pointer dir);

    void _ZN9direction9DirectionC1Efff(Pointer dir, float pitch, float yaw, float roll);

    void _ZN9direction9Direction3setEfff(Pointer dir, float pitch, float yaw, float roll);

    void _ZN9direction9Direction8setPitchEf(Pointer dir, float pitch);

    void _ZN9direction9Direction6setYawEf(Pointer dir, float yaw);

    void _ZN9direction9Direction7setRollEf(Pointer dir, float roll);

    float _ZNK9direction9Direction8getPitchEv(Pointer dir);

    float _ZNK9direction9Direction6getYawEv(Pointer dir);

    float _ZNK9direction9Direction7getRollEv(Pointer dir);

    void _ZN9direction9Direction20applyAngularVelocityERKS0_f(Pointer dir, Pointer angularVec, float dt);

    void _ZN9direction9Direction5resetEv(Pointer dir);

    // =========================================================================
    // direction::RotationalBody
    // =========================================================================
    void _ZN9direction14RotationalBodyC1Ev(Pointer body);

    void _ZN9direction14RotationalBodyC1Ef(Pointer body, float momentOfInertia);

    void _ZN9direction14RotationalBody11applyTorqueERKNS_7Vector3E(Pointer body, Pointer vec);

    void _ZN9direction14RotationalBody18applyImpulseTorqueERKNS_7Vector3E(Pointer body, Pointer vec);

    void _ZN9direction14RotationalBody16applyOscillationERKNS_7Vector3Ef(Pointer body, Pointer vec, float magnitude);

    void _ZN9direction14RotationalBody6updateEf(Pointer body, float dt);

    void _ZN9direction14RotationalBody9setStaticEb(Pointer body, byte value);

    byte _ZNK9direction14RotationalBody9getStaticEv(Pointer body);

    void _ZN9direction14RotationalBody5resetEv(Pointer body);

    // RotationalBody embeds a Direction at offset 0 (this is a getter that reads
    // the embedded Direction's values from the RotationalBody memory).
    float _ZNK9direction14RotationalBody8getPitchEv(Pointer body);

    float _ZNK9direction14RotationalBody6getYawEv(Pointer body);

    float _ZNK9direction14RotationalBody7getRollEv(Pointer body);

    // =========================================================================
    // direction::RotationalForce
    // =========================================================================
    void _ZN9direction15RotationalForceC1Ev(Pointer force);

    void _ZN9direction15RotationalForce9setTorqueERKNS_7Vector3E(Pointer force, Pointer vec);

    void _ZN9direction15RotationalForce9setTorqueEfff(Pointer force, float x, float y, float z);

    void _ZN9direction15RotationalForce10setDampingERKNS_7Vector3E(Pointer force, Pointer vec);

    void _ZN9direction15RotationalForce10setDampingEfff(Pointer force, float x, float y, float z);

    void _ZN9direction15RotationalForce18addTorqueFromForceERKNS_7Vector3ES3_S3_(Pointer force, Pointer fVec, Pointer pVec, Pointer cVec);

    void _ZN9direction15RotationalForce5resetEv(Pointer force);

    float _ZNK9direction15RotationalForce18calculateNetTorqueEv(Pointer force);

    // =========================================================================
    // direction::RotationalWorld
    // =========================================================================
    void _ZN9direction15RotationalWorldC1Ev(Pointer world);

    void _ZN9direction15RotationalWorldD1Ev(Pointer world);

    void _ZN9direction15RotationalWorld7addBodyEPNS_14RotationalBodyE(Pointer world, Pointer body);

    void _ZN9direction15RotationalWorld10removeBodyEPNS_14RotationalBodyE(Pointer world, Pointer body);

    void _ZN9direction15RotationalWorld11clearBodiesEv(Pointer world);

    void _ZN9direction15RotationalWorld14setGlobalForceERKNS_15RotationalForceE(Pointer world, Pointer force);

    Pointer _ZNK9direction15RotationalWorld14getGlobalForceEv(Pointer world);

    void _ZN9direction15RotationalWorld12setDeltaTimeEf(Pointer world, float dt);

    float _ZNK9direction15RotationalWorld12getDeltaTimeEv(Pointer world);

    void _ZN9direction15RotationalWorld17applyGlobalForcesEv(Pointer world);

    void _ZN9direction15RotationalWorld6updateEv(Pointer world);

    long _ZNK9direction15RotationalWorld12getBodyCountEv(Pointer world);

    Pointer _ZNK9direction15RotationalWorld7getBodyEm(Pointer world, long index);
}
