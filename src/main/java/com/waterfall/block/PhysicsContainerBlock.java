package com.waterfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.waterfall.WaterfallMod;
import com.waterfall.block.entity.PhysicsContainerBlockEntity;
import com.waterfall.entity.PhysicsBlockEntity;
import com.waterfall.physics.rigidbody.RigidBody;
import org.jetbrains.annotations.Nullable;

/**
 * 物理容器方块 - 核心方块，用于绑定和控制物理化的方块结构
 */
public class PhysicsContainerBlock extends BaseEntityBlock {
    public PhysicsContainerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PhysicsContainerBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhysicsContainerBlockEntity container) {
                if (player.isShiftKeyDown()) {
                    // Shift+右键：生成物理实体
                    RigidBody body = container.getRigidBody();
                    if (body == null || body.getBlocks().isEmpty()) {
                        player.displayClientMessage(
                            Component.literal("§7No blocks bound to container"), false);
                    } else {
                        spawnPhysicsEntity(level, pos, body, player);
                    }
                } else {
                    // 右键：激活/停用物理
                    RigidBody body = container.getRigidBody();
                    if (body != null) {
                        boolean wasActive = body.isActive();
                        if (!wasActive) {
                            body.setActive(true);
                            player.displayClientMessage(
                                Component.literal("§6Physics Activated!"), false);
                        } else {
                            body.setActive(false);
                            player.displayClientMessage(
                                Component.literal("§7Physics Deactivated"), false);
                        }
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    /**
     * 生成物理方块实体
     */
    private void spawnPhysicsEntity(Level level, BlockPos pos, RigidBody body, Player player) {
        if (level instanceof ServerLevel serverLevel) {
            // 创建物理实体
            PhysicsBlockEntity entity = new PhysicsBlockEntity(
                serverLevel, 
                body, 
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5));
            
            // 将实体加到世界
            serverLevel.addFreshEntity(entity);
            
            // 给玩家反馈
            player.displayClientMessage(
                Component.literal("§6Physics Blocks Spawned! Light: " + body.getLightBlockCount() + 
                               " Heavy: " + body.getHeavyBlockCount()), false);
            
            // 记录日志
            WaterfallMod.LOGGER.info("Spawned physics block entity at " + pos);
        }
    }
    
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhysicsContainerBlockEntity container) {
                container.initialize();
            }
        }
    }
    
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PhysicsContainerBlockEntity container) {
                RigidBody body = container.getRigidBody();
                if (body != null) {
                    // 清理逻辑
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
