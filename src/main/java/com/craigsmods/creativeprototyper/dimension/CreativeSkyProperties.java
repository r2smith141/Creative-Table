package com.craigsmods.creativeprototyper.dimension;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.joml.Matrix4f;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.registry.ModDimensions;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

@OnlyIn(Dist.CLIENT)
public class CreativeSkyProperties extends DimensionSpecialEffects {
    // Use a fully white sky
    private static final Vec3 WHITE_FOG_COLOR = new Vec3(1.0, 1.0, 1.0);

    public CreativeSkyProperties() {
        // Parameters: cloud height, has skylight, is bright, use fog
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, true, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return WHITE_FOG_COLOR;
    }

    @Override
    public boolean isFoggyAt(int x, int y) {
        return false; // No fog
    }

    // Customize cloud rendering - returning true disables clouds
    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, 
                               double camX, double camY, double camZ, 
                               Matrix4f projectionMatrix) {
        return true; // Skip rendering clouds
    }

    // Customize sky rendering - returning true disables default sky
    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, 
                            Camera camera, Matrix4f projectionMatrix, 
                            boolean isFoggy, Runnable setupFog) {
        // Custom sky rendering - pure white
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        
        // Set up shader for rendering a solid color
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        // Get a tessellator for drawing
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        
        // Draw a full-screen white quad
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        
        // Draw with full white color (RGBA)
        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        float alpha = 1.0F;
        
        // Draw the quad covering the entire sky
        bufferbuilder.vertex(-100.0D, -100.0D, -100.0D).color(red, green, blue, alpha).endVertex();
        bufferbuilder.vertex(-100.0D, -100.0D, 100.0D).color(red, green, blue, alpha).endVertex();
        bufferbuilder.vertex(100.0D, -100.0D, 100.0D).color(red, green, blue, alpha).endVertex();
        bufferbuilder.vertex(100.0D, -100.0D, -100.0D).color(red, green, blue, alpha).endVertex();
        
        tesselator.end();
        
        // Clean up rendering state
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        
        return true; // Skip default rendering
    }
}