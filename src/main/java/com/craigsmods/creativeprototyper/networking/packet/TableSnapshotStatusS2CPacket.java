package com.craigsmods.creativeprototyper.networking.packet;

import com.craigsmods.creativeprototyper.client.ClientParticleHandler;
import com.craigsmods.creativeprototyper.gui.CreativeTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client packet with snapshot status
 */
public class TableSnapshotStatusS2CPacket {
    private final boolean hasSnapshot;
    private final int blockCount;
    private final boolean buildComplete;
    private final int buildProgress;
    private final boolean isScanning;
    private final BlockPos tablePos; // Add this field
    
    public TableSnapshotStatusS2CPacket(boolean hasSnapshot, int blockCount, boolean buildComplete, 
                                      int buildProgress, boolean isScanning, BlockPos tablePos) {
        this.hasSnapshot = hasSnapshot;
        this.blockCount = blockCount;
        this.buildComplete = buildComplete;
        this.buildProgress = buildProgress;
        this.isScanning = isScanning;
        this.tablePos = tablePos;
    }
    public TableSnapshotStatusS2CPacket(FriendlyByteBuf buf) {
        this.hasSnapshot = buf.readBoolean();
        this.blockCount = buf.readInt();
        this.buildComplete = buf.readBoolean();
        this.buildProgress = buf.readInt();
        this.isScanning = buf.readBoolean();
        this.tablePos = buf.readBlockPos(); // Read the position
    }
    
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(hasSnapshot);
        buf.writeInt(blockCount);
        buf.writeBoolean(buildComplete);
        buf.writeInt(buildProgress);
        buf.writeBoolean(isScanning);
        buf.writeBlockPos(tablePos); // Write the position
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Update UI if open
                if (Minecraft.getInstance().screen instanceof CreativeTableScreen screen) {
                    screen.setHasExistingSnapshot(hasSnapshot, blockCount, buildComplete, buildProgress);
                }
                
            });
        });
        
        return true;
    }
}