package com.craigsmods.creativeprototyper.dimension;

import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.craigsmods.creativeprototyper.util.PlayerDataManager;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber
public class CreativeDimensionManager {
    // Map to store player's original game mode
    public static final Map<UUID, GameType> originalGameModes = new HashMap<>();
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    
    // Use a composite key to store table-specific data
    private static final Map<TableKey, BuildStatus> tableBuildStatus = new HashMap<>();
    
    // Map to track return portals (dimension table -> original table)
    private static final Map<BlockPos, TableKey> returnPortals = new HashMap<>();
    
    // Map to track the current active table for each player
    public static final Map<UUID, TableKey> activeTableKeys = new HashMap<>();
    
    // Central build position in creative dimension
    private static final Map<TableKey, BlockPos> tablePlacementPositions = new HashMap<>();

    /**
     * Create and store build status for a table
     */
    public static void setTableBuildStatus(Player player, BlockPos tablePos, int currentProgress, int totalBlocks) {
        UUID playerId = player.getUUID();
        ResourceKey<Level> dimension = player.level().dimension();
        
        // Create a key for this table
        TableKey tableKey = new TableKey(playerId, dimension, tablePos);
        
        // Store the data with build progress
        BuildStatus status = new BuildStatus(currentProgress, totalBlocks, false);
        tableBuildStatus.put(tableKey, status);
        
        // Set this as the active table for the player
        activeTableKeys.put(playerId, tableKey);
        
        LOGGER.info("Stored table build status for player {} at table {}: {}/{} blocks complete", 
                   player.getName().getString(), tablePos, currentProgress, totalBlocks);
    }
    
