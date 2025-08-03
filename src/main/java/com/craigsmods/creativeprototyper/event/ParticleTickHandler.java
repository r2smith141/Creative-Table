package com.craigsmods.creativeprototyper.event;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.client.ScanningParticleManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles ticking the particle systems
 */
@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID)
public class ParticleTickHandler {
    
    @SubscribeEvent
    public static void onServerLevelTick(TickEvent.LevelTickEvent event) {
        // Only process on server side and at the end of the tick
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel)) {
            return;
        }
        
        // Update particle systems
        ScanningParticleManager.tickParticles(event.level);
    }
}