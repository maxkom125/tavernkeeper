package maxitoson.tavernkeeper.tavern.upgrades;

import maxitoson.tavernkeeper.tavern.managers.CustomerManager;
import maxitoson.tavernkeeper.tavern.managers.DiningManager;
import maxitoson.tavernkeeper.tavern.managers.EconomyManager;
import maxitoson.tavernkeeper.tavern.managers.TavernContext;

/**
 * Defines tavern upgrade levels with requirements and benefits
 * Each level specifies what it unlocks and how it modifies manager limits
 * 
 * Pattern: Each upgrade level applies itself to managers
 */
public enum TavernUpgrade {
    LEVEL_1(
        0,      // No reputation required
        0,      // No money required
        "Basic Tavern"
    ) {
        @Override
        public void applyToDiningManager(DiningManager manager) {
            manager.setMaxTables(50);
            manager.setMaxChairs(200);  // 4 chairs per table on average
        }
        
        @Override
        public void applyToCustomerManager(CustomerManager manager) {
            manager.setSpawnRateMultiplier(1.0f);
        }
        
        @Override
        public void applyToEconomyManager(EconomyManager manager) {
            manager.setPaymentMultiplier(1.0f);
        }
    },
    
    LEVEL_2(
        100,    // Reputation required
        500,    // Copper coins required
        "Improved Tavern"
    ) {
        @Override
        public void applyToDiningManager(DiningManager manager) {
            manager.setMaxTables(75);
            manager.setMaxChairs(300);  // 4 chairs per table on average
        }
        
        @Override
        public void applyToCustomerManager(CustomerManager manager) {
            manager.setSpawnRateMultiplier(1.5f); // 50% faster spawns
        }
        
        @Override
        public void applyToEconomyManager(EconomyManager manager) {
            manager.setPaymentMultiplier(1.2f);  // +20% payment
        }
    },
    
    LEVEL_3(
        500,    // Reputation required
        2000,   // Copper coins required
        "Renowned Tavern"
    ) {
        @Override
        public void applyToDiningManager(DiningManager manager) {
            manager.setMaxTables(100);
            manager.setMaxChairs(400);  // 4 chairs per table on average
        }
        
        @Override
        public void applyToCustomerManager(CustomerManager manager) {
            manager.setSpawnRateMultiplier(2.0f); // 2x faster spawns
        }
        
        @Override
        public void applyToEconomyManager(EconomyManager manager) {
            manager.setPaymentMultiplier(1.5f);  // +50% payment
        }
    };
    
    private final int reputationRequired;
    private final int moneyRequired;
    private final String displayName;
    
    TavernUpgrade(int reputationRequired, int moneyRequired, String displayName) {
        this.reputationRequired = reputationRequired;
        this.moneyRequired = moneyRequired;
        this.displayName = displayName;
    }
    
    /**
     * Check if this upgrade's requirements are met
     */
    public boolean meetsRequirements(TavernContext tavern) {
        return tavern.getReputation() >= reputationRequired 
            && tavern.getTotalMoneyEarned() >= moneyRequired;
    }
    
    /**
     * Apply this upgrade to a DiningManager
     * Subclasses override to set specific limits
     */
    public abstract void applyToDiningManager(DiningManager manager);
    
    /**
     * Apply this upgrade to a CustomerManager
     * Subclasses override to set spawn rate multiplier
     */
    public abstract void applyToCustomerManager(CustomerManager manager);
    
    /**
     * Apply this upgrade to an EconomyManager
     * Subclasses override to set payment multiplier
     */
    public abstract void applyToEconomyManager(EconomyManager manager);
    
    /**
     * Get the next upgrade level, or null if this is max level
     */
    public TavernUpgrade getNextLevel() {
        TavernUpgrade[] values = TavernUpgrade.values();
        int nextOrdinal = this.ordinal() + 1;
        return nextOrdinal < values.length ? values[nextOrdinal] : null;
    }
    
    /**
     * Get the previous upgrade level, or null if this is first level
     */
    public TavernUpgrade getPreviousLevel() {
        int prevOrdinal = this.ordinal() - 1;
        return prevOrdinal >= 0 ? TavernUpgrade.values()[prevOrdinal] : null;
    }
    
    // Getters
    public int getReputationRequired() {
        return reputationRequired;
    }
    
    public int getMoneyRequired() {
        return moneyRequired;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}

