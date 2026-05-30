package com.waterfall.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import com.waterfall.physics.PhysicsEngineManager;

import javax.annotation.Nullable;
import java.util.UUID;

public class PhysicsEntity extends WaterAnimal {
    private static final EntityDataAccessor<Float> DATA_MASS = SynchedEntityData.defineId(PhysicsEntity.class, EntityDataSerializers.FLOAT);
    
    private UUID physicsId;
    private float mass = 1.0f;
    private boolean isPhysicsEnabled = true;
    
    public PhysicsEntity(EntityType<? extends PhysicsEntity> entityType, Level level) {
        super(entityType, level);
        this.physicsId = UUID.randomUUID();
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PhysicsEntityType.createAttributes();
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MASS, 1.0f);
    }
    
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!this.level().isClientSide && isPhysicsEnabled) {
            Vec3 position = this.position();
            PhysicsEngineManager.getInstance().registerEntity(physicsId, position, mass);
        }
    }
    
    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (!this.level().isClientSide) {
            PhysicsEngineManager.getInstance().unregisterEntity(physicsId);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide && isPhysicsEnabled) {
            PhysicsEngineManager.getInstance().updateEntity(physicsId, this.level());
            
            Vec3 newPosition = PhysicsEngineManager.getInstance().getEntityPosition(physicsId);
            if (newPosition != null) {
                this.setPos(newPosition.x, newPosition.y, newPosition.z);
            }
        }
    }
    
    public void setMass(float mass) {
        this.mass = mass;
        this.entityData.set(DATA_MASS, mass);
    }
    
    public float getMass() {
        return this.entityData.get(DATA_MASS);
    }
    
    public void setPhysicsEnabled(boolean enabled) {
        this.isPhysicsEnabled = enabled;
        if (!enabled) {
            PhysicsEngineManager.getInstance().unregisterEntity(physicsId);
        } else {
            Vec3 position = this.position();
            PhysicsEngineManager.getInstance().registerEntity(physicsId, position, mass);
        }
    }
    
    public boolean isPhysicsEnabled() {
        return isPhysicsEnabled;
    }
    
    public void applyPhysicsImpulse(Vec3 impulse) {
        if (isPhysicsEnabled && physicsId != null) {
            PhysicsEngineManager.getInstance().applyImpulse(physicsId, impulse);
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putUUID("PhysicsId", this.physicsId);
        tag.putFloat("Mass", this.mass);
        tag.putBoolean("PhysicsEnabled", this.isPhysicsEnabled);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PhysicsId")) {
            this.physicsId = tag.getUUID("PhysicsId");
        }
        if (tag.contains("Mass")) {
            this.mass = tag.getFloat("Mass");
        }
        if (tag.contains("PhysicsEnabled")) {
            this.isPhysicsEnabled = tag.getBoolean("PhysicsEnabled");
        }
    }
    
    @Override
    public boolean isInWater() {
        if (this.is Swimming) {
            return true;
        } else {
            BlockPos blockpos = this.getBlockPosBelowThatIsThisOrThis();
            FluidState fluidstate = this.level().getFluidState(blockpos);
            return fluidstate.is(Fluids.WATER) || fluidstate.is(Fluids.FLOWING_WATER);
        }
    }
}
