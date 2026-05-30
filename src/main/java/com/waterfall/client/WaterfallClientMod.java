package com.waterfall.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import com.waterfall.WaterfallMod;
import com.waterfall.client.renderer.PhysicsBlockRenderer;
import com.waterfall.entity.PhysicsBlockEntity;
import com.waterfall.entity.PhysicsEntityType;

/**
 * 客户端初始化：注册渲染器、着色器等客户端特有内容
 */
@EventBusSubscriber(modid = WaterfallMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class WaterfallClientMod {
    
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        WaterfallMod.LOGGER.info("Registering physics block entity renderer...");
        event.registerEntityRenderer(
            PhysicsEntityType.PHYSICS_BLOCK.get(),
            PhysicsBlockRenderer::new);
    }
}
