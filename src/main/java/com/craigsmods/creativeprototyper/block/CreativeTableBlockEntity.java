package com.craigsmods.creativeprototyper.block;

import java.util.UUID;

import com.craigsmods.creativeprototyper.client.ScanningParticleManager;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager.TableKey;
import com.craigsmods.creativeprototyper.registry.ModBlockEntities;
import com.craigsmods.creativeprototyper.registry.ModDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CreativeTableBlockEntity extends BlockEntity implements GeoBlockEntity {
    private UUID lastUsedBy;
    private boolean isReturnPortal = false; // Flag to identify tables in creative dimension
    private boolean buildingComplete = false; // Flag to indicate if build is complete
    private int currentScanProgress = 0; // Current progress of scan/build (blocks processed)
    private int totalScanBlocks = 0; // Total blocks to process in the scan/build
    private BlockPos sourceDimensionPos; // For return portal functionality
    private String sourceDimensionId; // Store dimension as string for easier serialization
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean isActive = false;
    private boolean isScanning = false;
    private int scanBlockCount;
    private int scanningTicksRemaining;
    private boolean scanningFinished;
    private static final int MIN_SCAN_DURATION_TICKS = 100;
    
    public CreativeTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_TABLE.get(), pos, state);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }
    
    private PlayState predicate(AnimationState<CreativeTableBlockEntity> event) {
        if (isScanning) {
            // Play the active animation when the table is being used
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.creative_table.scan"));
        } else if (isReturnPortal) {
            // Different animation for return portals
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.creative_table.idle"));
        } else {
            // Idle animation for normal tables
            return event.setAndContinue(RawAnimation.begin().thenPlay("animation.creative_table.idle"));
        }
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, CreativeTableBlockEntity blockEntity) {
        if (blockEntity.isScanning) {
            if (blockEntity.scanningTicksRemaining > 0) {
                blockEntity.scanningTicksRemaining--;
            } else if (blockEntity.scanningFinished) {
                blockEntity.stopScanning();
            }
        }
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    // Set table to active state (called when player interacts)
    public void setActive(boolean active) {
        this.isActive = active;
        setChanged();
    }

    public boolean isScanning() {
        return isScanning;
    }
    
    public void setScanning(boolean scanning) {
        this.isScanning = scanning;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Create a data packet for sync to client
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {

        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Process data from server
     */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {

        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            this.load(tag);
            // Debug
            System.out.println("Client received packet: isReturnPortal=" + isReturnPortal);
        }
    }

    /**
     * Get the update tag - this is called when a chunk is initially sent to a client
     */
    @Override
    public CompoundTag getUpdateTag() {

        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("IsReturnPortal", isReturnPortal);
        tag.putBoolean("IsScanning", isScanning);
        tag.putBoolean("BuildingComplete", buildingComplete);
        tag.putInt("CurrentScanProgress", currentScanProgress);
        tag.putInt("TotalScanBlocks", totalScanBlocks);
        if (sourceDimensionId != null) {
            tag.putString("SourceDim", sourceDimensionId);
        }
        if (sourceDimensionPos != null) {
            tag.put("SourcePos", NbtUtils.writeBlockPos(sourceDimensionPos));
        }
        return tag;
    }

    /**
     * Handle the update tag - this is called on the client
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        isScanning = tag.getBoolean("IsScanning");
        isReturnPortal = tag.getBoolean("IsReturnPortal");
        buildingComplete = tag.getBoolean("BuildingComplete");
        currentScanProgress = tag.getInt("CurrentScanProgress");
        totalScanBlocks = tag.getInt("TotalScanBlocks");
        if (tag.contains("SourceDim")) {
            sourceDimensionId = tag.getString("SourceDim");
        }
        if (tag.contains("SourcePos")) {
            sourceDimensionPos = NbtUtils.readBlockPos(tag.getCompound("SourcePos"));
        }


    }
    
    /**
     * Get if the build is complete
     */
    public boolean isBuildingComplete() {
        return buildingComplete;
    }
    
    /**
     * Set if the build is complete
     */
    public void setBuildingComplete(boolean complete) {
        this.buildingComplete = complete;
        setChanged();
        
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Get the current scan progress
     */
    public int getCurrentScanProgress() {
        return currentScanProgress;
    }
    
    /**
     * Set the current scan progress
     */
    public void setCurrentScanProgress(int progress) {
        this.currentScanProgress = progress;
        setChanged();
    }
    
    /**
     * Get the total scan blocks
     */
    public int getTotalScanBlocks() {
        return totalScanBlocks;
    }
    
    /**
     * Set the total scan blocks
     */
    public void setTotalScanBlocks(int total) {
        this.totalScanBlocks = total;
        setChanged();
    }
    
    /**
     * Calculate the scan progress percentage
     */
    public int getScanProgressPercentage() {
        if (totalScanBlocks > 0) {
            return (currentScanProgress * 100) / totalScanBlocks;
        }
        return 0;
    }
    
    /**
     * Set the last used by player
     */
    public void setLastUsedBy(Player player) {
        this.lastUsedBy = player.getUUID();
        setChanged();
    }

    /**
     * Start the scanning process
     */
    public void startScanning(int radius) {
        isScanning = true;
        scanningTicksRemaining = MIN_SCAN_DURATION_TICKS; 
        buildingComplete = false; 
        currentScanProgress = 0; 
        totalScanBlocks = 0; 
        setChanged();
        ResourceKey<Level> scanDimension = level != null ? level.dimension() : null;
        // Start particles
        if (level != null && !level.isClientSide()) {
            ScanningParticleManager.startScanParticles(
                this, worldPosition, radius, scanDimension);
        }
    }
    
    /**
     * Mark the scan as finished, but let it continue animating until minimum duration
     */
    public void markScanFinished(int totalBlocks) {
        scanningFinished = true;
        // Store any data needed about the completed scan
        this.scanBlockCount = totalBlocks;
        setChanged();
    }
    
    /**
     * Mark the build as complete
     */
    public void markBuildComplete(int blocksPlaced) {
        buildingComplete = true;
        currentScanProgress = blocksPlaced;
        totalScanBlocks = blocksPlaced;
        setChanged();
        

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Stop the scanning process
     */
    public void stopScanning() {
        if (isScanning) {
            isScanning = false;
            setChanged();
            

            if (level != null && !level.isClientSide()) {
                com.craigsmods.creativeprototyper.client.ScanningParticleManager.stopScanParticles(worldPosition);
            }
        }
    }
    
    /**
     * Mark this table as a return portal
     */
    public void markAsReturnPortal(BlockPos originalPos, String originalDimension) {
        isReturnPortal = true;
        sourceDimensionPos = originalPos;
        sourceDimensionId = originalDimension;
        setChanged();
        System.out.println("isReturnPortal is now: " + isReturnPortal + " in dimension " + 
                       (level != null ? level.dimension().location() : "null") +
                       " at " + worldPosition);
        

        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
            System.out.println("Sent block update to clients for position " + worldPosition);
            

            level.getChunkAt(worldPosition).setUnsaved(true);
        }
    }
    
    // Get source dimension position
    public BlockPos getSourcePosition() {
        return sourceDimensionPos;
    }
    
    // Get source dimension
    public ResourceKey<Level> getSourceDimension() {
        if (sourceDimensionId == null) {
            System.out.println("ERROR: sourceDimensionId is null when getting source dimension!");
            return null;
        }
        
        try {
            ResourceKey<Level> dimension = ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                new ResourceLocation(sourceDimensionId)
            );
            System.out.println("Getting source dimension: " + dimension.location());
            return dimension;
        } catch (Exception e) {
            System.out.println("ERROR creating ResourceKey from: " + sourceDimensionId);
            e.printStackTrace();
            return null;
        }
    }
    
    // Check if this is a return portal
    public boolean isReturnPortal() {
        return isReturnPortal;
    }
    
    // Get the last used by player
    public UUID getLastUsedBy() {
        return lastUsedBy;
    }
    
    /**
     * Save all data to NBT
     */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("IsScanning", isScanning);
        tag.putBoolean("BuildingComplete", buildingComplete);
        tag.putInt("CurrentScanProgress", currentScanProgress);
        tag.putInt("TotalScanBlocks", totalScanBlocks);
        

        tag.putBoolean("IsReturnPortal", isReturnPortal);
        
        if (lastUsedBy != null) {
            tag.putUUID("LastUsedBy", lastUsedBy);
        }
        
        if (sourceDimensionPos != null) {
            tag.put("SourcePos", NbtUtils.writeBlockPos(sourceDimensionPos));
            tag.putString("SourceDim", sourceDimensionId);
        }
        
        System.out.println("Saved block entity with isReturnPortal=" + isReturnPortal);
    }
    
    /**
     * Load data from NBT
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isScanning = tag.getBoolean("IsScanning");
        buildingComplete = tag.getBoolean("BuildingComplete");
        currentScanProgress = tag.getInt("CurrentScanProgress");
        totalScanBlocks = tag.getInt("TotalScanBlocks");
        

        isReturnPortal = tag.getBoolean("IsReturnPortal");
        
        if (tag.contains("LastUsedBy")) {
            lastUsedBy = tag.getUUID("LastUsedBy");
        }
        
        if (tag.contains("SourcePos")) {
            sourceDimensionPos = NbtUtils.readBlockPos(tag.getCompound("SourcePos"));
            sourceDimensionId = tag.getString("SourceDim");
        }
        
        System.out.println("Loaded block entity with isReturnPortal=" + isReturnPortal);
    }
}