package com.waterfall.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;

public class PhysicsBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, WaterfallMod.MODID);
    
    public static final DeferredHolder<Block, Block> PHYSICS_SPAWNER = BLOCKS.register("physics_spawner", () ->
        new PhysicsSpawnerBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIAMOND)
                .strength(3.0f, 3.0f)
                .requiresCorrectToolForDrops()
        )
    );
    
    public static final DeferredHolder<Block, Block> PHYSICS_PORTAL = BLOCKS.register("physics_portal", () ->
        new PhysicsPortalBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.GOLD)
                .strength(-1.0f, -1.0f)
                .noDrops()
                .pushReaction(PushReaction.BLOCK)
        )
    );
    
    public static final DeferredHolder<Block, Block> WATER_SIMULATION_BLOCK = BLOCKS.register("water_simulation_block", () ->
        new WaterSimulationBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.WATER)
                .strength(0.5f, 0.5f)
        )
    );
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
