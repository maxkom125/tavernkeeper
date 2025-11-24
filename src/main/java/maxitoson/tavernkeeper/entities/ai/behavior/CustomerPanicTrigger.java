package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

/**
 * Triggers panic activity when customer is hurt or sees hostile mobs
 * Similar to VillagerPanicTrigger but without golem spawning logic
 */
public class CustomerPanicTrigger extends Behavior<CustomerEntity> {
    
    public CustomerPanicTrigger() {
        super(ImmutableMap.of());
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity entity, long gameTime) {
        return isHurt(entity) || hasHostile(entity);
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity entity, long gameTime) {
        boolean hurt = isHurt(entity);
        boolean hostile = hasHostile(entity);
        
        if (hurt || hostile) {
            com.mojang.logging.LogUtils.getLogger().info("Customer {} panicking! Hurt: {}, Hostile: {}", 
                entity.getId(), hurt, hostile);
            
            Brain<?> brain = entity.getBrain();
            if (!brain.isActive(Activity.PANIC)) {
                // Save current state before panicking
                entity.saveStateBeforePanic();
                
                // Clear current goals when panicking
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
            
            // Switch to panic activity
            brain.setActiveActivityIfPossible(Activity.PANIC);
        }
    }
    
    public static boolean hasHostile(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_HOSTILE);
    }
    
    public static boolean isHurt(LivingEntity entity) {
        return entity.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }
}

