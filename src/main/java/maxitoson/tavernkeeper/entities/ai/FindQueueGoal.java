package maxitoson.tavernkeeper.entities.ai;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.slf4j.Logger;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Goal: Customer finds the nearest service area lectern and walks to it
 */
public class FindQueueGoal extends Goal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final CustomerEntity customer;
    private final double speedModifier;
    private BlockPos targetLectern;
    private int recheckCooldown = 0;
    
    public FindQueueGoal(CustomerEntity customer, double speed) {
        this.customer = customer;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }
    
    @Override
    public boolean canUse() {
        // Only run if in FINDING_QUEUE state and don't have a target
        if (customer.getCustomerState() != CustomerState.FINDING_QUEUE) {
            return false;
        }
        
        // Cooldown to avoid constant rechecking
        if (recheckCooldown > 0) {
            recheckCooldown--;
            return false;
        }
        
        return findNearestLectern();
    }
    
    @Override
    public boolean canContinueToUse() {
        // Continue if we haven't reached the lectern yet
        if (targetLectern == null) {
            return false;
        }
        
        // Check if we're close enough (within 2 blocks)
        return customer.blockPosition().distSqr(targetLectern) > 4.0;
    }
    
    @Override
    public void start() {
        if (targetLectern != null) {
            LOGGER.info("CustomerEntity {} -> moving to lectern at {}", 
                customer.getId(), targetLectern);
            customer.getNavigation().moveTo(
                targetLectern.getX() + 0.5, 
                targetLectern.getY(), 
                targetLectern.getZ() + 0.5, 
                speedModifier
            );
        }
    }
    
    @Override
    public void stop() {
        if (targetLectern != null && customer.blockPosition().distSqr(targetLectern) <= 4.0) {
            // Reached the lectern!
            LOGGER.info("CustomerEntity {} -> reached lectern at {}", 
                customer.getId(), targetLectern);
            customer.setCustomerState(CustomerState.WAITING_SERVICE);
        } else {
            // Failed to reach, try again later
            recheckCooldown = 60; // Wait 3 seconds before trying again
            targetLectern = null;
        }
    }
    
    private boolean findNearestLectern() {
        if (!(customer.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        
        Tavern tavern = Tavern.get(serverLevel);
        BlockPos customerPos = customer.blockPosition();
        
        // Find nearest lectern (no distance limit)
        Optional<ServiceLectern> nearestLectern = tavern.findNearestLectern(customerPos, Double.MAX_VALUE);
        
        if (nearestLectern.isPresent()) {
            targetLectern = nearestLectern.get().getPosition();
            double distance = Math.sqrt(customerPos.distSqr(targetLectern));
            LOGGER.debug("CustomerEntity {} -> found lectern at {} (distance: {})", 
                customer.getId(), targetLectern, distance);
            return true;
        }
        
        LOGGER.warn("CustomerEntity {} -> no lecterns found in any service area!", customer.getId());
        recheckCooldown = 100; // Wait 5 seconds before trying again
        return false;
    }
}

