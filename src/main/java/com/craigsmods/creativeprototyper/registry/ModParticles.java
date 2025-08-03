package com.craigsmods.creativeprototyper.registry;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.client.particle.TableScanParticle;
import com.craigsmods.creativeprototyper.client.particle.TableScanParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom particle types
 */
public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = 
        DeferredRegister.create(Registries.PARTICLE_TYPE, CreativePrototyper.MOD_ID);
        
    // Register the table scan particle type
    public static final RegistryObject<ParticleType<TableScanParticleOptions>> TABLE_SCAN_PARTICLE = 
    PARTICLE_TYPES.register("table_scan", 
        () -> {
            System.out.println("Creating TableScanParticle type");
            return new ParticleType<TableScanParticleOptions>(false, TableScanParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.Codec<TableScanParticleOptions> codec() {
                    return TableScanParticleOptions.codec(this);
                }
            };
        });
    
    /**
     * Register all particle types
     */
    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
    

}