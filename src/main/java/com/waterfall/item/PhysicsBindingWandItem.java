package com.waterfall.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import com.waterfall.WaterfallMod;
import com.waterfall.block.entity.PhysicsContainerBlockEntity;
import com.waterfall.physics.rigidbody.RigidBody;
import org.jetbrains.annotations.NotNull;

/**
 * 物理绑定工具 - 用于选择和绑定要物理化的方块
 */
public class PhysicsBindingWandItem extends Item {
    public static final String TAG_FIRST_POS = "first_pos";
    public static final String TAG_SECOND_POS = "second_pos";
    public static final String TAG_SELECTING = "selecting";
    
    public PhysicsBindingWandItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        
        if (!level.isClientSide && player != null) {
            if (player.isShiftKeyDown()) {
                // Shift+右键：清除选择
                clearSelection(stack);
                player.sendSystemMessage(Component.literal("§7Selection cleared"));
            } else {
                // 正常右键：选择第二个位置
                if (isSelectingFirst(stack)) {
                    // 正在选择第一个位置
                    if (isFirstPosSet(stack)) {
                        // 已经有第一个位置，现在选择第二个
                        setSecondPos(stack, pos);
                        setSelectingFirst(stack, false);
                        player.sendSystemMessage(Component.literal("§6Second position set! Ready to bind"));
                    } else {
                        // 设置第一个位置
                        setFirstPos(stack, pos);
                        player.sendSystemMessage(Component.literal("§5First position set"));
                    }
                } else {
                    // 检查是否在物理容器旁边
                    BlockPos containerPos = findPhysicsContainer(level, pos);
                    if (containerPos != null) {
                        // 绑定到物理容器
                        if (areBothPositionsSet(stack)) {
                            bindBlocksToContainer(level, containerPos, stack);
                            clearSelection(stack);
                        } else {
                            player.sendSystemMessage(Component.literal("§cPlease select both positions first!"));
                        }
                    } else {
                        // 开始选择
                        setFirstPos(stack, pos);
                        setSelectingFirst(stack, true);
                        player.sendSystemMessage(Component.literal("§5First position set! Right-click again for second"));
                    }
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            clearSelection(stack);
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("§7Selection cleared"));
            }
        }
        return InteractionResultHolder.success(stack);
    }
    
    private BlockPos findPhysicsContainer(Level level, BlockPos around) {
        // 检查周围是否有物理容器
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    mutable.set(around.getX() + dx, around.getY() + dy, around.getZ() + dz);
                    if (level.getBlockEntity(mutable) instanceof PhysicsContainerBlockEntity) {
                        return mutable.immutable();
                    }
                }
            }
        }
        return null;
    }
    
    private void bindBlocksToContainer(Level level, BlockPos containerPos, ItemStack wand) {
        BlockPos first = getFirstPos(wand);
        BlockPos second = getSecondPos(wand);
        
        if (first == null || second == null) return;
        
        // 获取范围
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        
        // 获取容器方块实体
        if (level.getBlockEntity(containerPos) instanceof PhysicsContainerBlockEntity container) {
            RigidBody body = container.getRigidBody();
            if (body != null) {
                BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
                int count = 0;
                
                // 扫描范围内的方块并绑定到刚体
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            mutable.set(x, y, z);
                            BlockState state = level.getBlockState(mutable);
                            if (!state.isAir()) {
                                // 计算相对位置
                                int relX = x - containerPos.getX();
                                int relY = y - containerPos.getY();
                                int relZ = z - containerPos.getZ();
                                
                                BlockPos localPos = new BlockPos(relX, relY, relZ);
                                body.addBlock(localPos, state);
                                count++;
                            }
                        }
                    }
                }
                
                if (count > 0) {
                    WaterfallMod.LOGGER.info("Bound " + count + " blocks to rigid body");
                }
            }
        }
    }
    
    private boolean isSelectingFirst(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean(TAG_SELECTING);
    }
    
    private void setSelectingFirst(ItemStack stack, boolean selecting) {
        stack.getOrCreateTag().putBoolean(TAG_SELECTING, selecting);
    }
    
    private boolean isFirstPosSet(ItemStack stack) {
        return stack.getOrCreateTag().contains(TAG_FIRST_POS);
    }
    
    private boolean areBothPositionsSet(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains(TAG_FIRST_POS) && tag.contains(TAG_SECOND_POS);
    }
    
    private void setFirstPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putIntArray(TAG_FIRST_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }
    
    private BlockPos getFirstPos(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_FIRST_POS)) {
            int[] arr = tag.getIntArray(TAG_FIRST_POS);
            return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }
    
    private void setSecondPos(ItemStack stack, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putIntArray(TAG_SECOND_POS, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }
    
    private BlockPos getSecondPos(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_SECOND_POS)) {
            int[] arr = tag.getIntArray(TAG_SECOND_POS);
            return new BlockPos(arr[0], arr[1], arr[2]);
        }
        return null;
    }
    
    private void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(TAG_FIRST_POS);
        tag.remove(TAG_SECOND_POS);
        tag.remove(TAG_SELECTING);
    }
}
