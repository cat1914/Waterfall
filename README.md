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

### Material-specific Underwater Physics
- **Light Block Buoyancy**: Wood, wool, bamboo, sponge, moss, etc. float upward in water
- **Heavy Block Sinking**: Stone, ores, obsidian, netherite, etc. sink downward in water
- **Perfect Buoyancy Balance**: 4 light blocks = 1 heavy block in terms of force
- **Depth Detection**: Automatic water detection for physics application
- **Realistic Force Calculation**: Light blocks have positive buoyancy, heavy blocks have negative buoyancy

### Underwater Physics (Legacy)
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
- **Physics Block Entity**: Movable physical structure with interactive elements

## Full Block Component Interaction

Physics Block Entities support interaction with all types of functional blocks:

### Switches & Controls
- **Levers**: Right-click to toggle (POWERED state changes)
- **Buttons**: Right-click to activate (POWERED state)
- **Pressure Plates**: Stepping on them triggers activation
- **Buttons (Wood/Stone)**: Same functionality as levers

### Doors & Gates
- **Doors (Wood/Iron)**: Right-click to open/close (OPEN state)
- **Trapdoors**: Right-click to open/close (OPEN state)
- **Fence Gates**: Right-click to open/close (OPEN state)

### Storage & Functional Blocks
- **Chests/Trapped Chests**: Right-click to open inventory
- **Ender Chests**: Access ender storage from anywhere
- **Barrels**: Functional barrel inventory
- **Crafting Tables/Enchanting Tables**: Use normally
- **Furnaces/Smokers/Blast Furnaces**: Full functionality

### Redstone Components
- **Redstone Lamps/Torches**: Toggle redstone states
- **Repeaters/Comparators**: Functional redstone
- **Dispensers/Droppers**: Can be activated
- **Pistons/Sticky Pistons**: Extend/retract

### Interactive Entities
- **Item Frames**: Place/remove items
- **Signs/Editor Signs**: Edit text
- **Flower Pots**: Plant/harvest flowers
- **Beds**: Sleep/respawn

## Architecture (Inspired by Sable and Valkyrien Skies)

### Core Concepts
1. **Rigid Body**: A collection of blocks bound together as a single physics entity
2. **Physics Container**: Block entity that manages rigid bodies
3. **Physics Dimension**: Isolated dimension where physics simulation runs
4. **Bindings**: Map between world coordinates and physics body local coordinates
5. **Physics Block Entity**: Moveable entity with full block interaction support

### Data Flow
1. Player places Physics Container Block
2. Uses Physics Binding Wand to select area to bind
3. Blocks are scanned and added to a RigidBody
4. Player activates the container to start physics simulation
5. Physics simulation runs in the physics dimension
6. Shift+Right-click converts to Physics Block Entity
7. Block positions are synchronized between dimensions

### Key Classes
- `RigidBody`: Represents a physics object made of blocks
- `RigidBodyId`: Unique identifier for rigid bodies
- `RigidBodyManager`: Manages all rigid bodies in the world
- `PhysicsContainerBlockEntity`: Block entity that manages rigid body
- `PhysicsContainerBlock`: Player-interactable block for physics containers
- `PhysicsBindingWandItem`: Tool for selecting and binding blocks
- `PhysicsWorldData`: Saved data for physics dimension state
- `PhysicsBlockEntity`: Movable entity with full block interaction
- `MaterialPhysics`: Material-specific property definitions
- `PhysicsBlockRenderer`: Client-side rendering of physics blocks

## Usage

### Using Material-Specific Underwater Physics

1. **Place a Physics Container Block** (purple) in or near water
2. **Use the Physics Binding Wand** to select a rectangular area with your blocks:
   - Right-click the first corner
   - Right-click the second corner
3. **Right-click near the Physics Container** to bind the selected blocks
4. **Shift+Right-click on Physics Container** to convert into physics block entity
5. **Watch as light blocks (wood, wool) float and heavy blocks (stone, ore) sink!**

#### Buoyancy Rules
- **Light Blocks**: Wood (all types), Wool (all colors), Bamboo, Sponge, Moss
- **Heavy Blocks**: Stone (all types), Ores, Ore Blocks, Obsidian, Netherite
- **Balance**: 4 light blocks = 1 heavy block in terms of force
- **Neutral Blocks**: Other blocks have neutral buoyancy

#### Perfect Balance Examples
- 4 wood blocks + 1 stone block = balanced (neutral buoyancy)
- 8 wool blocks + 2 ore blocks = balanced
- 16 wood blocks + 4 stone blocks = balanced

### Interacting with Physics Blocks

#### Basic Interactions
- **Right-click physics entity** to toggle physics on/off
- **Collide with physics entity** to push it around
- **Physics entity blocks have real collision** - you can stand on them
- **Underwater effects** automatically apply when submerged

#### Block-Specific Interactions
- **Right-click levers** on the structure - they toggle normally!
- **Right-click doors/trapdoors** - they open/close properly
- **Right-click chests** - access their inventories
- **Stand on pressure plates** - triggers activation
- **Press buttons** - works like in normal world
- **Use crafting tables/furnaces** - full functionality maintained

### Creating a Physics Body
1. Place a **Physics Container** block (purple)
2. Right-click with **Physics Binding Wand** to select first corner
3. Right-click again to select second corner (defines area)
4. Right-click on or near Physics Container to bind blocks
5. Right-click Physics Container (without Shift) to activate physics
6. Shift+Right-click to convert to movable Physics Block Entity

### Physics Dimension
- Use **Physics Portal** block to access the Physics Laboratory dimension
- Physics simulation runs in dedicated dimension for performance
- Dimension has its own physics world data
- Gravity in dimension can be configured

### Configuration
- Edit `config/waterfall-physics.toml` to configure:
  - Air/Underwater gravity values
  - Material buoyancy multipliers
  - Heavy/light block weight ratios
  - Physics tick rate
  - Dimension settings
  - Enable/disable specific physics features

## License

This mod is available under the MIT License.

## Credits

- **Sable & Valkyrien Skies**: Architecture inspiration for sub-dimension physics
- **Heavy Physics Engine**: Native physics simulation library
- **NeoForge**: Modding framework
- **Minecraft Community**: Support and inspiration
