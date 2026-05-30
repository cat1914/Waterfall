package com.waterfall.physics.rigidbody;

import java.util.UUID;

/**
 * 刚体唯一标识符
 */
public record RigidBodyId(UUID uuid) {
    
    public RigidBodyId() {
        this(UUID.randomUUID());
    }
    
    public static RigidBodyId fromString(String id) {
        return new RigidBodyId(UUID.fromString(id));
    }
    
    @Override
    public String toString() {
        return uuid.toString();
    }
}
