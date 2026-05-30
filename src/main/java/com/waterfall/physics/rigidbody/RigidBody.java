package com.waterfall.physics.rigidbody;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;
import com.waterfall.physics.MaterialPhysics;
import com.waterfall.physics.Vector3;
import com.waterfall.config.PhysicsConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 表示方块结构的刚体，包含一组方块及其相对位置
 * 具有精确的水下物理模拟：轻质方块有升力，重质方块下沉
 */
public class RigidBody implements AutoCloseable {
    private final RigidBodyId id;
    private final Map<BlockPos, BlockState> blocks;
    private final PhysicsBody physicsBody;
    private float mass;
    private float inertia;
    private boolean isStatic;
    private boolean isActive;
    private boolean isUnderwater;    // 是否在水下
    private float totalBuoyancy;    // 总浮力系数
    private int lightBlockCount;    // 轻质方块数量
    private int heavyBlockCount;    // 重质方块数量

    public RigidBody(RigidBodyId id) {
        this.id = id;
        this.blocks = new HashMap<>();
        this.physicsBody = new PhysicsBody(0, 0, 0, 1.0f);
        this.mass = 1.0f;
        this.inertia = 1.0f;
        this.isStatic = false;
        this.isActive = true;
        this.isUnderwater = false;
        this.totalBuoyancy = 0.0f;
        this.lightBlockCount = 0;
        this.heavyBlockCount = 0;
    }

    public RigidBodyId getId() {
        return id;
    }

    public Map<BlockPos, BlockState> getBlocks() {
        return new HashMap<>(blocks);
    }

    public void addBlock(BlockPos localPos, BlockState state) {
        blocks.put(localPos, state);
        recalculateMassAndInertia();
    }

    public void removeBlock(BlockPos localPos) {
        blocks.remove(localPos);
        recalculateMassAndInertia();
    }

    /**
     * 重新计算质量、惯性和浮力
     * 基于材质分类系统
     */
    private void recalculateMassAndInertia() {
        mass = 0.0f;
        totalBuoyancy = 0.0f;
        lightBlockCount = 0;
        heavyBlockCount = 0;
        
        // 遍历所有方块计算总质量和总浮力
        for (BlockState state : blocks.values()) {
            float blockMassFactor = MaterialPhysics.getMassFactor(state);
            float blockBuoyancy = MaterialPhysics.getBuoyancyFactor(state);
            
            mass += blockMassFactor;
            totalBuoyancy += blockBuoyancy;
            
            // 计数
            if (MaterialPhysics.isLightMaterial(state)) {
                lightBlockCount++;
            } else if (MaterialPhysics.isHeavyMaterial(state)) {
                heavyBlockCount++;
            }
        }
        
        // 避免质量为0
        if (mass <= 0) {
            mass = 1.0f;
        }
        
        // 简化的转动惯量计算 (假设是立方体)
        if (blocks.size() > 0) {
            BlockPos min = findMinPos();
            BlockPos max = findMaxPos();
            float dx = max.getX() - min.getX() + 1;
            float dy = max.getY() - min.getY() + 1;
            float dz = max.getZ() - min.getZ() + 1;
            inertia = (mass / 12.0f) * (dy * dy + dz * dz); // 绕x轴的转动惯量
        }
        
        physicsBody.setMass(mass);
    }

    private BlockPos findMinPos() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos pos : blocks.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }

    private BlockPos findMaxPos() {
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : blocks.keySet()) {
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new BlockPos(maxX, maxY, maxZ);
    }

    public PhysicsBody getPhysicsBody() {
        return physicsBody;
    }

    public float getMass() {
        return mass;
    }

    public float getInertia() {
        return inertia;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        physicsBody.setStatic(isStatic);
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isActive() {
        return isActive;
    }
    
    /**
     * 设置是否在水下
     */
    public void setUnderwater(boolean underwater) {
        this.isUnderwater = underwater;
    }
    
    public boolean isUnderwater() {
        return isUnderwater;
    }
    
    /**
     * 获取总浮力系数
     */
    public float getTotalBuoyancy() {
        return totalBuoyancy;
    }
    
    /**
     * 获取轻质方块数量
     */
    public int getLightBlockCount() {
        return lightBlockCount;
    }
    
    /**
     * 获取重质方块数量
     */
    public int getHeavyBlockCount() {
        return heavyBlockCount;
    }
    
    /**
     * 检查是否浮力平衡
     * 规则：四个轻质方块的升力 = 一个重质方块的重力
     */
    public boolean isBuoyancyBalanced() {
        return lightBlockCount == 4 * heavyBlockCount;
    }
    
    /**
     * 计算净浮力（仅在水下有效）
     * 返回正值表示上浮，负值表示下沉
     */
    public float calculateNetBuoyancy() {
        if (!isUnderwater || !PhysicsConfig.ENABLE_MATERIAL_PHYSICS) {
            return 0.0f; // 不在水下或未启用材质物理，无浮力
        }
        
        // 计算净浮力：轻质升力 - 重质重力
        // 规则：4个轻质升力 = 1个重质重力
        float netBuoyancy = (lightBlockCount * PhysicsConfig.LIGHT_BLOCK_BUOYANCY) - 
                          (heavyBlockCount * PhysicsConfig.HEAVY_BLOCK_WEIGHT * 0.25f);
        
        return netBuoyancy;
    }
    
    /**
     * 应用水下物理力
     * 只在isUnderwater为true时有效
     */
    public void applyUnderwaterForces() {
        if (!isUnderwater || !PhysicsConfig.ENABLE_MATERIAL_PHYSICS) {
            return;
        }
        
        float netBuoyancy = calculateNetBuoyancy();
        float forceMagnitude = netBuoyancy * PhysicsConfig.BUOYANCY_FORCE_MULTIPLIER;
        
        // 应用浮力：正值向上，负值向下
        Vector3 buoyancyForce = new Vector3(0, forceMagnitude, 0);
        physicsBody.applyForce(buoyancyForce);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        tag.putFloat("mass", mass);
        tag.putFloat("inertia", inertia);
        tag.putBoolean("isStatic", isStatic);
        tag.putBoolean("isActive", isActive);
        
        ListTag blocksList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putIntArray("pos", new int[]{entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()});
            blockTag.putString("state", entry.getValue().toString());
            blocksList.add(blockTag);
        }
        tag.put("blocks", blocksList);
        
        return tag;
    }

    public static RigidBody load(CompoundTag tag) {
        RigidBodyId id = RigidBodyId.fromString(tag.getString("id"));
        RigidBody body = new RigidBody(id);
        body.mass = tag.getFloat("mass");
        body.inertia = tag.getFloat("inertia");
        body.isStatic = tag.getBoolean("isStatic");
        body.isActive = tag.getBoolean("isActive");
        
        // 方块加载略去，实际需要解析BlockState
        return body;
    }

    @Override
    public void close() {
        physicsBody.close();
        blocks.clear();
    }
}
