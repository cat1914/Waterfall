package com.waterfall.physics.rigidbody;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * 表示方块结构的刚体，包含一组方块及其相对位置
 */
public class RigidBody implements AutoCloseable {
    private final RigidBodyId id;
    private final Map<BlockPos, BlockState> blocks;
    private final PhysicsBody physicsBody;
    private float mass;
    private float inertia;
    private boolean isStatic;
    private boolean isActive;

    public RigidBody(RigidBodyId id) {
        this.id = id;
        this.blocks = new HashMap<>();
        this.physicsBody = new PhysicsBody(0, 0, 0, 1.0f);
        this.mass = 1.0f;
        this.inertia = 1.0f;
        this.isStatic = false;
        this.isActive = true;
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

    private void recalculateMassAndInertia() {
        float blockMass = 1.0f; // 假设每个方块质量为1
        mass = blocks.size() * blockMass;
        
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
