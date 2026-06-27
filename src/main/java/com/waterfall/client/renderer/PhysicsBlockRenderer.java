package com.waterfall.client.renderer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.waterfall.WaterfallMod;
import com.waterfall.entity.PhysicsBlockEntity;

import java.util.Set;

/**
 * 物理方块实体渲染器
 * 
 * 从实体获取局部方块位置集合，为每个位置调用 getBlockState() 来获取方块状态，
 * 然后使用标准的 Minecraft 方块渲染器进行渲染。
 * 
 * 在客户端，方块状态由实体的本地缓存提供；
 * 在服务端，方块状态实际存储在物理维度中。
 */
public class PhysicsBlockRenderer extends EntityRenderer<PhysicsBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;
    
    public PhysicsBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }
    
    @Override
    public void render(PhysicsBlockEntity entity, float entityYaw, float partialTicks, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
        
        poseStack.pushPose();
        
        // 计算平滑位置
        double x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        
        poseStack.translate(x, y, z);
        
        // 获取实体的所有局部方块位置并渲染
        Set<BlockPos> localPositions = entity.getLocalBlockPositions();
        for (BlockPos localPos : localPositions) {
            // 通过实体获取方块状态（在客户端使用本地缓存，在服务端查询物理维度）
            BlockState state = entity.getBlockState(localPos);
            if (state == null || state.isAir()) {
                continue;
            }
            
            poseStack.pushPose();
            poseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());
            
            // 计算光照
            int actualLight = LevelRenderer.getLightColor(entity.level(),
                new BlockPos((int) entity.getX() + localPos.getX(),
                           (int) entity.getY() + localPos.getY(),
                           (int) entity.getZ() + localPos.getZ()));
            
            // 渲染方块
            try {
                blockRenderer.renderSingleBlock(
                    state,
                    poseStack,
                    bufferSource,
                    actualLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                // 渲染失败时不崩溃
            }
            
            poseStack.popPose();
        }
        
        // 调试边界框
        if (entity.isNoGravity()) {
            renderDebugBox(poseStack, bufferSource, entity, x, y, z);
        }
        
        poseStack.popPose();
    }
    
    private void renderDebugBox(PoseStack poseStack, MultiBufferSource bufferSource,
                             PhysicsBlockEntity entity, double x, double y, double z) {
        AABB aabb = entity.getBoundingBox().move(-x, -y, -z);
        
        LevelRenderer.renderLineBox(
            poseStack,
            bufferSource.getBuffer(RenderType.lines()),
            aabb,
            0.0f, 1.0f, 0.0f, 1.0f);
    }
    
    @Override
    public ResourceLocation getTextureLocation(PhysicsBlockEntity entity) {
        return ResourceLocation.tryParse(WaterfallMod.MODID + ":textures/entity/physics_block.png");
    }
    
    @Override
    protected float getShadowRadius(PhysicsBlockEntity entity) {
        float baseShadow = super.getShadowRadius(entity);
        float entitySize = (float) Math.max(entity.getBoundingBox().getXsize(), entity.getBoundingBox().getZsize());
        return Math.min(baseShadow, entitySize);
    }
}
