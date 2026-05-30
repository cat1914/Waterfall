package com.waterfall;

import com.waterfall.api.WaterfallPhysicsApi;
import com.waterfall.api.impl.WaterfallPhysicsApiImpl;

/**
 * Main entry point for Waterfall Physics API
 * 
 * Other mods should use this to access physics functionality.
 */
public class WaterfallPhysics {
    
    private static final WaterfallPhysicsApiImpl INSTANCE = new WaterfallPhysicsApiImpl();
    
    /**
     * Gets the Waterfall Physics API instance
     * 
     * @return The Physics API
     */
    public static WaterfallPhysicsApi getApi() {
        return INSTANCE;
    }
    
    /**
     * Checks if Waterfall Physics is loaded
     * 
     * @return True if loaded
     */
    public static boolean isLoaded() {
        return true;
    }
}
