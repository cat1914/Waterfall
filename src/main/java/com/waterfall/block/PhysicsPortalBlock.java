package com.waterfall.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.waterfall.dimension.PhysicsDimension;

import javax.annotation.Nullable;
import java.util.Random;

public class PhysicsPortalBlock extends BaseEntityBlock implements Portal {
    public PhysicsPortalBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
    
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, Random random) {
        if (random.nextInt(100) == 0) {
            double d0 = (double)pos.getX() + random.nextDouble();
            double d1 = (double)pos.getY() + random.nextDouble();
            double d2 = (double)pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.PORTAL, d0, d1, d2, 0, 0, 0);
        }
    }
    
    public static boolean isPortal(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(PhysicsBlocks.PHYSICS_PORTAL.get());
    }
    
    @Override
    public Vec3 getPortalDestination(Level level, BlockPos pos, PortalShape shape) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
    }
}
