package com.craigsmods.creativeprototyper.dimension;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
public class CreativeSkyRenderer extends DimensionSpecialEffects {
    // White color for sky and fog
    private static final Vec3 WHITE_COLOR = new Vec3(1.0, 1.0, 1.0);
    
    public CreativeSkyRenderer() {
        // Parameters: cloud height, has skylight, sky type, fog, brightness dependent fog
        super(Float.NaN, true, SkyType.NONE, false, false);
    }
    
    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        return WHITE_COLOR; // Always return white
    }
    
    @Override
    public boolean isFoggyAt(int x, int y) {
        return false; // No fog
    }
    
    // No need to override renderSky or renderClouds for this simplified version
    // The SkyType.NONE and Float.NaN cloud height should disable most of the default rendering
    
    // Register the sky renderer
    @Mod.EventBusSubscriber(modid = CreativePrototyper.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class RegistrationHandler {
        @SubscribeEvent
        public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
            event.register(
                new ResourceLocation(CreativePrototyper.MOD_ID, "creative_sky"), 
                new CreativeSkyRenderer()
            );
            System.out.println("Registered creative sky renderer!");
        }
    }
}
