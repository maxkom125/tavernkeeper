package maxitoson.tavernkeeper.entities.ai;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Simple wandering goal for testing - customers walk around randomly
 * This is a temporary goal to verify AI works before adding complex behavior
 */
public class CustomerWanderGoal extends Goal {
    private final CustomerEntity customer;
    private final double speedModifier;
    
    public CustomerWanderGoal(CustomerEntity customer, double speed) {
        this.customer = customer;
        this.speedModifier = speed;
    }
    
    @Override
    public boolean canUse() {
        // Only wander if not moving and random chance
        return customer.getNavigation().isDone() && customer.getRandom().nextInt(120) == 0;
    }
    
    @Override
    public boolean canContinueToUse() {
        return !customer.getNavigation().isDone();
    }
    
    @Override
    public void start() {
        // Pick a random position within 10 blocks
        var randomPos = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(
            customer, 10, 7
        );
        
        if (randomPos != null) {
            customer.getNavigation().moveTo(randomPos.x, randomPos.y, randomPos.z, speedModifier);
        }
    }
}

