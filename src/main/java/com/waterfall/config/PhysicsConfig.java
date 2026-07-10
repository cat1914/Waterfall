package com.waterfall.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PhysicsConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static float GRAVITY = 9.81f;
    public static float WATER_GRAVITY = 1.5f;
    public static float WATER_DENSITY = 1.025f;
    public static float AIR_DRAG = 0.01f;
    public static float WATER_DRAG = 0.05f;
    public static float BUOYANCY_FORCE = 2.0f;
    public static int MAX_ENTITIES = 1000;
    public static boolean ENABLE_UNDERWATER_PHYSICS = true;
    public static boolean ENABLE_PHYSICS_DIMENSION = true;
    public static float PHYSICS_TICK_RATE = 0.016f;
    
    // 水下物理精确配置
    public static float LIGHT_BLOCK_BUOYANCY = 1.0f;  // 轻质方块的浮力
    public static float HEAVY_BLOCK_WEIGHT = 4.0f;    // 重质方块的重量（相对于轻质）
    public static float BUOYANCY_FORCE_MULTIPLIER = 2.0f; // 浮力力量倍数
    public static boolean ENABLE_MATERIAL_PHYSICS = true;  // 启用材质区分物理
    
    private static final String CONFIG_FILE_NAME = "waterfall-physics.toml";
    
    public static void register() {
        loadConfig();
        // 将 PhysicsConfig 注册到 NeoForge 事件总线。
        // 事件总线要求被注册的类必须至少包含一个 @SubscribeEvent 方法，
        // 否则会在启动时抛出异常。onServerTick 即是为此保留的 dummy 入口，
        // 也方便后续在此处挂载配置热重载逻辑。
        NeoForge.EVENT_BUS.register(PhysicsConfig.class);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // dummy：保持 PhysicsConfig 在事件总线上注册
    }
    
    private static void loadConfig() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE_NAME);
        
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        
        try {
            String content = Files.readString(configPath);
            parseConfig(content);
            LOGGER.info("Loaded physics configuration from {}", configPath);
        } catch (IOException e) {
            LOGGER.error("Failed to load config file: {}", e.getMessage());
        }
    }
    
    private static void createDefaultConfig(Path path) {
        String defaultConfig = """
            # Waterfall Physics Configuration
            # Physics Engine Settings
            
            # Gravity in air (m/s^2)
            gravity = 9.81
            
            # Gravity in water (m/s^2)
            water_gravity = 1.5
            
            # Water density for buoyancy calculations
            water_density = 1.025
            
            # Air drag coefficient
            air_drag = 0.01
            
            # Water drag coefficient
            water_drag = 0.05
            
            # Buoyancy force multiplier
            buoyancy_force = 2.0
            
            # Maximum number of physics entities
            max_entities = 1000
            
            # Enable underwater physics
            enable_underwater_physics = true
            
            # Enable physics dimension
            enable_physics_dimension = true
            
            # Physics tick rate (seconds)
            physics_tick_rate = 0.016
            
            # ==============
            # Material-specific Underwater Physics
            # ==============
            
            # Enable material-specific physics (light blocks float, heavy blocks sink)
            enable_material_physics = true
            
            # Light block buoyancy (wood, wool, etc.)
            light_block_buoyancy = 1.0
            
            # Heavy block weight relative to light blocks (4 light = 1 heavy)
            heavy_block_weight = 4.0
            
            # Overall buoyancy force multiplier
            buoyancy_force_multiplier = 2.0
            """;
        
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, defaultConfig);
            LOGGER.info("Created default physics configuration at {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to create config file: {}", e.getMessage());
        }
    }
    
    private static void parseConfig(String content) {
        for (String line : content.split("\\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            
            String[] parts = line.split("=");
            if (parts.length != 2) continue;
            
            String key = parts[0].trim();
            String value = parts[1].trim();
            
            try {
                switch (key) {
                    case "gravity" -> GRAVITY = Float.parseFloat(value);
                    case "water_gravity" -> WATER_GRAVITY = Float.parseFloat(value);
                    case "water_density" -> WATER_DENSITY = Float.parseFloat(value);
                    case "air_drag" -> AIR_DRAG = Float.parseFloat(value);
                    case "water_drag" -> WATER_DRAG = Float.parseFloat(value);
                    case "buoyancy_force" -> BUOYANCY_FORCE = Float.parseFloat(value);
                    case "max_entities" -> MAX_ENTITIES = Integer.parseInt(value);
                    case "enable_underwater_physics" -> ENABLE_UNDERWATER_PHYSICS = Boolean.parseBoolean(value);
                    case "enable_physics_dimension" -> ENABLE_PHYSICS_DIMENSION = Boolean.parseBoolean(value);
                    case "physics_tick_rate" -> PHYSICS_TICK_RATE = Float.parseFloat(value);
                    case "enable_material_physics" -> ENABLE_MATERIAL_PHYSICS = Boolean.parseBoolean(value);
                    case "light_block_buoyancy" -> LIGHT_BLOCK_BUOYANCY = Float.parseFloat(value);
                    case "heavy_block_weight" -> HEAVY_BLOCK_WEIGHT = Float.parseFloat(value);
                    case "buoyancy_force_multiplier" -> BUOYANCY_FORCE_MULTIPLIER = Float.parseFloat(value);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid value for {}: {}", key, value);
            }
        }
    }
}
