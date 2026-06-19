// Quick C test that loads libheavy-0.0.1.so via dlopen and calls a few
// mangled symbols to verify the calling convention works.
#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
    const char* path = "/workspace/src/main/resources/natives/libheavy-0.0.1.so";
    void* lib = dlopen(path, RTLD_NOW | RTLD_GLOBAL);
    if (!lib) {
        printf("dlopen fail: %s\n", dlerror());
        return 1;
    }

    // 1. PhysicsBody default ctor
    void (*ctor)(void*) = (void (*)(void*))dlsym(lib, "_ZN5heavy11PhysicsBodyC1Ev");
    if (!ctor) { printf("ctor not found: %s\n", dlerror()); return 1; }
    printf("PhysicsBody::PhysicsBody() OK\n");

    // 2. PhysicsBody::setStatic(bool)
    void (*setStatic)(void*, unsigned char) =
        (void (*)(void*, unsigned char))dlsym(lib, "_ZN5heavy11PhysicsBody9setStaticEb");
    if (!setStatic) { printf("setStatic not found: %s\n", dlerror()); return 1; }
    printf("PhysicsBody::setStatic(bool) OK\n");

    // 3. Allocate body and call ctor
    void* body = calloc(1, 256); // 256 bytes is way more than enough for PhysicsBody
    ctor(body);
    printf("PhysicsBody constructed at %p\n", body);

    // 4. Force ctor
    void (*forceCtor)(void*) =
        (void (*)(void*))dlsym(lib, "_ZN5heavy5ForceC1Ev");
    if (!forceCtor) { printf("Force ctor not found: %s\n", dlerror()); return 1; }

    void* force = calloc(1, 64);
    forceCtor(force); // Should set gravity to (0, -9.8f, 0)
    float* forceFloats = (float*)force;
    printf("Force.gravity = (%f, %f, %f)\n", forceFloats[0], forceFloats[1], forceFloats[2]);
    // layout: gravity[0..2] at offset 0, lift[0..2] at offset 12, thrust[0..2] at offset 24
    printf("Force.lift    = (%f, %f, %f)\n", forceFloats[3], forceFloats[4], forceFloats[5]);
    printf("Force.thrust  = (%f, %f, %f)\n", forceFloats[6], forceFloats[7], forceFloats[8]);

    // 5. Force::setGravity(float, float, float)
    void (*setGravityFFF)(void*, float, float, float) =
        (void (*)(void*, float, float, float))dlsym(lib, "_ZN5heavy5Force10setGravityEfff");
    if (!setGravityFFF) { printf("setGravityFFF not found: %s\n", dlerror()); return 1; }
    setGravityFFF(force, 0.0f, -1.0f, 0.0f);
    printf("After setGravity: gravity = (%f, %f, %f)\n", forceFloats[0], forceFloats[1], forceFloats[2]);

    // 6. PhysicsBody::applyForce(const Vector3&)
    void (*applyForce)(void*, void*) =
        (void (*)(void*, void*))dlsym(lib, "_ZN5heavy11PhysicsBody10applyForceERKNS_7Vector3E");
    if (!applyForce) { printf("applyForce not found: %s\n", dlerror()); return 1; }

    // Build a Vector3 on stack (just 3 floats)
    float vec3[3] = {0.0f, 10.0f, 0.0f}; // big upward force
    applyForce(body, vec3);

    // 7. PhysicsBody::update(float dt)
    void (*update)(void*, float) =
        (void (*)(void*, float))dlsym(lib, "_ZN5heavy11PhysicsBody6updateEf");
    if (!update) { printf("update not found: %s\n", dlerror()); return 1; }

    update(body, 0.016f); // one frame

    // 8. Read position (offset 0 = Vector3 position)
    float* pos = (float*)body;
    printf("After update(dt=0.016): position=(%f, %f, %f)\n", pos[0], pos[1], pos[2]);
    // velocity at offset 12 (3 floats)
    printf("After update(dt=0.016): velocity=(%f, %f, %f)\n", pos[3], pos[4], pos[5]);
    // acceleration at offset 24
    printf("After update(dt=0.016): accel=(%f, %f, %f)\n", pos[6], pos[7], pos[8]);

    free(body);
    free(force);
    dlclose(lib);
    printf("Test passed!\n");
    return 0;
}
