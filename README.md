# Waterfall Physics

A NeoForge library mod that provides physics simulation capabilities for Minecraft mods.

**Waterfall Physics is an API library**, not designed for direct player use.
Other mods can use this library to create interactive physics structures in the game.

---

## Features

### API for Other Mods
- Simple, intuitive Java API for creating physics structures
- Programmatic block placement in physics entities
- Full control over physics properties
- Access to material-specific buoyancy calculations

### Physics System
- Material-specific buoyancy (wood/wool floats, stone/ores sink)
- Perfect balance system (4 light blocks = 1 heavy block in force)
- Realistic physics simulation
- Sub-dimension physics architecture (inspired by Sable & Valkyrien Skies)

### Interactive Structures
- Physics entities maintain real block interactions
- Levers, buttons, doors, chests still work normally
- Collision detection and player interaction
- Full block state support (redstone, etc.)

---

## Quick Start for Modders

### 1. Add Dependency

In your `build.gradle` or `gradle.properties`:
```gradle
dependencies {
    implementation fg.deobf("com.yourname:waterfall:1.0.0")
}
```

### 2. Access the API

```java
import com.waterfall.WaterfallPhysics;
import com.waterfall.api.WaterfallPhysicsApi;

// Check if Waterfall is loaded
if (WaterfallPhysics.isLoaded()) {
    // Get the API instance
    WaterfallPhysicsApi api = WaterfallPhysics.getApi();
}
```

### 3. Create a Physics Structure

```java
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

// Create a block map
Map<BlockPos, BlockState> blocks = new HashMap<>();

// Add your blocks
blocks.put(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
blocks.put(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
blocks.put(new BlockPos(0, 1, 0), Blocks.STONE.defaultBlockState());

// Create the physics structure
Vec3 position = new Vec3(100, 64, 100);
PhysicsBlockEntity structure = WaterfallPhysics.getApi()
    .createPhysicsStructure(level, position, blocks);
```

---

## API Reference

### `WaterfallPhysics.getApi()`

Main API entry point.

---

### `createPhysicsStructure(Level, Vec3, Map<BlockPos, BlockState>)`

Creates a physics structure from a map of block positions and states.

**Parameters:**
- `level`: The world level
- `position`: The center position for the structure
- `blocks`: Map of local BlockPos to BlockState

**Returns:**
- `PhysicsBlockEntity`: The created physics entity

---

### `createFromWorldArea(Level, Vec3, BlockPos, BlockPos, boolean)`

Creates a physics structure from an existing area of blocks.

**Parameters:**
- `level`: The world level
- `center`: The center position
- `min`: Minimum corner of area
- `max`: Maximum corner of area
- `consumeBlocks`: If true, removes blocks from the world

---

### `activatePhysics(PhysicsBlockEntity)` / `deactivatePhysics(PhysicsBlockEntity)`

Enables or disables physics simulation for an entity.

---

### `applyImpulse(PhysicsBlockEntity, Vec3)`

Applies an impulse force to a physics entity.

---

### `setVelocity(PhysicsBlockEntity, Vec3)`

Sets the velocity of a physics entity.

---

### `destroyPhysicsStructure(PhysicsBlockEntity, boolean)`

Destroys a physics structure, optionally restoring blocks to the world.

---

### `getLightBlockCount(PhysicsBlockEntity)` / `getHeavyBlockCount(PhysicsBlockEntity)`

Gets the count of light or heavy blocks in a physics entity.

---

### `isUnderwater(PhysicsBlockEntity)`

Checks if a physics entity is currently underwater.

---

### `calculateNetBuoyancy(PhysicsBlockEntity)`

Calculates the net buoyancy of an entity (positive = floats, negative = sinks).

---

## Material System

### Light Blocks (Float)
- Wood (all types)
- Wool (all colors)
- Bamboo
- Sponge
- Moss
- ...and more

### Heavy Blocks (Sink)
- Stone (all types)
- Ores (iron, gold, diamond, etc.)
- Obsidian
- Netherite
- ...and more

### Buoyancy Formula
```
Net Force = (Light Block Count) - (Heavy Block Count * 0.25)
```
- Positive = Floats
- Negative = Sinks
- Zero = Neutral

---

## Example Code

### Example 1: Simple Wood Platform

```java
Map<BlockPos, BlockState> blocks = new HashMap<>();

// 3x3 wood platform
for (int x = -1; x <= 1; x++) {
    for (int z = -1; z <= 1; z++) {
        blocks.put(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
    }
}

// Create it!
Vec3 pos = new Vec3(100, 64, 100);
PhysicsBlockEntity structure = WaterfallPhysics.getApi()
    .createPhysicsStructure(level, pos, blocks);

// Push it up a bit
WaterfallPhysics.getApi().applyImpulse(structure, new Vec3(0, 1, 0));
```

### Example 2: Balanced Buoyancy Structure

```java
Map<BlockPos, BlockState> blocks = new HashMap<>();

// 4 light blocks
blocks.put(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
blocks.put(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
blocks.put(new BlockPos(0, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
blocks.put(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());

// 1 heavy block (balances perfectly)
blocks.put(new BlockPos(0, 1, 0), Blocks.STONE.defaultBlockState());

// Create - should be neutrally buoyant!
Vec3 pos = new Vec3(100, 64, 100);
WaterfallPhysics.getApi().createPhysicsStructure(level, pos, blocks);
```

### Example 3: Structure with Interactive Blocks

```java
Map<BlockPos, BlockState> blocks = new HashMap<>();

// Base
for (int x = -2; x <= 2; x++) {
    for (int z = -2; z <= 2; z++) {
        blocks.put(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
    }
}

// Add a lever (still works!)
blocks.put(new BlockPos(0, 1, 0), Blocks.LEVER.defaultBlockState());

// Add a door
blocks.put(new BlockPos(2, 1, 0), Blocks.OAK_DOOR.defaultBlockState());
blocks.put(new BlockPos(2, 2, 0), Blocks.OAK_DOOR.defaultBlockState());

// Create and activate
Vec3 pos = new Vec3(100, 64, 100);
PhysicsBlockEntity structure = WaterfallPhysics.getApi()
    .createPhysicsStructure(level, pos, blocks);

// Physics will handle it!
```

---

## Architecture Overview

### Physics Entity
- `PhysicsBlockEntity` represents a physics structure
- Contains multiple blocks arranged in a coordinate system
- Maintains block states and updates them
- Handles player interactions on blocks

### Rigid Body
- Physics simulation data structure
- Manages forces (gravity, buoyancy, drag)
- Updates position and velocity

### Material Physics
- Classifies blocks into light/heavy/neutral
- Calculates mass and buoyancy
- Underwater detection

---

## License

MIT License - feel free to use this library in your mods!

---

## Credits

- Inspired by Sable and Valkyrien Skies
- Heavy Physics Engine integration
- NeoForge modding framework
