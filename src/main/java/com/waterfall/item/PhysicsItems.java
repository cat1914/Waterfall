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

public class PhysicsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, WaterfallMod.MODID);
    
    public static final DeferredHolder<Item, Item> PHYSICS_SPAWNER = ITEMS.register("physics_spawner", () ->
        new BlockItem(PhysicsBlocks.PHYSICS_SPAWNER.get(), new Item.Properties())
    );
    
    public static final DeferredHolder<Item, Item> PHYSICS_PORTAL = ITEMS.register("physics_portal", () ->
        new BlockItem(PhysicsBlocks.PHYSICS_PORTAL.get(), new Item.Properties().rarity(Rarity.RARE))
    );
    
    public static final DeferredHolder<Item, Item> WATER_SIMULATION_BLOCK = ITEMS.register("water_simulation_block", () ->
        new BlockItem(PhysicsBlocks.WATER_SIMULATION_BLOCK.get(), new Item.Properties())
    );
    
    public static final DeferredHolder<Item, Item> PHYSICS_WAND = ITEMS.register("physics_wand", () ->
        new PhysicsWandItem(new Item.Properties().stacksTo(1).durability(100).rarity(Rarity.EPIC))
    );
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
