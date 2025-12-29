package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.furniture.types.SleepingFurnitureType;
import maxitoson.tavernkeeper.tavern.managers.ManagerContext;

/**
 * Interface defining what SleepingSpace can query from its parent SleepingManager
 * 
 * This prevents SleepingSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface SleepingManagerContext extends ManagerContext {
    /**
     * Check if more furniture of the given type can be added based on current limits
     * @param type the type of furniture to check
     * @return true if furniture can be added, false if limit reached
     */
    boolean canAddFurniture(SleepingFurnitureType type);
}

