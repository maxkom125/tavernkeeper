package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
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
 * Behavior to make customers walk to the nearest lectern in a Service Area
 * Similar to how villagers find their job sites, but simpler
 */
public class MoveToLectern extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COOLDOWN_TICKS = 20; // Check every second
    private static final int MAX_DISTANCE = 48; // Same as FOLLOW_RANGE
    private static final int REACHED_DISTANCE = 2; // How close to get to lectern
    
    private final float speedModifier;
    private long nextCheckTime = 0;
    private BlockPos targetLectern = null;
    
    public MoveToLectern(float speedModifier) {
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
        // Only run if customer is in FINDING_QUEUE state
        if (customer.getCustomerState() != CustomerState.FINDING_QUEUE) {
            return false;
        }
        
        // Cooldown check
        long time = level.getGameTime();
        if (time < nextCheckTime) {
            return false;
        }
        nextCheckTime = time + COOLDOWN_TICKS;
        
        // Check if we already have a target and reached it
        if (targetLectern != null) {
            double distSq = customer.blockPosition().distSqr(targetLectern);
            if (distSq <= REACHED_DISTANCE * REACHED_DISTANCE) {
                LOGGER.info("Customer {} reached lectern at {}", customer.getId(), targetLectern);
                customer.setCustomerState(CustomerState.WAITING_SERVICE);
                targetLectern = null;
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Find nearest lectern in any Service Area
        BlockPos customerPos = customer.blockPosition();
        Tavern tavern = Tavern.get(level);
        
        Optional<ServiceLectern> nearestLectern = tavern.findNearestLectern(customerPos, MAX_DISTANCE);
        
        if (nearestLectern.isPresent()) {
            targetLectern = nearestLectern.get().getPosition();
            
            // Store lectern position in customer for later reference
            customer.setTargetPosition(targetLectern);
            
            // Set walk target (similar to how villagers walk to job sites)
            customer.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(targetLectern, speedModifier, REACHED_DISTANCE)
            );
            
            // Set look target
            customer.getBrain().setMemory(
                MemoryModuleType.LOOK_TARGET,
                new BlockPosTracker(targetLectern)
            );
            
            LOGGER.info("Customer {} walking to lectern at {} (distance: {})", 
                customer.getId(), 
                targetLectern,
                Math.sqrt(targetLectern.distSqr(customerPos))
            );
        } else {
            LOGGER.warn("Customer {} couldn't find any lectern within {} blocks", 
                customer.getId(), MAX_DISTANCE);
            // TODO: Maybe despawn after a while if no lectern found?
        }
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic)
        if (customer.getCustomerState() != CustomerState.FINDING_QUEUE) {
            return false;
        }
        
        // Stop if we reached the target
        if (targetLectern != null) {
            double distSq = customer.blockPosition().distSqr(targetLectern);
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
        if (targetLectern != null) {
            double distSq = customer.blockPosition().distSqr(targetLectern);
            if (distSq <= REACHED_DISTANCE * REACHED_DISTANCE) {
                LOGGER.info("Customer {} reached lectern at {}", customer.getId(), targetLectern);
                customer.setCustomerState(CustomerState.WAITING_SERVICE);
            }
        }
        
        // Clear look target when stopping
        customer.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }
}

