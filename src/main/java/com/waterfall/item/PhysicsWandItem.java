package com.waterfall.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.waterfall.entity.PhysicsEntity;
import com.waterfall.entity.PhysicsEntityType;
import com.waterfall.physics.PhysicsEngineManager;

public class PhysicsWandItem extends Item {
    public PhysicsWandItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        
        if (!level.isClientSide && player != null && level instanceof ServerLevel serverLevel) {
            PhysicsEntity entity = PhysicsEntityType.PHYSICS_ENTITY.get().create(serverLevel);
            if (entity != null) {
                Vec3 spawnPos = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                entity.setPos(spawnPos);
                entity.setMass(1.0f);
                serverLevel.addFreshEntity(entity);
                
                player.displayClientMessage(Component.literal("§6Spawned physics entity with wand!"), true);
                
                if (!player.getAbilities().instabuild) {
                    player.getItemInHand(context.getHand()).hurtAndBreak(1, serverLevel, player, 
                        item -> {});
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide && target.level() instanceof ServerLevel serverLevel) {
            PhysicsEntity entity = PhysicsEntityType.PHYSICS_ENTITY.get().create(serverLevel);
            if (entity != null) {
                Vec3 spawnPos = target.position().add(0, 1, 0);
                entity.setPos(spawnPos);
                entity.setMass(2.0f);
                serverLevel.addFreshEntity(entity);
                
                entity.applyPhysicsImpulse(new Vec3(
                    (random.nextDouble() - 0.5) * 5,
                    random.nextDouble() * 3,
                    (random.nextDouble() - 0.5) * 5
                ));
                
                if (attacker instanceof Player player && !player.getAbilities().instabuild) {
                    stack.hurtAndBreak(1, serverLevel, player, item -> {});
                }
            }
        }
        return true;
    }
    
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("§5Physics Wand");
    }
}
