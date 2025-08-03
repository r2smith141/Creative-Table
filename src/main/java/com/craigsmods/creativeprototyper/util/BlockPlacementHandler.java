package com.craigsmods.creativeprototyper.util;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.craigsmods.creativeprototyper.util.BannedBlocksManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class BlockPlacementHandler {
    
    // We'll use the general BlockPlaceEvent to catch ALL block placements
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAnyBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Check if the placement happens in our creative dimension
        // First try to find a dimension based on the entity
        boolean isCreativeDimension = false;
        
        if (event.getEntity() != null && event.getLevel() instanceof Level level) {
            isCreativeDimension = level.dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY);
        }
        
        // If we determined it's in the creative dimension, check banned blocks
        if (isCreativeDimension) {
            Block placedBlock = event.getPlacedBlock().getBlock();
            
            if (BannedBlocksManager.isBlockBanned(placedBlock)) {
                // Cancel the placement regardless of what placed it
                event.setCanceled(true);
                
                // If it was a player, notify them
                if (event.getEntity() instanceof Player player) {
                    player.displayClientMessage(
                        Component.literal("This block is not allowed in the creative dimension!")
                                 .withStyle(ChatFormatting.RED),
                        true // Action bar display
                    );
                }
                
                System.out.println("Prevented placement of banned block " + placedBlock.getName().getString() + 
                                  " by " + (event.getEntity() != null ? event.getEntity().getName().getString() : "unknown entity"));
            }
        }
    }
    
    // Also handle non-entity block placements with a different event
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onNonEntityBlockPlace(EntityPlaceEvent event) {
        // This event is fired for player placements, so we can directly check the level
        Level level = (Level) event.getLevel();
        
        if (level.dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            Block placedBlock = event.getPlacedBlock().getBlock();
            
            if (BannedBlocksManager.isBlockBanned(placedBlock)) {
                // Cancel the placement
                event.setCanceled(true);
                
                // Notify the player

                
                
            }
        }
    }
    
    // Additionally, let's add a general event for any block state changes
    // This will catch piston movements, block state changes, etc.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockModify(BlockEvent event) {
        // Only check specific types that we haven't already covered
        if (event instanceof BlockEvent.EntityPlaceEvent || event instanceof EntityPlaceEvent) {
            return; // Skip, we already handle these above
        }
        
        // Try to get level and check dimension
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof Level level) {
            if (level.dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
                // For state changes and other events, check if the RESULTING block is banned
                if (event instanceof BlockEvent.BlockToolModificationEvent toolEvent) {
                    Block resultBlock = toolEvent.getFinalState().getBlock();
                    if (BannedBlocksManager.isBlockBanned(resultBlock)) {
                        event.setCanceled(true);
                        System.out.println("Prevented block tool modification to banned block " + resultBlock.getName().getString());
                    }
                } 
                else if (event instanceof BlockEvent.NeighborNotifyEvent notifyEvent) {
                    Block block = notifyEvent.getState().getBlock();
                    if (BannedBlocksManager.isBlockBanned(block)) {
                        // We may not want to cancel neighbor notifications
                        // But this at least lets us log when banned blocks might be involved
                        System.out.println("Neighbor notification from banned block " + block.getName().getString());
                    }
                }
                // Add more specific event types as needed
            }
        }
    }
}