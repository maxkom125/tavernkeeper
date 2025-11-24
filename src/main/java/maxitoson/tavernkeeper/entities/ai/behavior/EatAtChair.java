package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.core.BlockPos;
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
        
        if (chairPos == null) {
            LOGGER.warn("Customer {} in EATING state but no target chair position!", customer.getId());
            customer.setCustomerState(CustomerState.LEAVING);
            return;
        }
        
        // Make customer sit (teleport to exact chair position and stop moving)
        // Add 0.5 to center on the block horizontally
        double sitX = chairPos.getX() + 0.5;
        double sitY = chairPos.getY() + 0.5; // Sit slightly above the block
        double sitZ = chairPos.getZ() + 0.5;
        
        customer.teleportTo(sitX, sitY, sitZ);
        customer.setDeltaMovement(0, 0, 0); // Stop all movement
        
        // Make customer look forward (toward the table they're facing)
        // This will be refined later with proper facing direction
        
        startTime = gameTime;
        lastParticleTime = gameTime;
        
        LOGGER.info("Customer {} started eating at chair {} for {} ticks", 
            customer.getId(), chairPos, EATING_DURATION);
    }
    
    @Override
    protected void tick(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Spawn eating particles occasionally
        if (gameTime - lastParticleTime >= PARTICLE_INTERVAL) {
            spawnEatingParticles(level, customer);
            lastParticleTime = gameTime;
        }
        
        // Keep customer in place (prevent gravity/movement)
        BlockPos chairPos = customer.getTargetPosition();
        if (chairPos != null) {
            double sitX = chairPos.getX() + 0.5;
            double sitY = chairPos.getY() + 0.5;
            double sitZ = chairPos.getZ() + 0.5;
            
            // Only teleport if customer moved too far (allows small movements for animations)
            if (customer.distanceToSqr(sitX, sitY, sitZ) > 0.1) {
                customer.teleportTo(sitX, sitY, sitZ);
                customer.setDeltaMovement(0, 0, 0);
            }
        }
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        BlockPos chairPos = customer.getTargetPosition();
        
        // Release the chair reservation
        if (chairPos != null) {
            Tavern.get(level).releaseChair(chairPos);
            LOGGER.info("Customer {} finished eating, released chair at {}", 
                customer.getId(), chairPos);
        }
        
        // Check if we completed the eating duration
        if (startTime > 0 && (gameTime - startTime) >= EATING_DURATION) {
            // Finished eating normally, transition to LEAVING
            customer.setCustomerState(CustomerState.LEAVING);
            LOGGER.info("Customer {} finished eating, now leaving", customer.getId());
        } else {
            // Interrupted (e.g., by panic), stay in current state
            LOGGER.debug("Customer {} eating interrupted at {} ticks", 
                customer.getId(), gameTime - startTime);
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

