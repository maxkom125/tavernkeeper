package maxitoson.tavernkeeper.entities.ai.behavior;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
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
    private static final int DISTANCE_GAP = 10; // How much further than target-to-spawn customer can run
    private static final int MIN_RAN_TOO_FAR_DIST = 25; // Minimum distance to consider "ran too far" (ensures room to flee even when target is close to spawn)

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
                        
                        LOGGER.debug("Customer {} calmed down (total panic time: {} ticks / {}s)", 
                            customer.getId(), customer.getTotalPanicTicks(), customer.getTotalPanicTicks() / 20);
                        
                        // Return to normal activity (IDLE)
                        customer.getBrain().setDefaultActivity(net.minecraft.world.entity.schedule.Activity.IDLE);
                        customer.getBrain().setActiveActivityIfPossible(net.minecraft.world.entity.schedule.Activity.IDLE);
                        
                        // Check if customer ran too far from their target position
                        BlockPos targetPos = customer.getTargetPosition(); // Lectern or chair
                        BlockPos spawnPos = customer.getSpawnPosition();
                        
                        if (targetPos != null && spawnPos != null) {
                            // Calculate distances
                            double customerToTargetDist = Math.sqrt(customer.blockPosition().distSqr(targetPos));
                            double targetToSpawnDist = Math.sqrt(targetPos.distSqr(spawnPos));
                            // Use the larger of: (target-to-spawn + gap) OR minimum "ran too far" threshold
                            double maxAllowedDistance = Math.max(targetToSpawnDist + DISTANCE_GAP, MIN_RAN_TOO_FAR_DIST);
                            
                            if (customerToTargetDist > maxAllowedDistance) {
                                // Customer ran further from target than target is from spawn (+ gap)
                                LOGGER.info("Customer {} ran too far from target (dist: {}, max allowed: {}), despawning", 
                                    customer.getId(), customerToTargetDist, maxAllowedDistance);
                                
                                // Decrease tavern reputation
                                if (level instanceof ServerLevel serverLevel) {
                                    TavernContext tavern = Tavern.get(serverLevel);
                                    if (tavern != null) {
                                        tavern.adjustReputation(-5);  // -5 reputation for customer running away
                                    }
                                }
                                
                                // Broadcast message to nearby players
                                level.getServer().getPlayerList().broadcastSystemMessage(
                                    Component.literal("A customer ran too far from your tavern! (-5 reputation)"), 
                                    false
                                );
                                
                                // Despawn
                                customer.discard();
                                return true;
                            } else {
                                // Close enough - return to appropriate state to walk back
                                CustomerState newState = customer.getStateAfterPanic();
                                LOGGER.info("Customer {} calm, target dist: {}, max: {}, returning to {} state", 
                                    customer.getId(), customerToTargetDist, maxAllowedDistance, newState);
                                customer.setCustomerState(newState);
                            }
                        } else {
                            // No target or spawn position - just return to appropriate state
                            CustomerState newState = customer.getStateAfterPanic();
                            LOGGER.info("Customer {} calmed down without target/spawn, returning to {} state", 
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

