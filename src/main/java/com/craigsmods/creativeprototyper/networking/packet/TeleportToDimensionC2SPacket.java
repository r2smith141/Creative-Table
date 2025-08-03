package com.craigsmods.creativeprototyper.networking.packet;

import com.craigsmods.creativeprototyper.block.CreativeTableBlock;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.craigsmods.creativeprototyper.util.PlayerDataManager;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class TeleportToDimensionC2SPacket {
    private final BlockPos tablePos;
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    
    public TeleportToDimensionC2SPacket(BlockPos tablePos) {
        this.tablePos = tablePos;
    }
    
    public TeleportToDimensionC2SPacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Get block entity
            BlockEntity be = player.level().getBlockEntity(tablePos);
            if (!(be instanceof CreativeTableBlockEntity tableEntity)) {
                player.displayClientMessage(
                    Component.literal("Error: Creative Table not found!"), false);
                return;
            }
            
            // Check if the table is currently scanning
            if (tableEntity.isScanning()) {
                player.displayClientMessage(
                    Component.literal("Please wait until scanning is complete."), false);
                return;
            }
            
            // Check if the build is complete
            if (!tableEntity.isBuildingComplete()) {
                int progress = tableEntity.getScanProgressPercentage();
                player.displayClientMessage(
                    Component.literal("Your creative space is still being built! Current progress: " + 
                                     progress + "%. Please wait for building to complete."), false);
                return;
            }
            
            // Store the current dimension before switching
            ResourceKey<Level> sourceDim = player.level().dimension();
            LOGGER.info("Original source dimension: {} at position {}", sourceDim.location(), tablePos);
            
            // CRITICAL: Create and store the table key BEFORE switching dimensions
            CreativeDimensionManager.TableKey tableKey = 
                CreativeDimensionManager.createTableKey(player.getUUID(), sourceDim, tablePos);
            
            // IMPORTANT: Set this as the active table key
            CreativeDimensionManager.setActiveTableKey(player.getUUID(), tableKey);
            
            // Get target dimension
            ServerLevel dimensionLevel = player.getServer().getLevel(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY);
            if (dimensionLevel == null) {
                player.displayClientMessage(
                    Component.literal("Error: Creative dimension not found!"), false);
                return;
            }
            

            BlockPos placementPos = calculatePlacementPosition(player.getUUID(), tablePos);
            CreativeDimensionManager.setTablePlacementPosition(tableKey, placementPos);        
            PlayerDataManager.switchToCreativeDimension(player, tablePos);
            

            player.teleportTo(dimensionLevel, 
                placementPos.getX() + 0.5, 
                placementPos.getY() + 5,  
                placementPos.getZ() + 0.5, 
                player.getYRot(), 
                player.getXRot());
        });
        
        return true;
    }
    
    // Generate a consistent placement position for each table
    private BlockPos calculatePlacementPosition(UUID playerId, BlockPos tablePos) {
        // Simple hash-based approach
        int hash = (playerId.toString() + tablePos.toString()).hashCode();
        int x = ((hash % 100) - 50) * 1000;
        int z = ((hash / 100 % 100) - 50) * 1000;
        return new BlockPos(x, 70, z);
    }
}