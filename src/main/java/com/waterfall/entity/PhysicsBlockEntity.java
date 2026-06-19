package com.waterfall.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.waterfall.WaterfallMod;
import com.waterfall.dimension.InteractionMapper;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.physics.rigidbody.RigidBody;
import com.waterfall.physics.rigidbody.RigidBodyId;
import com.waterfall.physics.rigidbody.RigidBodyManager;
import com.waterfall.physics.rotation.RotationalBodyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 物理方块代理实体（Physics Block Proxy Entity）
 * 
 * 基于物理维度映射系统的重构：
 * 1. 实际方块存储在"物理维度"中
 * 2. 主世界的此实体是一个"代理/显示层"
 * 3. 玩家交互通过 InteractionMapper 映射到物理维度
 * 4. 物理计算在物理维度中执行
 * 
 * 这个实体负责：
 * - 显示物理化结构给玩家（碰撞箱、视觉）
 * - 将玩家交互转发到物理维度中的方块
 * - 从物理维度的刚体同步位置到主世界
 */
public class PhysicsBlockEntity extends Entity {
    
    // ============ 同步数据 ============
    public static final EntityDataAccessor<Boolean> DATA_IS_PHYSICS_ACTIVE = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_LIGHT_BLOCKS = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_HEAVY_BLOCKS = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_STRUCTURE_ID = 
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    
    // ============ 物理相关 ============
    private RigidBodyId rigidBodyId;
    private UUID rotationalBodyId;
    private long structureId;
    
    // 物理维度中的结构原点
    private BlockPos physicsOrigin;
    
    // 局部方块位置集合（用于碰撞和渲染）
    private Set<BlockPos> localBlockPositions = new HashSet<>();
    
    // 缓存的方块状态（客户端渲染用）
    private Map<BlockPos, BlockState> cachedBlockStates = new HashMap<>();
    
    // 局部位置 -> 主世界原始位置（用于恢复方块）
    private Map<BlockPos, BlockPos> localToWorldMap = new HashMap<>();
    
    // 碰撞相关
    private AABB overallAABB = new AABB(0, 0, 0, 1, 1, 1);
    private Vec3 prevPos = Vec3.ZERO;
    private boolean isInitialized = false;
    
    // ============ 构造 ============
    
