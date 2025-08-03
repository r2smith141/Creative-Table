package com.craigsmods.creativeprototyper.item;

import java.util.function.Consumer;

import com.craigsmods.creativeprototyper.block.CreativeTableBlock;
import com.craigsmods.creativeprototyper.client.CreativeTableItemRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CreativeTableItem extends BlockItem implements GeoItem {
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("animation.creative_table.idle");
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    public CreativeTableItem(CreativeTableBlock block, Properties properties) {
        super(block, properties);
        // Register for server-side animation syncing
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    // Client initialization for custom renderer
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CreativeTableItemRenderer renderer = null;
            
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new CreativeTableItemRenderer();
                }
                return this.renderer;
            }
        });
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
            new AnimationController<>(this, "controller", 0, state -> {
                state.getController().setAnimation(IDLE_ANIMATION);
                return state.setAndContinue(IDLE_ANIMATION);
            })
        );
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}