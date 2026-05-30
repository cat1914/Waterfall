package com.waterfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.waterfall.block.entity.PhysicsContainerBlockEntity;
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
                    // Shift+右键：停用物理
                    container.deactivate();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Physics container deactivated"));
                } else {
                    // 右键：激活物理
                    container.activate();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§6Physics container activated!"));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
