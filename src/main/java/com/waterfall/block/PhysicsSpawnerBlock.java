package com.waterfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntityType;
import com.waterfall.entity.PhysicsEntity;
import com.waterfall.entity.PhysicsEntityType;

public class PhysicsSpawnerBlock extends Block {
    public PhysicsSpawnerBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            PhysicsEntity entity = PhysicsEntityType.PHYSICS_ENTITY.get().create(serverLevel);
            if (entity != null) {
                Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                entity.setPos(spawnPos);
                
                float mass = 1.0f;
                entity.setMass(mass);
                
                serverLevel.addFreshEntity(entity);
                
                WaterfallMod.LOGGER.info("Spawned physics entity at {}", spawnPos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
