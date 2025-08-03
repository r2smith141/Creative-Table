package com.craigsmods.creativeprototyper.config;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.util.BannedBlocksManager;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativePrototyperConfig {
    public static class Common {
        // Scanning settings
        public final ForgeConfigSpec.IntValue defaultScanRadius;
        public final ForgeConfigSpec.IntValue maxScanRadius;
        public final ForgeConfigSpec.IntValue blockEntitiesPerTick;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> bannedBlocks;
        // Block placement settings
        public final ForgeConfigSpec.IntValue blocksPerTick;
        
        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Common configuration settings for Creative Prototyper")
                   .push("scanning");

            defaultScanRadius = builder
                .comment("Default radius (in blocks) to scan around the Creative Table")
                .defineInRange("defaultScanRadius", 16, 1, 128);
                
            maxScanRadius = builder
                .comment("Maximum allowed scan radius (for server performance)")
                .defineInRange("maxScanRadius", 64, 1, 256);
                
            blockEntitiesPerTick = builder
                .comment("Number of block entities to process per tick during scanning")
                .defineInRange("blockEntitiesPerTick", 10, 1, 100);
                
            builder.pop();
            
            builder.comment("Block placement settings")
                   .push("placement");
                   
            blocksPerTick = builder
                .comment("Number of blocks to place per tick in the creative dimension")
                .defineInRange("blocksPerTick", 100, 1, 1000);
                
            builder.pop();
            builder.comment("Block restrictions")
            .push("restrictions");
            
            bannedBlocks = builder.comment("List of blocks that cannot be placed in the creative dimension")
            .defineListAllowEmpty(List.of("bannedBlocks"), 
             () -> List.of("minecraft:ender_chest"), // Default banned: ender chest
             entry -> entry instanceof String); // Validate entries are strings
             
            builder.pop();
        }
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }
    
    // These are the correct event methods with proper imports
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        // Initialize when config loads
        BannedBlocksManager.initialize();
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        // Reload when config changes
        BannedBlocksManager.initialize();
    }
}