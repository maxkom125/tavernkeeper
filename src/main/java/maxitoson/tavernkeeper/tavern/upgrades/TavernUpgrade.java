package maxitoson.tavernkeeper.tavern.upgrades;

import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.managers.domain.CustomerManager;
import maxitoson.tavernkeeper.tavern.managers.domain.DiningManager;
import maxitoson.tavernkeeper.tavern.managers.domain.SleepingManager;
import maxitoson.tavernkeeper.tavern.managers.system.EconomyManager;

/**
 * Defines tavern upgrade levels with requirements and benefits
 * Each level specifies what it unlocks and how it modifies manager limits
 * 
 * Pattern: Values stored as enum fields (single source of truth)
 * Apply methods use these fields to update managers!
 */
public enum TavernUpgrade {
    LEVEL_1(
        // Requirements
        0,      // No reputation required
        0,      // No money required
        "Basic Tavern",
        // Benefits
        2,      // Max tables
        8,      // Max chairs
        2,      // Max beds
        1.0f,   // Spawn rate multiplier
        1.0f    // Payment multiplier
    ),
    
    LEVEL_2(
        // Requirements
        100,    // Reputation required
        500,    // Copper coins required
        "Improved Tavern",
        // Benefits
        4,      // Max tables
        16,     // Max chairs
        4,      // Max beds
        1.1f,   // Spawn rate multiplier (10% faster)
        1.1f    // Payment multiplier (+10%)
    ),
    
    LEVEL_3(
        // Requirements
        500,    // Reputation required
        4000,   // Copper coins required
        "Renowned Tavern",
        // Benefits
        8,      // Max tables
        32,     // Max chairs
        8,      // Max beds
        1.3f,   // Spawn rate multiplier (30% faster)
        1.3f    // Payment multiplier (+30%)
    );
    
    // Requirements
    private final int reputationRequired;
    private final int moneyRequired;
    private final String displayName;
    
    // Benefits
    private final int maxTables;
    private final int maxChairs;
    private final int maxBeds;
    private final float spawnRateMultiplier;
    private final float paymentMultiplier;
    
    TavernUpgrade(int reputationRequired, int moneyRequired, String displayName,
                  int maxTables, int maxChairs, int maxBeds,
                  float spawnRateMultiplier, float paymentMultiplier) {
        this.reputationRequired = reputationRequired;
        this.moneyRequired = moneyRequired;
        this.displayName = displayName;
        this.maxTables = maxTables;
        this.maxChairs = maxChairs;
        this.maxBeds = maxBeds;
        this.spawnRateMultiplier = spawnRateMultiplier;
        this.paymentMultiplier = paymentMultiplier;
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
     * Uses the stored benefit values
     */
    public void applyToDiningManager(DiningManager manager) {
        manager.setMaxTables(maxTables);
        manager.setMaxChairs(maxChairs);
    }
    
    /**
     * Apply this upgrade to a CustomerManager
     * Uses the stored benefit values
     */
    public void applyToCustomerManager(CustomerManager manager) {
        manager.setSpawnRateMultiplier(spawnRateMultiplier);
    }
    
    /**
     * Apply this upgrade to a SleepingManager
     * Uses the stored benefit values
     */
    public void applyToSleepingManager(SleepingManager manager) {
        manager.setMaxBeds(maxBeds);
    }
    
    /**
     * Apply this upgrade to an EconomyManager
     * Uses the stored benefit values
     */
    public void applyToEconomyManager(EconomyManager manager) {
        manager.setPaymentMultiplier(paymentMultiplier);
    }
    
    // ========== Benefit Getters (for display/queries) ==========
    
    /**
     * Get the max tables this upgrade level provides
     * Used for display and comparison
     */
    public int getMaxTables() {
        return maxTables;
    }
    
    /**
     * Get the max chairs this upgrade level provides
     * Used for display and comparison
     */
    public int getMaxChairs() {
        return maxChairs;
    }
    
    /**
     * Get the max beds this upgrade level provides
     * Used for display and comparison
     */
    public int getMaxBeds() {
        return maxBeds;
    }
    
    /**
     * Get the spawn rate multiplier this upgrade level provides
     * Used for display and comparison
     */
    public float getSpawnRateMultiplier() {
        return spawnRateMultiplier;
    }
    
    /**
     * Get the payment multiplier this upgrade level provides
     * Used for display and comparison
     */
    public float getPaymentMultiplier() {
        return paymentMultiplier;
    }
    
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
    
    // ========== Requirement Getters ==========
    
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

