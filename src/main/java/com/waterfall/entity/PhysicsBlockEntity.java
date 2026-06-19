package com.waterfall.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.waterfall.WaterfallMod;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.physics.MaterialPhysics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 物理方块代理实体（主世界）
 *
 * 责任分工：
 * - 主世界：做真实物理（重力、碰撞、水中检测、浮力）、渲染、玩家交互
 * - 物理维度：存原版方块，提供交互映射的数据源（不做任何物理计算）
 *
 * 工作流程：
 * 1. tick 时自己算重力/浮力/碰撞（主世界）
 * 2. 渲染时从物理维度取每个局部方块的 BlockState
 * 3. 玩家交互时把局部坐标映射到物理维度坐标，调用原版 block.use / destroyBlock
 */
public class PhysicsBlockEntity extends Entity {

    // ============ 同步数据 ============
    public static final EntityDataAccessor<Boolean> DATA_IS_PHYSICS_ACTIVE =
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_LIGHT_BLOCKS =
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_HEAVY_BLOCKS =
        SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.INT);

    // ============ 结构数据 ============
    // 物理维度中结构原点（用来映射局部坐标 -> 物理维度坐标）
    private BlockPos physicsOrigin;

    // 所有局部方块位置集合（用于渲染和碰撞计算）
    private final Set<BlockPos> localBlockPositions = new HashSet<>();

    // 局部位置 -> 主世界原位置（用于结构销毁时还原方块）
    private final Map<BlockPos, BlockPos> localToWorldMap = new HashMap<>();

    // 客户端方块状态缓存（避免频繁查服务端维度）
    private final Map<BlockPos, BlockState> clientStateCache = new HashMap<>();

    // 碰撞相关
    private AABB overallAABB = new AABB(0, 0, 0, 1, 1, 1);
    private boolean isInitialized = false;

    // 物理属性（在创建时由轻/重方块数量计算得出）
    private float buoyancyForce = 0.0f;       // 净浮力系数
    private boolean physicsActive = true;

    // 旋转刚体ID（可选，物理仍在主世界做）
    private UUID rotationalBodyId;

    public PhysicsBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    // ============ 初始化 ============

    /**
     * 从物理维度结构初始化代理实体
     */
    public void initializeFromPhysicsDimension(BlockPos origin,
                                                Set<BlockPos> localPositions,
                                                Map<BlockPos, BlockPos> localWorldMap,
                                                int lightBlocks, int heavyBlocks) {
        this.physicsOrigin = origin;
        this.localBlockPositions.clear();
        this.localBlockPositions.addAll(localPositions);
        this.localToWorldMap.clear();
        this.localToWorldMap.putAll(localWorldMap);

        // 预计算属性
        this.getEntityData().set(DATA_LIGHT_BLOCKS, lightBlocks);
        this.getEntityData().set(DATA_HEAVY_BLOCKS, heavyBlocks);
        this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, true);

        // 净浮力：轻质升力 - 重质重力（4个轻质 = 1个重质）
        this.buoyancyForce = (lightBlocks * PhysicsConfig.LIGHT_BLOCK_BUOYANCY)
                           - (heavyBlocks * PhysicsConfig.HEAVY_BLOCK_WEIGHT * 0.25f);

        calculateOverallAABB();
        updateBoundingBox();
    }

    private void calculateOverallAABB() {
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
    }

    private void updateBoundingBox() {
        Vec3 pos = this.position();
        AABB moved = overallAABB.move(pos.x, pos.y, pos.z);
        if (moved.getXsize() < 0.1) moved = moved.inflate(0.1, 0, 0);
        if (moved.getYsize() < 0.1) moved = moved.inflate(0, 0.1, 0);
        if (moved.getZsize() < 0.1) moved = moved.inflate(0, 0, 0.1);
        this.setBoundingBox(moved);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IS_PHYSICS_ACTIVE, true);
        builder.define(DATA_LIGHT_BLOCKS, 0);
        builder.define(DATA_HEAVY_BLOCKS, 0);
    }

    // ============ Tick：主世界物理 ============

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && !isInitialized) {
            isInitialized = true;
            WaterfallMod.LOGGER.debug("PhysicsBlockEntity initialized at {}", this.position());
        }

        if (this.level().isClientSide) {
            // 客户端：只更新边界框以便渲染
            updateBoundingBox();
            return;
        }

        // ===== 服务端：做真实物理 =====
        physicsActive = this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE);

        if (physicsActive) {
            doActivePhysics();
        } else {
            // 未激活：只做标准重力
            applyGravity();
        }

        // 更新边界框（用于碰撞）
        updateBoundingBox();
    }

    /**
     * 主世界做的物理：
     * 1. 检查是否在水中（查主世界流体）
     * 2. 应用重力或浮力
     * 3. 移动并处理 Minecraft 原生碰撞
     */
    private void doActivePhysics() {
        Vec3 motion = this.getDeltaMovement();
        double dx = motion.x;
        double dy = motion.y;
        double dz = motion.z;

        // 1) 检查是否在水中（查主世界流体）
        boolean inWater = checkIfInWater();

        if (inWater) {
            // 在水中：应用净浮力（正值上浮，负值下沉）
            // 使用轻/重方块计数预先算好的浮力系数
            float forcePerUnit = 0.01f;
            dy += buoyancyForce * forcePerUnit;
            // 水中阻力
            dx *= 0.92;
            dy *= 0.92;
            dz *= 0.92;
        } else {
            // 不在水中：标准重力
            dy -= 0.08; // 重力
            // 空气阻力
            dx *= 0.98;
            dy *= 0.98;
            dz *= 0.98;
        }

        // 限制垂直速度
        dy = Mth.clamp(dy, -2.0, 2.0);

        this.setDeltaMovement(dx, dy, dz);

        // 2) 使用 Minecraft 原生移动 + 碰撞（move() 会处理地形碰撞）
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        // 3) 碰撞后阻尼（落地衰减）
        if (this.onGround()) {
            Vec3 newMotion = this.getDeltaMovement();
            this.setDeltaMovement(newMotion.x * 0.7, newMotion.y, newMotion.z * 0.7);
        }
    }

    /**
     * 在主世界检查实体整体是否接触水
     */
    private boolean checkIfInWater() {
        Vec3 pos = this.position();
        // 扫一遍结构内部/下方的方块位置，看是否接触水
        BlockPos centerPos = new BlockPos(
            (int) Math.floor(pos.x + (overallAABB.minX + overallAABB.maxX) / 2.0),
            (int) Math.floor(pos.y + overallAABB.minY),
            (int) Math.floor(pos.z + (overallAABB.minZ + overallAABB.maxZ) / 2.0)
        );

        // 检查几个关键点是否在水中
        BlockPos[] probes = new BlockPos[]{
            centerPos,
            centerPos.above(),
            centerPos.below(),
            centerPos.north(),
            centerPos.south(),
            centerPos.east(),
            centerPos.west()
        };

        for (BlockPos p : probes) {
            if (this.level().getFluidState(p).is(Fluids.WATER)
                || this.level().getFluidState(p).is(Fluids.FLOWING_WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void applyGravity() {
        Vec3 motion = this.getDeltaMovement();
        double y = motion.y - 0.08;
        this.setDeltaMovement(motion.x * 0.98, y * 0.98, motion.z * 0.98);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
    }

    // ============ 交互：映射到物理维度 ============

    /**
     * 查找玩家点击命中的局部方块位置
     */
    public BlockPos findClickedBlockPos(Player player, float partialTicks) {
        Vec3 eyePos = player.getEyePosition(partialTicks);
        Vec3 lookVec = player.getViewVector(partialTicks);
        double distance = player.blockInteractionRange();
        Vec3 endPos = eyePos.add(lookVec.scale(distance));
        Vec3 entityPos = this.position();

        double closestDist = Double.MAX_VALUE;
        BlockPos closestPos = null;

        for (BlockPos localPos : localBlockPositions) {
            AABB box = new AABB(
                localPos.getX(), localPos.getY(), localPos.getZ(),
                localPos.getX() + 1, localPos.getY() + 1, localPos.getZ() + 1
            ).move(entityPos.x, entityPos.y, entityPos.z);

            java.util.Optional<Vec3> hit = box.clip(eyePos, endPos);
            if (hit.isPresent()) {
                double d = eyePos.distanceTo(hit.get());
                if (d < closestDist) {
                    closestDist = d;
                    closestPos = localPos;
                }
            }
        }
        return closestPos;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.CONSUME;
        }

        // 找到点击的具体方块
        BlockPos clickedLocal = findClickedBlockPos(player, 1.0f);

        if (clickedLocal == null) {
            // 没点到方块：切换物理激活状态
            togglePhysicsState(player);
            return InteractionResult.SUCCESS;
        }

        // 从物理维度查当前方块状态，触发原版交互
        ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
        if (physicsLevel == null || physicsOrigin == null) {
            WaterfallMod.LOGGER.warn("Physics dimension not available for interaction");
            return InteractionResult.PASS;
        }

        BlockPos physicsPos = physicsOrigin.offset(clickedLocal);
        BlockState state = physicsLevel.getBlockState(physicsPos);

        if (state.isAir()) {
            return InteractionResult.PASS;
        }

        // 使用原版 BlockState.use 触发交互（箱子、拉杆、门、按钮…）
        // 玩家位置投射到物理维度的相对坐标进行交互
        Vec3 relativeHit = new Vec3(
            physicsPos.getX() + 0.5,
            physicsPos.getY() + 0.5,
            physicsPos.getZ() + 0.5
        );
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            relativeHit,
            net.minecraft.core.Direction.UP,
            physicsPos,
            false
        );

        // 使用 BlockState#use 在物理维度执行原版方块交互
        InteractionResult result = state.use(
            physicsLevel,
            player,
            hand,
            hit
        );

        if (result == InteractionResult.PASS) {
            togglePhysicsState(player);
            return InteractionResult.SUCCESS;
        }
        return result;
    }

    private void togglePhysicsState(Player player) {
        boolean current = this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE);
        this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, !current);
        player.displayClientMessage(
            Component.literal(current ? "Physics Disabled" : "Physics Enabled"),
            true
        );
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

    // ============ Getters / 状态查询 ============

    public BlockPos getPhysicsOrigin() {
        return physicsOrigin;
    }

    public Set<BlockPos> getLocalBlockPositions() {
        return localBlockPositions;
    }

    public Map<BlockPos, BlockPos> getLocalToWorldMap() {
        return localToWorldMap;
    }

    public UUID getRotationalBodyId() {
        return rotationalBodyId;
    }

    public void setRotationalBodyId(UUID id) {
        this.rotationalBodyId = id;
    }

    public int getLightBlockCount() {
        return this.getEntityData().get(DATA_LIGHT_BLOCKS);
    }

    public int getHeavyBlockCount() {
        return this.getEntityData().get(DATA_HEAVY_BLOCKS);
    }

    public float getNetBuoyancy() {
        return buoyancyForce;
    }

    /**
     * 对外施加冲量
     */
    public void applyImpulse(Vec3 force) {
        this.setDeltaMovement(this.getDeltaMovement().add(force));
    }

    /**
     * 获取指定局部位置的方块状态：
     * - 客户端：用本地缓存（服务端同步过来）
     * - 服务端：直接查询物理维度
     */
    public BlockState getBlockState(BlockPos localPos) {
        if (this.level().isClientSide) {
            return clientStateCache.getOrDefault(localPos, Blocks.STONE.defaultBlockState());
        } else {
            ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
            if (physicsLevel != null && physicsOrigin != null) {
                return physicsLevel.getBlockState(physicsOrigin.offset(localPos));
            }
            return Blocks.AIR.defaultBlockState();
        }
    }

    /**
     * 从物理维度收集所有方块状态（用于销毁时还原到主世界）
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

    /**
     * 更新客户端缓存（由网络同步调用）
     */
    public void updateClientBlockStates(Map<BlockPos, BlockState> states) {
        if (this.level().isClientSide) {
            clientStateCache.clear();
            clientStateCache.putAll(states);
        }
    }

    // ============ NBT ============

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (physicsOrigin != null) {
            tag.putInt("originX", physicsOrigin.getX());
            tag.putInt("originY", physicsOrigin.getY());
            tag.putInt("originZ", physicsOrigin.getZ());
        }

        // 局部位置
        ListTag positionsTag = new ListTag();
        for (BlockPos p : localBlockPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", p.getX());
            posTag.putInt("y", p.getY());
            posTag.putInt("z", p.getZ());
            positionsTag.add(posTag);
        }
        tag.put("localPositions", positionsTag);

        // 局部 -> 主世界原位置映射
        ListTag worldMapTag = new ListTag();
        for (Map.Entry<BlockPos, BlockPos> entry : localToWorldMap.entrySet()) {
            CompoundTag mapTag = new CompoundTag();
            mapTag.putIntArray("local", new int[]{entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()});
            mapTag.putIntArray("world", new int[]{entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ()});
            worldMapTag.add(mapTag);
        }
        tag.put("localToWorldMap", worldMapTag);

        tag.putBoolean("physicsActive", this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE));
        tag.putFloat("buoyancyForce", buoyancyForce);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("originX")) {
            this.physicsOrigin = new BlockPos(
                tag.getInt("originX"), tag.getInt("originY"), tag.getInt("originZ")
            );
        }

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

        if (tag.contains("physicsActive")) {
            this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, tag.getBoolean("physicsActive"));
        }
        this.buoyancyForce = tag.getFloat("buoyancyForce");

        calculateOverallAABB();
        updateBoundingBox();
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