    /**
     * Mark the build as complete
     */
    public static void markBuildComplete(Player player, int totalBlocks) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        if (activeKey != null && tableBuildStatus.containsKey(activeKey)) {
            // Get the current build status
            BuildStatus status = tableBuildStatus.get(activeKey);
            
            // Create a new status with build marked as complete
            BuildStatus newStatus = new BuildStatus(totalBlocks, totalBlocks, true);
            
            // Replace the old status with the new one
            tableBuildStatus.put(activeKey, newStatus);
            
            LOGGER.info("Marked build as complete for player {} at table {}. {}/{} blocks complete", 
                       player.getName().getString(), activeKey.tablePos, totalBlocks, totalBlocks);
        } else {
            LOGGER.warn("Could not mark build as complete - no active table found for player {}", 
                       player.getName().getString());
        }
    }
    
    /**
     * Check if the build is complete for a player's active table
     */
    public static boolean isBuildComplete(Player player) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        if (activeKey != null && tableBuildStatus.containsKey(activeKey)) {
            return tableBuildStatus.get(activeKey).buildComplete;
        }
        
        return false;
    }
    
    /**
     * Get the build progress for a player's active table
     */
    public static int getBuildProgress(Player player) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        if (activeKey != null && tableBuildStatus.containsKey(activeKey)) {
            BuildStatus status = tableBuildStatus.get(activeKey);
            return status.totalBlocks > 0 ? (status.currentProgress * 100) / status.totalBlocks : 0;
        }
        
        return 0;
    }
    
    /**
     * Update build progress
     */
    public static void updateBuildProgress(Player player, int currentProgress, int totalBlocks) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        if (activeKey != null && tableBuildStatus.containsKey(activeKey)) {
            // Get the current build status
            BuildStatus status = tableBuildStatus.get(activeKey);
            
            // Create a new status with updated progress
            BuildStatus newStatus = new BuildStatus(currentProgress, totalBlocks, 
                                                  currentProgress >= totalBlocks);
            
            // Replace the old status with the new one
            tableBuildStatus.put(activeKey, newStatus);
        }
    }
    
    /**
     * Gets the source position for the player's active table
     */
    public static BlockPos getSourcePosition(Player player) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        return activeKey != null ? activeKey.tablePos : null;
    }
    
    /**
     * Gets the source dimension for the player's active table
     */
    public static ResourceKey<Level> getSourceDimension(Player player) {
        UUID playerId = player.getUUID();
        TableKey activeKey = activeTableKeys.get(playerId);
        
        return activeKey != null ? activeKey.dimension : null;
    }
    
    /**
     * Sets a player to creative mode and stores their original game mode
     */
    public static void setPlayerCreativeMode(ServerPlayer player) {
        // Store original game mode
        originalGameModes.put(player.getUUID(), player.gameMode.getGameModeForPlayer());
        
        // Set to creative
        player.setGameMode(GameType.CREATIVE);
    }
    
    /**
     * Restores a player's original game mode
     */
    public static void restorePlayerGameMode(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (originalGameModes.containsKey(playerId)) {
            GameType originalMode = originalGameModes.remove(playerId);
            player.setGameMode(originalMode);
        } else {
            player.setGameMode(GameType.SURVIVAL);
        }
    }
    
    /**
     * Event handler for when a player changes dimensions Unexpectedly
     */
    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        
        // Only handle when players leave creative dimension unexpectedly
        if (event.getFrom().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            // Check if the player data says they're still in creative mode
            if (PlayerDataManager.isPlayerInCreative(playerId)) {
                LOGGER.warn("Player {} left creative dimension unexpectedly, restoring survival state", 
                            player.getName().getString());
                
                // Return to survival state
                PlayerDataManager.returnToSurvivalDimension(player, null);
            }
        }
    }
    
    /**
     * Registers a table in the creative dimension as a return portal to an original table
     */
    public static void registerReturnPortal(Player player, BlockPos dimensionTablePos, TableKey originalTableKey) {
        returnPortals.put(dimensionTablePos, originalTableKey);
        LOGGER.info("Registered return portal at {} to original table at {}", 
                   dimensionTablePos, originalTableKey.tablePos);
    }
    
    /**
     * Gets the original table key for a return portal
     */
    public static TableKey getOriginalTableKey(BlockPos dimensionTablePos) {
        return returnPortals.get(dimensionTablePos);
    }
    
    /**
     * Creates a new TableKey instance
     */
    public static TableKey createTableKey(UUID playerId, ResourceKey<Level> dimension, BlockPos tablePos) {
        return new TableKey(playerId, dimension, tablePos);
    }
    
    /**
     * Sets the active table key for a player
     */
    public static void setActiveTableKey(UUID playerId, TableKey tableKey) {
        activeTableKeys.put(playerId, tableKey);
    }
    
    /**
     * Gets the placement position for a specific table
     */
    public static BlockPos getTablePlacementPosition(TableKey tableKey) {
        return tablePlacementPositions.get(tableKey);
    }
    
    /**
     * Sets the placement position for a specific table
     */
    public static void setTablePlacementPosition(TableKey tableKey, BlockPos pos) {
        tablePlacementPositions.put(tableKey, pos);
    }
    
    /**
     * Gets all active table keys (for persistence)
     */
    public static Map<UUID, TableKey> getActiveTableKeys() {
        return activeTableKeys;
    }
    
    /**
     * Gets the map of original game modes (for persistence)
     */
    public static Map<UUID, GameType> getOriginalGameModes() {
        return originalGameModes;
    }
    
    /**
     * Gets the active table key for a player
     */
    public static TableKey getActiveTableKey(UUID playerId) {
        return activeTableKeys.get(playerId);
    }
    
    /**
     * Sets an original game mode for a player (used during loading)
     */
    public static void setOriginalGameMode(UUID playerId, GameType gameType) {
        originalGameModes.put(playerId, gameType);
    }
    
    /**
     * Gets all table build status data (for persistence)
     */
    public static Map<TableKey, BuildStatus> getTableBuildStatus() {
        return tableBuildStatus;
    }
    
    /**
     * Gets the map of return portals (for persistence)
     */
    public static Map<BlockPos, TableKey> getReturnPortals() {
        return returnPortals;
    }
    
    /**
     * Sets a return portal (used during loading)
     */
    public static void setReturnPortal(BlockPos pos, TableKey key) {
        returnPortals.put(pos, key);
    }
    
    /**
     * Clears all stored data (used during loading to prevent duplicates)
     */
    public static void clearAllData() {
        activeTableKeys.clear();
        tableBuildStatus.clear();
        tablePlacementPositions.clear();
        returnPortals.clear();
    }
    
    /**
     * A composite key for table-specific data
     */
    public static class TableKey {
        public final UUID playerId;
        public final ResourceKey<Level> dimension;
        public final BlockPos tablePos;
        
        public TableKey(UUID playerId, ResourceKey<Level> dimension, BlockPos tablePos) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.tablePos = tablePos;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableKey tableKey = (TableKey) o;
            return Objects.equals(playerId, tableKey.playerId) &&
                   Objects.equals(dimension, tableKey.dimension) &&
                   Objects.equals(tablePos, tableKey.tablePos);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(playerId, dimension, tablePos);
        }
    }
    
    /**
     * Data class for build status
     */
    public static class BuildStatus {
        public final int currentProgress;
        public final int totalBlocks;
        public final boolean buildComplete;
        
        public BuildStatus(int currentProgress, int totalBlocks, boolean buildComplete) {
            this.currentProgress = currentProgress;
            this.totalBlocks = totalBlocks;
            this.buildComplete = buildComplete;
        }
    }
}