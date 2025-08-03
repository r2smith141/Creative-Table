package com.craigsmods.creativeprototyper.client;

import com.craigsmods.creativeprototyper.client.model.CreativeTableItemModel;
import com.craigsmods.creativeprototyper.item.CreativeTableItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CreativeTableItemRenderer extends GeoItemRenderer<CreativeTableItem> {
    public CreativeTableItemRenderer() {
        super(new CreativeTableItemModel());
    }
    
    @Override
    public RenderType getRenderType(CreativeTableItem animatable, ResourceLocation texture, 
                                    MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, 
                           PoseStack poseStack, MultiBufferSource bufferSource, 
                           int packedLight, int packedOverlay) {
        // Only proceed if the item is a CreativeTableItem
        if (stack.getItem() instanceof CreativeTableItem) {
            // Scale the model down slightly for inventory/hand rendering
            
            poseStack.pushPose();
            
            
            // Render with enhanced lighting
            super.renderByItem(stack, displayContext, poseStack, bufferSource, 15728880, packedOverlay);
            
            poseStack.popPose();
        } else {
            // Fallback in case something goes wrong
            super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }
}