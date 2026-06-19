// Heavy library C shim
// Wraps heavy's C++ classes (heavy::PhysicsWorld, heavy::PhysicsBody, heavy::Vector3, heavy::Force)
// into plain-C free functions so Java/JNA can link against them.

#include <heavy/PhysicsWorld.h>
#include <heavy/PhysicsBody.h>
#include <heavy/Force.h>
#include <heavy/Vector3.h>

// -----------------------------------------------------------------------------
// opaque pointer helpers
// -----------------------------------------------------------------------------
using heavy::PhysicsWorld;
using heavy::PhysicsBody;
using heavy::Vector3;
using heavy::Force;

static inline Vector3* castV(void* p) { return reinterpret_cast<Vector3*>(p); }
static inline PhysicsBody* castB(void* p) { return reinterpret_cast<PhysicsBody*>(p); }
static inline PhysicsWorld* castW(void* p) { return reinterpret_cast<PhysicsWorld*>(p); }
static inline Force* castF(void* p) { return reinterpret_cast<Force*>(p); }

// -----------------------------------------------------------------------------
// Vector3
// -----------------------------------------------------------------------------
extern "C" {

void* heavy_Vector3_create() {
    return new Vector3();
}

void* heavy_Vector3_create_with_params(float x, float y, float z) {
    return new Vector3(x, y, z);
}

void heavy_Vector3_destroy(void* vec) {
    delete castV(vec);
}

void heavy_Vector3_set(void* vec, float x, float y, float z) {
    Vector3* v = castV(vec);
    v->x = x; v->y = y; v->z = z;
}

float heavy_Vector3_getX(void* vec) { return castV(vec)->x; }
float heavy_Vector3_getY(void* vec) { return castV(vec)->y; }
float heavy_Vector3_getZ(void* vec) { return castV(vec)->z; }

// -----------------------------------------------------------------------------
// PhysicsBody
// -----------------------------------------------------------------------------
void* heavy_PhysicsBody_create() {
    return new PhysicsBody();
}

void* heavy_PhysicsBody_create_with_params(float x, float y, float z, float mass) {
    return new PhysicsBody(Vector3(x, y, z), mass);
}

void heavy_PhysicsBody_destroy(void* body) {
    delete castB(body);
}

void heavy_PhysicsBody_applyForce(void* body, void* force) {
    castB(body)->applyForce(*castV(force));
}

void heavy_PhysicsBody_applyImpulse(void* body, void* impulse) {
    castB(body)->applyImpulse(*castV(impulse));
}

void heavy_PhysicsBody_applyOscillation(void* body, void* direction, float initialMagnitude) {
    castB(body)->applyOscillation(*castV(direction), initialMagnitude);
}

void heavy_PhysicsBody_update(void* body, float deltaTime) {
    castB(body)->update(deltaTime);
}

void heavy_PhysicsBody_setStatic(void* body, char value) {
    castB(body)->setStatic(value != 0);
}

char heavy_PhysicsBody_getStatic(void* body) {
    return castB(body)->getStatic() ? 1 : 0;
}

void heavy_PhysicsBody_reset(void* body) {
    castB(body)->reset();
}

// -----------------------------------------------------------------------------
// PhysicsWorld
// -----------------------------------------------------------------------------
void* heavy_PhysicsWorld_create() {
    return new PhysicsWorld();
}

void heavy_PhysicsWorld_destroy(void* world) {
    delete castW(world);
}

void heavy_PhysicsWorld_addBody(void* world, void* body) {
    castW(world)->addBody(castB(body));
}

void heavy_PhysicsWorld_removeBody(void* world, void* body) {
    castW(world)->removeBody(castB(body));
}

void heavy_PhysicsWorld_clearBodies(void* world) {
    castW(world)->clearBodies();
}

void heavy_PhysicsWorld_setGlobalForce(void* world, void* force) {
    // heavy::PhysicsWorld::setGlobalForce takes a heavy::Force, not a Vector3.
    // We overload on the C side: if force points to a Force, forward it;
    // else interpret as a Vector3 and create a Force with gravity set.
    if (force == nullptr) return;
    castW(world)->setGlobalForce(*castF(force));
}

void* heavy_PhysicsWorld_getGlobalForce(void* world) {
    Force* f = new Force(castW(world)->getGlobalForce());
    return f;
}

void heavy_PhysicsWorld_setDeltaTime(void* world, float dt) {
    castW(world)->setDeltaTime(dt);
}

float heavy_PhysicsWorld_getDeltaTime(void* world) {
    return castW(world)->getDeltaTime();
}

void heavy_PhysicsWorld_applyGlobalForces(void* world) {
    castW(world)->applyGlobalForces();
}

void heavy_PhysicsWorld_update(void* world) {
    castW(world)->update();
}

long heavy_PhysicsWorld_getBodyCount(void* world) {
    return (long)castW(world)->getBodyCount();
}

void* heavy_PhysicsWorld_getBody(void* world, long index) {
    return castW(world)->getBody((std::size_t)index);
}

// -----------------------------------------------------------------------------
// Force
// -----------------------------------------------------------------------------
void* heavy_Force_create() {
    return new Force();
}

void heavy_Force_destroy(void* force) {
    delete castF(force);
}

void* heavy_Force_calculateNetForce(void* force) {
    Vector3 net = castF(force)->calculateNetForce();
    return new Vector3(net);
}

void heavy_Force_setGravity(void* force, void* vec) {
    castF(force)->setGravity(*castV(vec));
}

void heavy_Force_setGravity_with_params(void* force, float x, float y, float z) {
    castF(force)->setGravity(x, y, z);
}

void heavy_Force_setLift(void* force, void* vec) {
    castF(force)->setLift(*castV(vec));
}

void heavy_Force_setLift_with_params(void* force, float x, float y, float z) {
    castF(force)->setLift(x, y, z);
}

void heavy_Force_setThrust(void* force, void* vec) {
    castF(force)->setThrust(*castV(vec));
}

void heavy_Force_setThrust_with_params(void* force, float x, float y, float z) {
    castF(force)->setThrust(x, y, z);
}

void heavy_Force_addThrustForward(void* force, float magnitude) {
    castF(force)->addThrustForward(magnitude);
}

void heavy_Force_addThrustBackward(void* force, float magnitude) {
    castF(force)->addThrustBackward(magnitude);
}

void heavy_Force_addThrustLeft(void* force, float magnitude) {
    castF(force)->addThrustLeft(magnitude);
}

void heavy_Force_addThrustRight(void* force, float magnitude) {
    castF(force)->addThrustRight(magnitude);
}

void heavy_Force_addThrustUp(void* force, float magnitude) {
    castF(force)->addThrustUp(magnitude);
}

void heavy_Force_addThrustDown(void* force, float magnitude) {
    castF(force)->addThrustDown(magnitude);
}

void heavy_Force_reset(void* force) {
    castF(force)->reset();
}

} // extern "C"
