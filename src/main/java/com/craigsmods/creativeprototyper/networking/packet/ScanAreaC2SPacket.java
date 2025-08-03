package com.craigsmods.creativeprototyper.networking.packet;

import com.craigsmods.creativeprototyper.block.CreativeTableBlock;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.craigsmods.creativeprototyper.util.AreaSnapshot;
import com.craigsmods.creativeprototyper.util.AsyncAreaScanner;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScanAreaC2SPacket {
    private final BlockPos tablePos;
    private final int scanRadius;
    
    public ScanAreaC2SPacket(BlockPos tablePos, int scanRadius) {
        this.tablePos = tablePos;
        this.scanRadius = scanRadius;
    }
    
    public ScanAreaC2SPacket(FriendlyByteBuf buf) {
        this.tablePos = buf.readBlockPos();
        this.scanRadius = buf.readInt();
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(tablePos);
        buf.writeInt(scanRadius);
    }
    
public boolean handle(Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    context.enqueueWork(() -> {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        
        // Don't scan if in creative dimension
        if (player.level().dimension().equals(ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY)) {
            player.displayClientMessage(
                Component.literal("Cannot scan from inside the creative dimension!"), false);
            return;
        }
        
        // Get the block entity
        BlockEntity be = player.level().getBlockEntity(tablePos);
        if (!(be instanceof CreativeTableBlockEntity tableEntity)) {

            return;
        }
        
        // Use AsyncAreaScanner instead of direct scanning
        AsyncAreaScanner.startScan(player, tablePos, scanRadius, tableEntity);
    });
    
    return true;
}
}