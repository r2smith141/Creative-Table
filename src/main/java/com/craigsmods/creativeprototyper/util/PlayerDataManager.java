package com.craigsmods.creativeprototyper.util;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles complete player data swapping for creative dimension
 */
public class PlayerDataManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_DIR = "creative_prototyper_data";
    
    // Map to track if a player has entered creative mode before
    private static final Map<UUID, Boolean> playerHasCreativeData = new HashMap<>();
    
    // Map to track current player dimension state
    public static final Map<UUID, Boolean> playerInCreative = new HashMap<>();
    

    /**
     * Switches the player to their creative dimension state
     */
    public static void switchToCreativeDimension(ServerPlayer player, BlockPos tablePos) {
        UUID playerId = player.getUUID();
        LOGGER.info("Switching player {} to creative dimension state", player.getName().getString());
        try {
            // 1. Save current state as vanilla (explicitly survival mode)
            saveVanillaPlayerData(player); // Uses new specific save

            // 2. Load creative state
             boolean hasCreativeData = playerHasCreativeData.getOrDefault(playerId, false);
             File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
             File dataDir = new File(playerDataDir, DATA_DIR);
             File creativeFile = new File(dataDir, player.getUUID() + ".creative.dat");
             File creativeBackup = new File(dataDir, creativeFile.getName() + ".bak");

            // Check if creative data exists (file or backup)
             if (!creativeFile.exists() && !creativeBackup.exists()) {
                 LOGGER.info("First creative entry or data missing for player {}, creating fresh creative data.", player.getName().getString());
                 // Create a 'default' creative state based on current player, but force gamemode
                 saveSpecificPlayerData(player, creativeFile, GameType.CREATIVE); // Save current state but mark as Creative
                playerHasCreativeData.put(playerId, true);
                 // Now load it back to apply it (redundant but ensures consistency with load path)
                 loadCreativePlayerData(player);
            } else {
                // Load existing creative data
                 loadCreativePlayerData(player); // Uses new specific load
            }

            // 3. Explicitly set GameMode AFTER loading - loadSpecificPlayerData handles this now, but double-check
            // player.setGameMode(GameType.CREATIVE); // This might be redundant if loadSpecificPlayerData works reliably

            // 4. Update tracking and metadata
            playerInCreative.put(playerId, true);
            savePlayerEntryMetadata(); // Save status change

            LOGGER.info("Player {} switched to creative state successfully", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Error switching {} to creative dimension: {}", player.getName().getString(), e.getMessage(), e);
            // Consider attempting to restore vanilla state on error
             try {
                LOGGER.warn("Attempting to restore vanilla state for {} after error.", player.getName().getString());
                 loadVanillaPlayerData(player);
             } catch (IOException ioe) {
                 LOGGER.error("FATAL: Failed to restore vanilla state for {} after error: {}", player.getName().getString(), ioe.getMessage());
                 // Player might be in a broken state here. Maybe kick them?
             }
        }
    }
    
   /**
     * Returns the player to their survival dimension state
     */
    public static void returnToSurvivalDimension(ServerPlayer player, BlockPos tablePos) {
        UUID playerId = player.getUUID();
        LOGGER.info("Returning player {} to survival dimension state", player.getName().getString());
        try {
            // 1. Save current state as creative (explicitly creative mode)
            saveCreativePlayerData(player); // Uses new specific save

            // 2. Load vanilla state
            loadVanillaPlayerData(player); // Uses new specific load

            // 3. Explicitly set GameMode AFTER loading - loadSpecificPlayerData handles this now, but double-check
            // player.setGameMode(GameType.SURVIVAL); // This might be redundant if loadSpecificPlayerData works reliably

            // 4. Update tracking and metadata
            playerInCreative.put(playerId, false);
            savePlayerEntryMetadata(); // Save status change

            LOGGER.info("Player {} returned to survival state successfully", player.getName().getString());
        } catch (Exception e) {
             LOGGER.error("Error returning {} to survival dimension: {}", player.getName().getString(), e.getMessage(), e);
             // Consider attempting to restore creative state on error
             try {
                 LOGGER.warn("Attempting to restore creative state for {} after error.", player.getName().getString());
                 loadCreativePlayerData(player);
             } catch (IOException ioe) {
                 LOGGER.error("FATAL: Failed to restore creative state for {} after error: {}", player.getName().getString(), ioe.getMessage());
                 // Player might be in a broken state here. Maybe kick them?
             }
        }
    }
    /**
     * When player leaves the game, save their current state using specific NBT data.
     */
    public static void onPlayerLogout(ServerPlayer player) {
        UUID playerId = player.getUUID();
        try {
            if (playerInCreative.getOrDefault(playerId, false)) {
                // They're in creative mode, save creative state snapshot
                LOGGER.info("Saving final creative state snapshot for disconnecting player {}", player.getName().getString());
                saveCreativePlayerData(player); // Uses new specific save
            } else {
                // They're in survival mode, save survival state snapshot
                LOGGER.info("Saving final survival state snapshot for disconnecting player {}", player.getName().getString());
                saveVanillaPlayerData(player); // Uses new specific save
            }
        } catch (Exception e) {
            LOGGER.error("Error saving player state snapshot on logout for {}: {}", player.getName().getString(), e.getMessage(), e);
        }
        // Metadata is saved separately by ServerLifecycleHandler.onServerStopping or potentially here if needed immediately

    }
        /**
     * Saves the player's current state as their "survival" state using specific NBT data.
     * Ensures GameMode is saved as SURVIVAL.
     */
    public static void saveVanillaPlayerData(ServerPlayer player) throws IOException {
        
        LOGGER.info("Saving vanilla player data snapshot for {}", player.getName().getString());
        File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, DATA_DIR);
        dataDir.mkdirs();
        File survivalFile = new File(dataDir, player.getUUID() + ".survival.dat");
        saveSpecificPlayerData(player, survivalFile, GameType.SURVIVAL);
        LOGGER.debug("Vanilla player data snapshot saved to {}", survivalFile.getAbsolutePath());
    }

    
    /**
     * Create initial creative player data from survival data
     */
    private static void createCreativeDataFromSurvival(ServerPlayer player) throws IOException {
        // Get the vanilla player.dat file
        File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File vanillaFile = new File(playerDataDir, player.getUUID() + ".dat");
        
        File dataDir = new File(playerDataDir, DATA_DIR);
        dataDir.mkdirs();
        
        File creativeFile = new File(dataDir, player.getUUID() + ".creative.dat");
        Files.copy(vanillaFile.toPath(), creativeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        LOGGER.debug("Created initial creative player data at {}", creativeFile.getAbsolutePath());
        
        // Modify copied file to set GameType to creative
        try {
            CompoundTag nbt = NbtIo.readCompressed(creativeFile);
            
            // Set game mode to creative
            if (nbt.contains("playerGameType")) {
                nbt.putInt("playerGameType", GameType.CREATIVE.getId());
                LOGGER.debug("Set creative game mode in player data");
            }
            
            // Write modified data
            NbtIo.writeCompressed(nbt, creativeFile);
        } catch (Exception e) {
            LOGGER.error("Error modifying creative player data: {}", e.getMessage());
        }
    }
    
    /**
     * Saves the player's current state as their "creative" state using specific NBT data.
     * Ensures GameMode is saved as CREATIVE.
     */
    public static void saveCreativePlayerData(ServerPlayer player) throws IOException {
        LOGGER.info("Saving creative player data snapshot for {}", player.getName().getString());
       File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
       File dataDir = new File(playerDataDir, DATA_DIR);
       dataDir.mkdirs();
       File creativeFile = new File(dataDir, player.getUUID() + ".creative.dat");
       // Use the new helper, explicitly setting intended game mode to CREATIVE
       saveSpecificPlayerData(player, creativeFile, GameType.CREATIVE);
       LOGGER.debug("Creative player data snapshot saved to {}", creativeFile.getAbsolutePath());
   }


       /**
     * Saves specific player data (Inventory, Health, XP, GameMode, etc., plus any mod data included by saveWithoutId) to a file.
     * Ensures the *intended* GameMode for this snapshot is stored correctly.
     * @param player The player whose data is being saved.
     * @param targetFile The file to save the NBT data to.
     * @param intendedGameMode The GameMode that should be stored in this data snapshot (e.g., SURVIVAL for the survival snapshot).
     * @throws IOException If saving fails.
     */
    private static void saveSpecificPlayerData(ServerPlayer player, File targetFile, GameType intendedGameMode) throws IOException {
        LOGGER.debug("Saving specific player data for {} to {} with intended GameMode {}", player.getName().getString(), targetFile.getName(), intendedGameMode.getName());
        CompoundTag dataTag = new CompoundTag();

        // --- Save ALL data provided by the standard save method ---
        // This captures everything the game normally saves for persistence,
        // including inventory, stats, advancements, health, XP, position, velocity,
        // effects, and potentially data added by other mods via capabilities or mixins.
        player.saveWithoutId(dataTag);
        LOGGER.debug("Captured base player NBT data. Root keys: {}", dataTag.getAllKeys());

        // --- Crucially, ensure the intended GameMode is correctly set in the tag ---
        // Override whatever game mode might be in the captured tag with the
        // one appropriate for the file we're saving (survival.dat or creative.dat).
        dataTag.putInt("playerGameType", intendedGameMode.getId());
        LOGGER.debug("Ensured 'playerGameType' is set to {} ({}) in the tag.", intendedGameMode.getName(), intendedGameMode.getId());

        // --- Save the complete tag using temporary file logic for safety ---
        File playerDataDir = targetFile.getParentFile(); // Assumes targetFile is in the DATA_DIR
        File tempFile = new File(playerDataDir, targetFile.getName() + ".tmp");
        File backupFile = new File(playerDataDir, targetFile.getName() + ".bak");

        try {
     
            NbtIo.writeCompressed(dataTag, tempFile);
            LOGGER.debug("Wrote data tag to temp file: {}", tempFile.getAbsolutePath());

     
            if (!tempFile.exists() || tempFile.length() == 0) {
                throw new IOException("Failed to write temporary player data file or file is empty.");
            }
            CompoundTag verifyNbt = NbtIo.readCompressed(tempFile);
             if (!verifyNbt.contains("playerGameType")) { // Check if essential data is present
                 LOGGER.error("Temporary data file {} is missing playerGameType tag after write!", tempFile.getName());
                throw new IOException("Temporary data file verification failed (missing playerGameType).");
             }
             
            LOGGER.debug("Verified temp file successfully.");

            // Backup existing file
            if (targetFile.exists()) {
                if (backupFile.exists()) {
                    if (!backupFile.delete()) {
                        LOGGER.warn("Could not delete old backup file: {}", backupFile.getName());
                    }
                }
                if (!targetFile.renameTo(backupFile)) {
                     LOGGER.warn("Could not create backup of existing data file: {}", targetFile.getName());
                } else {
                    LOGGER.debug("Created backup: {}", backupFile.getName());
                }
            }

            // Rename temp file to actual target file
            if (!tempFile.renameTo(targetFile)) {
                 
                 if (backupFile.exists()) {
                     backupFile.renameTo(targetFile);
                 }
                 throw new IOException("Failed to rename temporary file " + tempFile.getName() + " to " + targetFile.getName());
            }

            LOGGER.debug("Successfully saved specific player data (full tag) to {}", targetFile.getAbsolutePath());

        } catch (Exception e) {
            
             if (tempFile.exists()) { tempFile.delete(); }
             if (backupFile.exists() && !targetFile.exists()) {
                LOGGER.warn("Attempting to restore backup {} due to error.", backupFile.getName());
                 backupFile.renameTo(targetFile);
             }
            if (e instanceof IOException) { throw e; }
            else { throw new IOException("Error during specific player data save: " + e.getMessage(), e); }
        }
    }


    /**
     * Loads specific player data (Inventory, Health, XP, GameMode, etc.) from a file
     * and applies it directly to the player object.
     * @param player The player to apply the data to.
     * @param sourceFile The file containing the NBT data.
     * @throws IOException If reading or applying data fails.
     */
    private static void loadSpecificPlayerData(ServerPlayer player, File sourceFile) throws IOException {
        LOGGER.debug("Loading specific player data for {} from {}", player.getName().getString(), sourceFile.getName());
        if (!sourceFile.exists()) {
            // Attempt to load backup if main file is missing
            File backupFile = new File(sourceFile.getParentFile(), sourceFile.getName() + ".bak");
             if (backupFile.exists()) {
                 LOGGER.warn("Main data file {} not found, attempting to load backup {}.", sourceFile.getName(), backupFile.getName());
                 sourceFile = backupFile;
             } else {
                throw new IOException("Player data file not found: " + sourceFile.getAbsolutePath() + " and no backup exists.");
            }
        }

        try {
            CompoundTag dataTag = NbtIo.readCompressed(sourceFile);
            LOGGER.debug("Read data tag from {}. Contains keys: {}", sourceFile.getName(), dataTag.getAllKeys());

            // --- Apply the loaded data directly to the player ---
            // This loads Inventory, Health, XP, EnderItems, etc. from the tag.
            // It also loads GameMode, but we will override it explicitly afterwards for clarity.
            player.load(dataTag);

            // --- Explicitly set the GameMode from the loaded tag ---
            // Although player.load() might handle it, setting it explicitly ensures correctness.
            if (dataTag.contains("playerGameType", Tag.TAG_INT)) {
                int gameTypeId = dataTag.getInt("playerGameType");
                GameType gameType = GameType.byId(gameTypeId); // Default to survival if ID is invalid
                player.setGameMode(gameType);
                LOGGER.debug("Applied GameMode {} from data tag.", gameType.getName());
            } else {
                LOGGER.warn("Data tag from {} missing 'playerGameType'. GameMode might not be restored correctly.", sourceFile.getName());

            }

            // --- Refresh client-side ---
            player.getInventory().setChanged(); 
            player.inventoryMenu.broadcastChanges(); 
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(player.experienceProgress, player.totalExperience, player.experienceLevel)); // Sync XP
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel())); // Sync Health/Food
            player.getServer().getPlayerList().sendPlayerPermissionLevel(player); 
            player.getServer().getPlayerList().sendAllPlayerInfo(player); 

            LOGGER.debug("Successfully loaded and applied specific player data.");

        } catch (Exception e) {
            if (e instanceof IOException) { throw e; }
             else { throw new IOException("Error during specific player data load from " + sourceFile.getName() + ": " + e.getMessage(), e); }
        }
    }
    /**
     * Loads the player's "creative" state using specific NBT data.
     */
    private static void loadCreativePlayerData(ServerPlayer player) throws IOException {
        LOGGER.info("Loading creative player data snapshot for {}", player.getName().getString());
        File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, DATA_DIR);
        File creativeFile = new File(dataDir, player.getUUID() + ".creative.dat");

        if (!creativeFile.exists() && !new File(dataDir, creativeFile.getName() + ".bak").exists()) {
             LOGGER.warn("Creative player data file ({}) not found and no backup exists for {}. Cannot load creative state.", creativeFile.getName(), player.getName().getString());

            throw new IOException("Creative player data file not found and no backup exists."); 
        }


        loadSpecificPlayerData(player, creativeFile);
        LOGGER.debug("Creative player data snapshot loaded for {}", player.getName().getString());

    }
    
    /**
     * Loads the player's "survival" state using specific NBT data.
     */
    private static void loadVanillaPlayerData(ServerPlayer player) throws IOException {
        LOGGER.info("Loading vanilla player data snapshot for {}", player.getName().getString());
        File playerDataDir = player.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, DATA_DIR);
        File survivalFile = new File(dataDir, player.getUUID() + ".survival.dat");

         if (!survivalFile.exists() && !new File(dataDir, survivalFile.getName() + ".bak").exists()) {
             LOGGER.warn("Survival player data file ({}) not found and no backup exists for {}. Cannot load survival state.", survivalFile.getName(), player.getName().getString());

            throw new IOException("Survival player data file not found and no backup exists."); 
         }


        loadSpecificPlayerData(player, survivalFile);
        LOGGER.debug("Vanilla player data snapshot loaded for {}", player.getName().getString());

    }

    


