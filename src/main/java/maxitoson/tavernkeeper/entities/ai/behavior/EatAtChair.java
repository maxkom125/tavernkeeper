package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.SittingEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.TavernKeeperMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * Behavior for customer sitting at a chair and eating
 * Similar to how villagers sleep in beds, but for chairs
 */
public class EatAtChair extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int EATING_DURATION = 200; // 10 seconds (20 ticks = 1 second)
    private static final int PARTICLE_INTERVAL = 20; // Show particles every second
    
    private long startTime = 0;
    private long lastParticleTime = 0;
    
    public EatAtChair() {
        // No memory requirements, run indefinitely
        super(ImmutableMap.of(), Integer.MAX_VALUE);
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is in EATING state
        return customer.getCustomerState() == CustomerState.EATING;
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Stop if state changed (e.g., panic) or duration elapsed
        if (customer.getCustomerState() != CustomerState.EATING) {
            return false;
        }
        
        // Check if eating duration is complete
        if (startTime > 0) {
            long elapsed = gameTime - startTime;
            return elapsed < EATING_DURATION;
        }
        
        return true;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos chairPos = customer.getTargetPosition();
        TavernContext tavern = Tavern.get(level);
        
        if (chairPos == null) {
            LOGGER.warn("Customer {} in EATING state but no target chair position!", customer.getId());
            customer.setCustomerState(CustomerState.LEAVING);
            return;
        }
        
        // Verify the chair still exists and is reserved by this customer
        // (prevents another customer from taking it during panic/interruption)
        if (!tavern.hasChairAt(chairPos)) {
            LOGGER.warn("Customer {} chair at {} no longer exists, transitioning to FINDING_SEAT", 
                customer.getId(), chairPos);
            customer.setTargetPosition(null);
            customer.setCustomerState(CustomerState.FINDING_SEAT);
            return;
        }
        
        if (!tavern.isChairReservedBy(chairPos, customer.getUUID())) {
            LOGGER.warn("Customer {} chair at {} is no longer reserved by them (taken by another customer?), transitioning to FINDING_SEAT", 
                customer.getId(), chairPos);
            customer.setTargetPosition(null);
            customer.setCustomerState(CustomerState.FINDING_SEAT);
            return;
        }
        
        // Get facing from Chair if available
        Direction facing = tavern.getChairAt(chairPos)
            .map(Chair::getFacing)
            .orElse(null);
        
        // Make customer sit using SittingEntity
        boolean success = SittingEntity.sitDown(
            chairPos, 
            customer, 
            EATING_DURATION, 
            TavernKeeperMod.SITTING.get(),
            facing
        );
        
        if (!success) {
            // Failed to sit (position occupied? - shouldn't happen as we reserved it)
            LOGGER.warn("Customer {} failed to sit at chair {}, transitioning to FINDING_SEAT", 
                customer.getId(), chairPos);
            customer.setTargetPosition(null);
            customer.setCustomerState(CustomerState.FINDING_SEAT);
            return;
        }
        
        startTime = gameTime;
        lastParticleTime = gameTime;
        
        LOGGER.info("Customer {} started sitting and eating at chair {} for {} ticks", 
            customer.getId(), chairPos, EATING_DURATION);
    }
    
    @Override
    protected void tick(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Check if still sitting
        if (!(customer.getVehicle() instanceof SittingEntity)) {
            // Was dismounted early (timeout or interrupted)
            LOGGER.debug("Customer {} was dismounted from SittingEntity during eating", customer.getId());
            // canStillUse will return false and stop() will be called
            return;
        }
        
        // Spawn eating particles occasionally
        if (gameTime - lastParticleTime >= PARTICLE_INTERVAL) {
            spawnEatingParticles(level, customer);
            lastParticleTime = gameTime;
        }
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos chairPos = customer.getTargetPosition();
        TavernContext tavern = Tavern.get(level);
        
        // Make customer stand up if still sitting
        if (customer.getVehicle() instanceof SittingEntity sittingEntity) {
            // Force dismount by setting lifetime to 0
            sittingEntity.setMaxLifeTime(0);
            LOGGER.debug("Customer {} standing up from chair", customer.getId());
        }
        
        // Check if chair still exists (asks tavern instead of checking block types)
        boolean chairExists = chairPos != null && tavern.hasChairAt(chairPos);
        
        // Release the chair reservation
        if (chairExists) {
            tavern.releaseChair(chairPos);
            LOGGER.info("Customer {} finished eating, released chair at {}", 
                customer.getId(), chairPos);
        }
        
        // Check if we completed the eating duration
        if (startTime > 0 && (gameTime - startTime) >= EATING_DURATION) {
            // Finished eating normally, use lifecycle to determine next state
            if (customer.getLifecycle() != null) {
                customer.transitionToNextState(level);
                LOGGER.info("Customer {} finished eating, transitioning via lifecycle", customer.getId());
            } else {
                // Fallback if no lifecycle
                customer.setCustomerState(CustomerState.LEAVING);
                LOGGER.error("Customer {} has no lifecycle, defaulting to LEAVING", customer.getId());
            }
        } else {
            // Interrupted (e.g., by panic or chair destroyed)
            if (!chairExists) {
                // Chair was destroyed, go find another one
                customer.setCustomerState(CustomerState.FINDING_SEAT);
                LOGGER.info("Customer {} chair was destroyed, searching for another seat", customer.getId());
            } else {
                // Other interruption (panic, damage), stay in current state
                LOGGER.debug("Customer {} eating interrupted at {} ticks", 
                    customer.getId(), gameTime - startTime);
            }
        }
        
        // Clear target position
        customer.setTargetPosition(null);
        startTime = 0;
        lastParticleTime = 0;
    }
    
    /**
     * Spawn eating particles around the customer's head
     * Similar to how animals eat particles work
     */
    private void spawnEatingParticles(ServerLevel level, CustomerEntity customer) {
        // Use carrot particles for now (matches the food request)
        ItemStack foodItem = new ItemStack(Items.CARROT);
        
        // Spawn particles near customer's head
        double x = customer.getX();
        double y = customer.getY() + customer.getEyeHeight();
        double z = customer.getZ();
        
        // Spawn 3-5 particles in front of customer's face
        for (int i = 0; i < 4; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;
            
            level.sendParticles(
                new ItemParticleOption(ParticleTypes.ITEM, foodItem),
                x + offsetX,
                y + offsetY,
                z + offsetZ,
                1, // particle count
                0, 0, 0, // velocity
                0.1 // speed
            );
        }
    }
}

