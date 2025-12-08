package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import org.slf4j.Logger;

/**
 * Behavior for customer lying in bed and sleeping
 * Similar to EatAtChair but sleeps until morning
 */
public class SleepInBed extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MORNING_START = 0; // Daytime starts at tick 0
    private static final long MORNING_END = 4000;
    private static final long MIN_SLEEP_DURATION = 6; // Sleep for at least 5 minutes //TODO: Revert to 6000!!
    
    private long sleepStartTime = 0;
    
    public SleepInBed() {
        // No memory requirements, run indefinitely
        super(ImmutableMap.of(), Integer.MAX_VALUE);
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is in SLEEPING state
        return customer.getCustomerState() == CustomerState.SLEEPING;
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic)
        if (customer.getCustomerState() != CustomerState.SLEEPING) {
            return false;
        }
        
        // Must sleep for minimum duration first (prevents immediate wake-up if arrived in morning)
        if (gameTime - this.sleepStartTime < MIN_SLEEP_DURATION) {
            return true; // Keep sleeping
        }
        
        // After minimum duration, check if it's morning (time to wake up)
        long dayTime = level.getDayTime() % 24000; // Get time of day (0-24000)
        return !(dayTime >= MORNING_START && dayTime < MORNING_END);
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos bedPos = customer.getTargetPosition();
        TavernContext tavern = Tavern.get(level);
        
        if (bedPos == null) {
            LOGGER.warn("Customer {} in SLEEPING state but no target bed position!", customer.getId());
            customer.setCustomerState(CustomerState.LEAVING);
            return;
        }
        
        // Verify the bed still exists and is reserved by this customer
        // (prevents another customer from taking it during panic/interruption)
        if (!tavern.hasBedAt(bedPos)) {
            LOGGER.warn("Customer {} bed at {} no longer exists, transitioning to FINDING_BED", 
                customer.getId(), bedPos);
            customer.setTargetPosition(null);
            customer.setCustomerState(CustomerState.FINDING_BED);
            return;
        }
        
        if (!tavern.isBedReservedBy(bedPos, customer.getUUID())) {
            LOGGER.warn("Customer {} bed at {} is no longer reserved by them (taken by another customer?), transitioning to FINDING_BED", 
                customer.getId(), bedPos);
            customer.setTargetPosition(null);
            customer.setCustomerState(CustomerState.FINDING_BED);
            return;
        }
        
        // Track when sleep started
        this.sleepStartTime = gameTime;
        
        // Use vanilla sleeping (sets pose, initial position, sleeping state)
        // Similar to Villager.startSleeping() (Villager.java line 939-944)
        customer.startSleeping(bedPos);
        
        LOGGER.info("Customer {} started sleeping at bed {} until morning", 
            customer.getId(), bedPos);
    }
    
    @Override
    protected void tick(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos bedPos = customer.getTargetPosition();
        
        if (bedPos == null) {
            return;
        }
        
        // Verify customer is still sleeping
        if (!customer.isSleeping()) {
            LOGGER.warn("Customer {} lost sleeping state during SleepInBed behavior", customer.getId());
            customer.startSleeping(bedPos);
        }
        
        // Keep customer anchored to bed position (vanilla sleeping handles pose, but not continuous positioning)
        // Brain behaviors in vanilla mobs handle this, we need to do it manually
        double targetX = bedPos.getX() + 0.5;
        double targetY = bedPos.getY() + 0.6875; // Vanilla bed sleep height (LivingEntity.setPosToBed line 3448)
        double targetZ = bedPos.getZ() + 0.5;
        
        // Anchor customer to exact bed position
        if (customer.distanceToSqr(targetX, targetY, targetZ) > 0.01) {
            customer.setPos(targetX, targetY, targetZ);
            customer.setDeltaMovement(0, 0, 0);
        }
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos bedPos = customer.getTargetPosition();
        TavernContext tavern = Tavern.get(level);
        
        // Check if bed still exists
        boolean bedExists = bedPos != null && tavern.hasBedAt(bedPos);
        
        // Release the bed reservation
        if (bedExists) {
            tavern.releaseBed(bedPos);
            LOGGER.info("Customer {} finished sleeping, released bed at {}", 
                customer.getId(), bedPos);
        }
        
        // Wake up customer (similar to Villager.stopSleeping() line 946-949)
        customer.stopSleeping();
        
        // Check if it's morning (normal wake up)
        long dayTime = level.getDayTime() % 24000;
        if (dayTime >= MORNING_START && dayTime < MORNING_END) {
            // Woke up naturally in the morning, use lifecycle to determine next state
            if (customer.getLifecycle() != null) {
                customer.transitionToNextState(level);
                LOGGER.info("Customer {} woke up in the morning, transitioning via lifecycle", customer.getId());
            } else {
                // Fallback if no lifecycle
                customer.setCustomerState(CustomerState.LEAVING);
                LOGGER.warn("Customer {} has no lifecycle, defaulting to LEAVING", customer.getId());
            }
        } else {
            // Interrupted (e.g., by panic or bed destroyed)
            if (!bedExists) {
                // Bed was destroyed, go find another one
                customer.setCustomerState(CustomerState.FINDING_BED);
                LOGGER.info("Customer {} bed was destroyed, searching for another bed", customer.getId());
            } else {
                // Other interruption (panic, damage), stay in that state
                LOGGER.debug("Customer {} sleeping interrupted", customer.getId());
            }
        }
        
        // Clear target and request
        customer.setTargetPosition(null);
        customer.setRequest(null);
        this.sleepStartTime = 0;
    }
}

