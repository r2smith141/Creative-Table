package com.craigsmods.creativeprototyper.client.model;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class CreativeTableModel extends GeoModel<CreativeTableBlockEntity> {
    @Override
    public ResourceLocation getModelResource(CreativeTableBlockEntity object) {
        return new ResourceLocation(CreativePrototyper.MOD_ID, "geo/block/creative_table.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CreativeTableBlockEntity object) {
        return new ResourceLocation(CreativePrototyper.MOD_ID, "textures/block/creative_table.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CreativeTableBlockEntity object) {
        return new ResourceLocation(CreativePrototyper.MOD_ID, "animations/creative_table.animation.json");
    }
}