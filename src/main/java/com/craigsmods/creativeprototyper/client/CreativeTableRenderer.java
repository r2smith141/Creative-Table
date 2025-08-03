package com.craigsmods.creativeprototyper.client;

import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.client.model.CreativeTableModel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CreativeTableRenderer extends GeoBlockRenderer<CreativeTableBlockEntity> {
    public CreativeTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new CreativeTableModel());
    }

    @Override
    public RenderType getRenderType(CreativeTableBlockEntity animatable, ResourceLocation texture, 
                                  MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucentCull(getTextureLocation(animatable));
    }
    
    @Override
    public void renderRecursively(PoseStack poseStack, CreativeTableBlockEntity animatable, GeoBone bone, 
                               RenderType renderType, MultiBufferSource bufferSource, 
                               VertexConsumer buffer, boolean isReRender, float partialTick, 
                               int packedLight, int packedOverlay, 
                               float red, float green, float blue, float alpha) {
        
        // Check if this is the bone we want to recolor
        if (bone.getName().equals("Eye") && animatable != null) {
            // Check if the block entity is in scanning state
            boolean isScanning = animatable.isScanning();
            boolean isReturnPortal = animatable.isReturnPortal();
            
            // Update particle tracking exactly when we detect a scanning table
            if (isScanning) {
                // Use bright blue color for scanning
                ClientParticleHandler.registerScanningTable(animatable.getBlockPos());
                super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, 
                                      buffer, isReRender, partialTick, packedLight, packedOverlay, 
                                      0.2f, 0.4f, 1.0f, 0.7f);
            } else {
                // If not scanning, ensure we remove from active tables
                ClientParticleHandler.unregisterScanningTable(animatable.getBlockPos());
                
                if (isReturnPortal || animatable.isBuildingComplete()) {
                    // Use bright Yellow color for returning
                    super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, 
                                          buffer, isReRender, partialTick, packedLight, packedOverlay, 
                                          1.0f, 1.0f, 0.0f, 0.7f);
                } else {
                    // Use green color for idle
                    super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, 
                                          buffer, isReRender, partialTick, packedLight, packedOverlay, 
                                          0.0f, 1.0f, 0.0f, 0.7f);
                }
            }
        } else {
            // Use normal colors for other bones
            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, 
                                  buffer, isReRender, partialTick, packedLight, packedOverlay, 
                                  red, green, blue, alpha);
        }
    }

    @Override
    public void actuallyRender(PoseStack poseStack, CreativeTableBlockEntity animatable, BakedGeoModel model,
                             RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                             boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                             float red, float green, float blue, float alpha) {
        
        // Enhance the lighting
        int enhancedLight = LightTexture.pack(15, LightTexture.sky(packedLight));
        
        // Save the state
        poseStack.pushPose();
        // Call the parent render method with our enhanced light
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                           isReRender, partialTick, enhancedLight, packedOverlay,
                           red, green, blue, alpha);
        // Restore state
        poseStack.popPose();
    }
}