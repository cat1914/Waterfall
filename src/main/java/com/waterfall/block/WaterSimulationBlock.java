package com.waterfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.waterfall.physics.PhysicsEngineManager;

import java.util.Random;

public class WaterSimulationBlock extends Block {
    private static final double FLUID_DENSITY = 1.025;
    private static final double VISCOSITY = 0.05;
    
    public WaterSimulationBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getShape(level, pos);
    }
    
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            updatePhysicsInArea(serverLevel, pos);
        }
    }
    
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, Random random) {
        super.tick(state, level, pos, random);
        if (!level.isClientSide) {
            updatePhysicsInArea(level, pos);
        }
    }
    
    private void updatePhysicsInArea(ServerLevel level, BlockPos center) {
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.is(this)) {
                        applyFluidDynamics(level, checkPos);
                    }
                }
            }
        }
    }
    
    private void applyFluidDynamics(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        
        if (!aboveState.isSolid()) {
            level.scheduleTick(pos, this, 1);
        }
    }
    
    public double getFluidDensity() {
        return FLUID_DENSITY;
    }
    
    public double getViscosity() {
        return VISCOSITY;
    }
}
