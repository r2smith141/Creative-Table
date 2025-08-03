package com.craigsmods.creativeprototyper.client;

import com.craigsmods.creativeprototyper.block.CreativeTableBlockEntity;
import com.craigsmods.creativeprototyper.client.particle.TableScanParticleOptions;
import com.craigsmods.creativeprototyper.registry.ModParticles;
import com.craigsmods.creativeprototyper.util.AreaSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Vector3f;

/**
 * Handles particle effects for the Creative Table scanning process
 */
public class ScanningParticleManager {
    private static final Random RANDOM = new Random();
    private static final int PARTICLES_PER_BLOCK = 2; 
    private static final int PARTICLE_DISTANCE = 32; // Maximum distance to send particles to clients
    
    // Track ongoing scan particle sessions
    private static final ConcurrentHashMap<BlockPos, ScanParticleSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Start a new particle session for a table scan
     */
public static void startScanParticles(CreativeTableBlockEntity table, BlockPos center, int radius, 
                                     ResourceKey<Level> sourceDimension) {
    BlockPos tablePos = table.getBlockPos();
    if (activeSessions.containsKey(tablePos)) {
        // Session already exists, just update it with new dimension info
        activeSessions.get(tablePos).reset(center, radius, sourceDimension);
        return;
    }
    
    // Create a new session with dimension info
    ScanParticleSession session = new ScanParticleSession(table, center, radius, sourceDimension);
    activeSessions.put(tablePos, session);
    System.out.println("Started scan particles for table at " + tablePos + 
                       " sampling from dimension " + 
                       (sourceDimension != null ? sourceDimension.location() : "unknown"));
}
    
    /**
     * Stop particles for a specific table
     */
    public static void stopScanParticles(BlockPos tablePos) {
        activeSessions.remove(tablePos);
        System.out.println("Stopped scan particles for table at " + tablePos);
    }
    
    /**
     * Update all active particle sessions
     * Call this from a tick event
     */
    public static void tickParticles(Level level) {
        if (level.isClientSide()) return; // Server-side only
        
        // Process each active session
        activeSessions.forEach((tablePos, session) -> {
            if (session.isDone()) {
                activeSessions.remove(tablePos);
            } else {
                session.tick((ServerLevel) level);
            }
        });
    }
    
    /**
     * Represents an ongoing particle session for a table scan
     */
    private static class ScanParticleSession {
        private final CreativeTableBlockEntity table;
        private final Set<BlockPos> scannedPositions = new HashSet<>();
        private BlockPos center;
        private int radius;
        private int tickCount = 0;
        private boolean isDone = false;
        private ResourceKey<Level> sourceDimension; // Add dimension tracking
        
        // Update constructor
        public ScanParticleSession(CreativeTableBlockEntity table, BlockPos center, int radius,
                                 ResourceKey<Level> sourceDimension) {
            this.table = table;
            this.center = center;
            this.radius = radius;
            this.sourceDimension = sourceDimension;
        }
        
        // Update reset method
        public void reset(BlockPos center, int radius, ResourceKey<Level> sourceDimension) {
            this.center = center;
            this.radius = radius;
            this.sourceDimension = sourceDimension;
            this.tickCount = 0;
            this.isDone = false;
            this.scannedPositions.clear();
        }
        /**
         * Check if this session is complete
         */
        public boolean isDone() {
            return isDone || !table.isScanning();
        }
        
        /**
         * Process a tick for this particle session
         */
        public void tick(ServerLevel level) {
            tickCount++;
            
            // Only spawn particles every few ticks
            if (tickCount % 2 != 0) return;
            
            // Check if scanning is still active
            if (!table.isScanning()) {
                isDone = true;
                return;
            }
            
            // Get the correct source level using the stored dimension
            ServerLevel sourceLevel = level.getServer().getLevel(sourceDimension);
            if (sourceLevel == null) {
                // Fallback to current level if source dimension not available
                System.out.println("Warning: Source dimension " + 
                                   (sourceDimension != null ? sourceDimension.location() : "null") + 
                                   " not available, using current level");
                sourceLevel = level;
            } else {
                System.out.println("Using source dimension: " + sourceLevel.dimension().location());
            }
            
            BlockPos tablePos = table.getBlockPos();
            Vec3 tableCenter = Vec3.atCenterOf(tablePos).add(0, 0.5, 0);
            
            // Process blocks to generate particles
            int particlesToSpawn = 8 + RANDOM.nextInt(8);
            
            for (int i = 0; i < particlesToSpawn; i++) {
                // Generate a random position within the scan radius
                BlockPos randomPos = getRandomPosInRadius();
                if (scannedPositions.contains(randomPos)) continue;
                
                scannedPositions.add(randomPos);
                
                // Get the block at this position FROM THE CORRECT DIMENSION
                BlockState state = sourceLevel.getBlockState(randomPos);
                if (state.isAir()) continue;
                
                System.out.println("Sampled " + state.getBlock().getName().getString() + 
                                  " from dimension " + sourceLevel.dimension().location());
                
                // Create funnel particles using our custom particle type
                spawnFunnelParticles(level, tablePos, tableCenter, state);
            }
            
            // Also spawn some enchantment table style particles
            if (RANDOM.nextInt(3) == 0) {
                spawnEnchantmentParticles(level, tablePos);
            }
            
            // Check if we've covered enough of the area
            double coverage = (double) scannedPositions.size() / 
                              (Math.pow(radius * 2 + 1, 3) * 0.3); // 30% coverage is enough
            if (coverage >= 1.0) {
                isDone = true;
            }
        }
        
