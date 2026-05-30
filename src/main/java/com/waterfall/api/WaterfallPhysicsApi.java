package com.waterfall.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.waterfall.entity.PhysicsBlockEntity;
import java.util.Map;

/**
 * Waterfall Physics API
 * 
 * Provides methods for other mods to create and manage physics structures in Minecraft.
 */
public interface WaterfallPhysicsApi {

    /**
     * Creates a physics structure from a map of block positions and states.
     * 
     * @param level The level to create the structure in
     * @param position The world position to place the structure at
     * @param blocks Map of local block positions to block states
     * @return The created PhysicsBlockEntity
     */
    PhysicsBlockEntity createPhysicsStructure(Level level, Vec3 position, Map<BlockPos, BlockState> blocks);

    /**
     * Creates a physics structure from world coordinates.
     * 
     * @param level The level to create the structure in
     * @param center The center position
     * @param min Minimum corner
     * @param max Maximum corner
     * @param consumeBlocks If true, removes original blocks
     * @return The created PhysicsBlockEntity
     */
    PhysicsBlockEntity createFromWorldArea(Level level, Vec3 center, BlockPos min, BlockPos max, boolean consumeBlocks);

    /**
     * Activates physics for an existing entity.
     * 
     * @param entity The physics entity
     */
    void activatePhysics(PhysicsBlockEntity entity);

    /**
     * Deactivates physics for an entity.
     * 
     * @param entity The physics entity
     */
    void deactivatePhysics(PhysicsBlockEntity entity);

    /**
     * Applies an impulse force to a physics entity.
     * 
     * @param entity The physics entity
     * @param force The force vector
     */
    void applyImpulse(PhysicsBlockEntity entity, Vec3 force);

    /**
     * Sets the velocity of a physics entity.
     * 
     * @param entity The physics entity
     * @param velocity The new velocity
     */
    void setVelocity(PhysicsBlockEntity entity, Vec3 velocity);

    /**
     * Destroys a physics structure, optionally restoring blocks.
     * 
     * @param entity The physics entity
     * @param restoreBlocks If true, places blocks back in the world
     */
    void destroyPhysicsStructure(PhysicsBlockEntity entity, boolean restoreBlocks);

    /**
     * Gets the total light blocks count.
     * 
     * @param entity The physics entity
     * @return Number of light blocks
     */
    int getLightBlockCount(PhysicsBlockEntity entity);

    /**
     * Gets the total heavy blocks count.
     * 
     * @param entity The physics entity
     * @return Number of heavy blocks
     */
    int getHeavyBlockCount(PhysicsBlockEntity entity);

    /**
     * Checks if a physics entity is underwater.
     * 
     * @param entity The physics entity
     * @return True if underwater
     */
    boolean isUnderwater(PhysicsBlockEntity entity);

    /**
     * Calculates the net buoyancy of an entity.
     * 
     * @param entity The physics entity
     * @return Net buoyancy value
     */
    float calculateNetBuoyancy(PhysicsBlockEntity entity);
}
