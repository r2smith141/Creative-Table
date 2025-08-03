package com.craigsmods.creativeprototyper.event;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles disabling advancements in the creative dimension
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class AdvancementHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        // Check if the player is in the creative dimension
        if (event.getEntity() instanceof ServerPlayer player && 
            player.level().dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            
            // Cancel the advancement
            //event.setCanceled(true);
            
            // Log for debugging
            Advancement advancement = event.getAdvancement();
            ResourceLocation id = advancement.getId();
        }
    }
    
    /**
     * Handle advancement progress events
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAdvancementProgress(AdvancementEvent.AdvancementProgressEvent event) {
        // Check if the player is in the creative dimension
        if (event.getEntity() instanceof ServerPlayer player && 
            player.level().dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            
            // Cancel the advancement progress
            //event.setCanceled(true);
            
            
        }
    }
}