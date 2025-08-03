package com.craigsmods.creativeprototyper.event;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlock;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class TableProtectionHandler {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        
        // Check if we're in the creative dimension
        if (level.dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            BlockState state = event.getState();
            
            // Check if the block being broken is our creative table
            if (state.getBlock() instanceof CreativeTableBlock) {
                BlockPos pos = event.getPos();
                Player player = event.getPlayer();
                
                // Check if this is a return portal
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CreativeTableBlockEntity tableEntity && tableEntity.isReturnPortal()) {
                    // Cancel the break event
                    event.setCanceled(true);
                    
                    // Notify the player
                    player.displayClientMessage(
                        Component.literal("You cannot break the return portal! Use it to return to your original location.")
                                 .withStyle(ChatFormatting.RED),
                        false
                    );
                    
                    System.out.println("Prevented player " + player.getName().getString() + 
                                      " from breaking return portal at " + pos);
                }
            }
        }
    }
    
    // Also handle non-player block breaking (like explosions, pistons, etc.)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockDestroy(BlockEvent.BreakEvent event) {
        // This handles any kind of block breaking event
        Level level = (Level) event.getLevel();
        
        // Check if we're in the creative dimension
        if (level.dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            BlockState state = event.getState();
            
            // Check if the block being broken is our creative table
            if (state.getBlock() instanceof CreativeTableBlock) {
                BlockPos pos = event.getPos();
                
                // Check if this is a return portal
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CreativeTableBlockEntity tableEntity && tableEntity.isReturnPortal()) {
                    // Cancel the break event
                    event.setCanceled(true);
                    
                    System.out.println("Prevented destruction of return portal at " + pos);
                }
            }
        }
    }
}