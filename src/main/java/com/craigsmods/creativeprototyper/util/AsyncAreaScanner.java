package com.craigsmods.creativeprototyper.util;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.config.CreativePrototyperConfig;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.networking.ModMessages;
import com.craigsmods.creativeprototyper.networking.packet.ScanCompleteS2CPacket;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optimized area scanner that scans blocks and builds them simultaneously
 */
public class AsyncAreaScanner {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService SCAN_EXECUTOR = Executors.newSingleThreadExecutor();
    
    // Map of active scans
    private static final Map<UUID, ScanData> activeScanMap = new ConcurrentHashMap<>();
    
    // Register tick listener (call this in your mod setup)
    public static void init() {
        MinecraftForge.EVENT_BUS.register(AsyncAreaScanner.class);
    }
    
    /**
     * Start an optimized scan that builds as it scans
     */
    public static void startScan(ServerPlayer player, BlockPos tablePos, int radius, 
                               CreativeTableBlockEntity tableEntity) {
        UUID playerId = player.getUUID();
        
        // Cancel any existing scan for this player
        if (activeScanMap.containsKey(playerId)) {
            player.displayClientMessage(
                Component.literal("Cancelled previous scan"), false);
            activeScanMap.remove(playerId);
        }
        
        tableEntity.setScanning(true);
        
        // Calculate placement position in the creative dimension
        BlockPos placementPos = calculatePlacementPosition(playerId, tablePos);
        
        // Get creative dimension level
        ServerLevel creativeLevel = player.getServer().getLevel(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY);
        if (creativeLevel == null) {
            player.displayClientMessage(
                Component.literal("Error: Creative dimension not found!"), false);
            return;
        }
        
        // Store the current dimension before switching
        ResourceKey<Level> sourceDim = player.level().dimension();
        LOGGER.info("Original source dimension: {} at position {}", sourceDim.location(), tablePos);
        
        // Create a key for this table
        CreativeDimensionManager.TableKey tableKey = 
            CreativeDimensionManager.createTableKey(playerId, sourceDim, tablePos);
        
        // Set the active table key
        CreativeDimensionManager.setActiveTableKey(playerId, tableKey);
        
        // Store the placement position
        CreativeDimensionManager.setTablePlacementPosition(tableKey, placementPos);
        
        // Create scan data
        ScanData scanData = new ScanData(
            player,
            (ServerLevel) player.level(),
            tablePos,
            radius,
            tableEntity,
            creativeLevel,
            placementPos,
            sourceDim.location().toString()
        );
        
        // Store scan data
        activeScanMap.put(playerId, scanData);
        tableEntity.startScanning(radius);
        
        // Start asynchronous scan for non-tile entity blocks
        CompletableFuture.runAsync(() -> {
            try {
                scanAndBuildNonTileEntityBlocks(scanData);
            } catch (Exception e) {
                player.displayClientMessage(
                    Component.literal("Error during scan: " + e.getMessage()), false);
                activeScanMap.remove(playerId);
            }
        }, SCAN_EXECUTOR);
        

    }
    
    /**
     * Generate a consistent placement position for each table
     */
    private static BlockPos calculatePlacementPosition(UUID playerId, BlockPos tablePos) {
        // Simple hash-based approach
        int hash = (playerId.toString() + tablePos.toString()).hashCode();
        int x = ((hash % 100) - 50) * 1000;
        int z = ((hash / 100 % 100) - 50) * 1000;
        return new BlockPos(x, 70, z);
    }
    
