package com.waterfall.physics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Set;
import java.util.HashSet;

/**
 * 材质分类系统 - 区分轻质和重质方块
 */
public class MaterialPhysics {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 材质类型
     */
    public enum MaterialType {
        LIGHT,      // 轻质：有升力
        HEAVY,      // 重质：无升力（下沉）
        NEUTRAL     // 中性：特殊处理
    }
    
    // 轻质方块：木材、羊毛等
    private static final Set<Block> LIGHT_MATERIALS = new HashSet<>();
    
    // 重质方块：石头、矿物等
    private static final Set<Block> HEAVY_MATERIALS = new HashSet<>();
    
    static {
        // 初始化轻质材质
        registerLightMaterials();
        // 初始化重质材质
        registerHeavyMaterials();
    }
    
    private static void registerLightMaterials() {
        // 木材
        LIGHT_MATERIALS.add(Blocks.OAK_WOOD);
        LIGHT_MATERIALS.add(Blocks.SPRUCE_WOOD);
        LIGHT_MATERIALS.add(Blocks.BIRCH_WOOD);
        LIGHT_MATERIALS.add(Blocks.JUNGLE_WOOD);
        LIGHT_MATERIALS.add(Blocks.ACACIA_WOOD);
        LIGHT_MATERIALS.add(Blocks.DARK_OAK_WOOD);
        LIGHT_MATERIALS.add(Blocks.CRIMSON_WOOD);
        LIGHT_MATERIALS.add(Blocks.WARPED_WOOD);
        
        LIGHT_MATERIALS.add(Blocks.OAK_PLANKS);
        LIGHT_MATERIALS.add(Blocks.SPRUCE_PLANKS);
        LIGHT_MATERIALS.add(Blocks.BIRCH_PLANKS);
        LIGHT_MATERIALS.add(Blocks.JUNGLE_PLANKS);
        LIGHT_MATERIALS.add(Blocks.ACACIA_PLANKS);
        LIGHT_MATERIALS.add(Blocks.DARK_OAK_PLANKS);
        LIGHT_MATERIALS.add(Blocks.CRIMSON_PLANKS);
        LIGHT_MATERIALS.add(Blocks.WARPED_PLANKS);
        
        LIGHT_MATERIALS.add(Blocks.OAK_LOG);
        LIGHT_MATERIALS.add(Blocks.SPRUCE_LOG);
        LIGHT_MATERIALS.add(Blocks.BIRCH_LOG);
        LIGHT_MATERIALS.add(Blocks.JUNGLE_LOG);
        LIGHT_MATERIALS.add(Blocks.ACACIA_LOG);
        LIGHT_MATERIALS.add(Blocks.DARK_OAK_LOG);
        LIGHT_MATERIALS.add(Blocks.CRIMSON_STEM);
        LIGHT_MATERIALS.add(Blocks.WARPED_STEM);
        
        // 羊毛
        LIGHT_MATERIALS.add(Blocks.WHITE_WOOL);
        LIGHT_MATERIALS.add(Blocks.ORANGE_WOOL);
        LIGHT_MATERIALS.add(Blocks.MAGENTA_WOOL);
        LIGHT_MATERIALS.add(Blocks.LIGHT_BLUE_WOOL);
        LIGHT_MATERIALS.add(Blocks.YELLOW_WOOL);
        LIGHT_MATERIALS.add(Blocks.LIME_WOOL);
        LIGHT_MATERIALS.add(Blocks.PINK_WOOL);
        LIGHT_MATERIALS.add(Blocks.GRAY_WOOL);
        LIGHT_MATERIALS.add(Blocks.LIGHT_GRAY_WOOL);
        LIGHT_MATERIALS.add(Blocks.CYAN_WOOL);
        LIGHT_MATERIALS.add(Blocks.PURPLE_WOOL);
        LIGHT_MATERIALS.add(Blocks.BLUE_WOOL);
        LIGHT_MATERIALS.add(Blocks.BROWN_WOOL);
        LIGHT_MATERIALS.add(Blocks.GREEN_WOOL);
        LIGHT_MATERIALS.add(Blocks.RED_WOOL);
        LIGHT_MATERIALS.add(Blocks.BLACK_WOOL);
        
        // 其他轻质材质
        LIGHT_MATERIALS.add(Blocks.BAMBOO);
        LIGHT_MATERIALS.add(Blocks.SUGAR_CANE);
        LIGHT_MATERIALS.add(Blocks.HAY_BLOCK);
        LIGHT_MATERIALS.add(Blocks.SPONGE);
        LIGHT_MATERIALS.add(Blocks.WET_SPONGE);
        LIGHT_MATERIALS.add(Blocks.MOSS_BLOCK);
        LIGHT_MATERIALS.add(Blocks.MOSS_CARPET);
    }
    
