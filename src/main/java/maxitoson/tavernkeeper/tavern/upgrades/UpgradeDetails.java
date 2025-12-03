package maxitoson.tavernkeeper.tavern.upgrades;

import maxitoson.tavernkeeper.tavern.Tavern;

/**
 * Result object containing upgrade information for display
 * Data-only - no UI logic, just the facts about current/next upgrades
 * 
 * Pattern: Bridge between business layer (Tavern) and UI layer (formatters/displays)
 */
public class UpgradeDetails {
    private final TavernUpgrade currentLevel;
    private final TavernUpgrade nextLevel;
    private final boolean canUpgrade;
    
    // Current stats
    private final int currentMaxTables;
    private final int currentMaxChairs;
    private final float currentSpawnRate;
    private final float currentPaymentMultiplier;
    
    // Next level stats (if available)
    private final Integer nextMaxTables;
    private final Integer nextMaxChairs;
    private final Float nextSpawnRate;
    private final Float nextPaymentMultiplier;
    
    // Requirements progress
    private final long currentMoney;
    private final int currentReputation;
    private final Integer requiredMoney;
    private final Integer requiredReputation;
    
    private UpgradeDetails(Builder builder) {
        this.currentLevel = builder.currentLevel;
        this.nextLevel = builder.nextLevel;
        this.canUpgrade = builder.canUpgrade;
        this.currentMaxTables = builder.currentMaxTables;
        this.currentMaxChairs = builder.currentMaxChairs;
        this.currentSpawnRate = builder.currentSpawnRate;
        this.currentPaymentMultiplier = builder.currentPaymentMultiplier;
        this.nextMaxTables = builder.nextMaxTables;
        this.nextMaxChairs = builder.nextMaxChairs;
        this.nextSpawnRate = builder.nextSpawnRate;
        this.nextPaymentMultiplier = builder.nextPaymentMultiplier;
        this.currentMoney = builder.currentMoney;
        this.currentReputation = builder.currentReputation;
        this.requiredMoney = builder.requiredMoney;
        this.requiredReputation = builder.requiredReputation;
    }
    
    /**
     * Create upgrade details from current tavern state
     */
    public static UpgradeDetails from(Tavern tavern) {
        TavernUpgrade current = tavern.getCurrentUpgrade();
        TavernUpgrade next = tavern.getUpgradeManager().getNextUpgrade();
        boolean canUpgrade = tavern.getUpgradeManager().canUpgradeToNext();
        
        Builder builder = new Builder()
            .currentLevel(current)
            .nextLevel(next)
            .canUpgrade(canUpgrade)
            .currentMaxTables(tavern.getDiningManager().getMaxTables())
            .currentMaxChairs(tavern.getDiningManager().getMaxChairs())
            .currentSpawnRate(tavern.getCustomerManager().getSpawnRateMultiplier())
            .currentPaymentMultiplier(tavern.getEconomyManager().getPaymentMultiplierValue())
            .currentMoney(tavern.getTotalMoneyEarned())
            .currentReputation(tavern.getReputation());
        
        // If there's a next level, include its requirements and benefits
        if (next != null) {
            builder.requiredMoney(next.getMoneyRequired())
                   .requiredReputation(next.getReputationRequired());
            
            // Query what next level would provide by getting a snapshot
            // We don't actually apply it, just query what it would be
            builder.nextMaxTables(next.getMaxTables())
                   .nextMaxChairs(next.getMaxChairs())
                   .nextSpawnRate(next.getSpawnRateMultiplier())
                   .nextPaymentMultiplier(next.getPaymentMultiplier());
        }
        
        return builder.build();
    }
    
    // Getters
    public TavernUpgrade getCurrentLevel() { return currentLevel; }
    public TavernUpgrade getNextLevel() { return nextLevel; }
    public boolean hasNextLevel() { return nextLevel != null; }
    public boolean canUpgrade() { return canUpgrade; }
    
    public int getCurrentMaxTables() { return currentMaxTables; }
    public int getCurrentMaxChairs() { return currentMaxChairs; }
    public float getCurrentSpawnRate() { return currentSpawnRate; }
    public float getCurrentPaymentMultiplier() { return currentPaymentMultiplier; }
    
    public Integer getNextMaxTables() { return nextMaxTables; }
    public Integer getNextMaxChairs() { return nextMaxChairs; }
    public Float getNextSpawnRate() { return nextSpawnRate; }
    public Float getNextPaymentMultiplier() { return nextPaymentMultiplier; }
    
    public long getCurrentMoney() { return currentMoney; }
    public int getCurrentReputation() { return currentReputation; }
    public Integer getRequiredMoney() { return requiredMoney; }
    public Integer getRequiredReputation() { return requiredReputation; }
    
    public boolean isMoneyRequirementMet() {
        return requiredMoney == null || currentMoney >= requiredMoney;
    }
    
    public boolean isReputationRequirementMet() {
        return requiredReputation == null || currentReputation >= requiredReputation;
    }
    
    // Builder pattern for flexible construction
    public static class Builder {
        private TavernUpgrade currentLevel;
        private TavernUpgrade nextLevel;
        private boolean canUpgrade;
        private int currentMaxTables;
        private int currentMaxChairs;
        private float currentSpawnRate;
        private float currentPaymentMultiplier;
        private Integer nextMaxTables;
        private Integer nextMaxChairs;
        private Float nextSpawnRate;
        private Float nextPaymentMultiplier;
        private long currentMoney;
        private int currentReputation;
        private Integer requiredMoney;
        private Integer requiredReputation;
        
        public Builder currentLevel(TavernUpgrade val) { currentLevel = val; return this; }
        public Builder nextLevel(TavernUpgrade val) { nextLevel = val; return this; }
        public Builder canUpgrade(boolean val) { canUpgrade = val; return this; }
        public Builder currentMaxTables(int val) { currentMaxTables = val; return this; }
        public Builder currentMaxChairs(int val) { currentMaxChairs = val; return this; }
        public Builder currentSpawnRate(float val) { currentSpawnRate = val; return this; }
        public Builder currentPaymentMultiplier(float val) { currentPaymentMultiplier = val; return this; }
        public Builder nextMaxTables(Integer val) { nextMaxTables = val; return this; }
        public Builder nextMaxChairs(Integer val) { nextMaxChairs = val; return this; }
        public Builder nextSpawnRate(Float val) { nextSpawnRate = val; return this; }
        public Builder nextPaymentMultiplier(Float val) { nextPaymentMultiplier = val; return this; }
        public Builder currentMoney(long val) { currentMoney = val; return this; }
        public Builder currentReputation(int val) { currentReputation = val; return this; }
        public Builder requiredMoney(Integer val) { requiredMoney = val; return this; }
        public Builder requiredReputation(Integer val) { requiredReputation = val; return this; }
        
        public UpgradeDetails build() {
            return new UpgradeDetails(this);
        }
    }
}

