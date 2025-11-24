package maxitoson.tavernkeeper.tavern.managers;

import net.minecraft.core.BlockPos;

/**
 * Interface defining what Managers can query from their parent Tavern
 * 
 * This follows the Interface Segregation Principle (ISP):
 * - Managers depend on an abstraction, not the concrete Tavern
 * - Compile-time enforcement: Managers can ONLY call these methods
 * - Each manager gets exactly what it needs, no more
 * 
 * Benefits:
 * - Prevents managers from accessing other managers (getDiningManager() not exposed)
 * - Prevents managers from modifying tavern (createArea() not exposed)
 * - Clear contract: "This is what a Manager can ask from its Tavern"
 * - Easier to test: Mock this interface instead of entire Tavern
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
}

