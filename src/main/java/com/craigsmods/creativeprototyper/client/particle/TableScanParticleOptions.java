package com.craigsmods.creativeprototyper.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.craigsmods.creativeprototyper.registry.ModParticles;

/**
 * Particle options for table scan particles with target position
 */
public class TableScanParticleOptions implements ParticleOptions {
    private final BlockState state;
    private final float targetX;
    private final float targetY;
    private final float targetZ;
    private final float arcHeight;

    public static final Deserializer<TableScanParticleOptions> DESERIALIZER = new Deserializer<>() {
        @Override
        public TableScanParticleOptions fromCommand(ParticleType<TableScanParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            BlockState blockState = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false).blockState();
            reader.expect(' ');
            float targetX = reader.readFloat();
            reader.expect(' ');
            float targetY = reader.readFloat();
            reader.expect(' ');
            float targetZ = reader.readFloat();
            reader.expect(' ');
            float archeight = reader.readFloat();
            return new TableScanParticleOptions(blockState, targetX, targetY, targetZ,archeight);
        }
        
        @Override
        public TableScanParticleOptions fromNetwork(ParticleType<TableScanParticleOptions> type, FriendlyByteBuf buf) {
            return new TableScanParticleOptions(
                Block.stateById(buf.readInt()),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat() // Read arc height
            );
    }
};
    public static Codec<TableScanParticleOptions> codec(ParticleType<TableScanParticleOptions> type) {
        return Codec.unit(() -> new TableScanParticleOptions(
            net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
            0.0F, 0.0F, 0.0F,0.0f)); // Placeholder codec for registration
    }

    public TableScanParticleOptions(BlockState state, float targetX, float targetY, float targetZ,float arcHeight) {
        this.state = state;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.arcHeight = arcHeight;
    }
    
    @Override
    public ParticleType<?> getType() {
        return ModParticles.TABLE_SCAN_PARTICLE.get();
    }
    
    @Override
    public void writeToNetwork(FriendlyByteBuf buf) {
        buf.writeInt(Block.getId(this.state));
        buf.writeFloat(this.targetX);
        buf.writeFloat(this.targetY);
        buf.writeFloat(this.targetZ);
        buf.writeFloat(this.arcHeight);
    }
    
    @Override
    public String writeToString() {
        return String.format("%s %s %.2f %.2f %.2f", 
            BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), 
            BlockStateParser.serialize(this.state),
            this.targetX, this.targetY, this.targetZ);
    }
    
    public BlockState getState() {
        return this.state;
    }
    
    public float getTargetX() {
        return this.targetX;
    }
    
    public float getTargetY() {
        return this.targetY;
    }
    
    public float getTargetZ() {
        return this.targetZ;
    }
    public float getArcHeight() {
        return this.arcHeight;
    }

 }
