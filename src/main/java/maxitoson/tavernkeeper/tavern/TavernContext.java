package maxitoson.tavernkeeper.tavern;

import net.minecraft.core.BlockPos;

/**
 * Interface defining what components can query from Tavern
 * 
 * This follows the Interface Segregation Principle (ISP):
 * - Components (Managers, Entities) depend on an abstraction, not the concrete Tavern
 * - Compile-time enforcement: Components can ONLY call these methods
 * - Each component gets exactly what it needs, no more
 * 
 * Benefits:
 * - Prevents direct access to Tavern internals
 * - Prevents components from accessing other components (getDiningManager() not exposed)
 * - Clear contract: "This is what you can ask from Tavern"
 * - Easier to test: Mock this interface instead of entire Tavern
 * 
 * Used by:
 * - Managers (to access tavern state and record statistics)
 * - Entities (to adjust reputation and notify players)
 */
public interface TavernContext {
    /**
     * Check if tavern is open for business
     * Used by CustomerManager to determine if customers should spawn
     */
    boolean isOpen();
    
    /**
     * Get a random center point for the tavern (e.g., random lectern)
     * Used by CustomerManager to determine where customers spawn around
     */
    BlockPos getTavernCenter();
    
    // ========== Statistics Queries ==========
    
    /**
     * Get total money earned by the tavern (in copper coins)
     */
    long getTotalMoneyEarned();
    
    /**
     * Get current tavern reputation
     */
    int getReputation();
    
    /**
     * Get total customers served
     */
    int getTotalCustomersServed();
    
    // ========== Statistics Recording ==========
    
    /**
     * Record a sale (customer payment)
     * Updates statistics and checks for upgrade unlocks
     * 
     * @param copperAmount Amount in copper coins
     */
    void recordSale(int copperAmount);
    
    /**
     * Adjust tavern reputation (positive to increase, negative to decrease)
     * Triggers upgrade checks and persistence
     * 
     * @param amount Amount to change (can be positive or negative)
     */
    void adjustReputation(int amount);
    
    // ========== Server Access ==========
    
    /**
     * Get the server level for broadcasting messages
     * Used by entities to send messages to all players
     */
    net.minecraft.server.level.ServerLevel getLevel();
}

