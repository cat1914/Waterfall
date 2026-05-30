package com.waterfall.block;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;

/**
 * Physics Blocks Registration - No player-facing blocks; API only
 */
public class PhysicsBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, WaterfallMod.MODID);
    
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
