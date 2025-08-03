package com.craigsmods.creativeprototyper.block;



import java.util.UUID;

import javax.annotation.Nullable;

import com.craigsmods.creativeprototyper.dimension.CreativeDimensionManager;
import com.craigsmods.creativeprototyper.gui.CreativeTableScreen;
import com.craigsmods.creativeprototyper.util.PlayerDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class CreativeTableBlock extends Block implements EntityBlock {
    public CreativeTableBlock(Properties properties) {

        super(properties
            .strength(2.0F, 1200.0F)
            .noOcclusion()
            .lightLevel((state) -> 15)
        );
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {

        return RenderShape.ENTITYBLOCK_ANIMATED;
    }
        @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; 
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return false; 
    }
    
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F; 
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction direction) {
        return false; 
    }
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false; 
    }
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return (lvl, pos, blockState, blockEntity) -> {
            if (blockEntity instanceof CreativeTableBlockEntity creativeTable) {
                CreativeTableBlockEntity.tick(lvl, pos, blockState, creativeTable);
            }
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeTableBlockEntity(pos, state);
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, 
                               Player player, InteractionHand hand, BlockHitResult hit) {

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
    

        
        if (level.isClientSide()) {
            // Client-side handling
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CreativeTableBlockEntity tableEntity) {

                if (tableEntity.isReturnPortal()) {

                    player.displayClientMessage(
                        Component.literal("Right-click to return to your original location"), false);
                } else {


                    openCreativeTableScreen(pos);
                }
            }
            return InteractionResult.SUCCESS;
        } else {

            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CreativeTableBlockEntity tableEntity) {
                    
                    if (tableEntity.isReturnPortal()) {

                        handleReturnPortal(serverPlayer, tableEntity);
                        

                        level.sendBlockUpdated(pos, state, state, 3);
                        
 
                        return InteractionResult.SUCCESS;
                    } else {

                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
    }
    
private void handleReturnPortal(ServerPlayer player, CreativeTableBlockEntity tableEntity) {
    BlockPos sourcePos = tableEntity.getSourcePosition();
    ResourceKey<Level> sourceDim = tableEntity.getSourceDimension();
    if (!PlayerDataManager.isPlayerInCreative(player.getUUID())) {
        return;
    }
    
    UUID playerId = player.getUUID();

    GameType originalGameMode = GameType.SURVIVAL;
    if (CreativeDimensionManager.getOriginalGameModes().containsKey(playerId)) {
        originalGameMode = CreativeDimensionManager.getOriginalGameModes().get(playerId);
        System.out.println("Found preserved game mode for player: " + originalGameMode.getName());
    }
    
    if (sourcePos != null && sourceDim != null) {

        ServerLevel sourceLevel = player.getServer().getLevel(sourceDim);
        if (sourceLevel != null) {
            PlayerDataManager.returnToSurvivalDimension(player, sourcePos);
            

            player.teleportTo(
                sourceLevel,
                sourcePos.getX() + 0.5,
                sourcePos.getY() + 1.0,
                sourcePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
            );
            

            player.setGameMode(originalGameMode);
            
            player.displayClientMessage(
                Component.literal("Returned to original location"), false);
            
            System.out.println("Successfully returned player to " + sourceDim.location() + 
                               " at " + sourcePos + " with game mode " + originalGameMode.getName());
        } else {
            player.displayClientMessage(
                Component.literal("Error: Could not find original dimension"), false);
        }
    } else {
        player.displayClientMessage(
            Component.literal("Error: Missing return location data"), false);
    }
}
    
    @OnlyIn(Dist.CLIENT)
    private void openCreativeTableScreen(BlockPos pos) {
        Minecraft.getInstance().setScreen(new CreativeTableScreen(pos));
    }


}