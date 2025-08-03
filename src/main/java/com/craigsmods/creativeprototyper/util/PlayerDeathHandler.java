package com.craigsmods.creativeprototyper.util;

import java.util.UUID;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class PlayerDeathHandler {

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    /**
     * Intercept player death in the creative dimension and return them to safety
     */
@SubscribeEvent(priority = EventPriority.HIGHEST)
public static void onPlayerDeath(LivingDeathEvent event) {
    // Check if it's a player and in our dimension
    if (event.getEntity() instanceof ServerPlayer player && 
        player.level().dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
        
        // Cancel the death event
        event.setCanceled(true);
        
        // Heal the player
        player.setHealth(player.getMaxHealth());
        
        // Reset velocity to prevent fall damage after teleport
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true; // Force a velocity update
        
        // Apply resistance effect (level 5 gives immunity to most damage)
        player.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE, 
            200,  // 10 seconds (200 ticks)
            4,    // Level 5 (0-based index)
            false, // No ambient particles
            true,  // Show particles
            true   // Show icon
        ));
        
        // Also add slow falling to prevent fall damage
        player.addEffect(new MobEffectInstance(
            MobEffects.SLOW_FALLING,
            200,  // 10 seconds
            0,    // Level 1
            false, 
            true,
            true
        ));
        
        // Notify player
        player.displayClientMessage(
            Component.literal("You were about to die in the creative dimension! Returning to safety..."), false);

        // Find return position - first try active table key
        UUID playerId = player.getUUID();
        BlockPos returnPos = null;
        ResourceKey<Level> returnDimension = null;
        
        // Get the active table key for this player
        CreativeDimensionManager.TableKey activeKey = CreativeDimensionManager.getActiveTableKey(playerId);
        if (activeKey != null) {
            returnPos = activeKey.tablePos;
            returnDimension = activeKey.dimension;
            LOGGER.info("Found return position from active table key: {} in {}", 
                       returnPos, returnDimension.location());
        }
        
        // If no position found, check for any return portal nearby
        if (returnPos == null) {
            // Search in a 64-block radius for return portals
            int searchRadius = 64;
            BlockPos playerPos = player.blockPosition();
            
            for (int x = -searchRadius; x <= searchRadius; x += 16) {
                for (int y = -searchRadius; y <= searchRadius; y += 16) {
                    for (int z = -searchRadius; z <= searchRadius; z += 16) {
                        BlockPos checkPos = playerPos.offset(x, y, z);
                        
                        // Check if this position has a return portal registered
                        CreativeDimensionManager.TableKey portalKey = 
                            CreativeDimensionManager.getOriginalTableKey(checkPos);
                        
                        if (portalKey != null) {
                            returnPos = portalKey.tablePos;
                            returnDimension = portalKey.dimension;
                            LOGGER.info("Found return position from nearby portal: {} in {}", 
                                       returnPos, returnDimension.location());
                            break;
                        }
                    }
                    if (returnPos != null) break;
                }
                if (returnPos != null) break;
            }
        }
        
        // If we still don't have a return position, use world spawn as last resort
        if (returnPos == null) {
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                returnPos = overworld.getSharedSpawnPos();
                returnDimension = Level.OVERWORLD;
                LOGGER.warn("No return position found for player {}. Using world spawn as fallback.", 
                           player.getName().getString());
            }
        }
        
        // Preserve the player's original game mode if possible
        GameType originalGameMode = GameType.SURVIVAL; // Default fallback
        if (CreativeDimensionManager.getOriginalGameModes().containsKey(playerId)) {
            originalGameMode = CreativeDimensionManager.getOriginalGameModes().get(playerId);
            LOGGER.info("Found preserved game mode for player {}: {}", 
                       player.getName().getString(), originalGameMode.getName());
        }
        
        try {
            // The returned dimension must not be null at this point
            if (returnDimension == null) {
                returnDimension = Level.OVERWORLD;
            }
            
            // Get the target level for teleportation
            ServerLevel targetLevel = player.getServer().getLevel(returnDimension);
            if (targetLevel == null) {
                // If target dimension is invalid, fall back to overworld
                targetLevel = player.getServer().getLevel(Level.OVERWORLD);
                returnPos = targetLevel.getSharedSpawnPos();
                LOGGER.warn("Invalid return dimension. Falling back to overworld spawn.");
            }
            
            // Restore player's survival state
            PlayerDataManager.returnToSurvivalDimension(player, returnPos);
            
            // Mark player as no longer in creative dimension
            PlayerDataManager.playerInCreative.put(playerId, false);
            
            // Save the updated state
            PlayerDataManager.savePlayerEntryMetadata();
            
            // Force the teleport to make sure they get out
            player.teleportTo(
                targetLevel,
                returnPos.getX() + 0.5,
                returnPos.getY() + 1.0,
                returnPos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
            );
            
            // Apply the preserved game mode after teleport
            player.setGameMode(originalGameMode);
            
            LOGGER.info("Successfully returned player {} to {} at {} with game mode {}", 
                       player.getName().getString(), returnDimension.location(), 
                       returnPos, originalGameMode.getName());
            
        } catch (Exception e) {
            LOGGER.error("Error during emergency return: {}", e.getMessage(), e);
            
            // Emergency fallback - teleport to overworld spawn and set survival
            try {
                ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    BlockPos spawnPos = overworld.getSharedSpawnPos();
                    
                    // Clear inventory to prevent item duplication in this emergency case
                    player.getInventory().clearContent();
                    
                    // Force teleport to overworld spawn
                    player.teleportTo(
                        overworld,
                        spawnPos.getX() + 0.5,
                        spawnPos.getY() + 1.0,
                        spawnPos.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                    );
                    
                    // Set to survival mode as last resort
                    player.setGameMode(GameType.SURVIVAL);
                    
                    LOGGER.info("Used emergency fallback to return player to overworld spawn");
                }
            } catch (Exception e2) {
                LOGGER.error("Critical failure in emergency teleport: {}", e2.getMessage(), e2);
            }
        }
    }
}
}