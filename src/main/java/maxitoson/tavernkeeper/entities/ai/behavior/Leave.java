package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Behavior for customer leaving the tavern and despawning
 * Customer returns to their spawn position, then despawns
 */
public class Leave extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_WALK_DURATION = 400; // Maximum 20 seconds (safety timeout)
    private static final int REACHED_SPAWN_DISTANCE = 2; // How close to get to spawn position before despawning
    private static final float WALK_SPEED = 0.6F; // Walk slightly faster when leaving
    
    private long startTime = 0;
    private BlockPos startPos = null;
    private BlockPos targetSpawnPos = null;
    
    public Leave() {
        super(
            ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, // Only run if not already walking
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
            ),
            MAX_WALK_DURATION
        );
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is in LEAVING state
        return customer.getCustomerState() == CustomerState.LEAVING;
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic)
        if (customer.getCustomerState() != CustomerState.LEAVING) {
            return false;
        }
        
        // Check if customer reached spawn position or hit timeout
        if (startTime > 0) {
            long elapsed = gameTime - startTime;
            
            // Check if reached spawn position
            if (targetSpawnPos != null) {
                double distanceToSpawn = Math.sqrt(customer.blockPosition().distSqr(targetSpawnPos));
                if (distanceToSpawn <= REACHED_SPAWN_DISTANCE) {
                    return false;
                }
            }
            
            // Safety timeout
            if (elapsed >= MAX_WALK_DURATION) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        startTime = gameTime;
        startPos = customer.blockPosition();
        targetSpawnPos = customer.getSpawnPosition();
        
        if (targetSpawnPos == null) {
            LOGGER.warn("Customer {} has no spawn position, will despawn immediately", customer.getId());
            return;
        }
        
        // Return to spawn position
        Vec3 targetVec = Vec3.atBottomCenterOf(targetSpawnPos);
        customer.getBrain().setMemory(
            MemoryModuleType.WALK_TARGET,
            new WalkTarget(targetVec, WALK_SPEED, REACHED_SPAWN_DISTANCE)
        );
        
        LOGGER.info("Customer {} leaving tavern, returning to spawn position {} (distance: {})", 
            customer.getId(), 
            targetSpawnPos,
            Math.sqrt(customer.blockPosition().distSqr(targetSpawnPos))
        );
    }
    
    @Override
    protected void tick(ServerLevel level, CustomerEntity customer, long gameTime) {
        // If target was lost, reset it to spawn position
        if (!customer.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) && targetSpawnPos != null) {
            Vec3 targetVec = Vec3.atBottomCenterOf(targetSpawnPos);
            customer.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetVec, WALK_SPEED, REACHED_SPAWN_DISTANCE)
            );
            // LOGGER.debug("Customer {} re-targeting spawn position during tick", customer.getId());
        }
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Calculate how far the customer walked and how long it took
        double distanceTraveled = 0;
        long elapsedTicks = 0;
        
        if (startPos != null) {
            distanceTraveled = Math.sqrt(customer.blockPosition().distSqr(startPos));
        }
        
        if (startTime > 0) {
            elapsedTicks = gameTime - startTime;
        }
        
        // Despawn the customer
        LOGGER.info("Customer {} despawning after walking {} blocks in {} ticks", 
            customer.getId(), 
            String.format("%.1f", distanceTraveled),
            elapsedTicks);
        
        customer.discard();
        
        // Clear memories
        customer.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        customer.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        
        startTime = 0;
        startPos = null;
    }
}

