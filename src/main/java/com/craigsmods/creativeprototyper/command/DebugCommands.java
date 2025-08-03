/**
 * Create a debug command class to help troubleshoot data persistence issues
 */
package com.craigsmods.creativeprototyper.command;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.util.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

import java.io.File;
import java.util.UUID;

/**
 * Debug commands to investigate player data issues
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class DebugCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Register the commands
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Debug command
        LiteralArgumentBuilder<CommandSourceStack> debugCommand = 
            Commands.literal("cpdb")
                .requires(source -> source.hasPermission(2)) // Require level 2 permission (ops)
                .then(Commands.literal("status")
                    .executes(DebugCommands::checkStatus))
                .then(Commands.literal("files")
                    .executes(DebugCommands::listFiles))
                .then(Commands.literal("fix")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(DebugCommands::fixPlayerData)))
                .then(Commands.literal("metadata")
                    .executes(DebugCommands::checkMetadata));
        
        dispatcher.register(debugCommand);
        
        LOGGER.info("Registered CreativePrototyper debug commands");
    }
    
    /**
     * Check status of player data tracking
     */
    private static int checkStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        source.sendSuccess(() -> Component.literal("=== Player Data Status ==="), false);
        
        // Output tracked states
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerId = player.getUUID();
            boolean inCreative = PlayerDataManager.isPlayerInCreative(playerId);
            
            source.sendSuccess(() -> Component.literal("Player " + player.getName().getString() + 
                                            ": " + (inCreative ? "CREATIVE" : "SURVIVAL")), false);
        }
        
        return 1;
    }
    
    /**
     * List player data files
     */
    private static int listFiles(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        
        try {
            File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
            File dataDir = new File(playerDataDir, "creative_prototyper_data");
            
            if (!dataDir.exists()) {
                source.sendSuccess(() -> Component.literal("No data directory found!"), false);
                return 0;
            }
            
            File[] files = dataDir.listFiles();
            if (files == null || files.length == 0) {
                source.sendSuccess(() -> Component.literal("No player data files found!"), false);
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("=== Player Data Files ==="), false);
            for (File file : files) {
                // Try to read the file to check if it's valid
                boolean valid = true;
                String error = "";
                
                if (file.getName().endsWith(".dat")) {
                    try {
                        CompoundTag nbt = NbtIo.readCompressed(file);
                        // Check if it has expected data
                        if (file.getName().contains("creative") && !nbt.contains("playerGameType")) {
                            valid = false;
                            error = " (Missing game type!)";
                        }
                    } catch (Exception e) {
                        valid = false;
                        error = " (ERROR: " + e.getMessage() + ")";
                    }
                }
                
                String fileInfo = file.getName() + " - " + (file.length() / 1024) + "KB" + 
                                 (valid ? "" : " [INVALID" + error + "]");
                
                source.sendSuccess(() -> Component.literal(fileInfo), false);
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error listing files: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }
    
    /**
     * Fix player data issues
     */
    private static int fixPlayerData(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            UUID playerId = player.getUUID();
            
            // Force save current state
            source.sendSuccess(() -> Component.literal("Forcing inventory save for " + 
                                           player.getName().getString()), false);
            
            // Force save of player data
            player.getServer().getPlayerList().saveAll();
            
            // Update player state tracking
            if (player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.CREATIVE) {
                // Player is in creative mode, update creative data
                PlayerDataManager.saveCreativePlayerData(player);
                PlayerDataManager.playerInCreative.put(playerId, true);
                source.sendSuccess(() -> Component.literal("Updated creative data for player"), false);
            } else {
                // Player is in survival mode, update survival data
                PlayerDataManager.saveVanillaPlayerData(player);
                PlayerDataManager.playerInCreative.remove(playerId);
                source.sendSuccess(() -> Component.literal("Updated survival data for player"), false);
            }
            
            // Save metadata
            PlayerDataManager.savePlayerEntryMetadata();
            
            source.sendSuccess(() -> Component.literal("Player data fix complete!"), false);
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error fixing player data: " + e.getMessage()));
            return 0;
        }
        
        return 1;
    }

    /**
 * Check metadata file contents
 */
private static int checkMetadata(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    MinecraftServer server = source.getServer();
    
    try {
        File playerDataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile();
        File dataDir = new File(playerDataDir, "creative_prototyper_data");
        File metadataFile = new File(dataDir, "creative_players.dat");
        
        if (!metadataFile.exists()) {
            source.sendSuccess(() -> Component.literal("No metadata file found!"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("=== Metadata File Contents ==="), false);
        source.sendSuccess(() -> Component.literal("File size: " + metadataFile.length() + " bytes"), false);
        
        try {
            CompoundTag nbt = NbtIo.readCompressed(metadataFile);
            
            // Check CreativePlayers tag
            if (nbt.contains("CreativePlayers")) {
                CompoundTag playersTag = nbt.getCompound("CreativePlayers");
                source.sendSuccess(() -> Component.literal("CreativePlayers entries: " + 
                                           playersTag.getAllKeys().size()), false);
                
                for (String key : playersTag.getAllKeys()) {
                    source.sendSuccess(() -> Component.literal("  Player " + key + ": " + 
                                               playersTag.getBoolean(key)), false);
                }
            } else {
                source.sendSuccess(() -> Component.literal("No CreativePlayers tag found!"), false);
            }
            
            // Check PlayerState tag
            if (nbt.contains("PlayerState")) {
                CompoundTag stateTag = nbt.getCompound("PlayerState");
                source.sendSuccess(() -> Component.literal("PlayerState entries: " + 
                                           stateTag.getAllKeys().size()), false);
                
                for (String key : stateTag.getAllKeys()) {
                    source.sendSuccess(() -> Component.literal("  Player " + key + ": " + 
                                               stateTag.getBoolean(key)), false);
                }
            } else {
                source.sendSuccess(() -> Component.literal("No PlayerState tag found!"), false);
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error reading metadata: " + e.getMessage()));
            return 0;
        }
        
    } catch (Exception e) {
        source.sendFailure(Component.literal("Error checking metadata: " + e.getMessage()));
        return 0;
    }
    
    return 1;
}




}