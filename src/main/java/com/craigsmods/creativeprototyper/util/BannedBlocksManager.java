package com.craigsmods.creativeprototyper.util;

import com.craigsmods.creativeprototyper.config.CreativePrototyperConfig;
//import com.mojang.datafixers.types.templates.List;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BannedBlocksManager {
    private static final Set<Block> bannedBlocks = new HashSet<>();
    private static boolean initialized = false;
    
    /**
     * Initialize the banned blocks set from config
     */
    public static void initialize() {
        bannedBlocks.clear();
        
        // Get banned block IDs from config
        List<? extends String> bannedIds = CreativePrototyperConfig.COMMON.bannedBlocks.get();
        
        System.out.println("Loading banned blocks: " + bannedIds);
        
        // Convert block IDs to actual Block instances
        for (String blockId : bannedIds) {
            try {
                ResourceLocation resourceLocation = new ResourceLocation(blockId);
                Block block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
                
                if (block != null && block != Blocks.AIR) {
                    bannedBlocks.add(block);
                    System.out.println("Added banned block: " + blockId);
                } else {
                    System.out.println("Warning: Could not find block with ID: " + blockId);
                }
            } catch (Exception e) {
                System.out.println("Error processing banned block ID: " + blockId);
                e.printStackTrace();
            }
        }
        
        initialized = true;
        System.out.println("Banned blocks initialized with " + bannedBlocks.size() + " entries");
    }
    
    /**
     * Check if a block is banned
     */
    public static boolean isBlockBanned(Block block) {
        if (!initialized) {
            initialize();
        }
        return bannedBlocks.contains(block);
    }
    
    /**
     * Check if a block state is banned
     */
    public static boolean isBlockStateBanned(BlockState state) {
        return isBlockBanned(state.getBlock());
    }
    
}