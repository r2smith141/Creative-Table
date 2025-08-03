package com.craigsmods.creativeprototyper.registry;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreativePrototyper.MOD_ID);

    // Register the Creative Table Block Entity
    public static final RegistryObject<BlockEntityType<CreativeTableBlockEntity>> CREATIVE_TABLE = 
        BLOCK_ENTITIES.register("creative_table",
            () -> BlockEntityType.Builder.of(
                CreativeTableBlockEntity::new, 
                ModBlocks.CREATIVE_TABLE.get()
            ).build(null)
        );

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}