/**
 * Load player entry metadata with explicit UUID stringify/parse fix
 */
public static void loadPlayerEntryMetadata() {
    try {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.error("Cannot load player metadata - server is null");
            return;
        }
        
        File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, DATA_DIR);
        File metadataFile = new File(dataDir, "creative_players.dat");
        
        if (!metadataFile.exists()) {
            LOGGER.info("No player metadata file found, starting fresh");
 
            playerHasCreativeData.clear();
            playerInCreative.clear();
            return;
        }
        

        try {
            CompoundTag rawNbt = NbtIo.readCompressed(metadataFile);

            LOGGER.info("Raw metadata file tags: {}", rawNbt.getAllKeys());
            for (String key : rawNbt.getAllKeys()) {
                LOGGER.info("  Tag '{}' is of type {}", key, rawNbt.get(key).getType());
            }
            

            if (rawNbt.contains("PlayerState")) {
                Tag playerStateTag = rawNbt.get("PlayerState");
                LOGGER.info("  PlayerState tag type: {}", playerStateTag.getType());
                
                if (playerStateTag instanceof CompoundTag stateCompound) {
                    LOGGER.info("  PlayerState contains {} entries", stateCompound.getAllKeys().size());
                    
                   
                    for (String key : stateCompound.getAllKeys()) {
                        LOGGER.info("    Key: {}, Value type: {}, Value: {}", 
                                   key, stateCompound.get(key).getType(), stateCompound.getBoolean(key));
                    }
                } else {
                    LOGGER.warn("  PlayerState is not a CompoundTag! This is a problem.");
                }
            } else {
                LOGGER.warn("  No PlayerState tag found in metadata file!");
            }
        } catch (Exception e) {
            LOGGER.error("Error reading raw NBT: {}", e.getMessage());
        }
        

    } catch (Exception e) {
        LOGGER.error("Error loading player metadata: {}", e.getMessage(), e);
        

        playerHasCreativeData.clear();
        playerInCreative.clear();
    }
}

