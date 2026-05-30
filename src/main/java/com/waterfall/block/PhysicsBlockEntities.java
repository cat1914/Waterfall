package com.waterfall.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;
import com.waterfall.block.entity.PhysicsContainerBlockEntity;

public class PhysicsBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WaterfallMod.MODID);
    
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhysicsContainerBlockEntity>> PHYSICS_CONTAINER = 
        BLOCK_ENTITIES.register("physics_container", () ->
            BlockEntityType.Builder.of(PhysicsContainerBlockEntity::new, 
                PhysicsBlocks.PHYSICS_CONTAINER_BLOCK.get()).build(null)
        );
    
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
