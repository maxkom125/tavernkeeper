package maxitoson.tavernkeeper.entities.ai.lifecycle;

import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.util.RandomSource;

/**
 * Factory for creating customer lifecycles based on spawn probabilities or type
 * Similar to Villager profession assignment
 */
public class CustomerLifecycleFactory {
    
    // Probability thresholds (sum to 1.0)
    private static final float DINING_ONLY_THRESHOLD = 0.7f;    // 0.00 - DINING_ONLY_THRESHOLD
    private static final float SLEEPING_ONLY_THRESHOLD = 0.90f;  // DINING_ONLY_THRESHOLD - SLEEPING_ONLY_THRESHOLD
    // FULL_SERVICE is the remainder                             // SLEEPING_ONLY_THRESHOLD - 1.00
    
    /**
     * Create a lifecycle for a new customer
     * 
     * @param tavern The tavern context (for context-aware decisions based on tavern level)
     * @param random Random source for probability
     * @return A new CustomerLifecycle instance
     */
    public static CustomerLifecycle create(TavernContext tavern, RandomSource random) {
        // Check tavern level - sleeping customers only available from level 2+
        TavernUpgrade currentLevel = tavern.getCurrentUpgrade();
        
        // Level 1: Only dining customers (no sleeping)
        if (currentLevel == TavernUpgrade.LEVEL_1) {
            return new DiningOnlyLifecycle();
        }
        
        // Level 2+: Normal probability distribution
        float roll = random.nextFloat();
        
        if (roll < DINING_ONLY_THRESHOLD) {
            return new DiningOnlyLifecycle();
        } else if (roll < SLEEPING_ONLY_THRESHOLD) {
            return new SleepingOnlyLifecycle();
        } else {
            return new FullServiceLifecycle();
        }
    }
    
    /**
     * Recreate lifecycle from a specific type (used for NBT loading)
     * 
     * @param type The customer type to create lifecycle for
     * @return A new CustomerLifecycle instance of the specified type
     */
    public static CustomerLifecycle fromType(LifecycleType type) {
        return switch (type) {
            case DINING_ONLY -> new DiningOnlyLifecycle();
            case SLEEPING_ONLY -> new SleepingOnlyLifecycle();
            case FULL_SERVICE -> new FullServiceLifecycle();
        };
    }
    
    /**
     * Get distribution statistics (for debugging/logging)
     */
    public static String getDistributionInfo() {
        int diningPercent = (int)(DINING_ONLY_THRESHOLD * 100);
        int sleepingPercent = (int)((SLEEPING_ONLY_THRESHOLD - DINING_ONLY_THRESHOLD) * 100);
        int fullServicePercent = (int)((1.0f - SLEEPING_ONLY_THRESHOLD) * 100);
        
        return String.format("Dining: %d%%, Sleeping: %d%%, Full Service: %d%%",
            diningPercent, sleepingPercent, fullServicePercent);
    }
}
