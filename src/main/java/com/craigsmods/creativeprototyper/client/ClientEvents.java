package com.craigsmods.creativeprototyper.client;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.client.particle.TableScanParticle;
import com.craigsmods.creativeprototyper.dimension.CreativeSkyProperties;
import com.craigsmods.creativeprototyper.registry.ModBlockEntities;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.craigsmods.creativeprototyper.registry.ModParticles;

import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
        // Register our custom sky properties for the creative dimension
        event.register(
            ModDimensions.CREATIVE_DIMENSION_LEVEL_KEY.location(), 
            new CreativeSkyProperties()
        );
    }
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CREATIVE_TABLE.get(), CreativeTableRenderer::new);
    }
    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        System.out.println("=== RegisterParticleProvidersEvent fired ===");
        System.out.println("Registering particle provider for: " + 
            ModParticles.TABLE_SCAN_PARTICLE.getId());
        
        try {
            event.registerSpriteSet(ModParticles.TABLE_SCAN_PARTICLE.get(), sprite -> {
                System.out.println("Creating sprite-based factory for table_scan particle");
                return (options, level, x, y, z, xSpeed, ySpeed, zSpeed) -> {
                    System.out.println("Factory creating particle at " + x + "," + y + "," + z);
                    return new TableScanParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, 
                        options.getState(), new BlockPos((int)x, (int)y, (int)z),
                        options.getTargetX(), options.getTargetY(), options.getTargetZ(), options.getArcHeight());
                };
            });
            System.out.println("Successfully registered particle provider");
        } catch (Exception e) {
            System.out.println("Error registering particle provider: " + e.getMessage());
            e.printStackTrace();
        }
    }
}