        /**
         * Generate a random position within the scan radius
         */
        private BlockPos getRandomPosInRadius() {
            // Bias toward higher Y values to avoid too many underground particles
            int x = center.getX() + RANDOM.nextInt(radius * 2 + 1) - radius;
            int minY = Math.max(center.getY() - radius/2, 0);
            int maxY = center.getY() + radius;
            int y = minY + RANDOM.nextInt(maxY - minY + 1);
            int z = center.getZ() + RANDOM.nextInt(radius * 2 + 1) - radius;
            return new BlockPos(x, y, z);
        }
        
        /**
         * Spawn funnel-shaped particles flowing into the table using custom particles
         */
        private void spawnFunnelParticles(ServerLevel level, BlockPos tablePos, Vec3 tableCenter, BlockState state) {
            try {
                // Reduce the number of particles by only proceeding 40% of the time
                if (RANDOM.nextFloat() > 0.4f) {
                    return;
                }
                
                // Parameters for more varied funnel
                double circleRadius = 3.0 + RANDOM.nextDouble() * 2.0; // 3.0-5.0 block radius (wider)
                double height = 2.0 + RANDOM.nextDouble() * 3.0; // 2.0-5.0 blocks above table (more varied)
                double angle = RANDOM.nextDouble() * Math.PI * 2; // Random angle in circle
                
                // Start position - on the circle above the table with more variance
                double startX = tableCenter.x + Math.cos(angle) * circleRadius;
                double startY = tableCenter.y + height;
                double startZ = tableCenter.z + Math.sin(angle) * circleRadius;
                
                // For the arc effect, add an arc parameter
                float arcHeight = 0.5f + RANDOM.nextFloat() * 1.0f; // 0.5-1.5 blocks arc height
                
                // Use our custom particle options with target position and arc height
                TableScanParticleOptions options = new TableScanParticleOptions(
                    state, 
                    (float)tableCenter.x, 
                    (float)tableCenter.y, 
                    (float)tableCenter.z,
                    arcHeight // Add arc height parameter
                );
                
                // Initial velocity hint (not as important now because our custom particle handles motion)
                double velX = (tableCenter.x - startX) * 0.05;
                double velY = -0.02;
                double velZ = (tableCenter.z - startZ) * 0.05;
                
                // Send directly to players
                for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                    // Calculate distance to player
                    double distance = player.position().distanceTo(tableCenter);
                    if (distance <= 64.0) {
                        player.connection.send(new ClientboundLevelParticlesPacket(
                            options,
                            false, // alwaysVisible 
                            startX, startY, startZ,
                            (float)velX, (float)velY, (float)velZ,
                            0.0f, // speed
                            1 // count
                        ));
                    }
                }
            } catch (Exception e) {
                System.out.println("Error spawning particles: " + e.getMessage());
            }
        }
        /**
         * Spawn enchantment table style particles
         */
        private void spawnEnchantmentParticles(ServerLevel level, BlockPos tablePos) {
            // Calculate base position (above the table)
            double x = tablePos.getX() + 0.5;
            double y = tablePos.getY() + 1.2;
            double z = tablePos.getZ() + 0.5;
            
            // Spawn a circle of particles
            double radius = 0.5 + RANDOM.nextDouble() * 0.5;
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            
            for (int i = 0; i < 3; i++) {
                double circleX = x + Math.cos(angle + i * (Math.PI * 2 / 3)) * radius;
                double circleZ = z + Math.sin(angle + i * (Math.PI * 2 / 3)) * radius;
                
                // Calculate velocity toward the table
                double velX = (x - circleX) * 0.1;
                double velY = -0.03 - RANDOM.nextDouble() * 0.02;
                double velZ = (z - circleZ) * 0.1;
                
                // Spawn enchant particles
                level.sendParticles(
                    ParticleTypes.ENCHANT,
                    circleX, y, circleZ, // position
                    1, // count
                    velX, velY, velZ, // velocity
                    0.1 // speed
                );
            }
        }
    }
}