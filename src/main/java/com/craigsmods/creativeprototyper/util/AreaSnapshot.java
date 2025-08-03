package com.craigsmods.creativeprototyper.util;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public class AreaSnapshot {
    private final Map<BlockPos, BlockStateData> blocks = new HashMap<>();
    private final BlockPos origin;
    private final int radius;

    public AreaSnapshot(BlockPos origin, int radius) {
        this.origin = origin;
        this.radius = radius;
    }

    public void addBlock(BlockPos pos, BlockState state, CompoundTag blockEntityData) {
        blocks.put(pos.immutable(), new BlockStateData(state, blockEntityData));
    }

    public Map<BlockPos, BlockStateData> getBlocks() {
        return blocks;
    }

    public BlockPos getOrigin() {
        return origin;
    }

    public int getRadius() {
        return radius;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putInt("Radius", radius);
    
        ListTag blocksTag = new ListTag();
        int blockCount = 0;
        
        for (Map.Entry<BlockPos, BlockStateData> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockStateData stateData = entry.getValue();
            
            // Skip air blocks to save space
            if (stateData.state.isAir()) continue;
    
            CompoundTag blockTag = new CompoundTag();
            blockTag.put("Pos", NbtUtils.writeBlockPos(pos));
    
            // Store block ID
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(stateData.state.getBlock());
            if (blockId != null) {
                blockTag.putString("BlockId", blockId.toString());
    
                // Store block state - improved to capture all state properties
                CompoundTag stateTag = NbtUtils.writeBlockState(stateData.state);
                blockTag.put("State", stateTag);
            }
    
            if (stateData.blockEntityData != null) {
                blockTag.put("TileData", stateData.blockEntityData);
            }
    
            blocksTag.add(blockTag);
            blockCount++;
        }
    
        tag.put("Blocks", blocksTag);
        tag.putInt("BlockCount", blockCount); // Add block count for validation
        
        System.out.println("Serialized AreaSnapshot with " + blockCount + " blocks, radius: " + radius);
        return tag;
    }

    public static AreaSnapshot deserializeNBT(CompoundTag tag, Level level) {
        if (level == null) {
            System.err.println("ERROR: Cannot deserialize AreaSnapshot with null level");
            return null;
        }
        
        try {
            BlockPos origin = NbtUtils.readBlockPos(tag.getCompound("Origin"));
            int radius = tag.getInt("Radius");
            int expectedBlockCount = tag.contains("BlockCount") ? tag.getInt("BlockCount") : -1;
    
            AreaSnapshot snapshot = new AreaSnapshot(origin, radius);
            int actualBlockCount = 0;
    
            ListTag blocksTag = tag.getList("Blocks", 10); // 10 = CompoundTag type ID
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompound(i);
                BlockPos pos = NbtUtils.readBlockPos(blockTag.getCompound("Pos"));
    
                BlockState state = Blocks.AIR.defaultBlockState(); // Default to air
    
                // Read the block ID
                if (blockTag.contains("BlockId")) {
                    String blockIdStr = blockTag.getString("BlockId");
                    ResourceLocation blockId = new ResourceLocation(blockIdStr);
                    Block block = ForgeRegistries.BLOCKS.getValue(blockId);
    
                    if (block != null) {
                        // Default state
                        state = block.defaultBlockState();
    
                        if (blockTag.contains("State")) {
                            try {
                                CompoundTag stateTag = blockTag.getCompound("State");
                                if (stateTag.contains("Name")) {
                                    state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), stateTag);
                                } else if (stateTag.contains("Meta")) {
                                    int meta = stateTag.getInt("Meta");
                                    BlockState potentialState = Block.stateById(meta);
                                    if (potentialState.getBlock() == block) {
                                        state = potentialState;
                                    }
                                }
                            } catch (Exception e) {
                                
                            }
                        }
                    }
                }
    
                CompoundTag tileData = null;
                if (blockTag.contains("TileData")) {
                    tileData = blockTag.getCompound("TileData");
                }
    
                snapshot.addBlock(pos, state, tileData);
                actualBlockCount++;
            }
    
            // Validate the snapshot
            if (expectedBlockCount != -1 && expectedBlockCount != actualBlockCount) {

            }
            
            return snapshot;
        } catch (Exception e) {
            System.err.println("Error deserializing AreaSnapshot: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static class BlockStateData {
        public final BlockState state;
        public final CompoundTag blockEntityData;

        public BlockStateData(BlockState state, CompoundTag blockEntityData) {
            this.state = state;
            this.blockEntityData = blockEntityData;
        }
    }
}