    private static void registerHeavyMaterials() {
        // 石头
        HEAVY_MATERIALS.add(Blocks.STONE);
        HEAVY_MATERIALS.add(Blocks.GRANITE);
        HEAVY_MATERIALS.add(Blocks.DIORITE);
        HEAVY_MATERIALS.add(Blocks.ANDESITE);
        HEAVY_MATERIALS.add(Blocks.COBBLESTONE);
        HEAVY_MATERIALS.add(Blocks.MOSSY_COBBLESTONE);
        HEAVY_MATERIALS.add(Blocks.STONE_BRICKS);
        HEAVY_MATERIALS.add(Blocks.MOSSY_STONE_BRICKS);
        HEAVY_MATERIALS.add(Blocks.CRACKED_STONE_BRICKS);
        HEAVY_MATERIALS.add(Blocks.CHISELED_STONE_BRICKS);
        
        // 矿物和矿物方块
        HEAVY_MATERIALS.add(Blocks.IRON_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_IRON_ORE);
        HEAVY_MATERIALS.add(Blocks.GOLD_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_GOLD_ORE);
        HEAVY_MATERIALS.add(Blocks.COPPER_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_COPPER_ORE);
        HEAVY_MATERIALS.add(Blocks.COAL_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_COAL_ORE);
        HEAVY_MATERIALS.add(Blocks.LAPIS_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_LAPIS_ORE);
        HEAVY_MATERIALS.add(Blocks.REDSTONE_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_REDSTONE_ORE);
        HEAVY_MATERIALS.add(Blocks.DIAMOND_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_DIAMOND_ORE);
        HEAVY_MATERIALS.add(Blocks.EMERALD_ORE);
        HEAVY_MATERIALS.add(Blocks.DEEPSLATE_EMERALD_ORE);
        
        // 矿物方块
        HEAVY_MATERIALS.add(Blocks.IRON_BLOCK);
        HEAVY_MATERIALS.add(Blocks.GOLD_BLOCK);
        HEAVY_MATERIALS.add(Blocks.COPPER_BLOCK);
        HEAVY_MATERIALS.add(Blocks.DIAMOND_BLOCK);
        HEAVY_MATERIALS.add(Blocks.EMERALD_BLOCK);
        HEAVY_MATERIALS.add(Blocks.LAPIS_BLOCK);
        HEAVY_MATERIALS.add(Blocks.COAL_BLOCK);
        HEAVY_MATERIALS.add(Blocks.REDSTONE_BLOCK);
        
        // 其他重质材质
        HEAVY_MATERIALS.add(Blocks.OBSIDIAN);
        HEAVY_MATERIALS.add(Blocks.CRYING_OBSIDIAN);
        HEAVY_MATERIALS.add(Blocks.NETHERITE_BLOCK);
        HEAVY_MATERIALS.add(Blocks.ANCIENT_DEBRIS);
    }
    
    /**
     * 获取方块材质类型
     */
    public static MaterialType getMaterialType(Block block) {
        if (LIGHT_MATERIALS.contains(block)) {
            return MaterialType.LIGHT;
        }
        if (HEAVY_MATERIALS.contains(block)) {
            return MaterialType.HEAVY;
        }
        return MaterialType.NEUTRAL;
    }
    
    public static MaterialType getMaterialType(BlockState state) {
        return getMaterialType(state.getBlock());
    }
    
    /**
     * 检查是否是轻质材质
     */
    public static boolean isLightMaterial(Block block) {
        return getMaterialType(block) == MaterialType.LIGHT;
    }
    
    public static boolean isLightMaterial(BlockState state) {
        return isLightMaterial(state.getBlock());
    }
    
    /**
     * 检查是否是重质材质
     */
    public static boolean isHeavyMaterial(Block block) {
        return getMaterialType(block) == MaterialType.HEAVY;
    }
    
    public static boolean isHeavyMaterial(BlockState state) {
        return isHeavyMaterial(state.getBlock());
    }
    
    /**
     * 计算单个方块的浮力系数
     * 规则：轻质方块有正浮力，重质方块有负浮力（下沉）
     */
    public static float getBuoyancyFactor(Block block) {
        MaterialType type = getMaterialType(block);
        return switch (type) {
            case LIGHT -> 1.0f;    // 轻质：完全升力
            case HEAVY -> -0.25f;   // 重质：负浮力（下沉）
            case NEUTRAL -> 0.0f;   // 中性：无升力
        };
    }
    
    public static float getBuoyancyFactor(BlockState state) {
        return getBuoyancyFactor(state.getBlock());
    }
    
    /**
     * 计算材质的质量系数
     * 规则：四个轻质方块的升力 = 一个重质方块的重力
     * 即：1个重质质量 = 4个轻质质量
     */
    public static float getMassFactor(Block block) {
        MaterialType type = getMaterialType(block);
        return switch (type) {
            case LIGHT -> 1.0f;     // 轻质质量为1
            case HEAVY -> 4.0f;     // 重质质量为4（4个轻质 = 1个重质）
            case NEUTRAL -> 2.0f;   // 中性为中间值
        };
    }
    
    public static float getMassFactor(BlockState state) {
        return getMassFactor(state.getBlock());
    }
}
