package com.craigsmods.creativeprototyper.registry;

import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.item.CreativeTableItem;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreativePrototyper.MOD_ID);

    public static final RegistryObject<Item> CREATIVE_TABLE_ITEM = ITEMS.register("creative_table",
            () -> new CreativeTableItem(ModBlocks.CREATIVE_TABLE.get(), 
                    new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}