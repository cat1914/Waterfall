# Waterfall Physics

A NeoForge mod that integrates a real physics simulation engine into Minecraft, featuring underwater physics and sub-dimension support.

## Features

### Physics Engine Integration
- **Heavy C++ Physics Engine**: Utilizes the native `heavy` library for accurate physics calculations
- **JNA Integration**: Native library bindings for seamless integration with Minecraft
- **Real-time Simulation**: 60 FPS physics tick rate for smooth entity movement

### Sub-Dimension Physics (Sable/Valkyrien Skies Inspired)
- **Rigid Body System**: Groups of blocks become physics entities that can move and rotate
- **Physics Container Block**: Core block that binds surrounding blocks into a rigid body
- **Physics Binding Wand**: Tool to select and bind blocks to physics containers
- **Physics Laboratory Dimension**: Isolated dimension for physics experiments
- **Real-time Physics Tick**: Physics simulation runs in the physics dimension

### Underwater Physics
- **Buoyancy**: Objects float realistically based on their mass and volume
- **Water Drag**: Resistance based on fluid density and velocity
- **Depth Pressure**: Pressure increases with depth for realistic underwater behavior
- **Lift Forces**: Objects experience upward lift in water

### Interactive Elements
- **Physics Spawner Block**: Spawns physics-enabled entities
- **Physics Wand**: Creative mode tool to spawn physics entities anywhere
- **Physics Entity**: Custom entity with realistic physics behavior
- **Water Simulation Block**: Advanced fluid dynamics block
- **Physics Container**: Binds blocks into rigid physics bodies
- **Physics Binding Wand**: Select and bind blocks to physics containers

## Architecture (Inspired by Sable and Valkyrien Skies)

### Core Concepts
1. **Rigid Body**: A collection of blocks bound together as a single physics entity
2. **Physics Container**: Block entity that manages rigid bodies
3. **Physics Dimension**: Isolated dimension where physics simulation runs
4. **Bindings**: Map between world coordinates and physics body local coordinates

### Data Flow
1. Player places Physics Container Block
2. Uses Physics Binding Wand to select area to bind
3. Blocks are scanned and added to a RigidBody
4. Player activates the container to start physics simulation
5. Physics simulation runs in the physics dimension
6. Block positions are synchronized between dimensions

### Key Classes
- `RigidBody`: Represents a physics object made of blocks
- `RigidBodyId`: Unique identifier for rigid bodies
- `RigidBodyManager`: Manages all rigid bodies in the world
- `PhysicsContainerBlockEntity`: Block entity that manages rigid body
- `PhysicsContainerBlock`: Player-interactable block for physics containers
- `PhysicsBindingWandItem`: Tool for selecting and binding blocks
- `PhysicsWorldData`: Saved data for physics dimension state

## Usage

### Creating a Physics Body
1. Place a **Physics Container** block (purple)
2. Right-click with **Physics Binding Wand** to select first corner
3. Right-click again to select second corner (defines area)
4. Right-click on or near Physics Container to bind blocks
5. Right-click Physics Container (without Shift) to activate physics

### Basic Physics Entity
1. Place a Physics Spawner block
2. Right-click to spawn a physics entity
3. Watch it fall and interact with the environment

### Physics Wand
1. Get the Physics Wand item (creative mode)
2. Right-click to spawn physics entities
3. Attack entities to create physics objects

### Underwater Physics
1. Submerge physics entities in water
2. Observe buoyancy and drag effects
3. Configure parameters in `config/waterfall-physics.toml`

### Physics Dimension
- Use Physics Portal to access the Physics Laboratory dimension
- Physics simulation runs here for performance and isolation

## Installation

### Prerequisites
- Minecraft 1.21.1
- NeoForge 21.1.231 or compatible version
- Java 21

### Building
1. Clone the repository
2. Ensure `libheavy-0.0.1.so` is in the project root
3. Run `./gradlew build`
4. Find the mod JAR in `build/libs/`

## Configuration

Edit `config/waterfall-physics.toml`:

```toml
gravity = 9.81
water_gravity = 1.5
water_density = 1.025
air_drag = 0.01
water_drag = 0.05
buoyancy_force = 2.0
max_entities = 1000
enable_underwater_physics = true
enable_physics_dimension = true
physics_tick_rate = 0.016
```

## Technical Details

### Native Library
The mod uses JNA to load the `heavy` physics engine shared library:
- Location: `libheavy-0.0.1.so` in project root
- API: PhysicsWorld, PhysicsBody, Vector3, Force classes

### Architecture
```
WaterfallMod
├── natives/         # JNA bindings
├── physics/         # Physics engine wrappers
│   ├── rigidbody/   # Rigid body system (core feature)
│   ├── Vector3
│   ├── Force
│   ├── PhysicsBody
│   ├── PhysicsWorld
│   └── PhysicsEngineManager
├── dimension/       # Sub-dimension implementation
│   ├── PhysicsDimension
│   ├── PhysicsDimensionType
│   └── PhysicsWorldData
├── entity/          # Physics entity types
├── block/           # Custom blocks
│   ├── PhysicsBlocks
│   ├── PhysicsBlockEntities
│   └── PhysicsContainerBlock
├── item/            # Custom items
│   ├── PhysicsItems
│   ├── PhysicsWandItem
│   └── PhysicsBindingWandItem
├── config/          # Configuration management
├── network/         # Network synchronization
└── client/          # Client-side rendering
```

### Physics Simulation
- Tick rate: 60 Hz (configurable)
- Gravity: 9.81 m/s² (air), 1.5 m/s² (water)
- Buoyancy: Calculated based on fluid density and submerged volume
- Drag: Velocity-dependent resistance force

### Rigid Body System (Inspired by Sable)
1. **Block Selection**: Use Physics Binding Wand to select an area
2. **Binding**: Blocks are added to a RigidBody with local coordinates
3. **Activation**: PhysicsContainer is activated to start simulation
4. **Simulation**: Physics runs in the physics dimension
5. **Rendering**: Blocks are rendered at their physics-calculated positions

## License
GPL-3.0

## Credits
- Physics Engine: [heavy](https://github.com/cat1914/heavy)
- Inspiration: [Sable Mod](https://github.com/...), [Valkyrien Skies](https://github.com/ValkyrienSkies/Valkyrien-Skies)
- NeoForge Team
