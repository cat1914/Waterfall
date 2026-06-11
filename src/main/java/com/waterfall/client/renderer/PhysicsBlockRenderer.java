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

import java.util.Map;

/**
 * 物理方块实体渲染器：渲染物理化的方块
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
        
        // 保存当前pose状态
        poseStack.pushPose();
        
        // 计算平滑位置（使用线性插值）
        double x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        
        // 移到实体位置
        poseStack.translate(x, y, z);
        
        // 获取所有方块状态并渲染
        Map<BlockPos, BlockState> blockStates = entity.getAllBlockStates();
        for (Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
            BlockPos localPos = entry.getKey();
            BlockState state = entry.getValue();
            
            // 移到方块相对位置
            poseStack.pushPose();
            poseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());
            
            // 计算光照（简单处理，使用实体位置的光照）
            int actualLight = LevelRenderer.getLightColor(entity.level(),
                new BlockPos((int)entity.getX() + localPos.getX(),
                           (int)entity.getY() + localPos.getY(),
                           (int)entity.getZ() + localPos.getZ()));
            
            // 渲染方块
            try {
                blockRenderer.renderSingleBlock(
                    state, 
                    poseStack, 
                    bufferSource, 
                    actualLight, 
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } catch (Exception e) {
                // 如果渲染出错，至少不要崩溃
            }
            
            poseStack.popPose();
        }
        
        // 可选：渲染调试边界框
        if (entity.isNoGravity()) { // 使用某个调试标志
            renderDebugBox(poseStack, bufferSource, entity, x, y, z);
        }
        
        // 恢复pose状态
        poseStack.popPose();
    }
    
    /**
     * 渲染调试边界框
     */
    private void renderDebugBox(PoseStack poseStack, MultiBufferSource bufferSource,
                             PhysicsBlockEntity entity, double x, double y, double z) {
        // 获取边界框
        AABB aabb = entity.getBoundingBox().move(-x, -y, -z);
        
        // 渲染线条框
        LevelRenderer.renderLineBox(
            poseStack, 
            bufferSource.getBuffer(RenderType.lines()), 
            aabb, 
            0.0f, 1.0f, 0.0f, 1.0f);
    }
    
    @Override
    public ResourceLocation getTextureLocation(PhysicsBlockEntity entity) {
        // 返回一个默认的纹理（实际渲染使用方块自身纹理）
        return ResourceLocation.tryParse(WaterfallMod.MODID + ":textures/entity/physics_block.png");
    }
    
    /**
     * 覆盖默认的阴影半径，根据实体大小调整
     */
    @Override
    protected float getShadowRadius(PhysicsBlockEntity entity) {
        // 修复类型转换问题
        float baseShadow = super.getShadowRadius(entity);
        float entitySize = (float)Math.max(entity.getBoundingBox().getXsize(), entity.getBoundingBox().getZsize());
        return Math.min(baseShadow, entitySize);
    }
}
