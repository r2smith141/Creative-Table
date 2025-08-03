package com.craigsmods.creativeprototyper.util;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.dimension.TablePersistenceManager;
import com.craigsmods.creativeprototyper.util.PlayerDataManager;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/**
 * Handles server lifecycle events for data saving/loading
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class ServerLifecycleHandler {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static boolean dataLoaded = false;
    
    /**
     * Load persistent data early in server startup process
     */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        LOGGER.info("Server about to start, preparing Creative Prototyper data");
        MinecraftServer server = event.getServer();
        
        try {
            // Clear any existing data first
            CreativeDimensionManager.clearAllData();
            dataLoaded = false;
        } catch (Exception e) {
            LOGGER.error("Error clearing data during server start", e);
        }
    }
    
    /**
     * Load persistent data when the server starts
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (dataLoaded) {
            LOGGER.info("Data already loaded, skipping");
            return;
        }
        
        LOGGER.info("Server started, loading Creative Prototyper data");
        MinecraftServer server = event.getServer();
        
        try {
            // Load player entry data
            PlayerDataManager.loadPlayerEntryMetadata();
            
            // Load table connections
            TablePersistenceManager.loadTableConnections(server);
            
            dataLoaded = true;
            
            // Log current state

        } catch (Exception e) {
            LOGGER.error("Error loading data during server start", e);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerDataManager.onPlayerLogout(player);
        }
    }
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer player){
          PlayerDataManager.onPlayerLogin(player);  
        }
    }
    /**
     * Save persistent data when the server stops
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping, saving Creative Prototyper data");
        MinecraftServer server = event.getServer();

        try {

            TablePersistenceManager.saveTableConnections(server);
            
        } catch (Exception e) {
            LOGGER.error("Error saving data during server stop", e);
        }
    }
}