    public PhysicsBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }
    
    /**
     * 使用已复制到物理维度的结构数据初始化
     */
    public void initializeFromPhysicsDimension(long structId, BlockPos origin,
                                               Set<BlockPos> localPositions,
                                               Map<BlockPos, BlockPos> localWorldMap,
                                               RigidBodyId bodyId,
                                               int lightBlocks, int heavyBlocks) {
        this.structureId = structId;
        this.physicsOrigin = origin;
        this.localBlockPositions = localPositions;
        this.localToWorldMap = localWorldMap;
        this.rigidBodyId = bodyId;
        
        this.getEntityData().set(DATA_STRUCTURE_ID, (int) structId);
        this.getEntityData().set(DATA_LIGHT_BLOCKS, lightBlocks);
        this.getEntityData().set(DATA_HEAVY_BLOCKS, heavyBlocks);
        this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, true);
        
        calculateCollisionBoxes();
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IS_PHYSICS_ACTIVE, true);
        builder.define(DATA_LIGHT_BLOCKS, 0);
        builder.define(DATA_HEAVY_BLOCKS, 0);
        builder.define(DATA_STRUCTURE_ID, 0);
    }
    
    // ============ 碰撞计算 ============
    
    private void calculateCollisionBoxes() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        
        for (BlockPos localPos : localBlockPositions) {
            minX = Math.min(minX, localPos.getX());
            minY = Math.min(minY, localPos.getY());
            minZ = Math.min(minZ, localPos.getZ());
            maxX = Math.max(maxX, localPos.getX() + 1);
            maxY = Math.max(maxY, localPos.getY() + 1);
            maxZ = Math.max(maxZ, localPos.getZ() + 1);
        }
        
        if (localBlockPositions.isEmpty()) {
            overallAABB = new AABB(0, 0, 0, 1, 1, 1);
        } else {
            overallAABB = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        
        updateBoundingBox();
    }
    
    private void updateBoundingBox() {
        Vec3 pos = this.position();
        AABB movedAABB = overallAABB.move(pos.x, pos.y, pos.z);
        if (movedAABB.getXsize() < 0.1) movedAABB = movedAABB.inflate(0.1, 0, 0);
        if (movedAABB.getYsize() < 0.1) movedAABB = movedAABB.inflate(0, 0.1, 0);
        if (movedAABB.getZsize() < 0.1) movedAABB = movedAABB.inflate(0, 0, 0.1);
        this.setBoundingBox(movedAABB);
    }
    
    // ============ 射线检测（点击到的局部方块） ============
    
    public BlockPos findClickedBlockPos(Player player, float partialTicks) {
        Vec3 eyePosition = player.getEyePosition(partialTicks);
        Vec3 lookVector = player.getViewVector(partialTicks);
        double distance = player.blockInteractionRange();
        Vec3 endPosition = eyePosition.add(lookVector.scale(distance));
        
        Vec3 entityPos = this.position();
        double closestDistance = Double.MAX_VALUE;
        BlockPos closestPos = null;
        
        for (BlockPos localPos : localBlockPositions) {
            AABB box = new AABB(
                localPos.getX(), localPos.getY(), localPos.getZ(),
                localPos.getX() + 1, localPos.getY() + 1, localPos.getZ() + 1
            ).move(entityPos.x, entityPos.y, entityPos.z);
            
            java.util.Optional<Vec3> hitOpt = box.clip(eyePosition, endPosition);
            if (hitOpt.isPresent()) {
                double dist = eyePosition.distanceTo(hitOpt.get());
                if (dist < closestDistance) {
                    closestDistance = dist;
                    closestPos = localPos;
                }
            }
        }
        
        return closestPos;
    }
    
    // ============ Tick 更新 ============
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide && !isInitialized) {
            isInitialized = true;
            // 向交互映射系统注册此实体
            InteractionMapper.registerMapping(this.getUUID(), physicsOrigin, localToWorldMap);
        }
        
        prevPos = this.position();
        
        if (this.level().isClientSide) {
            tickClient();
        } else {
            tickServer();
        }
    }
    
    private void tickClient() {
        updateBoundingBox();
    }
    
    private void tickServer() {
        // 从物理维度的刚体同步位置
        if (rigidBodyId != null && this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE)) {
            RigidBody body = RigidBodyManager.getInstance().getRigidBody(rigidBodyId);
            if (body != null && body.isActive()) {
                com.waterfall.physics.Vector3 physPos = body.getPhysicsBody().getPosition();
                Vec3 newPos = new Vec3(physPos.getX(), physPos.getY(), physPos.getZ());
                
                // 检查水中状态（通过物理维度检查）
                ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
                if (physicsLevel != null) {
                    boolean inWater = checkIfInWater(physicsLevel, newPos);
                    body.setUnderwater(inWater);
                    if (inWater) {
                        body.applyUnderwaterForces();
                    }
                }
                
                this.setPos(newPos.x, newPos.y, newPos.z);
            }
        }
        
        // 基础重力（未激活物理时）
        if (!this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE)) {
            applyGravity();
        }
        
        updateBoundingBox();
    }
    
    private boolean checkIfInWater(ServerLevel physicsLevel, Vec3 pos) {
        BlockPos blockPos = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z);
        return physicsLevel.getFluidState(blockPos).isSourceOfType(net.minecraft.world.level.material.Fluids.WATER)
            || physicsLevel.getFluidState(blockPos).isSourceOfType(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
    }
    
    @Override
    protected void applyGravity() {
        Vec3 motion = this.getDeltaMovement();
        double y = motion.y - 0.08;
        this.setDeltaMovement(motion.x * 0.98, y * 0.98, motion.z * 0.98);
    }
    
    // ============ 交互处理（映射到物理维度） ============
    
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.CONSUME;
        }
        
        // 查找点击的具体方块
        BlockPos clickedLocalPos = findClickedBlockPos(player, 1.0f);
        
        if (clickedLocalPos != null) {
            // 获取物理维度并转发交互
            ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
            if (physicsLevel != null) {
                Vec3 hitPos = player.position();
                InteractionResult result = InteractionMapper.handleRightClick(
                    this.getUUID(), player, hand, clickedLocalPos, hitPos, physicsLevel
                );
                
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        }
        
        // 未点击到可交互方块，切换物理状态
        boolean current = this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE);
        this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, !current);
        player.displayClientMessage(
            Component.literal(current ? "Physics Disabled" : "Physics Enabled"),
            true
        );
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return false;
    }
    
    // ============ Misc ============
    
    public boolean canCollideWith(Entity entity) {
        return entity.canBeCollidedWith() && !isRemoved();
    }
    
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    public PushReaction getPushReaction() {
        return PushReaction.NORMAL;
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    // ============ Getters ============
    
    public RigidBodyId getRigidBodyId() {
        return rigidBodyId;
    }
    
    public void setRigidBodyId(RigidBodyId id) {
        this.rigidBodyId = id;
    }
    
    public UUID getRotationalBodyId() {
        return rotationalBodyId;
    }
    
    public void setRotationalBodyId(UUID id) {
        this.rotationalBodyId = id;
    }
    
    public long getStructureId() {
        return structureId;
    }
    
    public BlockPos getPhysicsOrigin() {
        return physicsOrigin;
    }
    
    public Set<BlockPos> getLocalBlockPositions() {
        return localBlockPositions;
    }
    
    public Map<BlockPos, BlockPos> getLocalToWorldMap() {
        return localToWorldMap;
    }
    
    /**
     * 从物理维度获取当前的方块状态（用于渲染）
     */
    public BlockState getBlockState(BlockPos localPos) {
        if (this.level().isClientSide) {
            // 客户端：使用缓存
            return cachedBlockStates.getOrDefault(localPos, Blocks.STONE.defaultBlockState());
        } else {
            // 服务端：从物理维度读取
            ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
            if (physicsLevel != null && physicsOrigin != null) {
                return physicsLevel.getBlockState(physicsOrigin.offset(localPos));
            }
            return Blocks.AIR.defaultBlockState();
        }
    }
    
    /**
     * 获取所有方块状态（用于结构恢复）
     */
    public Map<BlockPos, BlockState> getAllBlockStatesFromPhysics() {
        Map<BlockPos, BlockState> result = new HashMap<>();
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
        if (physicsLevel == null || physicsOrigin == null) return result;
        
        for (BlockPos localPos : localBlockPositions) {
            BlockPos physicsPos = physicsOrigin.offset(localPos);
            BlockState state = physicsLevel.getBlockState(physicsPos);
            if (!state.isAir()) {
                result.put(localPos, state);
            }
        }
        return result;
    }
    
    // ============ NBT 保存/加载 ============
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putLong("structureId", structureId);
        
        if (physicsOrigin != null) {
            tag.putInt("originX", physicsOrigin.getX());
            tag.putInt("originY", physicsOrigin.getY());
            tag.putInt("originZ", physicsOrigin.getZ());
        }
        
        // 保存局部方块位置
        ListTag positionsTag = new ListTag();
        for (BlockPos pos : localBlockPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            positionsTag.add(posTag);
        }
        tag.put("localPositions", positionsTag);
        
        // 保存局部->主世界位置映射
        ListTag worldMapTag = new ListTag();
        for (Map.Entry<BlockPos, BlockPos> entry : localToWorldMap.entrySet()) {
            CompoundTag mapTag = new CompoundTag();
            mapTag.putIntArray("local", new int[]{entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()});
            mapTag.putIntArray("world", new int[]{entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ()});
            worldMapTag.add(mapTag);
        }
        tag.put("localToWorldMap", worldMapTag);
        
        if (rigidBodyId != null) {
            tag.putString("rigidBodyId", rigidBodyId.toString());
        }
        
        tag.putBoolean("physicsActive", this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE));
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.structureId = tag.getLong("structureId");
        
        if (tag.contains("originX")) {
            this.physicsOrigin = new BlockPos(
                tag.getInt("originX"), tag.getInt("originY"), tag.getInt("originZ")
            );
        }
        
        // 读取局部方块位置
        localBlockPositions.clear();
        if (tag.contains("localPositions")) {
            ListTag positionsTag = tag.getList("localPositions", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < positionsTag.size(); i++) {
                CompoundTag posTag = positionsTag.getCompound(i);
                localBlockPositions.add(new BlockPos(
                    posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")
                ));
            }
        }
        
        // 读取局部->主世界位置映射
        localToWorldMap.clear();
        if (tag.contains("localToWorldMap")) {
            ListTag worldMapTag = tag.getList("localToWorldMap", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < worldMapTag.size(); i++) {
                CompoundTag mapTag = worldMapTag.getCompound(i);
                int[] localArr = mapTag.getIntArray("local");
                int[] worldArr = mapTag.getIntArray("world");
                localToWorldMap.put(
                    new BlockPos(localArr[0], localArr[1], localArr[2]),
                    new BlockPos(worldArr[0], worldArr[1], worldArr[2])
                );
            }
        }
        
        if (tag.contains("rigidBodyId")) {
            this.rigidBodyId = RigidBodyId.fromString(tag.getString("rigidBodyId"));
        }
        
        if (tag.contains("physicsActive")) {
            this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, tag.getBoolean("physicsActive"));
        }
        
        calculateCollisionBoxes();
    }
    
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // 清理映射
        if (!this.level().isClientSide) {
            InteractionMapper.removeMapping(this.getUUID());
        }
    }
    
    public List<AABB> getBlockCollisionBoxes() {
        List<AABB> result = new ArrayList<>();
        Vec3 pos = this.position();
        for (BlockPos localPos : localBlockPositions) {
            result.add(new AABB(
                pos.x + localPos.getX(), pos.y + localPos.getY(), pos.z + localPos.getZ(),
                pos.x + localPos.getX() + 1, pos.y + localPos.getY() + 1, pos.z + localPos.getZ() + 1
            ));
        }
        return result;
    }
}
