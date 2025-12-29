package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.furniture.types.ServiceFurnitureType;
import maxitoson.tavernkeeper.tavern.managers.ManagerContext;

/**
 * Interface defining what ServiceSpace can query from its parent ServiceManager
 * 
 * This prevents ServiceSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface ServiceManagerContext extends ManagerContext {
    /**
     * Check if more furniture of the given type can be added based on current limits
     * @param type the type of furniture to check
     * @return true if furniture can be added, false if limit reached
     */
    boolean canAddFurniture(ServiceFurnitureType type);
}

