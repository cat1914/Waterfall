package com.waterfall.example;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import com.waterfall.WaterfallPhysics;
import com.waterfall.entity.PhysicsBlockEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * Example mod showing how to use Waterfall Physics API
 * 
 * This example demonstrates:
 * - Creating physics structures programmatically
 * - Using block maps
 * - Creating from world areas
 */
@Mod(WaterfallPhysicsExample.MOD_ID)
public class WaterfallPhysicsExample {

    public static final String MOD_ID = "waterfall_example";
    
    // Items registration
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MOD_ID);
    
    // Example test item
    public static final DeferredHolder<Item, PhysicsStructureSpawnerItem> TEST_SPAWNER = 
        ITEMS.register("test_spawner", PhysicsStructureSpawnerItem::new);

    public WaterfallPhysicsExample(IEventBus modEventBus, ModContainer container) {
        ITEMS.register(modEventBus);
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // This is just an example mod, no setup needed
    }
    
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(TEST_SPAWNER.get());
        }
    }
    
    /**
     * Example item that demonstrates Waterfall Physics API usage
     */
    public static class PhysicsStructureSpawnerItem extends Item {
        
        public PhysicsStructureSpawnerItem() {
            super(new Properties().stacksTo(1));
        }
        
        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (!context.getLevel().isClientSide) {
                Level level = context.getLevel();
                BlockPos pos = context.getClickedPos();
                
                // Check if Waterfall API is loaded
                if (WaterfallPhysics.isLoaded()) {
                    // Example 1: Create from block map
                    spawnExampleStructure1(level, pos.above());
                    
                    // Example 2: Create from world area
                    // You would use createFromWorldArea for that
                }
            }
            
            return InteractionResult.SUCCESS;
        }
        
        /**
         * Example 1: Creates a simple wooden house using the block map method
         */
        private void spawnExampleStructure1(Level level, BlockPos startPos) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            
            // Floor (8x8 wood planks)
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    blocks.put(new BlockPos(x, 0, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
            
            // Walls (4x3)
            for (int y = 1; y <= 3; y++) {
                for (int x = -3; x <= 3; x++) {
                    blocks.put(new BlockPos(x, y, -3), Blocks.OAK_PLANKS.defaultBlockState());
                    blocks.put(new BlockPos(x, y, 3), Blocks.OAK_PLANKS.defaultBlockState());
                }
                for (int z = -3; z <= 3; z++) {
                    blocks.put(new BlockPos(-3, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                    blocks.put(new BlockPos(3, y, z), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
            
            // Door
            blocks.put(new BlockPos(0, 1, -3), Blocks.OAK_DOOR.defaultBlockState());
            blocks.put(new BlockPos(0, 2, -3), Blocks.OAK_DOOR.defaultBlockState());
            
            // Lever on wall
            blocks.put(new BlockPos(-2, 1, 2), Blocks.LEVER.defaultBlockState());
            
            // Create physics structure
            Vec3 center = new Vec3(startPos.getX(), startPos.getY(), startPos.getZ());
            PhysicsBlockEntity structure = WaterfallPhysics.getApi()
                .createPhysicsStructure(level, center, blocks);
                
            if (structure != null) {
                // Optional: Apply an initial impulse to make it move
                WaterfallPhysics.getApi().applyImpulse(structure, new Vec3(0, 0.5, 0));
            }
        }
        
        /**
         * Example 2: Creates a balanced buoyancy test structure
         * (4 light blocks + 1 heavy block = balanced)
         */
        private void spawnBuoyancyTestStructure(Level level, BlockPos startPos) {
            Map<BlockPos, BlockState> blocks = new HashMap<>();
            
            // 4 light blocks (wood)
            blocks.put(new BlockPos(0, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
            blocks.put(new BlockPos(1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
            blocks.put(new BlockPos(0, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
            blocks.put(new BlockPos(1, 0, 1), Blocks.OAK_PLANKS.defaultBlockState());
            
            // 1 heavy block (stone)
            blocks.put(new BlockPos(0, 1, 0), Blocks.STONE.defaultBlockState());
            
            // Create
            Vec3 center = new Vec3(startPos.getX() + 0.5, startPos.getY(), startPos.getZ() + 0.5);
            PhysicsBlockEntity structure = WaterfallPhysics.getApi()
                .createPhysicsStructure(level, center, blocks);
        }
    }
}
