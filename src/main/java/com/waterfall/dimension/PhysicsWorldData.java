package com.waterfall.dimension;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import com.waterfall.WaterfallMod;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 物理维度世界数据 - 存储和管理物理世界的状态
 */
public class PhysicsWorldData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String DATA_NAME = WaterfallMod.MODID + "_physics_world";
    
    public static final ResourceKey<Level> PHYSICS_DIMENSION = ResourceKey.create(
        Registries.LEVEL,
        new ResourceLocation(WaterfallMod.MODID, "physics_lab")
    );
    
    private boolean isInitialized;
    private long lastTick;
    
    public PhysicsWorldData() {
        this.isInitialized = false;
        this.lastTick = 0;
    }
    
    public static PhysicsWorldData create() {
        return new PhysicsWorldData();
    }
    
    public static PhysicsWorldData load(CompoundTag tag) {
        PhysicsWorldData data = new PhysicsWorldData();
        data.isInitialized = tag.getBoolean("isInitialized");
        data.lastTick = tag.getLong("lastTick");
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.world.level.saveddata.SaveData.Factory factory) {
        tag.putBoolean("isInitialized", isInitialized);
        tag.putLong("lastTick", lastTick);
        return tag;
    }
    
    public void tick(net.minecraft.server.level.ServerLevel level) {
        if (!isInitialized) {
            initialize(level);
        }
        
        // 每tick更新刚体
        RigidBodyManager.getInstance().tick(level);
        
        lastTick = level.getGameTime();
        setDirty();
    }
    
    private void initialize(net.minecraft.server.level.ServerLevel level) {
        if (!isInitialized) {
            LOGGER.info("Initializing physics dimension world data");
            isInitialized = true;
            setDirty();
        }
    }
    
    public static PhysicsWorldData get(net.minecraft.server.level.ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new Factory<>(PhysicsWorldData::create, PhysicsWorldData::load),
            DATA_NAME
        );
    }
}
