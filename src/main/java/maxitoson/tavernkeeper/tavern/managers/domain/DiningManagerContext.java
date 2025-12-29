package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.furniture.types.DiningFurnitureType;
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
     * Check if more furniture of the given type can be added based on current upgrade limits
     * @param type the type of furniture to check
     * @return true if furniture can be added, false if limit reached
     */
    boolean canAddFurniture(DiningFurnitureType type);
}

