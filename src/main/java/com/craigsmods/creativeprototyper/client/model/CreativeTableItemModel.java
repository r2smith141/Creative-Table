package com.craigsmods.creativeprototyper.client.model;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.item.CreativeTableItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CreativeTableItemModel extends GeoModel<CreativeTableItem> {
    @Override
    public ResourceLocation getModelResource(CreativeTableItem object) {
        // Use the same model file as the block
        return new ResourceLocation(CreativePrototyper.MOD_ID, "geo/block/creative_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CreativeTableItem object) {
        // Use the same texture file as the block
        return new ResourceLocation(CreativePrototyper.MOD_ID, "textures/block/creative_table.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CreativeTableItem object) {
        // Use the same animation file name structure as in your block
        return new ResourceLocation(CreativePrototyper.MOD_ID, "animations/creative_table.animation.json");
    }
}