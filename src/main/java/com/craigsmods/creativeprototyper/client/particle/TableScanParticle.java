package com.craigsmods.creativeprototyper.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.util.Mth;

/**
 * Custom block particle for table scanning that maintains a controlled trajectory
 * and ignores gravity
 */
@OnlyIn(Dist.CLIENT)
public class TableScanParticle extends TerrainParticle {
    private final float targetX;
    private final float targetY;
    private final float targetZ;
    private final float initialDistance;
    private final float arcHeight; // Arc height for parabolic motion
    private float progress = 0.0F;
    private final Vec3 startPos; // Store the starting position
    
    public TableScanParticle(ClientLevel level, double x, double y, double z, 
                              double xSpeed, double ySpeed, double zSpeed, 
                              BlockState state, BlockPos pos, 
                              float targetX, float targetY, float targetZ, float arcHeight) {
        super(level, x, y, z, 0, 0, 0, state);
        
        // Store start position
        this.startPos = new Vec3(x, y, z);
        
        // Store target position and arc height
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.arcHeight = arcHeight;
        
        // Calculate initial distance to target
        float dx = this.targetX - (float)x;
        float dy = this.targetY - (float)y;
        float dz = this.targetZ - (float)z;
        this.initialDistance = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Set varying lifetime based on distance and a bit of randomness
        this.lifetime = (int)(20 + initialDistance * 1.5 + random.nextInt(10));
        
        // Disable gravity
        this.gravity = 0.0F;
        
        // Slightly vary particle size
        this.quadSize *= 0.8F + random.nextFloat() * 0.4F; // 0.8 to 1.2 times normal size
    }
    
    @Override
    public void tick() {
        // Update previous position
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        // Update progress and fade
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Calculate progress (0.0 - 1.0)
        progress = Math.min(1.0F, (float)age / (float)lifetime);
        
        // Ease-in-out function for smooth acceleration and deceleration
        float easedProgress = progress < 0.5F
            ? 2.0F * progress * progress
            : 1.0F - (float)Math.pow(-2.0F * progress + 2.0F, 2) / 2.0F;
        
        // Calculate arc movement
        // Linear interpolation from start to target
        double linearX = Mth.lerp(easedProgress, startPos.x, targetX);
        double linearZ = Mth.lerp(easedProgress, startPos.z, targetZ);
        
        // For Y, add a parabolic arc that rises then falls
        // Using a sin curve for the arc - peaks at progress=0.5
        double arcFactor = Math.sin(easedProgress * Math.PI) * arcHeight;
        double linearY = Mth.lerp(easedProgress, startPos.y, targetY);
        
        // Set position with arc
        this.x = linearX;
        this.y = linearY + arcFactor; // Add the arc effect
        this.z = linearZ;
        
        // Update alpha for fading effect
        // Fade in during first 20% of lifetime
        // Fade out during last 30% of lifetime
        if (progress < 0.2F) {
            this.alpha = progress / 0.2F;
        } else if (progress > 0.7F) {
            this.alpha = 1.0F - (progress - 0.7F) / 0.3F;
        } else {
            this.alpha = 1.0F;
        }
        
        // Gradually decrease size as it approaches target
        if (progress > 0.7F) {
            float sizeReduction = (progress - 0.7F) / 0.3F;
            this.quadSize = this.quadSize * (1.0F - sizeReduction * 0.3F);
        }
    }
    
    
    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<TableScanParticleOptions> {
        @Override
        public Particle createParticle(TableScanParticleOptions options, ClientLevel level, 
                                      double x, double y, double z, 
                                      double xSpeed, double ySpeed, double zSpeed) {
            BlockState blockState = options.getState();
            BlockPos blockPos = new BlockPos((int)x, (int)y, (int)z);
            
            return new TableScanParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, 
                                       blockState, blockPos, 
                                       options.getTargetX(), options.getTargetY(), options.getTargetZ(),
                                       options.getArcHeight());
        }
    }
}