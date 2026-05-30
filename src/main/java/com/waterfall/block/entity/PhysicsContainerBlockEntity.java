package com.waterfall.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import com.waterfall.physics.rigidbody.RigidBodyManager;

/**
 * 物理实体容器方块实体 - 将一组方块绑定为刚体
 */
public class PhysicsContainerBlockEntity extends BlockEntity {
    private RigidBodyId rigidBodyId;
    private boolean isInitialized;
    
    public PhysicsContainerBlockEntity(BlockPos pos, BlockState state) {
        super(com.waterfall.block.PhysicsBlockEntities.PHYSICS_CONTAINER.get(), pos, state);
        this.isInitialized = false;
    }
    
    public RigidBodyId getRigidBodyId() {
        return rigidBodyId;
    }
    
    public RigidBody getRigidBody() {
        if (rigidBodyId == null) return null;
        return RigidBodyManager.getInstance().getRigidBody(rigidBodyId);
    }
    
    public void initialize() {
        if (level != null && !level.isClientSide && !isInitialized) {
            if (rigidBodyId == null) {
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    RigidBody body = RigidBodyManager.getInstance().createRigidBody(serverLevel);
                    this.rigidBodyId = body.getId();
                    
                    // 初始时绑定相邻方块
                    captureAdjacentBlocks(body);
                }
            }
            this.isInitialized = true;
            setChanged();
        }
    }
    
    private void captureAdjacentBlocks(RigidBody body) {
        // 简单的相邻方块捕获（实际中需要更复杂的递归扫描）
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    mutable.set(worldPosition.getX() + dx, worldPosition.getY() + dy, worldPosition.getZ() + dz);
                    BlockState state = level.getBlockState(mutable);
                    if (!state.isAir()) {
                        BlockPos localPos = new BlockPos(dx, dy, dz);
                        body.addBlock(localPos, state);
                    }
                }
            }
        }
    }
    
    public void activate() {
        RigidBody body = getRigidBody();
        if (body != null) {
            body.setActive(true);
            body.setStatic(false);
            setChanged();
        }
    }
    
    public void deactivate() {
        RigidBody body = getRigidBody();
        if (body != null) {
            body.setActive(false);
            body.setStatic(true);
            setChanged();
        }
    }
    
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(5); // 渲染范围稍大
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (rigidBodyId != null) {
            tag.putString("rigidBodyId", rigidBodyId.toString());
        }
        tag.putBoolean("isInitialized", isInitialized);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("rigidBodyId")) {
            this.rigidBodyId = RigidBodyId.fromString(tag.getString("rigidBodyId"));
        }
        this.isInitialized = tag.getBoolean("isInitialized");
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (rigidBodyId != null) {
            tag.putString("rigidBodyId", rigidBodyId.toString());
        }
        return tag;
    }
    
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            initialize();
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && rigidBodyId != null) {
            RigidBodyManager.getInstance().destroyRigidBody(rigidBodyId);
        }
    }
}
