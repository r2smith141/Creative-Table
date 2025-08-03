package com.craigsmods.creativeprototyper;

import com.craigsmods.creativeprototyper.client.ScanningParticleManager;
import com.craigsmods.creativeprototyper.config.CreativePrototyperConfig;
import com.craigsmods.creativeprototyper.networking.ModMessages;
import com.craigsmods.creativeprototyper.registry.ModBlockEntities;
import com.craigsmods.creativeprototyper.registry.ModBlocks;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.craigsmods.creativeprototyper.registry.ModItems;
import com.craigsmods.creativeprototyper.registry.ModParticles;
import com.craigsmods.creativeprototyper.util.AsyncAreaScanner;
import com.craigsmods.creativeprototyper.util.BannedBlocksManager;
//import com.craigsmods.creativeprototyper.util.DataCleanupManager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import software.bernie.geckolib.GeckoLib;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(CreativePrototyper.MOD_ID)
public class CreativePrototyper {
    public static final String MOD_ID = "creativeprototyper";
    public static final String LOGGER = null;

    public CreativePrototyper(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        CreativePrototyperConfig.register();
        
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModParticles.register(modEventBus);
        
        GeckoLib.initialize();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);
        modEventBus.addListener(this::handleConfigEvents);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }
    private void commonSetup(final FMLCommonSetupEvent event) {

        event.enqueueWork(ModMessages::register);
        AsyncAreaScanner.init();

    }
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        /* 
        tableData.clear();
        activeTableKeys.clear();
        tablePlacementPositions.clear();*/
    }
    private void enqueueIMC(final InterModEnqueueEvent event) {

        InterModComms.sendTo("forge", "dimensions", () -> {
            System.out.println("IMC: Registering dimension information");
            return new ResourceLocation(MOD_ID, "creative_dimension");
        });
    }
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CREATIVE_TABLE_ITEM.get());
        }
    }

    private void handleConfigEvents(final ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Loading || event instanceof ModConfigEvent.Reloading) {
            BannedBlocksManager.initialize();
        }
    }
    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event) {
        System.out.println("Client setup event - ModParticles.TABLE_SCAN_PARTICLE registered");
        
        ResourceLocation particleJson = new ResourceLocation(CreativePrototyper.MOD_ID, 
            "particles/table_scan.json");
        
        try {
            Resource resource = Minecraft.getInstance().getResourceManager()
                .getResource(particleJson).orElse(null);
            System.out.println("Resource check: table_scan.json exists: " + (resource != null));
        } catch (Exception e) {
            System.out.println("Resource check failed: " + e.getMessage());
        }
    }

class DimensionInit {
    public static void registerDimension() {
        System.out.println("Registering Creative Dimension...");
    }
}
}