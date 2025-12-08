package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

/**
 * Behavior packages for CustomerEntity
 * Similar to VillagerGoalPackages but simpler
 * 
 * Each package returns a list of BehaviorControl pairs (priority, behavior)
 */
public class CustomerGoalPackages {
    
    /**
     * Core behaviors - always active
     * Similar to VillagerGoalPackages.getCorePackage() (line 33-39)
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CustomerEntity>>> getCorePackage(float speedModifier) {
        return ImmutableList.of(
            // Swimming (priority 0)
            Pair.of(0, new Swim(0.8F)),
            
            // Look at target (priority 0)
            Pair.of(0, new LookAtTargetSink(45, 90)),
            
            // Panic trigger - switches to PANIC activity when hurt (priority 0)
            Pair.of(0, new CustomerPanicTrigger()),
            
            // Move to walk target (priority 1)
            Pair.of(1, new MoveToTargetSink())
        );
    }
    
    /**
     * Idle behaviors - default activity when not doing anything specific
     * Similar to VillagerGoalPackages.getIdlePackage() (line 68-70)
     * Customer lifecycle:
     *   FOOD: Find Queue -> Wait at Lectern -> Find Seat -> Eat -> Leave
     *   SLEEPING: Find Queue -> Wait at Reception -> Find Bed -> Sleep -> Leave
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CustomerEntity>>> getIdlePackage(float speedModifier) {
        return ImmutableList.of(
            // Priority 0a: Move to lectern for food customers (only if FINDING_LECTERN)
            Pair.of(0, new MoveToLectern(speedModifier)),
            
            // Priority 0b: Move to reception desk for sleeping customers (only if FINDING_RECEPTION)
            Pair.of(0, new MoveToReceptionDesk(speedModifier)),
            
            // Priority 1: Wait at lectern and show food request (only if WAITING_SERVICE)
            Pair.of(1, new WaitAtLectern()),
            
            // Priority 2: Wait at reception desk and hold money (only if WAITING_RECEPTION)
            Pair.of(2, new WaitAtReceptionDesk()),
            
            // Priority 3: Find and walk to a chair after being served (only if FINDING_SEAT)
            Pair.of(3, new FindSeat(speedModifier)),
            
            // Priority 4: Sit at chair and eat food (only if EATING)
            Pair.of(4, new EatAtChair()),
            
            // Priority 5: Find and walk to a bed (only if FINDING_BED)
            Pair.of(5, new FindBed(speedModifier)),
            
            // Priority 6: Lie in bed and sleep (only if SLEEPING)
            Pair.of(6, new SleepInBed()),
            
            // Priority 7: Walk away and despawn (only if LEAVING)
            Pair.of(7, new Leave())
        );
    }
    
    /**
     * Panic behaviors - when hurt or scared
     * Similar to VillagerGoalPackages.getPanicPackage() (line 72-75)
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CustomerEntity>>> getPanicPackage(float speedModifier) {
        float panicSpeed = speedModifier * 1.5F;
        return ImmutableList.of(
            // Calm down when safe (priority 0) - returns to normal activity
            Pair.of(0, CustomerCalmDown.create()),
            
            // Run away from entity that hurt us (priority 1) - vanilla behavior
            Pair.of(1, SetWalkTargetAwayFrom.entity(
                MemoryModuleType.HURT_BY_ENTITY, 
                panicSpeed, 6, false
            )),
            
            // Run away from nearest hostile (priority 1) - vanilla behavior (only zombies!)
            Pair.of(1, SetWalkTargetAwayFrom.entity(
                MemoryModuleType.NEAREST_HOSTILE, 
                panicSpeed, 6, false
            ))
        );
    }
    
    /**
     * Rest activity - for future use when customer sits and eats
     * Similar to VillagerGoalPackages.getRestPackage() (line 56-60)
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super CustomerEntity>>> getRestPackage(float speedModifier) {
        // TODO: Add sitting/eating behaviors when implementing chairs
        return ImmutableList.of();
    }
}

