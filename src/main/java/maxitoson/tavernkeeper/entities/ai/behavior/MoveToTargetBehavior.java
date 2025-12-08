package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Base behavior for customers moving to a target position
 * Template Method Pattern: Handles all common pathfinding logic
 * Subclasses provide: state filter, target search, and completion action
 * 
 * Used by: MoveToLectern, MoveToReceptionDesk, FindSeat, FindBed
 */
public abstract class MoveToTargetBehavior extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COOLDOWN_TICKS = 20; // Check every second
    private static final int MAX_DISTANCE = 48; // Same as FOLLOW_RANGE
    
    private final float speedModifier;
    private long nextCheckTime = 0;
    private BlockPos targetPosition = null;
    
    protected MoveToTargetBehavior(float speedModifier) {
        super(
            ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
            ),
            COOLDOWN_TICKS * 2
        );
        this.speedModifier = speedModifier;
    }
    
    // ========== Template Methods (subclasses implement) ==========
    
    /**
     * Check if customer is in the correct state for this behavior
     */
    protected abstract boolean isInCorrectState(CustomerEntity customer);
    
    /**
     * Find the target position (lectern, reception desk, chair, bed, etc.)
     */
    protected abstract Optional<BlockPos> findTarget(TavernContext tavern, BlockPos from, double maxDistance);
    
    /**
     * Called when customer reaches the target
     * Typically: set target position, transition state
     */
    protected abstract void onTargetReached(ServerLevel level, CustomerEntity customer, BlockPos target);
    
    /**
     * Get human-readable name of target for logging
     */
    protected abstract String getTargetName();
    
    /**
     * How close customer needs to be to consider target "reached"
     */
    protected abstract int getReachedDistance();
    
    // ========== Optional Template Methods (for reservation system) ==========
    
    /**
     * Try to reserve the target (e.g., chair, bed)
     * Override for resources that need reservation
     * @return true if reservation succeeded or not needed, false if failed
     */
    protected boolean tryReserveTarget(TavernContext tavern, BlockPos target, CustomerEntity customer) {
        return true; // Default: no reservation needed
    }
    
    /**
     * Release the target reservation if customer didn't reach it
     * Override for resources that were reserved
     */
    protected void releaseTarget(TavernContext tavern, BlockPos target) {
        // Default: no-op
    }
    
    // ========== Common Implementation ==========
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Check state
        if (!isInCorrectState(customer)) {
            return false;
        }
        
        // Cooldown check
        long time = level.getGameTime();
        if (time < nextCheckTime) {
            return false;
        }
        nextCheckTime = time + COOLDOWN_TICKS;
        
        // Check if we already have a target and reached it
        if (targetPosition != null) {
            double distSq = customer.blockPosition().distSqr(targetPosition);
            if (distSq <= getReachedDistance() * getReachedDistance()) {
                LOGGER.info("Customer {} reached {} at {}", 
                    customer.getId(), getTargetName(), targetPosition);
                onTargetReached(level, customer, targetPosition);
                targetPosition = null;
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos customerPos = customer.blockPosition();
        TavernContext tavern = Tavern.get(level);
        
        Optional<BlockPos> target = findTarget(tavern, customerPos, MAX_DISTANCE);
        
        if (target.isPresent()) {
            targetPosition = target.get();
            
            // Try to reserve target (for chairs/beds that need reservation)
            if (tryReserveTarget(tavern, targetPosition, customer)) {
                setupWalkTarget(customer, targetPosition, customerPos);
            } else {
                LOGGER.warn("Customer {} failed to reserve {} at {} (race condition?)",
                    customer.getId(), getTargetName(), targetPosition);
                targetPosition = null;
            }
        } else {
            LOGGER.debug("Customer {} ({}) couldn't find any {} within {} blocks, will keep looking", 
                customer.getId(), customer.getLifecycleType(), getTargetName(), MAX_DISTANCE);
        }
    }
    
    private void setupWalkTarget(CustomerEntity customer, BlockPos target, BlockPos customerPos) {
        // Set walk target
        customer.getBrain().setMemory(
            MemoryModuleType.WALK_TARGET,
            new WalkTarget(target, speedModifier, getReachedDistance())
        );
        
        // Set look target
        customer.getBrain().setMemory(
            MemoryModuleType.LOOK_TARGET,
            new BlockPosTracker(target)
        );
        
        LOGGER.info("Customer {} ({}) walking to {} at {} (distance: {})", 
            customer.getId(),
            customer.getLifecycleType(),
            getTargetName(),
            target,
            Math.sqrt(target.distSqr(customerPos))
        );
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic)
        if (!isInCorrectState(customer)) {
            return false;
        }
        
        // Stop if we reached the target
        if (targetPosition != null) {
            double distSq = customer.blockPosition().distSqr(targetPosition);
            if (distSq <= getReachedDistance() * getReachedDistance()) {
                return false;
            }
        }
        
        // Continue if we have a walk target
        return customer.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        TavernContext tavern = Tavern.get(level);
        
        // Check if we reached the target
        if (targetPosition != null) {
            double distSq = customer.blockPosition().distSqr(targetPosition);
            if (distSq <= getReachedDistance() * getReachedDistance()) {
                LOGGER.info("Customer {} reached {} at {}", 
                    customer.getId(), getTargetName(), targetPosition);
                onTargetReached(level, customer, targetPosition);
            } else {
                // Didn't reach target, release if needed
                LOGGER.debug("Customer {} didn't reach {} at {}",
                    customer.getId(), getTargetName(), targetPosition);
                releaseTarget(tavern, targetPosition);
            }
        }
        
        // Clear look target when stopping
        customer.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        targetPosition = null;
    }
}