/**
 * Enhanced metadata saving with corruption prevention
 */
public static void savePlayerEntryMetadata() {
    try {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.error("Cannot save player metadata - server is null");
            return;
        }
        

        LOGGER.info("Preparing to save metadata - playerHasCreativeData: {} entries, playerInCreative: {} entries", 
                   playerHasCreativeData.size(), playerInCreative.size());
        

        if (playerInCreative.isEmpty()) {
            LOGGER.warn("playerInCreative map is empty when saving metadata!");
        } else {
            LOGGER.info("Current playerInCreative entries:");
            for (Map.Entry<UUID, Boolean> entry : playerInCreative.entrySet()) {
                LOGGER.info("  Player UUID: {} = {}", entry.getKey(), entry.getValue());
            }
        }
        
        File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, DATA_DIR);
        dataDir.mkdirs();
        
        File metadataFile = new File(dataDir, "creative_players.dat");
        File tempFile = new File(dataDir, "creative_players.dat.tmp");
        

        CompoundTag nbt = new CompoundTag();
        

        CompoundTag playersTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : playerHasCreativeData.entrySet()) {
            String uuidString = entry.getKey().toString();
            playersTag.putBoolean(uuidString, entry.getValue());
        }
        nbt.put("CreativePlayers", playersTag);
        

        CompoundTag stateTag = new CompoundTag();
        for (Map.Entry<UUID, Boolean> entry : playerInCreative.entrySet()) {
            String uuidString = entry.getKey().toString();
            stateTag.putBoolean(uuidString, entry.getValue());
        }
        nbt.put("PlayerState", stateTag);
        
        // Debug: Log the NBT structure before writing
        LOGGER.info("NBT to save - CreativePlayers: {} entries, PlayerState: {} entries",
                   playersTag.getAllKeys().size(), stateTag.getAllKeys().size());
        
        // Write to a temporary file first
        NbtIo.writeCompressed(nbt, tempFile);
        
        // Verify the temporary file
        if (!tempFile.exists() || tempFile.length() == 0) {
            LOGGER.error("Failed to write temporary metadata file");
            return;
        }
        

        try {
            CompoundTag verifyNbt = NbtIo.readCompressed(tempFile);
            if (!verifyNbt.contains("CreativePlayers") || !verifyNbt.contains("PlayerState")) {
                LOGGER.error("Temporary metadata file is missing required tags");
                return;
            }
            

            if (verifyNbt.contains("PlayerState")) {
                CompoundTag verifyStateTag = verifyNbt.getCompound("PlayerState");
                LOGGER.info("Verified PlayerState tag has {} entries", verifyStateTag.getAllKeys().size());
                

                if (verifyStateTag.getAllKeys().isEmpty() && !playerInCreative.isEmpty()) {
                    LOGGER.warn("PlayerState tag is empty in verification but playerInCreative had entries!");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to verify temporary metadata file: {}", e.getMessage());
            return;
        }
        

        if (metadataFile.exists()) {
            File backupFile = new File(dataDir, "creative_players.dat.bak");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            metadataFile.renameTo(backupFile);
        }
        

        boolean success = tempFile.renameTo(metadataFile);
        
        if (success) {
            LOGGER.info("Successfully saved player metadata ({} bytes)", metadataFile.length());
        } else {
            LOGGER.error("Failed to rename temp file to final metadata file");
        }
    } catch (Exception e) {
        LOGGER.error("Error saving player metadata: {}", e.getMessage(), e);
    }
}
/**
 * Helper to find a player by UUID
 */
