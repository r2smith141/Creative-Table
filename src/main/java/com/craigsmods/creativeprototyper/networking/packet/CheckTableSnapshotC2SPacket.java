package com.craigsmods.creativeprototyper.networking.packet;

import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.networking.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client to server packet to check if a table already has a snapshot
 */
public class CheckTableSnapshotC2SPacket {
    private final BlockPos tablePos;
    
    public CheckTableSnapshotC2SPacket(BlockPos tablePos) {
        this.tablePos = tablePos;
    }
    
    public CheckTableSnapshotC2SPacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Get the player that sent the packet
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // Check if this table has a snapshot already
            boolean hasSnapshot = false;
            int blockCount = 0;
            boolean buildComplete = false;
            int buildProgress = 0;
            
            // Get the block entity
            BlockEntity be = player.level().getBlockEntity(tablePos);
            if (be instanceof CreativeTableBlockEntity tableEntity) {
                // Check if we have scan progress
                hasSnapshot = tableEntity.getCurrentScanProgress() > 0 || tableEntity.isBuildingComplete();
                blockCount = tableEntity.getTotalScanBlocks();
                buildComplete = tableEntity.isBuildingComplete();
                buildProgress = tableEntity.getScanProgressPercentage();
            }
            
            // Also look in the dimension manager for this table
            CreativeDimensionManager.TableKey tableKey = 
                CreativeDimensionManager.createTableKey(player.getUUID(), player.level().dimension(), tablePos);
            
            CreativeDimensionManager.BuildStatus status = 
                CreativeDimensionManager.getTableBuildStatus().get(tableKey);
            
            if (status != null) {
                hasSnapshot = true;
                if (status.totalBlocks > 0) {
                    blockCount = status.totalBlocks;
                }
                buildComplete = status.buildComplete;
                if (status.totalBlocks > 0) {
                    buildProgress = (status.currentProgress * 100) / status.totalBlocks;
                }
            }
            
            // Send response back to client
            ModMessages.sendToPlayer(
                new TableSnapshotStatusS2CPacket(hasSnapshot, blockCount, buildComplete, buildProgress, buildComplete, tablePos), 
                player
            );
        });
        
        return true;
    }
}