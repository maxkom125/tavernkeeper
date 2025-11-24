package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
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
 * Behavior to make customers walk to the nearest valid chair in a Dining Area
 * Similar to MoveToLectern but for chairs
 */
public class FindSeat extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COOLDOWN_TICKS = 20; // Check every second
    private static final int MAX_DISTANCE = 48; // Same as FOLLOW_RANGE
    private static final int REACHED_DISTANCE = 1; // How close to get to chair
    
    private final float speedModifier;
    private long nextCheckTime = 0;
    private BlockPos targetChair = null;
    
    public FindSeat(float speedModifier) {
        super(
            ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, // Only run if not already walking
                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED
            ),
            COOLDOWN_TICKS * 2 // Run for at most 2 seconds before re-checking
        );
        this.speedModifier = speedModifier;
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is in FINDING_SEAT state
        if (customer.getCustomerState() != CustomerState.FINDING_SEAT) {
            return false;
        }
        
        // Cooldown check
        long time = level.getGameTime();
        if (time < nextCheckTime) {
            return false;
        }
        nextCheckTime = time + COOLDOWN_TICKS;
        
        // Check if we already have a target and reached it
        if (targetChair != null) {
            double distSq = customer.blockPosition().distSqr(targetChair);
            if (distSq <= REACHED_DISTANCE * REACHED_DISTANCE) {
                LOGGER.info("Customer {} reached chair at {}", customer.getId(), targetChair);
                customer.setTargetPosition(targetChair);
                customer.setCustomerState(CustomerState.EATING);
                targetChair = null;
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Find nearest valid and available chair in any Dining Area
        BlockPos customerPos = customer.blockPosition();
        Tavern tavern = Tavern.get(level);
        
        Optional<Chair> nearestChair = tavern.findNearestAvailableChair(customerPos, MAX_DISTANCE);
        
        if (nearestChair.isPresent()) {
            targetChair = nearestChair.get().getPosition();
            
            // Reserve this chair for this customer
            if (tavern.reserveChair(targetChair, customer.getUUID())) {
                // Set walk target (similar to how villagers walk to job sites)
                customer.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(targetChair, speedModifier, REACHED_DISTANCE)
                );
                
                // Set look target
                customer.getBrain().setMemory(
                    MemoryModuleType.LOOK_TARGET,
                    new BlockPosTracker(targetChair)
                );
                
                LOGGER.info("Customer {} reserved and walking to chair at {} (distance: {})", 
                    customer.getId(), 
                    targetChair,
                    Math.sqrt(targetChair.distSqr(customerPos))
                );
            } else {
                LOGGER.warn("Customer {} failed to reserve chair at {} (race condition?)", 
                    customer.getId(), targetChair);
                targetChair = null;
            }
        } else {
            LOGGER.debug("Customer {} couldn't find any available chair, will keep looking", 
                customer.getId());
            // Stay in FINDING_SEAT state and try again next cooldown
        }
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic)
        if (customer.getCustomerState() != CustomerState.FINDING_SEAT) {
            return false;
        }
        
        // Stop if we reached the target
        if (targetChair != null) {
            double distSq = customer.blockPosition().distSqr(targetChair);
            if (distSq <= REACHED_DISTANCE * REACHED_DISTANCE) {
                return false;
            }
        }
        
        // Continue if we have a walk target
        return customer.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Check if we reached the target
        if (targetChair != null) {
            double distSq = customer.blockPosition().distSqr(targetChair);
            if (distSq <= REACHED_DISTANCE * REACHED_DISTANCE) {
                LOGGER.info("Customer {} reached chair at {}", customer.getId(), targetChair);
                customer.setTargetPosition(targetChair);
                customer.setCustomerState(CustomerState.EATING);
            } else {
                // Didn't reach the chair, release the reservation
                LOGGER.debug("Customer {} didn't reach chair at {}, releasing reservation", 
                    customer.getId(), targetChair);
                Tavern.get(level).releaseChair(targetChair);
            }
        }
        
        // Clear look target when stopping
        customer.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
        targetChair = null;
    }
}