private static ServerPlayer findPlayerByUUID(MinecraftServer server, UUID uuid) {
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
        if (player.getUUID().equals(uuid)) {
            return player;
        }
    }
    return null;
}

    /**
     * Improved player login handler. Loads specific NBT data if needed.
     */
    public static void onPlayerLogin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        String playerUuidString = playerId.toString();
        LOGGER.info("Player login detected: {} (UUID: {})", player.getName().getString(), playerUuidString);
    

        boolean isInCreativeDim = player.level().dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY);
        

        boolean wasInCreative = false;
        if (playerInCreative.containsKey(playerId)) {
            wasInCreative = playerInCreative.get(playerId);
            LOGGER.info("Found player state in metadata map: inCreative = {}", wasInCreative);
        } else {
            LOGGER.info("Player {} not found in loaded playerInCreative map. Assuming survival.", player.getName().getString());
            playerInCreative.put(playerId, false); // Add to map as survival if not present
        }
        

        if (isInCreativeDim && !wasInCreative) {

            LOGGER.warn("State mismatch detected! Player {} is in creative dimension but marked as survival. Correcting state.", 
                       player.getName().getString());
            
            playerInCreative.put(playerId, true); // Update the state flag
            
            // Load creative data for the player
            try {
                loadCreativePlayerData(player);
                LOGGER.info("Successfully loaded creative state for player in creative dimension");
            } catch (IOException e) {
                LOGGER.error("Failed to load creative data: {}. Creating fresh creative state.", e.getMessage());
                
                // If no creative data exists, create it based on current inventory
                player.setGameMode(GameType.CREATIVE);
                try {
                    saveCreativePlayerData(player);
                    LOGGER.info("Created new creative state based on current inventory");
                } catch (IOException saveError) {
                    LOGGER.error("Failed to save new creative state: {}", saveError.getMessage());
                }
            }
        } else if (!isInCreativeDim && wasInCreative) {
            // Player is in survival dimension but state says creative - fix it
            LOGGER.warn("State mismatch detected! Player {} is in normal dimension but marked as creative. Correcting state.", 
                       player.getName().getString());
            
            playerInCreative.put(playerId, false); // Update the state flag
            
            // Load survival data for the player
            try {
                loadVanillaPlayerData(player);
                LOGGER.info("Successfully loaded survival state for player in normal dimension");
            } catch (IOException e) {
                LOGGER.error("Failed to load survival data: {}. Setting default survival state.", e.getMessage());
                
                // If no survival data exists, set to default survival
                player.setGameMode(GameType.SURVIVAL);
            }
        } else if (wasInCreative && isInCreativeDim) {
            // Correct state, but make sure the player has creative data loaded
            LOGGER.info("Player {} is correctly in creative state and dimension. Ensuring creative mode.", 
                       player.getName().getString());
            
            try {
                loadCreativePlayerData(player);
            } catch (IOException e) {
                LOGGER.error("Error loading creative state: {}", e.getMessage());
                // Ensure they're at least in creative mode as fallback
                player.setGameMode(GameType.CREATIVE);
            }
        } else {
            // Correct survival state, normal operation
            LOGGER.debug("Player {} is in normal state and dimension. No correction needed.", 
                       player.getName().getString());
        }
        
        // Always save metadata to ensure it's up to date
        savePlayerEntryMetadata();
        
        LOGGER.info("Player login handling complete. Current state: {}", 
                   playerInCreative.getOrDefault(playerId, false) ? "Creative" : "Survival");
    }
    /**
     * Check if a player is currently in creative dimension state
     */
    public static boolean isPlayerInCreative(UUID playerId) {
        return playerInCreative.getOrDefault(playerId, false);
    }
    

    
}