package com.waterfall.item;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.waterfall.WaterfallMod;

/**
 * Physics Items Registration - No player-facing items; API only
 */
public class PhysicsItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, WaterfallMod.MODID);
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
