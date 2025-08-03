package com.craigsmods.creativeprototyper.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Utility class for placing blocks from an AreaSnapshot

public class BlockPlacer {
    /**
     * Places blocks from a snapshot into the world
     
    public static void placeBlocks(ServerLevel level, BlockPos targetPos, AreaSnapshot snapshot) {
        Map<BlockPos, AreaSnapshot.BlockStateData> blocks = snapshot.getBlocks();
        
        // First pass: place all blocks
        for (Map.Entry<BlockPos, AreaSnapshot.BlockStateData> entry : blocks.entrySet()) {
            BlockPos relativePos = entry.getKey();
            BlockPos actualPos = targetPos.offset(relativePos.getX(), relativePos.getY(), relativePos.getZ());
            BlockState state = entry.getValue().state;
            
            // Place the block
            level.setBlock(actualPos, state, 3);
        }
        
        // Second pass: set block entity data 
        for (Map.Entry<BlockPos, AreaSnapshot.BlockStateData> entry : blocks.entrySet()) {
            if (entry.getValue().blockEntityData == null) continue;
            
            BlockPos relativePos = entry.getKey();
            BlockPos actualPos = targetPos.offset(relativePos.getX(), relativePos.getY(), relativePos.getZ());
            CompoundTag blockEntityData = entry.getValue().blockEntityData;
            
            // Get the block entity
            BlockEntity blockEntity = level.getBlockEntity(actualPos);
            if (blockEntity != null) {
                // Make a copy of the data to avoid modifying the original
                CompoundTag dataCopy = blockEntityData.copy();
                
                // Update position to match the new location
                dataCopy.putInt("x", actualPos.getX());
                dataCopy.putInt("y", actualPos.getY());
                dataCopy.putInt("z", actualPos.getZ());
                
                // Load the data
                blockEntity.load(dataCopy);
                blockEntity.setChanged();
            }
        }
    }
}
*/