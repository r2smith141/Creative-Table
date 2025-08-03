package com.craigsmods.creativeprototyper.dimension;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.util.AreaSnapshot;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles saving and loading table connections between server restarts
 */
public class TablePersistenceManager {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_DIR = "creative_prototyper_data";
    
   /**
 * Saves all active table connections to disk
 */
public static void saveTableConnections(MinecraftServer server) {
    try {
        File dataDir = new File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), DATA_DIR);
        dataDir.mkdirs();
        File tableFile = new File(dataDir, "table_connections.dat");
        
        CompoundTag rootTag = new CompoundTag();
        
        // Save active table keys
        ListTag activeKeysTag = new ListTag();
        for (Map.Entry<UUID, CreativeDimensionManager.TableKey> entry : CreativeDimensionManager.getActiveTableKeys().entrySet()) {
            UUID playerId = entry.getKey();
            CreativeDimensionManager.TableKey key = entry.getValue();
            
            CompoundTag keyTag = new CompoundTag();
            keyTag.putUUID("PlayerId", playerId);
            keyTag.putString("Dimension", key.dimension.location().toString());
            keyTag.putInt("PosX", key.tablePos.getX());
            keyTag.putInt("PosY", key.tablePos.getY());
            keyTag.putInt("PosZ", key.tablePos.getZ());
            activeKeysTag.add(keyTag);
        }
        rootTag.put("ActiveKeys", activeKeysTag);
        
        // Save all table build status data
        ListTag tableBuildStatusTag = new ListTag();
        for (Map.Entry<CreativeDimensionManager.TableKey, CreativeDimensionManager.BuildStatus> entry : 
             CreativeDimensionManager.getTableBuildStatus().entrySet()) {
            
            CreativeDimensionManager.TableKey key = entry.getKey();
            CreativeDimensionManager.BuildStatus status = entry.getValue();
            
            CompoundTag dataTag = new CompoundTag();
            
            // Save key info
            dataTag.putUUID("PlayerId", key.playerId);
            dataTag.putString("Dimension", key.dimension.location().toString());
            dataTag.putInt("PosX", key.tablePos.getX());
            dataTag.putInt("PosY", key.tablePos.getY());
            dataTag.putInt("PosZ", key.tablePos.getZ());
            
            // Save build status
            dataTag.putInt("CurrentProgress", status.currentProgress);
            dataTag.putInt("TotalBlocks", status.totalBlocks);
            dataTag.putBoolean("BuildComplete", status.buildComplete);
            
            // Save placement position if it exists
            BlockPos placementPos = CreativeDimensionManager.getTablePlacementPosition(key);
            if (placementPos != null) {
                dataTag.putInt("PlacementX", placementPos.getX());
                dataTag.putInt("PlacementY", placementPos.getY());
                dataTag.putInt("PlacementZ", placementPos.getZ());
            }
            
            tableBuildStatusTag.add(dataTag);
            
            LOGGER.info("Saving table build status for player {} at {}: {}/{} blocks, complete={}", 
                       key.playerId, key.tablePos, status.currentProgress, status.totalBlocks, status.buildComplete);
        }
        rootTag.put("TableBuildStatus", tableBuildStatusTag);
        
        // Save original game modes
        CompoundTag gameModesTag = new CompoundTag();
        for (Map.Entry<UUID, GameType> entry : CreativeDimensionManager.getOriginalGameModes().entrySet()) {
            gameModesTag.putInt(entry.getKey().toString(), entry.getValue().getId());
        }
        rootTag.put("GameModes", gameModesTag);
        
        // Save return portals
        ListTag returnPortalsTag = new ListTag();
        for (Map.Entry<BlockPos, CreativeDimensionManager.TableKey> entry : 
            CreativeDimensionManager.getReturnPortals().entrySet()) {
            
            BlockPos portalPos = entry.getKey();
            CreativeDimensionManager.TableKey key = entry.getValue();
            
            CompoundTag portalTag = new CompoundTag();
            
            // Save portal position
            portalTag.putInt("PortalX", portalPos.getX());
            portalTag.putInt("PortalY", portalPos.getY());
            portalTag.putInt("PortalZ", portalPos.getZ());
            
            // Save key info
            portalTag.putUUID("PlayerId", key.playerId);
            portalTag.putString("Dimension", key.dimension.location().toString());
            portalTag.putInt("PosX", key.tablePos.getX());
            portalTag.putInt("PosY", key.tablePos.getY());
            portalTag.putInt("PosZ", key.tablePos.getZ());
            
            returnPortalsTag.add(portalTag);
        }
        rootTag.put("ReturnPortals", returnPortalsTag);
        
        // Write to file
        NbtIo.writeCompressed(rootTag, tableFile);
        LOGGER.info("Successfully saved table connections data");

    } catch (IOException e) {
        LOGGER.error("Failed to save table connections data", e);
    }
}
    
/**
 * Loads all table connections from disk
 */