    /**
     * Scan non-tile entity blocks and build them immediately
     */
    private static void scanAndBuildNonTileEntityBlocks(ScanData scanData) {
        ServerLevel sourceLevel = scanData.level;
        BlockPos sourceCenter = scanData.tablePos;
        int radius = scanData.radius;
        ServerLevel targetLevel = scanData.creativeLevel;
        BlockPos targetCenter = scanData.placementPos;
        
        // Process blocks in an orderly fashion
        List<BlockPos> allPositions = new ArrayList<>();
        
        // First, collect all positions that need to be scanned
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos relativePos = new BlockPos(x, y, z);
                    BlockPos sourcePos = sourceCenter.offset(x, y, z);
                    
                    if (!sourceLevel.isLoaded(sourcePos)) continue;
                    
                    BlockState state = sourceLevel.getBlockState(sourcePos);
                    if (state.isAir()) continue;
                    
                    // Add all non-air blocks to the list
                    allPositions.add(relativePos);
                }
            }
        }
        
        scanData.totalBlocks.set(allPositions.size());
        
        for (BlockPos relativePos : allPositions) {
            // Check if scan was cancelled
            if (!activeScanMap.containsKey(scanData.player.getUUID())) {
                return;
            }
            
            BlockPos sourcePos = sourceCenter.offset(relativePos);
            BlockState state = sourceLevel.getBlockState(sourcePos);
            
            // Skip if this has a block entity - we'll handle those on the main thread
            if (state.hasBlockEntity()) {
                scanData.blockEntityPositions.add(new BlockData(relativePos, state, null));
                continue;
            }
            
            // Build this block immediately in the creative dimension
            BlockPos targetPos = targetCenter.offset(relativePos);
            
            // Check if this block is allowed before placing
            if (!BannedBlocksManager.isBlockStateBanned(state)) {
                // Add to the pending blocks to be placed on the main thread
                scanData.pendingBlocks.add(new BlockData(relativePos, state, null));
            }
            
            // Update progress
            scanData.processedBlocks.incrementAndGet();
        }
        
        // Mark as ready for block entity scanning and pending block placement
        scanData.needsBlockPlacement = true;
    }
    
    /**
     * Server tick event handler for processing tile entities and placing blocks on the main thread
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeScanMap.isEmpty()) {
            return;
        }
        
        // Process for each active scan
        activeScanMap.forEach((playerId, scanData) -> {
            if (!scanData.needsBlockPlacement) {
                return; // Still waiting for async scan
            }
            
            // Process a small batch of block placements per tick
            processBlockPlacement(scanData);
            
            // Process a small batch of block entities per tick
            if (scanData.pendingBlocks.isEmpty() && !scanData.blockEntityPositions.isEmpty()) {
                processBlockEntityChunk(scanData);
            }
        });
    }
    
    /**
     * Process a chunk of pending blocks
     */
    private static void processBlockPlacement(ScanData scanData) {
        final int BATCH_SIZE = CreativePrototyperConfig.COMMON.blocksPerTick.get();
        int processed = 0;
        
        while (!scanData.pendingBlocks.isEmpty() && processed < BATCH_SIZE) {
            BlockData blockData = scanData.pendingBlocks.remove(0);
            processed++;
            
            try {
                // Place the block in the creative dimension
                BlockPos targetPos = scanData.placementPos.offset(blockData.relativePos);
                scanData.creativeLevel.setBlock(targetPos, blockData.state, 3);
                
                // Update progress
                scanData.placedBlocks.incrementAndGet();
                
                // Track progress for persistence
                scanData.tableEntity.setCurrentScanProgress(scanData.placedBlocks.get());
                scanData.tableEntity.setTotalScanBlocks(scanData.totalBlocks.get());
            } catch (Exception e) {
                LOGGER.error("Error placing block: " + e.getMessage());
            }
        }
        
        // Update progress message every 20 ticks
        if (scanData.creativeLevel.getGameTime() % 20 == 0) {
            int totalBlocks = scanData.totalBlocks.get();
            int placedBlocks = scanData.placedBlocks.get();
            int percentage = totalBlocks > 0 ? (placedBlocks * 100) / totalBlocks : 0;
            
        }
    }
    
    /**
     * Process a chunk of block entities
     */
    private static void processBlockEntityChunk(ScanData scanData) {
        final int BATCH_SIZE = CreativePrototyperConfig.COMMON.blockEntitiesPerTick.get();
        int processed = 0;
        
        ServerLevel sourceLevel = scanData.level;
        BlockPos sourceCenter = scanData.tablePos;
        ServerLevel targetLevel = scanData.creativeLevel;
        BlockPos targetCenter = scanData.placementPos;
        
        while (!scanData.blockEntityPositions.isEmpty() && processed < BATCH_SIZE) {
            BlockData blockData = scanData.blockEntityPositions.remove(0);
            processed++;
            
            try {
                // Get source block and entity data
                BlockPos sourcePos = sourceCenter.offset(blockData.relativePos);
                BlockState state = sourceLevel.getBlockState(sourcePos);
                BlockEntity blockEntity = sourceLevel.getBlockEntity(sourcePos);
                
                if (blockEntity != null) {
                    // Get block entity data
                    CompoundTag blockEntityData = blockEntity.saveWithoutMetadata();
                    
                    // Place in creative dimension
                    BlockPos targetPos = targetCenter.offset(blockData.relativePos);
                    
                    // Check if this block is allowed
                    if (!BannedBlocksManager.isBlockStateBanned(state) && 
                    !(blockEntity instanceof CreativeTableBlockEntity)) {
                    // Place the block
                    targetLevel.setBlock(targetPos, state, 3);
                    
                    // Set block entity data
                    BlockEntity targetEntity = targetLevel.getBlockEntity(targetPos);
                    if (targetEntity != null) {
                        // Copy the data but update position
                        CompoundTag dataCopy = blockEntityData.copy();
                        dataCopy.putInt("x", targetPos.getX());
                        dataCopy.putInt("y", targetPos.getY());
                        dataCopy.putInt("z", targetPos.getZ());
                        
                        // Load the data
                        targetEntity.load(dataCopy);
                        targetEntity.setChanged();
                    }
                }
                    
                    // Update progress counters
                    scanData.placedBlocks.incrementAndGet();
                    scanData.tableEntity.setCurrentScanProgress(scanData.placedBlocks.get());
                }
            } catch (Exception e) {
                LOGGER.error("Error processing block entity: " + e.getMessage());
            }
        }
        
        // If all block entities processed, finalize the scan
        if (scanData.blockEntityPositions.isEmpty()) {
            finalizeScan(scanData);
        }
    }
    
    /**
     * Finalize the scan and mark it as complete
     */
    private static void finalizeScan(ScanData scanData) {
        try {
            // Get total scanned and placed blocks
            int totalBlocks = scanData.totalBlocks.get();
            int placedBlocks = scanData.placedBlocks.get();
            
            scanData.tableEntity.setScanning(false);
            scanData.tableEntity.setBuildingComplete(true);
            
            
            // Create a return portal at the build location
            createReturnPortal(scanData);
            
            // Mark build as complete in both the block entity and dimension manager
            scanData.tableEntity.markBuildComplete(placedBlocks);
            CreativeDimensionManager.markBuildComplete(scanData.player, placedBlocks);
            
            // Force the chunk to be saved
            scanData.level.getChunkAt(scanData.tablePos).setUnsaved(true);
            
            // Notify player
            //scanData.player.displayClientMessage(
                //Component.literal("Scan and build complete! " + placedBlocks + " blocks placed."), false);
            
            // Send packet to client to notify scan completion
            ModMessages.sendToPlayer(new ScanCompleteS2CPacket(placedBlocks), scanData.player);
            
            // Remove from active scans
            activeScanMap.remove(scanData.player.getUUID());
            
        } catch (Exception e) {
            LOGGER.error("SCAN ERROR: Error completing scan: " + e.getMessage());
            e.printStackTrace();
            scanData.player.displayClientMessage(
                Component.literal("Error completing scan: " + e.getMessage()), false);
            activeScanMap.remove(scanData.player.getUUID());
        }
    }
    
    /**
     * Create a return portal in the creative dimension
     */
    private static void createReturnPortal(ScanData scanData) {
        try {
            // Find a suitable location for the return portal
            BlockPos portalPos = findPortalLocation(scanData);
            
            // Create the return portal
            scanData.creativeLevel.setBlockAndUpdate(portalPos, 
                com.craigsmods.creativeprototyper.registry.ModBlocks.CREATIVE_TABLE.get().defaultBlockState());
            
            // Get the block entity
            BlockEntity be = scanData.creativeLevel.getBlockEntity(portalPos);
            if (be instanceof CreativeTableBlockEntity portalEntity) {
                // Mark as a return portal
                portalEntity.markAsReturnPortal(scanData.tablePos, scanData.sourceDimensionId);
                
                // Make sure the entity is saved
                portalEntity.setChanged();
                scanData.creativeLevel.getChunkAt(portalPos).setUnsaved(true);
                
                // Register in the dimension manager
                CreativeDimensionManager.TableKey originalTableKey = 
                    CreativeDimensionManager.createTableKey(
                        scanData.player.getUUID(), 
                        scanData.level.dimension(), 
                        scanData.tablePos
                    );
                
                CreativeDimensionManager.registerReturnPortal(scanData.player, portalPos, originalTableKey);
                
                LOGGER.info("Created return portal at {} in creative dimension", portalPos);
            }
        } catch (Exception e) {
            LOGGER.error("Error creating return portal: " + e.getMessage());
        }
    }
    
    /**
     * Find a suitable location for the return portal
     */
    private static BlockPos findPortalLocation(ScanData scanData) {
        // Place it at a visible location near the center, 5 blocks above ground
        BlockPos center = scanData.placementPos;
        BlockPos portalPos = center.offset(0, 0, 0);
        
        // Make sure the space above is clear for the player
        scanData.creativeLevel.setBlockAndUpdate(portalPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        scanData.creativeLevel.setBlockAndUpdate(portalPos.above(1), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        
        return portalPos;
    }
    
    /**
     * Data class for an active scan
     */
    private static class ScanData {
        final ServerPlayer player;
        final ServerLevel level;
        final BlockPos tablePos;
        final int radius;
        final CreativeTableBlockEntity tableEntity;
        final ServerLevel creativeLevel;
        final BlockPos placementPos;
        final String sourceDimensionId;
        final List<BlockData> blockEntityPositions = new ArrayList<>();
        final List<BlockData> pendingBlocks = new ArrayList<>();
        final AtomicInteger processedBlocks = new AtomicInteger(0);
        final AtomicInteger placedBlocks = new AtomicInteger(0);
        final AtomicInteger totalBlocks = new AtomicInteger(0);
        boolean needsBlockPlacement = false;
        
        ScanData(ServerPlayer player, ServerLevel level, BlockPos tablePos, int radius, 
                CreativeTableBlockEntity tableEntity, ServerLevel creativeLevel, 
                BlockPos placementPos, String sourceDimensionId) {
            this.player = player;
            this.level = level;
            this.tablePos = tablePos;
            this.radius = radius;
            this.tableEntity = tableEntity;
            this.creativeLevel = creativeLevel;
            this.placementPos = placementPos;
            this.sourceDimensionId = sourceDimensionId;
        }
    }
    
    /**
     * Data class for a block position, state, and entity data
     */
    private static class BlockData {
        final BlockPos relativePos;
        final BlockState state;
        final CompoundTag blockEntityData;
        
        BlockData(BlockPos relativePos, BlockState state, CompoundTag blockEntityData) {
            this.relativePos = relativePos;
            this.state = state;
            this.blockEntityData = blockEntityData;
        }
    }
}