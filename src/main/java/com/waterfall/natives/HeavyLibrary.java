package com.waterfall.natives;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Direct JNA binding to heavy's exported C++ symbols.
 *
 * On x86_64 Linux (System V ABI), C++ non-static member functions pass the
 * {@code this} pointer as the first argument, identical to a regular C function.
 * That means every method below has a {@link Pointer} as its first argument
 * representing the object instance, and the symbol name is the Itanium C++
 * mangled name produced by the compiler.
 *
 * Objects themselves are raw blocks of native memory (use {@code Memory} to
 * allocate them; sizes are documented in each wrapper class). Constructors are
 * called on pre-allocated memory. Trivial destructors need not be invoked for
 * PhysicsBody / Force (they contain only scalars and small aggregates), but
 * PhysicsWorld owns an {@code std::vector} so its destructor MUST be called.
 */
public interface HeavyLibrary extends Library {

    HeavyLibrary INSTANCE = Native.load("heavy-0.0.1", HeavyLibrary.class);

    // =========================================================================
    // heavy::PhysicsBody
    // =========================================================================
    void _ZN5heavy11PhysicsBodyC1Ev(Pointer body);

    void _ZN5heavy11PhysicsBodyC1ERKNS_7Vector3Ef(Pointer body, Pointer vec, float mass);

    void _ZN5heavy11PhysicsBody10applyForceERKNS_7Vector3E(Pointer body, Pointer vec);

    void _ZN5heavy11PhysicsBody12applyImpulseERKNS_7Vector3E(Pointer body, Pointer vec);

    void _ZN5heavy11PhysicsBody16applyOscillationERKNS_7Vector3Ef(Pointer body, Pointer vec, float magnitude);

    void _ZN5heavy11PhysicsBody6updateEf(Pointer body, float dt);

    void _ZN5heavy11PhysicsBody9setStaticEb(Pointer body, byte value);

    byte _ZNK5heavy11PhysicsBody9getStaticEv(Pointer body);

    void _ZN5heavy11PhysicsBody5resetEv(Pointer body);

    // =========================================================================
    // heavy::Force
    // =========================================================================
    void _ZN5heavy5ForceC1Ev(Pointer force);

    void _ZN5heavy5Force10setGravityERKNS_7Vector3E(Pointer force, Pointer vec);

    void _ZN5heavy5Force10setGravityEfff(Pointer force, float x, float y, float z);

    void _ZN5heavy5Force7setLiftERKNS_7Vector3E(Pointer force, Pointer vec);

    void _ZN5heavy5Force7setLiftEfff(Pointer force, float x, float y, float z);

    void _ZN5heavy5Force9setThrustERKNS_7Vector3E(Pointer force, Pointer vec);

    void _ZN5heavy5Force9setThrustEfff(Pointer force, float x, float y, float z);

    void _ZN5heavy5Force16addThrustForwardEf(Pointer force, float magnitude);

    void _ZN5heavy5Force17addThrustBackwardEf(Pointer force, float magnitude);

    void _ZN5heavy5Force13addThrustLeftEf(Pointer force, float magnitude);

    void _ZN5heavy5Force14addThrustRightEf(Pointer force, float magnitude);

    void _ZN5heavy5Force11addThrustUpEf(Pointer force, float magnitude);

    void _ZN5heavy5Force13addThrustDownEf(Pointer force, float magnitude);

    void _ZN5heavy5Force5resetEv(Pointer force);

    // =========================================================================
    // heavy::PhysicsWorld
    // =========================================================================
    void _ZN5heavy12PhysicsWorldC1Ev(Pointer world);

    void _ZN5heavy12PhysicsWorldD1Ev(Pointer world);

    void _ZN5heavy12PhysicsWorld7addBodyEPNS_11PhysicsBodyE(Pointer world, Pointer body);

    void _ZN5heavy12PhysicsWorld10removeBodyEPNS_11PhysicsBodyE(Pointer world, Pointer body);

    void _ZN5heavy12PhysicsWorld11clearBodiesEv(Pointer world);

    void _ZN5heavy12PhysicsWorld14setGlobalForceERKNS_5ForceE(Pointer world, Pointer force);

    void _ZN5heavy12PhysicsWorld12setDeltaTimeEf(Pointer world, float dt);

    float _ZNK5heavy12PhysicsWorld12getDeltaTimeEv(Pointer world);

    void _ZN5heavy12PhysicsWorld17applyGlobalForcesEv(Pointer world);

    void _ZN5heavy12PhysicsWorld6updateEv(Pointer world);

    long _ZNK5heavy12PhysicsWorld12getBodyCountEv(Pointer world);

    Pointer _ZNK5heavy12PhysicsWorld7getBodyEm(Pointer world, long index);
}
