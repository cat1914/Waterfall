package com.waterfall.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenSummary;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.gen.StructureSettings;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blender.Blender;
import net.minecraft.world.level.levelgen.surfacerules.SurfaceRules;
import net.minecraft.world.level.noise.NoiseRouter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class PhysicsChunkGenerator extends ChunkGenerator {
    public static final com.mojang.serialization.Codec<PhysicsChunkGenerator> CODEC = 
        com.mojang.serialization.Codec.unit(PhysicsChunkGenerator::new);
    
    private static final int SEA_LEVEL = 32;
    
    public PhysicsChunkGenerator(BiomeSource biomeSource, StructureSettings structureSettings) {
        super(biomeSource, structureSettings);
    }
    
    public PhysicsChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }
    
    public PhysicsChunkGenerator() {
        // 简化的默认构造函数
        super(net.minecraft.world.level.biome.FixedBiomeSource.create(
            net.minecraft.world.level.biome.Biomes.PLAINS
        ));
    }
    
    @Override
    protected com.mojang.serialization.Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }
    
    @Override
    public void buildSurface(WorldGenAccess world, StructureManager structureManager, RandomState randomState, LevelChunk chunk) {
        // 空实现 - 物理维度不需要生成地形
    }
    
    @Override
    public void applyBiomeDecoration(WorldGenAccess world, ChunkAccess chunk, StructureManager structureManager) {
        // 空实现
    }
    
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }
    
    @Override
    public int getGenDepth() {
        return 384;
    }
    
    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }
    
    @Override
    public int getMinY() {
        return -64;
    }
    
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        return SEA_LEVEL;
    }
    
    @Override
    public NoiseRouter router(RandomState randomState) {
        return new NoiseRouter(
            null, null, null, null, null, null, null
        );
    }
    
    @Override
    public SurfaceRules.RuleSource surfaceRule(RandomState randomState) {
        return SurfaceRules.noiseBasedSource(null, null, null);
    }
    
    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        // 物理维度的调试信息
        info.add("Physics Dimension");
        info.add("Light blocks: 0");
        info.add("Heavy blocks: 0");
    }
}
