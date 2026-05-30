package com.waterfall.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;
import com.waterfall.block.PhysicsBlocks;

/**
 * Physics Items Registration
 * 
 * NOTE: These items are internal to Waterfall Physics.
 * Other mods should use the API to create physics structures programmatically.
 */
public class PhysicsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, WaterfallMod.MODID);
    
    // Internal items - not intended for direct player use
    public static final DeferredHolder<Item, Item> PHYSICS_CONTAINER = ITEMS.register("physics_container", () ->
        new BlockItem(PhysicsBlocks.PHYSICS_CONTAINER_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC))
    );
    
    public static final DeferredHolder<Item, Item> PHYSICS_BINDING_WAND = ITEMS.register("physics_binding_wand", () ->
        new PhysicsBindingWandItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
    );
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
