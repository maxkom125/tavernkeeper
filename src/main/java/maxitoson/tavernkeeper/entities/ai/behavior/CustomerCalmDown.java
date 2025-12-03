package maxitoson.tavernkeeper.entities.ai.behavior;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.slf4j.Logger;

/**
 * Simple calm down behavior - returns to normal activity when safe
 * Generic version of VillagerCalmDown without villager-specific logic
 */
public class CustomerCalmDown {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAFE_DISTANCE_FROM_DANGER = 36;
    private static final int MAX_DISTANCE_FROM_LECTERN = 20; // Max distance to stay after panic

    public static BehaviorControl<CustomerEntity> create() {
        return BehaviorBuilder.create(instance -> 
            instance.group(
                instance.registered(MemoryModuleType.HURT_BY),
                instance.registered(MemoryModuleType.HURT_BY_ENTITY),
                instance.registered(MemoryModuleType.NEAREST_HOSTILE)
            ).apply(instance, (hurtBy, hurtByEntity, nearestHostile) -> 
                (level, customer, gameTime) -> {
                    // Check if we're still in danger
                    boolean stillInDanger = instance.tryGet(hurtBy).isPresent() 
                        || instance.tryGet(nearestHostile).isPresent() 
                        || instance.tryGet(hurtByEntity).filter(entity -> 
                            entity.distanceToSqr(customer) <= SAFE_DISTANCE_FROM_DANGER
                        ).isPresent();
                    
                    if (!stillInDanger) {
                        // Safe! Clear panic memories
                        hurtBy.erase();
                        hurtByEntity.erase();
                        nearestHostile.erase();
                        
                        // Return to normal activity (IDLE)
                        customer.getBrain().setDefaultActivity(net.minecraft.world.entity.schedule.Activity.IDLE);
                        customer.getBrain().setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.IDLE);
                        
                        // Check if customer ran too far from their target location
                        BlockPos targetPos = customer.getTargetPosition(); // Will be lectern, chair, or null
                        
                        if (targetPos != null) {
                            // Customer has a target position (lectern or chair)
                            double distSq = customer.blockPosition().distSqr(targetPos);
                            double distance = Math.sqrt(distSq);
                            
                            if (distSq > MAX_DISTANCE_FROM_LECTERN * MAX_DISTANCE_FROM_LECTERN) {
                                // Ran too far! Send message and despawn
                                LOGGER.info("Customer {} ran too far from target (distance: {}), despawning", 
                                    customer.getId(), distance);
                                
                                // Decrease tavern reputation
                                if (level instanceof ServerLevel serverLevel) {
                                    Tavern tavern = Tavern.get(serverLevel);
                                    if (tavern != null) {
                                        tavern.adjustReputation(-5);  // -5 reputation for customer running away
                                    }
                                }
                                
                                // Broadcast message to nearby players
                                level.getServer().getPlayerList().broadcastSystemMessage(
                                    Component.literal("A customer ran away from danger near your tavern! (-5 reputation)"), 
                                    false
                                );
                                
                                // Despawn
                                customer.discard();
                                return true;
                            } else {
                                // Close enough - return to appropriate state to walk back
                                CustomerState newState = customer.getStateAfterPanic();
                                LOGGER.info("Customer {} ran from target (distance: {}), returning to {} state", 
                                    customer.getId(), distance, newState);
                                customer.setCustomerState(newState);
                            }
                        } else {
                            // No target position set - use state transition logic anyway
                            CustomerState newState = customer.getStateAfterPanic();
                            LOGGER.info("Customer {} calmed down with no target, returning to {} state", 
                                customer.getId(), newState);
                            customer.setCustomerState(newState);
                        }
                        
                        LOGGER.info("Customer {} calmed down, returning to normal activity", customer.getId());
                    }
                    
                    return true;
                }
            )
        );
    }
}

