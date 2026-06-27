// Print sizeof() and field offsets for all heavy classes
#include <stdio.h>
#include <stddef.h>
#include <heavy/PhysicsBody.h>
#include <heavy/Force.h>
#include <heavy/Vector3.h>
#include <heavy/PhysicsWorld.h>

int main() {
    printf("=== heavy object sizes and offsets ===\n");
    printf("sizeof(Vector3)       = %zu bytes\n", sizeof(heavy::Vector3));
    printf("sizeof(PhysicsBody)   = %zu bytes\n", sizeof(heavy::PhysicsBody));
    printf("sizeof(Force)         = %zu bytes\n", sizeof(heavy::Force));
    printf("sizeof(PhysicsWorld)  = %zu bytes\n", sizeof(heavy::PhysicsWorld));
    printf("\n");

    // PhysicsBody field offsets (approximate via dummy struct with known layout)
    struct probe_body {
        heavy::Vector3 p, v, a;
        float m;
        bool is;
        heavy::Vector3 of, lod;
        bool io;
    };
    printf("PhysicsBody: position@0, velocity@%zu, accel@%zu, mass@%zu\n",
           offsetof(probe_body, v), offsetof(probe_body, a), offsetof(probe_body, m));
    printf("Force: gravity@0, lift@%zu, thrust@%zu\n",
           sizeof(heavy::Vector3), 2 * sizeof(heavy::Vector3));
    return 0;
}
