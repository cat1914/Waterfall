package com.waterfall.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;
import com.waterfall.physics.PhysicsEngineManager;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class PhysicsEntityType {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, WaterfallMod.MODID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<PhysicsEntity>> PHYSICS_ENTITY = 
        ENTITY_TYPES.register("physics_entity", () ->
            EntityType.Builder.<PhysicsEntity>of(PhysicsEntity::new, MobCategory.CREATURE)
                .sized(0.6f, 0.6f)
                .clientTrackingRange(64)
                .updateInterval(1)
                .build(ResourceLocation.tryParse(WaterfallMod.MODID + ":physics_entity").toString()));
    
    public static final DeferredHolder<EntityType<?>, EntityType<PhysicsBlockEntity>> PHYSICS_BLOCK = 
        ENTITY_TYPES.register("physics_block", () ->
            EntityType.Builder.<PhysicsBlockEntity>of(PhysicsBlockEntity::new, MobCategory.MISC)
                .sized(1.0f, 1.0f)
                .clientTrackingRange(128)
                .updateInterval(3)
                .build(ResourceLocation.tryParse(WaterfallMod.MODID + ":physics_block").toString()));
    
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PhysicsEntity.createAttributes();
    }
}
