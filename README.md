# Waterfall Physics

A NeoForge mod that integrates a real physics simulation engine into Minecraft, featuring underwater physics and sub-dimension support.

## Features

### Physics Engine Integration
- **Heavy C++ Physics Engine**: Utilizes the native `heavy` library for accurate physics calculations
- **JNA Integration**: Native library bindings for seamless integration with Minecraft
- **Real-time Simulation**: 60 FPS physics tick rate for smooth entity movement

### Underwater Physics
- **Buoyancy**: Objects float realistically based on their mass and volume
- **Water Drag**: Resistance based on fluid density and velocity
- **Depth Pressure**: Pressure increases with depth for realistic underwater behavior
- **Lift Forces**: Objects experience upward lift in water

### Sub-Dimension: Physics Laboratory
- **Isolated Physics World**: A dedicated dimension for physics experiments
- **Custom Gravity**: Configurable gravity settings per dimension
- **Portal Access**: Build portals to transport between dimensions

### Interactive Elements
- **Physics Spawner Block**: Spawns physics-enabled entities
- **Physics Wand**: Creative mode tool to spawn physics entities anywhere
- **Physics Entity**: Custom entity with realistic physics behavior
- **Water Simulation Block**: Advanced fluid dynamics block

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

### Usage

#### Basic Physics Entity
1. Place a Physics Spawner block
2. Right-click to spawn a physics entity
3. Watch it fall and interact with the environment

#### Physics Wand
1. Get the Physics Wand item (creative mode)
2. Right-click to spawn physics entities
3. Attack entities to create physics objects

#### Underwater Physics
1. Submerge physics entities in water
2. Observe buoyancy and drag effects
3. Configure parameters in `config/waterfall-physics.toml`

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
├── physics/        # Physics engine wrappers
├── dimension/      # Sub-dimension implementation
├── entity/         # Physics entity types
├── block/          # Custom blocks
├── item/           # Custom items
└── config/         # Configuration management
```

### Physics Simulation
- Tick rate: 60 Hz (configurable)
- Gravity: 9.81 m/s² (air), 1.5 m/s² (water)
- Buoyancy: Calculated based on fluid density and submerged volume
- Drag: Velocity-dependent resistance force

## License
GPL-3.0

## Credits
- Physics Engine: [heavy](https://github.com/cat1914/heavy)
- NeoForge Team
