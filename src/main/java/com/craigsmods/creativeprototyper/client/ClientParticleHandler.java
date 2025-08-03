package com.craigsmods.creativeprototyper.client;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Handles client-side particles for active tables
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID, value = Dist.CLIENT)
public class ClientParticleHandler {
    private static final Random RANDOM = new Random();
    private static final Set<BlockPos> ACTIVE_SCANNING_TABLES = new HashSet<>();


    public static void registerScanningTable(BlockPos pos) {
        ACTIVE_SCANNING_TABLES.add(pos.immutable());
    }

    public static void unregisterScanningTable(BlockPos pos) {
        ACTIVE_SCANNING_TABLES.remove(pos);
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        

        if (level == null || minecraft.player == null) return;

        BlockPos playerBlockPos = minecraft.player.blockPosition();
        int rangeSq = 32 * 32; 
        for (BlockPos tablePos : ACTIVE_SCANNING_TABLES) {
            if (tablePos.distSqr(playerBlockPos) <= rangeSq) {
                spawnClientParticles(level, tablePos);
            }
        }
    }
    
    /**
     * Spawn client-side particles for a scanning table
     */
    private static void spawnClientParticles(ClientLevel level, BlockPos tablePos) {
        // Enchantment table style particles
        if (RANDOM.nextInt(2) == 0) { 
            double x = tablePos.getX() + 0.5;
            double y = tablePos.getY() + 1.2;
            double z = tablePos.getZ() + 0.5;
            
            
            for (int i = 0; i < 2; i++) {
                double radius = 0.75;
                double angle = (level.getGameTime() / 20.0) + i * (Math.PI);
                
                double particleX = x + Math.cos(angle) * radius;
                double particleZ = z + Math.sin(angle) * radius;
                
                level.addParticle(
                    ParticleTypes.ENCHANT,
                    particleX,
                    y, 
                    particleZ,
                    0, 0.1, 0
                );
            }
            
            
        }
    }
}