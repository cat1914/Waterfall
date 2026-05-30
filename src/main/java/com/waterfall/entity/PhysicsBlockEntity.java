package com.waterfall.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.waterfall.WaterfallMod;
import com.waterfall.physics.MaterialPhysics;
import com.waterfall.physics.Vector3;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 物理方块实体：代表一组绑定在一起的物理化方块
 * 包含碰撞体积、渲染、玩家交互功能
 */
public class PhysicsBlockEntity extends Entity {
    public static final EntityDataAccessor<Boolean> DATA_IS_PHYSICS_ACTIVE = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_LIGHT_BLOCKS = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_HEAVY_BLOCKS = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    
    private RigidBodyId rigidBodyId;
    private final Map<BlockPos, BlockState> blockStates = new HashMap<>();
    private final Map<BlockPos, AABB> collisionBoxes = new HashMap<>();
    private AABB overallAABB = new AABB(0, 0, 0, 1, 1, 1);
    private Vec3 prevPos = Vec3.ZERO;
    private Vec3 prevMotion = Vec3.ZERO;
    private boolean isInitialized = false;
    
    public PhysicsBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }
    
    public PhysicsBlockEntity(Level level, RigidBody rigidBody, Vec3 pos) {
        this(PhysicsEntityType.PHYSICS_BLOCK.get(), level);
        this.rigidBodyId = rigidBody.getId();
        
        // 复制方块状态
        this.blockStates.putAll(rigidBody.getBlocks());
        
        // 计算碰撞体积
        calculateCollisionBoxes();
        
        // 设置位置
        this.setPos(pos);
        this.prevPos = pos;
        
        // 同步数据
        this.entityData.set(DATA_LIGHT_BLOCKS, rigidBody.getLightBlockCount());
        this.entityData.set(DATA_HEAVY_BLOCKS, rigidBody.getHeavyBlockCount());
        this.entityData.set(DATA_IS_PHYSICS_ACTIVE, rigidBody.isActive());
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IS_PHYSICS_ACTIVE, true);
        builder.define(DATA_LIGHT_BLOCKS, 0);
        builder.define(DATA_HEAVY_BLOCKS, 0);
    }
    
    /**
     * 计算所有方块的碰撞体积
     */
    private void calculateCollisionBoxes() {
        collisionBoxes.clear();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;
        
        for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
            BlockPos localPos = entry.getKey();
            BlockState state = entry.getValue();
            
            // 获取方块的碰撞形状
            VoxelShape shape = state.getCollisionShape(this.level, localPos);
            
            // 将形状转换为AABB并偏移到相对位置
            for (AABB aabb : shape.toAabbs()) {
                AABB offsetAABB = aabb.move(localPos.getX(), localPos.getY(), localPos.getZ());
                collisionBoxes.put(localPos, offsetAABB);
                
                // 更新整体边界
                minX = Math.min(minX, offsetAABB.minX);
                minY = Math.min(minY, offsetAABB.minY);
                minZ = Math.min(minZ, offsetAABB.minZ);
                maxX = Math.max(maxX, offsetAABB.maxX);
                maxY = Math.max(maxY, offsetAABB.maxY);
                maxZ = Math.max(maxZ, offsetAABB.maxZ);
            }
        }
        
        // 如果没有方块，使用默认尺寸
        if (collisionBoxes.isEmpty()) {
            overallAABB = new AABB(0, 0, 0, 1, 1, 1);
        } else {
            overallAABB = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        
        // 更新实体的边界框
        updateBoundingBox();
    }
    
    /**
     * 更新实体的碰撞边界
     */
    private void updateBoundingBox() {
        // 将整体边界框移到实体位置
        Vec3 pos = this.position();
        AABB movedAABB = overallAABB.move(pos.x, pos.y, pos.z);
        
        // 确保边界框不会太小
        if (movedAABB.getXsize() < 0.1) movedAABB = movedAABB.inflate(0.1, 0, 0);
        if (movedAABB.getYsize() < 0.1) movedAABB = movedAABB.inflate(0, 0.1, 0);
        if (movedAABB.getZsize() < 0.1) movedAABB = movedAABB.inflate(0, 0, 0.1);
        
        this.setBoundingBox(movedAABB);
    }
    
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(DATA_IS_PHYSICS_ACTIVE)) {
            // 物理状态改变时更新
            if (rigidBodyId != null && level instanceof ServerLevel serverLevel) {
                RigidBody body = RigidBodyManager.getInstance().getRigidBody(rigidBodyId);
                if (body != null) {
                    body.setActive(this.entityData.get(DATA_IS_PHYSICS_ACTIVE));
                }
            }
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 初始化物理
        if (!level.isClientSide && !isInitialized) {
            initializePhysics();
            isInitialized = true;
        }
        
        // 保存前一帧状态
        prevPos = this.position();
        prevMotion = this.getDeltaMovement();
        
        if (level.isClientSide) {
            tickClient();
        } else {
            tickServer();
        }
    }
    
    /**
     * 初始化物理系统
     */
    private void initializePhysics() {
        if (rigidBodyId == null && !blockStates.isEmpty()) {
            // 创建新的刚体
            if (level instanceof ServerLevel serverLevel) {
                RigidBody body = RigidBodyManager.getInstance().createRigidBody(serverLevel);
                this.rigidBodyId = body.getId();
                
                // 添加方块到刚体
                for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
                    body.addBlock(entry.getKey(), entry.getValue());
                }
                
                // 设置位置
                Vec3 pos = this.position();
                body.getPhysicsBody().setPosition(new Vector3((float)pos.x, (float)pos.y, (float)pos.z));
                
                // 同步数据
                this.entityData.set(DATA_LIGHT_BLOCKS, body.getLightBlockCount());
                this.entityData.set(DATA_HEAVY_BLOCKS, body.getHeavyBlockCount());
            }
        }
    }
    
    /**
     * 服务端tick
     */
    private void tickServer() {
        // 更新物理
        if (rigidBodyId != null && this.entityData.get(DATA_IS_PHYSICS_ACTIVE)) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(rigidBodyId);
            if (body != null && body.isActive()) {
                // 获取物理位置
                Vector3 physPos = body.getPhysicsBody().getPosition();
                Vec3 newPos = new Vec3(physPos.getX(), physPos.getY(), physPos.getZ());
                
                // 检查是否在水中
                boolean inWater = checkIfInWater(newPos);
                body.setUnderwater(inWater);
                
                // 如果在水中，应用浮力
                if (inWater) {
                    body.applyUnderwaterForces();
                }
                
                // 移动实体
                Vec3 currentPos = this.position();
                Vec3 movement = newPos.subtract(currentPos);
                this.moveNoCollide(movement.x, movement.y, movement.z);
                
                // 同步位置
                this.setPos(newPos.x, newPos.y, newPos.z);
            }
        }
        
        // 基础物理（未激活时使用）
        if (!this.entityData.get(DATA_IS_PHYSICS_ACTIVE)) {
            applyGravity();
            checkInsideBlocks();
        }
        
        // 更新边界框
        updateBoundingBox();
    }
    
    /**
     * 客户端tick
     */
    private void tickClient() {
        // 客户端预测和渲染
        updateBoundingBox();
    }
    
    /**
     * 检查是否在水中
     */
    private boolean checkIfInWater(Vec3 pos) {
        BlockPos blockPos = new BlockPos((int)pos.x, (int)pos.y, (int)pos.z);
        return level.getFluidState(blockPos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER) ||
               level.getFluidState(blockPos).isSourceOfType(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
    }
    
    /**
     * 应用重力（未激活物理时）
     */
    private void applyGravity() {
        Vec3 motion = this.getDeltaMovement();
        double y = motion.y - 0.08; // 标准重力
        this.setDeltaMovement(motion.x * 0.98, y * 0.98, motion.z * 0.98);
    }
    
    @Override
    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith() && !isRemoved();
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    @Override
    public PushReaction getPushReaction() {
        return PushReaction.NORMAL;
    }
    
    /**
     * 玩家右键交互
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            // 右键切换物理状态
            boolean current = this.entityData.get(DATA_IS_PHYSICS_ACTIVE);
            this.entityData.set(DATA_IS_PHYSICS_ACTIVE, !current);
            
            player.displayClientMessage(
                Component.literal(current ? "Physics Disabled" : "Physics Enabled"), 
                true);
            
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
    
    /**
     * 玩家左键碰撞检测
     */
    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return false; // 允许攻击
    }
    
    /**
     * 获取方块的碰撞体积（用于精确碰撞）
     */
    public List<AABB> getBlockCollisionBoxes() {
        List<AABB> result = new ArrayList<>();
        Vec3 pos = this.position();
        
        for (AABB localBox : collisionBoxes.values()) {
            result.add(localBox.move(pos.x, pos.y, pos.z));
        }
        
        return result;
    }
    
    /**
     * 获取指定局部位置的方块状态
     */
    public BlockState getBlockState(BlockPos localPos) {
        return blockStates.getOrDefault(localPos, Blocks.AIR.defaultBlockState());
    }
    
    /**
     * 获取所有方块状态
     */
    public Map<BlockPos, BlockState> getAllBlockStates() {
        return new HashMap<>(blockStates);
    }
    
    /**
     * 设置刚体ID
     */
    public void setRigidBodyId(RigidBodyId id) {
        this.rigidBodyId = id;
    }
    
    public RigidBodyId getRigidBodyId() {
        return rigidBodyId;
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // 保存方块状态
        ListTag blocksList = new ListTag();
        for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putIntArray("pos", new int[]{
                entry.getKey().getX(), 
                entry.getKey().getY(), 
                entry.getKey().getZ()});
            blockTag.putString("state", entry.getValue().toString());
            blocksList.add(blockTag);
        }
        tag.put("blocks", blocksList);
        
        // 保存刚体ID
        if (rigidBodyId != null) {
            tag.putString("rigidBodyId", rigidBodyId.toString());
        }
        
        // 保存其他数据
        tag.putBoolean("physicsActive", this.entityData.get(DATA_IS_PHYSICS_ACTIVE));
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // 读取方块状态
        blockStates.clear();
        if (tag.contains("blocks")) {
            ListTag blocksList = tag.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < blocksList.size(); i++) {
                CompoundTag blockTag = blocksList.getCompound(i);
                int[] posArr = blockTag.getIntArray("pos");
                BlockPos pos = new BlockPos(posArr[0], posArr[1], posArr[2]);
                
                // 简单的BlockState解析（实际需要更完善的解析）
                String stateStr = blockTag.getString("state");
                BlockState state = parseBlockState(stateStr);
                if (state != null) {
                    blockStates.put(pos, state);
                }
            }
        }
        
        // 读取刚体ID
        if (tag.contains("rigidBodyId")) {
            this.rigidBodyId = RigidBodyId.fromString(tag.getString("rigidBodyId"));
        }
        
        // 读取其他数据
        if (tag.contains("physicsActive")) {
            this.entityData.set(DATA_IS_PHYSICS_ACTIVE, tag.getBoolean("physicsActive"));
        }
        
        // 重新计算碰撞体积
        calculateCollisionBoxes();
    }
    
    /**
     * 简单的BlockState解析（实际需要更完善的实现）
     */
    private BlockState parseBlockState(String str) {
        // 简化实现，实际需要根据方块名解析
        return Blocks.STONE.defaultBlockState();
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean mayInteract(Player player, Vec3 pos) {
        return true;
    }
}
