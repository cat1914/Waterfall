package com.waterfall.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;

/**
 * Physics Blocks Registration
 * 
 * NOTE: These blocks are internal to Waterfall Physics.
 * Other mods should use the API to create physics structures programmatically.
 */
public class PhysicsBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, WaterfallMod.MODID);
    
    // Internal blocks - not intended for direct player use
    public static final DeferredHolder<Block, Block> PHYSICS_CONTAINER_BLOCK = BLOCKS.register("physics_container", () ->
        new PhysicsContainerBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(2.0f, 6.0f)
                .requiresCorrectToolForDrops()
        )
    );
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
