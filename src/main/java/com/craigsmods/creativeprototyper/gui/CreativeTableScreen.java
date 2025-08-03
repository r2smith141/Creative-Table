package com.craigsmods.creativeprototyper.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.craigsmods.creativeprototyper.CreativePrototyper;
import com.craigsmods.creativeprototyper.config.CreativePrototyperConfig;
import com.craigsmods.creativeprototyper.networking.ModMessages;
import com.craigsmods.creativeprototyper.networking.packet.CheckTableSnapshotC2SPacket;
import com.craigsmods.creativeprototyper.networking.packet.ResetAndScanC2SPacket;
import com.craigsmods.creativeprototyper.networking.packet.TeleportToDimensionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CreativeTableScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CreativePrototyper.MOD_ID, "textures/gui/creative_table_gui.png");
    private static final int WIDTH = 200;
    private static final int HEIGHT = 178;

    private final BlockPos tablePos;
    private int leftPos, topPos;
    private EditBox rangeField;
    private Button scanButton;
    private Button teleportButton;
    private boolean buildComplete = false;
    private int scanProgress = 0;
    private Component statusText = Component.literal("");

    public CreativeTableScreen(BlockPos tablePos) {
        super(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".creative_table"));
        this.tablePos = tablePos;
    }

    @Override
    protected void init() {
        leftPos = (width - WIDTH) / 2;
        topPos = (height - HEIGHT) / 2;

        rangeField = new EditBox(font, leftPos + 80, topPos + 30, 60, 20, 
        Component.translatable("gui." + CreativePrototyper.MOD_ID + ".range_field"));
        rangeField.setValue(String.valueOf(CreativePrototyperConfig.COMMON.defaultScanRadius.get())); // Default value from config
        rangeField.setFilter(this::isValidRange);
        
        // Scan area button
        scanButton = Button.builder(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".scan_area"), (button) -> {
            try {
                int range = Integer.parseInt(rangeField.getValue());
                // Clamp range between 8 and 64
                range = Math.max(8, Math.min(64, range));
                
                // Send packet to server to scan the area and reset blocks placed flag
                ModMessages.sendToServer(new ResetAndScanC2SPacket(tablePos, range));
                
                // Update button state immediately to show scanning in progress
                button.setMessage(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".scanning"));
                button.active = false;
                statusText = Component.literal("Scanning area and building in creative dimension...");
                
                // Disable teleport button during scanning
                teleportButton.active = false;
                
            } catch (NumberFormatException e) {
                // Invalid number, reset to default
                rangeField.setValue("16");
            }
        }).pos(leftPos + 20, topPos + 60).size(WIDTH - 40, 20).build();

        // Teleport button (initially disabled, will enable after checking build status)
        teleportButton = Button.builder(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".teleport"), (button) -> {
            // Send packet to server to teleport player
            ModMessages.sendToServer(new TeleportToDimensionC2SPacket(tablePos));
            onClose(); // Close the screen
        }).pos(leftPos + 20, topPos + 90).size(WIDTH - 40, 20).build();
        teleportButton.active = buildComplete;

        // Add widgets to the screen
        addRenderableWidget(rangeField);
        addRenderableWidget(scanButton);
        addRenderableWidget(teleportButton);
        
        // Check if this table already has a snapshot and build status
        checkForExistingSnapshot();
    }
    
    /**
     * Send a packet to the server to check if this table already has a snapshot
     */
    private void checkForExistingSnapshot() {
        // Set initial state
        teleportButton.active = false;
        statusText = Component.literal("Checking table status...");
        
        // Send packet to check for existing snapshot
        ModMessages.sendToServer(new CheckTableSnapshotC2SPacket(tablePos));
    }
    
    /**
     * Called when the server notifies that scanning is complete
     */
    public void setScanComplete(int blockCount) {
        buildComplete = true;
        teleportButton.active = true;
        scanButton.setMessage(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".scan_again"));
        scanButton.active = true;
        statusText = Component.literal("Build complete! " + blockCount + " blocks placed. Ready to teleport.");
    }
    
    /**
     * Called when we receive a response from the server about an existing snapshot
     */
    public void setHasExistingSnapshot(boolean hasSnapshot, int blockCount, boolean isBuildComplete, int progress) {
        if (hasSnapshot) {
            this.buildComplete = isBuildComplete;
            this.scanProgress = progress;
            
            if (isBuildComplete) {
                teleportButton.active = true;
                statusText = Component.literal("Ready to teleport! Creative space contains " + blockCount + " blocks.");
            } else {
                teleportButton.active = false;
                statusText = Component.literal("Building in progress: " + progress + "% complete. Please wait.");
            }
            
            scanButton.setMessage(Component.translatable("gui." + CreativePrototyper.MOD_ID + ".scan_again"));
        } else {
            buildComplete = false;
            teleportButton.active = false;
            statusText = Component.literal("Scan an area to begin.");
        }
    }
    
    /**
     * Update build progress
     */
    public void updateBuildProgress(int progress) {
        this.scanProgress = progress;
        
        if (progress < 100) {
            buildComplete = false;
            teleportButton.active = false;
            statusText = Component.literal("Building in progress: " + progress + "% complete. Please wait.");
        } else {
            buildComplete = true;
            teleportButton.active = true;
            statusText = Component.literal("Build complete! Ready to teleport.");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        
        // Draw the background texture
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, WIDTH, HEIGHT);
        
        // Draw title
        graphics.drawCenteredString(font, title, leftPos + WIDTH / 2, topPos + 10, 0xFFFFFF);
        
        // Draw range label
        graphics.drawString(font, Component.translatable("gui." + CreativePrototyper.MOD_ID + ".scan_range"), 
                           leftPos + 20, topPos + 35, 0xFFFFFF);
        
        // Draw status text
        graphics.drawCenteredString(font, statusText, leftPos + WIDTH / 2, topPos + 145, 0xFFFFFF);
        
        // Draw progress bar if building is in progress
        if (!buildComplete && scanProgress > 0) {
            // Draw progress bar background
            graphics.fill(leftPos + 20, topPos + 120, leftPos + WIDTH - 20, topPos + 130, 0xFF333333);
            
            // Draw progress bar fill
            int fillWidth = (int)((WIDTH - 40) * (scanProgress / 100.0f));
            graphics.fill(leftPos + 20, topPos + 120, leftPos + 20 + fillWidth, topPos + 130, 0xFF33AA33);
            
            // Draw percentage text
            String progressText = scanProgress + "%";
            int textWidth = font.width(progressText);
            graphics.drawString(font, progressText, leftPos + WIDTH / 2 - textWidth / 2, topPos + 121, 0xFFFFFF);
        }
        
        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private boolean isValidRange(String text) {
        if (text.isEmpty()) return true;
        try {
            int val = Integer.parseInt(text);
            int maxRadius = CreativePrototyperConfig.COMMON.maxScanRadius.get();
            return val >= 1 && val <= maxRadius; // Allow range between 1 and maxRadius
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}