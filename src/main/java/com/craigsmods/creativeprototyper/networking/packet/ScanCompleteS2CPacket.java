package com.craigsmods.creativeprototyper.networking.packet;

import com.craigsmods.creativeprototyper.gui.CreativeTableScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client packet to notify when a scan is complete
 */
public class ScanCompleteS2CPacket {
    private final int blockCount;
    
    public ScanCompleteS2CPacket(int blockCount) {
        this.blockCount = blockCount;
    }
    
    public ScanCompleteS2CPacket(FriendlyByteBuf buf) {
        this.blockCount = buf.readInt();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(blockCount);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Update the GUI if it's open
                if (Minecraft.getInstance().screen instanceof CreativeTableScreen screen) {
                    screen.setScanComplete(blockCount);
                }
            });
        });
        
        return true;
    }
}