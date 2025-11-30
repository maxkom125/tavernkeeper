package maxitoson.tavernkeeper.tavern;

import net.minecraft.nbt.CompoundTag;

/**
 * Value object holding tavern statistics
 * Encapsulates all tracked metrics for the tavern
 * 
 * Pattern: Immutable data holder with package-private modifiers
 * Only Tavern can modify, everyone can read
 */
public class TavernStatistics {
    private long totalMoneyEarned;
    private int currentReputation;
    private int totalCustomersServed;
    
    public TavernStatistics() {
        this.totalMoneyEarned = 0;
        this.currentReputation = 0;
        this.totalCustomersServed = 0;
    }
    
    // Package-private modifiers (only Tavern can call)
    // WARNING: Do not call these directly! Use Tavern.modifyStatistics()
    
    void addMoney(long amount) {
        this.totalMoneyEarned += amount;
    }
    
    void addReputation(int amount) {
        this.currentReputation += amount;
    }
    
    void incrementCustomersServed() {
        this.totalCustomersServed++;
    }
    
    // Public getters (anyone can read)
    public long getTotalMoneyEarned() {
        return totalMoneyEarned;
    }
    
    public int getReputation() {
        return currentReputation;
    }
    
    public int getTotalCustomersServed() {
        return totalCustomersServed;
    }
    
    // NBT Serialization
    public void save(CompoundTag tag) {
        tag.putLong("totalMoneyEarned", totalMoneyEarned);
        tag.putInt("currentReputation", currentReputation);
        tag.putInt("totalCustomersServed", totalCustomersServed);
    }
    
    public void load(CompoundTag tag) {
        if (tag.contains("totalMoneyEarned")) {
            this.totalMoneyEarned = tag.getLong("totalMoneyEarned");
        }
        if (tag.contains("currentReputation")) {
            this.currentReputation = tag.getInt("currentReputation");
        }
        if (tag.contains("totalCustomersServed")) {
            this.totalCustomersServed = tag.getInt("totalCustomersServed");
        }
    }
}

