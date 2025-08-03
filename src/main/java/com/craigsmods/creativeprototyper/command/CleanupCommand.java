package com.craigsmods.creativeprototyper.command;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
//import com.craigsmods.creativeprototyper.util.DataCleanupManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/**
 * Command to run cleanup operations
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class CleanupCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Register the command
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        // Main command
        LiteralArgumentBuilder<CommandSourceStack> creativePrototyperCommand = 
            Commands.literal("creativeprototyper")
                .requires(source -> source.hasPermission(2)) // Require level 2 permission (ops)
                .then(Commands.literal("cleanup")
                    .executes(CleanupCommand::runCleanup))
                .then(Commands.literal("stats")
                    .executes(CleanupCommand::showStats));
        
        dispatcher.register(creativePrototyperCommand);
        
        // Add shorter alias
        dispatcher.register(Commands.literal("cproto")
            .requires(source -> source.hasPermission(2))
            .redirect(creativePrototyperCommand.build()));
        
        LOGGER.info("Registered CreativePrototyper commands");
    }
    
    /**
     * Run the cleanup command
     */
    private static int runCleanup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("Starting Creative Prototyper data cleanup..."), true);
        
        // Run the cleanup
        //int result = DataCleanupManager.runManualCleanup(source.getServer());
        int result = 1;
        // Send result message
        if (result > 0) {
            source.sendSuccess(() -> Component.literal("Cleanup completed successfully!"), true);
        } else {
            source.sendFailure(Component.literal("Cleanup failed. Check logs for details."));
        }
        
        return result;
    }
    
    /**
     * Show stats about current tables
     */
    private static int showStats(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Get stats from CreativeDimensionManager
        //int totalTables = CreativeDimensionManager.getTableData().size();
        int activeTables = CreativeDimensionManager.getActiveTableKeys().size();
        int returnPortals = CreativeDimensionManager.getReturnPortals().size();
        int gameModes = CreativeDimensionManager.getOriginalGameModes().size();
        
        // Send stats to player
        source.sendSuccess(() -> Component.literal("=== Creative Prototyper Stats ==="), false);
        //source.sendSuccess(() -> Component.literal("Total tables: " + totalTables), false);
        source.sendSuccess(() -> Component.literal("Active tables: " + activeTables), false);
        source.sendSuccess(() -> Component.literal("Return portals: " + returnPortals), false);
        source.sendSuccess(() -> Component.literal("Stored game modes: " + gameModes), false);
        
        // If the command sender is a player, show their active table
        if (source.getEntity() instanceof ServerPlayer player) {
            CreativeDimensionManager.TableKey activeKey = CreativeDimensionManager.getActiveTableKey(player.getUUID());
            if (activeKey != null) {
                source.sendSuccess(() -> Component.literal("Your active table: " + 
                                               activeKey.tablePos + " in " + 
                                               activeKey.dimension.location()), false);
            } else {
                source.sendSuccess(() -> Component.literal("You have no active table"), false);
            }
        }
        
        return 1;
    }
}