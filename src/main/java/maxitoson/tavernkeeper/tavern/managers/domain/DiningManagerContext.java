package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.managers.ManagerContext;

/**
 * Interface defining what DiningSpace can query from its parent DiningManager
 * 
 * This prevents DiningSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface DiningManagerContext extends ManagerContext {
    /**
     * Check if more tables can be added based on current upgrade limits
     */
    boolean canAddTable();
    
    /**
     * Check if more chairs can be added based on current upgrade limits
     */
    boolean canAddChair();
}