public static void loadTableConnections(MinecraftServer server) {
    try {
        File dataDir = new File(server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile(), DATA_DIR);
        File tableFile = new File(dataDir, "table_connections.dat");
        
        if (!tableFile.exists()) {
            LOGGER.info("No table connections data found. Starting fresh.");
            return;
        }
        
        CompoundTag rootTag = NbtIo.readCompressed(tableFile);
        
        // Clear existing data
        CreativeDimensionManager.clearAllData();
        
        // Load table build status first
        if (rootTag.contains("TableBuildStatus")) {
            ListTag buildStatusTag = rootTag.getList("TableBuildStatus", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < buildStatusTag.size(); i++) {
                CompoundTag dataTag = buildStatusTag.getCompound(i);
                
                // Get key info
                UUID playerId = dataTag.getUUID("PlayerId");
                String dimensionStr = dataTag.getString("Dimension");
                ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, 
                    new ResourceLocation(dimensionStr)
                );
                BlockPos tablePos = new BlockPos(
                    dataTag.getInt("PosX"),
                    dataTag.getInt("PosY"),
                    dataTag.getInt("PosZ")
                );
                
                // Create the table key
                CreativeDimensionManager.TableKey tableKey = 
                    CreativeDimensionManager.createTableKey(playerId, dimension, tablePos);
                
                // Load build status
                int currentProgress = dataTag.getInt("CurrentProgress");
                int totalBlocks = dataTag.getInt("TotalBlocks");
                boolean buildComplete = dataTag.getBoolean("BuildComplete");
                
                // Create build status
                CreativeDimensionManager.BuildStatus status = 
                    new CreativeDimensionManager.BuildStatus(currentProgress, totalBlocks, buildComplete);
                
                // Store in manager
                CreativeDimensionManager.getTableBuildStatus().put(tableKey, status);
                
                // Restore placement position if it exists
                if (dataTag.contains("PlacementX")) {
                    BlockPos placementPos = new BlockPos(
                        dataTag.getInt("PlacementX"),
                        dataTag.getInt("PlacementY"),
                        dataTag.getInt("PlacementZ")
                    );
                    CreativeDimensionManager.setTablePlacementPosition(tableKey, placementPos);
                }
                
                // Also update the block entity if it exists
                ServerLevel level = server.getLevel(dimension);
                if (level != null) {
                    BlockEntity be = level.getBlockEntity(tablePos);
                    if (be instanceof CreativeTableBlockEntity tableEntity) {
                        tableEntity.setCurrentScanProgress(currentProgress);
                        tableEntity.setTotalScanBlocks(totalBlocks);
                        tableEntity.setBuildingComplete(buildComplete);
                        LOGGER.info("Updated block entity with persisted data at {}", tablePos);
                    }
                }
                
                LOGGER.info("Loaded table build status for player {} at {}: {}/{} blocks, complete={}", 
                           playerId, tablePos, currentProgress, totalBlocks, buildComplete);
            }
        }
        
        // Load active keys
        if (rootTag.contains("ActiveKeys")) {
            ListTag activeKeysTag = rootTag.getList("ActiveKeys", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < activeKeysTag.size(); i++) {
                CompoundTag keyTag = activeKeysTag.getCompound(i);
                
                // Get key info
                UUID playerId = keyTag.getUUID("PlayerId");
                String dimensionStr = keyTag.getString("Dimension");
                ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, 
                    new ResourceLocation(dimensionStr)
                );
                BlockPos tablePos = new BlockPos(
                    keyTag.getInt("PosX"),
                    keyTag.getInt("PosY"),
                    keyTag.getInt("PosZ")
                );
                
                // Create the table key
                CreativeDimensionManager.TableKey tableKey = 
                    CreativeDimensionManager.createTableKey(playerId, dimension, tablePos);
                
                // Set as active
                CreativeDimensionManager.setActiveTableKey(playerId, tableKey);
            }
        }
        
        // Load game modes
        if (rootTag.contains("GameModes")) {
            CompoundTag gameModesTag = rootTag.getCompound("GameModes");
            
            for (String key : gameModesTag.getAllKeys()) {
                try {
                    UUID playerId = UUID.fromString(key);
                    int gameTypeId = gameModesTag.getInt(key);
                    GameType gameType = GameType.byId(gameTypeId);
                    
                    CreativeDimensionManager.setOriginalGameMode(playerId, gameType);
                } catch (Exception e) {
                    LOGGER.error("Error loading game mode for player {}: {}", key, e.getMessage());
                }
            }
        }
        
        // Load return portals
        if (rootTag.contains("ReturnPortals")) {
            ListTag returnPortalsTag = rootTag.getList("ReturnPortals", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < returnPortalsTag.size(); i++) {
                CompoundTag portalTag = returnPortalsTag.getCompound(i);
                
                // Get portal position
                BlockPos portalPos = new BlockPos(
                    portalTag.getInt("PortalX"),
                    portalTag.getInt("PortalY"),
                    portalTag.getInt("PortalZ")
                );
                
                // Get key info
                UUID playerId = portalTag.getUUID("PlayerId");
                String dimensionStr = portalTag.getString("Dimension");
                ResourceKey<Level> dimension = ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, 
                    new ResourceLocation(dimensionStr)
                );
                BlockPos tablePos = new BlockPos(
                    portalTag.getInt("PosX"),
                    portalTag.getInt("PosY"),
                    portalTag.getInt("PosZ")
                );
                
                // Create the table key
                CreativeDimensionManager.TableKey tableKey = 
                    CreativeDimensionManager.createTableKey(playerId, dimension, tablePos);
                
                // Set the return portal
                CreativeDimensionManager.setReturnPortal(portalPos, tableKey);
            }
        }
        
        LOGGER.info("Successfully loaded table connections data");
    } catch (Exception e) {
        LOGGER.error("Failed to load table connections data", e);
    }
}
}