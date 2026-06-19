package com.waterfall.natives;

import com.waterfall.physics.Force;
import com.waterfall.physics.PhysicsBody;
import com.waterfall.physics.PhysicsWorld;
import com.waterfall.physics.Vector3;

/**
 * Standalone smoke test that loads heavy and exercises the JNA binding.
 *
 * Run:  java -cp <jar-with-classes> -Djna.library.path=/path/to/.so com.waterfall.natives.HeavySmokeTest
 */
public class HeavySmokeTest {

    public static void main(String[] args) throws Exception {
        // 1) Load the native library via our loader
        System.out.println("=== Loading heavy native library ===");
        try {
            NativeLoader.loadHeavy();
        } catch (Throwable t) {
            System.out.println("NativeLoader failed: " + t.getMessage());
            System.out.println("Trying to proceed - maybe already loaded...");
        }

        // 2) Test Force
        System.out.println("\n=== Testing Force ===");
        Force force = new Force();
        System.out.println("Force.gravity: " + force.getGravity().x + ", " + force.getGravity().y + ", " + force.getGravity().z);
        System.out.println("Expected: (0, -9.8, 0)");
        Vector3 net = force.calculateNetForce();
        System.out.println("calculateNetForce: (" + net.x + ", " + net.y + ", " + net.z + ")");

        force.setGravity(0, -1, 0);
        System.out.println("After setGravity(0,-1,0): gravity.y = " + force.getGravity().y);

        force.addThrustUp(15);
        System.out.println("After addThrustUp(15): thrust.y = " + force.getThrust().y);

        Vector3 net2 = force.calculateNetForce();
        System.out.println("calculateNetForce (gravity=-1 + thrust up=15): (" + net2.x + ", " + net2.y + ", " + net2.z + ")");

        // 3) Test PhysicsBody
        System.out.println("\n=== Testing PhysicsBody ===");
        PhysicsBody body = new PhysicsBody();
        Vector3 initialPos = body.getPosition();
        System.out.println("Initial position: (" + initialPos.x + ", " + initialPos.y + ", " + initialPos.z + ")");
        System.out.println("Is static: " + body.isStatic());

        // Apply upward force
        body.applyForce(new Vector3(0, 20, 0));  // y+=20/mass
        System.out.println("After applyForce(0,20,0) - velocity (before update): (" + body.getVelocity().x + "," + body.getVelocity().y +"," + body.getVelocity().z+")");
        // Heavy physics: applyForce adds to acceleration, update() applies acceleration * dt to velocity, velocity * dt to position

        body.update(0.016f);  // ~1 tick at 60fps
        Vector3 pos1 = body.getPosition();
        Vector3 vel1 = body.getVelocity();
        System.out.println("After update(0.016): position=(" + pos1.x + ", " + pos1.y + ", " + pos1.z + "), velocity=(" + vel1.x + "," + vel1.y + "," + vel1.z +")");

        // Apply and update again - check gravity isn't magically applied
        body.applyForce(new Vector3(0, 0, 5));
        body.update(0.016f);
        Vector3 pos2 = body.getPosition();
        Vector3 vel2 = body.getVelocity();
        System.out.println("After applyForce(0,0,5) + update: pos=(" + pos2.x + ", " + pos2.y + ", " + pos2.z + "), vel=(" + vel2.x + "," + vel2.y + "," + vel2.z+")");

        // 4) Test PhysicsBody with initial position and mass
        System.out.println("\n=== Testing PhysicsBody(Vector3, mass) ===");
        PhysicsBody body2 = new PhysicsBody(new Vector3(10, 20, 30), 2.0f);
        Vector3 init2 = body2.getPosition();
        System.out.println("Position after ctor(10,20,30,mass=2): (" + init2.x + "," + init2.y + "," + init2.z +")");
        body2.applyForce(new Vector3(0, 4, 0));  // acceleration = force/mass = 2
        body2.update(1.0f);
        System.out.println("After applyForce(0,4,0) + update(1.0): pos.y = " + body2.getPosition().y + " (expect: 20 + 2*1 = 22)");

        // 5) Test setPosition and setVelocity
        body.setPosition(new Vector3(100, 200, 300));
        System.out.println("After setPosition(100,200,300): pos = " + body.getPosition().y);

        // 6) Test static body
        body.setStatic(true);
        body.applyForce(new Vector3(999, 999, 999));  // should be ignored
        body.update(1.0f);
        System.out.println("Static body - after huge force + update: pos = (" + body.getPosition().x + "," + body.getPosition().y + "," + body.getPosition().z + ") (expect unchanged at (100,200,300))");
        body.setStatic(false);

        // 7) Test applyImpulse
        body.applyImpulse(new Vector3(0, 10, 0));  // velocity += 10
        System.out.println("After applyImpulse(0,10,0): vel.y = " + body.getVelocity().y);

        // 8) Test reset
        body.reset();
        System.out.println("After reset: pos=(" + body.getPosition().x + "," + body.getPosition().y + "," + body.getPosition().z + ") vel=(" + body.getVelocity().x + "," + body.getVelocity().y + "," + body.getVelocity().z +")");

        // 9) Test PhysicsWorld
        System.out.println("\n=== Testing PhysicsWorld ===");
        PhysicsWorld world = new PhysicsWorld();
        System.out.println("World created");
        world.addBody(body);
        System.out.println("After addBody(body)");
        world.setGlobalForce(force);
        System.out.println("After setGlobalForce(force)");
        world.setDeltaTime(0.05f);
        System.out.println("After setDeltaTime(0.05): delta = " + world.getDeltaTime());
        world.update();
        System.out.println("After world.update()");
        world.removeBody(body);
        System.out.println("After removeBody(body)");
        world.close();
        System.out.println("After world.close()");

        System.out.println("\n=== ALL TESTS PASSED ===");
    }
}
