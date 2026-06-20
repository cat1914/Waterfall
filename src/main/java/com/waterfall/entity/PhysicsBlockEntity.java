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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

import com.waterfall.WaterfallMod;
import com.waterfall.config.PhysicsConfig;
import com.waterfall.dimension.PhysicsDimension;
import com.waterfall.physics.Force;
import com.waterfall.physics.MaterialPhysics;
import com.waterfall.physics.PhysicsBody;
import com.waterfall.physics.Vector3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 物理方块代理实体（主世界）
 *
 * 角色分工：
 * - 物理计算：100% 委托给 heavy 原生库（通过 PhysicsBody / Force 调用）
 * - 实体本身：只做显示、交互、与主世界的碰撞盒（Minecraft 原生）
 * - 物理维度（waterfall:physics_dimension）：存放原版方块，提供交互映射
 *
 * 每 tick 的工作流程（全部由 heavy 完成）：
 *   1. 读取主世界流体状态，判断是否在水下
 *   2. 根据材质分类，配置 heavy 的 Force 对象（gravity / lift / thrust）
 *   3. 调用 heavy_PhysicsBody_update(deltaTime) 推进一帧
 *   4. 从 heavy body 取回新位置同步给 Entity
 *   5. 调用 Entity.move(MoverType.SELF, delta) 做地形/实体碰撞
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
    private BlockPos physicsOrigin;
    private final Set<BlockPos> localBlockPositions = new HashSet<>();
    private final Map<BlockPos, BlockPos> localToWorldMap = new HashMap<>();
    private final Map<BlockPos, BlockState> clientStateCache = new HashMap<>();

    // ============ heavy 原生库的物理对象 ============
    private PhysicsBody heavyBody;     // 对应 heavy_PhysicsBody
    private Force heavyForce;          // 对应 heavy_Force（每 tick 重建一次）
    private float lastTickMillis = 50f;
    private boolean physicsActive = true;
    private UUID rotationalBodyId;

    // 轻质/重质计数（heavy 不会直接关心方块材质）
    private int lightBlockCount = 0;
    private int heavyBlockCount = 0;
    private float totalMass = 1.0f;

    public PhysicsBlockEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
        // 客户端也创建一个 PhysicsBody（主要由服务端驱动）
        if (level.isClientSide()) {
            this.heavyBody = new PhysicsBody(0, 0, 0, 1.0f);
            this.heavyForce = new Force();
        }
    }

    // ============ 初始化 ============

    /**
     * 从方块结构初始化代理实体 + 创建 heavy body
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

        this.lightBlockCount = lightBlocks;
        this.heavyBlockCount = heavyBlocks;
        this.getEntityData().set(DATA_LIGHT_BLOCKS, lightBlocks);
        this.getEntityData().set(DATA_HEAVY_BLOCKS, heavyBlocks);
        this.getEntityData().set(DATA_IS_PHYSICS_ACTIVE, true);

        // 结构质量 = 轻质数量*轻质系数 + 重质数量*重质系数
        this.totalMass = Math.max(1.0f,
            lightBlocks * PhysicsConfig.LIGHT_BLOCK_BUOYANCY +
            heavyBlocks * PhysicsConfig.HEAVY_BLOCK_WEIGHT * 0.25f);

        // 交给 heavy：创建 physics body，放到当前实体位置
        Vec3 pos = this.position();
        if (heavyBody != null) {
            heavyBody.close();
        }
        if (heavyForce != null) {
            heavyForce.close();
        }
        this.heavyBody = new PhysicsBody((float) pos.x, (float) pos.y, (float) pos.z, totalMass);
        this.heavyForce = new Force();

        // 加入全局的 heavy PhysicsWorld（由 WaterfallMod 管理）
        WaterfallMod.addPhysicsBodyToWorld(heavyBody);
    }

    // ============ Tick（服务端）：heavy 物理计算 ============

    @Override
    public void tick() {
        super.tick();

        // 客户端：不做 heavy 计算，只跟随实体位置由服务端同步
        if (this.level().isClientSide()) return;

        if (heavyBody == null) return;

        physicsActive = this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE);

        // --- 1. 决定这一帧的力（交给 heavy） ---
        boolean inWater = checkIfInWater();
        configureHeavyForce(inWater);

        // --- 2. 调用 heavy 推进物理 ---
        float dt = lastTickMillis / 1000.0f; // 50ms = 0.05s
        heavyBody.applyForce(heavyForce.calculateNetForce());

        if (physicsActive) {
            heavyBody.update(dt);
        }

        // --- 3. 从 heavy body 读回新位置，移动 MC 实体做碰撞 ---
        Vector3 heavyPos = heavyBody.getPosition();
        Vec3 currentPos = this.position();
        Vec3 delta = new Vec3(
            heavyPos.getX() - currentPos.x,
            heavyPos.getY() - currentPos.y,
            heavyPos.getZ() - currentPos.z
        );

        this.move(net.minecraft.world.entity.MoverType.SELF, delta);

        // 如果被 Minecraft 碰撞卡住，同步回 heavy body 避免漂移
        Vec3 newPos = this.position();
        if (Math.abs(newPos.x - heavyPos.getX()) > 0.01 ||
            Math.abs(newPos.y - heavyPos.getY()) > 0.01 ||
            Math.abs(newPos.z - heavyPos.getZ()) > 0.01) {
            heavyBody.setPosition((float) newPos.x, (float) newPos.y, (float) newPos.z);
        }

        lastTickMillis = 50f; // 默认 20tps
    }

    /**
     * 根据水下状态和结构质量，配置 heavy 的 Force 对象
     */
    private void configureHeavyForce(boolean inWater) {
        heavyForce.reset();

        // 重力：始终作用于质量
        float g = PhysicsConfig.GRAVITY; // m/s^2，正值表示向下
        heavyForce.setGravity(0, -g * totalMass, 0);

        if (inWater) {
            // 水下：轻质方块有升力，重质方块增加下沉力
            // 净力 = 轻质数量 * 升力系数 - 重质数量 * 重量系数/4
            float lift = lightBlockCount * PhysicsConfig.LIGHT_BLOCK_BUOYANCY * PhysicsConfig.BUOYANCY_FORCE_MULTIPLIER;
            float sink = heavyBlockCount * PhysicsConfig.HEAVY_BLOCK_WEIGHT * 0.25f;
            float netVertical = lift - sink;

            if (netVertical > 0.0f) {
                heavyForce.addThrustUp(netVertical);
            } else if (netVertical < 0.0f) {
                heavyForce.addThrustDown(-netVertical);
            }
        }
    }

    private boolean checkIfInWater() {
        Vec3 pos = this.position();
        BlockPos center = new BlockPos((int) Math.floor(pos.x),
                                        (int) Math.floor(pos.y),
                                        (int) Math.floor(pos.z));
        BlockPos[] probes = {center, center.above(), center.below(),
                              center.north(), center.south(), center.east(), center.west()};
        for (BlockPos p : probes) {
            if (this.level().getFluidState(p).is(Fluids.WATER)
                || this.level().getFluidState(p).is(Fluids.FLOWING_WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_IS_PHYSICS_ACTIVE, true);
        builder.define(DATA_LIGHT_BLOCKS, 0);
        builder.define(DATA_HEAVY_BLOCKS, 0);
    }

    // ============ 玩家交互：映射到物理维度的原版方块 ============

    public BlockPos findClickedBlockPos(Player player, float partialTicks) {
        Vec3 eyePos = player.getEyePosition(partialTicks);
        Vec3 lookVec = player.getViewVector(partialTicks);
        double distance = player.blockInteractionRange();
        Vec3 endPos = eyePos.add(lookVec.scale(distance));
        Vec3 entityPos = this.position();

        double closestDist = Double.MAX_VALUE;
        BlockPos closestPos = null;

        for (BlockPos localPos : localBlockPositions) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                entityPos.x + localPos.getX(),
                entityPos.y + localPos.getY(),
                entityPos.z + localPos.getZ(),
                entityPos.x + localPos.getX() + 1,
                entityPos.y + localPos.getY() + 1,
                entityPos.z + localPos.getZ() + 1
            );
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
        if (player.level().isClientSide()) {
            return InteractionResult.CONSUME;
        }

        BlockPos clickedLocal = findClickedBlockPos(player, 1.0f);

        if (clickedLocal == null) {
            togglePhysicsState(player);
            return InteractionResult.SUCCESS;
        }

        // 映射到物理维度的真实方块 → 调用原版 BlockState.use
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

        Vec3 hitPos = new Vec3(
            physicsPos.getX() + 0.5,
            physicsPos.getY() + 0.5,
            physicsPos.getZ() + 0.5
        );
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            hitPos, net.minecraft.core.Direction.UP, physicsPos, false
        );

        return state.getBlock().use(state, physicsLevel, player, hand, hit);
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

    // ============ 对外 API：冲量、速度、激活 ============

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

    public void applyImpulse(Vec3 force) {
        if (heavyBody != null) {
            heavyBody.applyImpulse(new Vector3(
                (float) force.x, (float) force.y, (float) force.z
            ));
        }
    }

    public void setVelocity(Vec3 velocity) {
        if (heavyBody != null) {
            heavyBody.applyImpulse(new Vector3(
                (float) velocity.x, (float) velocity.y, (float) velocity.z
            ));
        }
        // 同步给 MC 实体的 deltaMovement 用于渲染
        this.setDeltaMovement(velocity);
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
        return (lightBlockCount * PhysicsConfig.LIGHT_BLOCK_BUOYANCY) -
               (heavyBlockCount * PhysicsConfig.HEAVY_BLOCK_WEIGHT * 0.25f);
    }

    public PhysicsBody getHeavyBody() {
        return heavyBody;
    }

    public BlockState getBlockState(BlockPos localPos) {
        if (this.level().isClientSide()) {
            return clientStateCache.getOrDefault(localPos, Blocks.STONE.defaultBlockState());
        } else {
            ServerLevel physicsLevel = PhysicsDimension.getCachedLevel();
            if (physicsLevel != null && physicsOrigin != null) {
                return physicsLevel.getBlockState(physicsOrigin.offset(localPos));
            }
            return Blocks.AIR.defaultBlockState();
        }
    }

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

    public void updateClientBlockStates(Map<BlockPos, BlockState> states) {
        if (this.level().isClientSide()) {
            clientStateCache.clear();
            clientStateCache.putAll(states);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (physicsOrigin != null) {
            tag.putInt("originX", physicsOrigin.getX());
            tag.putInt("originY", physicsOrigin.getY());
            tag.putInt("originZ", physicsOrigin.getZ());
        }
        ListTag positionsTag = new ListTag();
        for (BlockPos p : localBlockPositions) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", p.getX());
            posTag.putInt("y", p.getY());
            posTag.putInt("z", p.getZ());
            positionsTag.add(posTag);
        }
        tag.put("localPositions", positionsTag);

        ListTag worldMapTag = new ListTag();
        for (Map.Entry<BlockPos, BlockPos> entry : localToWorldMap.entrySet()) {
            CompoundTag mapTag = new CompoundTag();
            mapTag.putIntArray("local", new int[]{entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()});
            mapTag.putIntArray("world", new int[]{entry.getValue().getX(), entry.getValue().getY(), entry.getValue().getZ()});
            worldMapTag.add(mapTag);
        }
        tag.put("localToWorldMap", worldMapTag);
        tag.putBoolean("physicsActive", this.getEntityData().get(DATA_IS_PHYSICS_ACTIVE));
        tag.putFloat("totalMass", totalMass);
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
        this.totalMass = tag.getFloat("totalMass");
        if (this.totalMass <= 0) this.totalMass = 1.0f;

        // 重建 heavy body
        if (heavyBody != null) heavyBody.close();
        if (heavyForce != null) heavyForce.close();
        Vec3 pos = this.position();
        this.heavyBody = new PhysicsBody((float) pos.x, (float) pos.y, (float) pos.z, totalMass);
        this.heavyForce = new Force();
        WaterfallMod.addPhysicsBodyToWorld(heavyBody);
    }
}
