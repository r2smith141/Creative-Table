package com.craigsmods.creativeprototyper.networking;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.networking.packet.CheckTableSnapshotC2SPacket;
import com.craigsmods.creativeprototyper.networking.packet.ResetAndScanC2SPacket;
import com.craigsmods.creativeprototyper.networking.packet.ScanAreaC2SPacket;
import com.craigsmods.creativeprototyper.networking.packet.ScanCompleteS2CPacket;
import com.craigsmods.creativeprototyper.networking.packet.TableSnapshotStatusS2CPacket;
import com.craigsmods.creativeprototyper.networking.packet.TeleportToDimensionC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }
    
    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(CreativePrototyper.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();
                
        INSTANCE = net;
        
        // Register packets
        net.messageBuilder(ScanAreaC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(ScanAreaC2SPacket::new)
                .encoder(ScanAreaC2SPacket::toBytes)
                .consumerMainThread(ScanAreaC2SPacket::handle)
                .add();
                
        net.messageBuilder(TeleportToDimensionC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(TeleportToDimensionC2SPacket::new)
                .encoder(TeleportToDimensionC2SPacket::toBytes)
                .consumerMainThread(TeleportToDimensionC2SPacket::handle)
                .add();
                
        net.messageBuilder(CheckTableSnapshotC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CheckTableSnapshotC2SPacket::new)
                .encoder(CheckTableSnapshotC2SPacket::toBytes)
                .consumerMainThread(CheckTableSnapshotC2SPacket::handle)
                .add();
                
        net.messageBuilder(TableSnapshotStatusS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(TableSnapshotStatusS2CPacket::new)
                .encoder(TableSnapshotStatusS2CPacket::toBytes)
                .consumerMainThread(TableSnapshotStatusS2CPacket::handle)
                .add();
        net.messageBuilder(ResetAndScanC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
        .decoder(ResetAndScanC2SPacket::new)
        .encoder(ResetAndScanC2SPacket::toBytes)
        .consumerMainThread(ResetAndScanC2SPacket::handle)
        .add();
        net.messageBuilder(ScanCompleteS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
        .decoder(ScanCompleteS2CPacket::new)
        .encoder(ScanCompleteS2CPacket::toBytes)
        .consumerMainThread(ScanCompleteS2CPacket::handle)
        .add();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
    
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}