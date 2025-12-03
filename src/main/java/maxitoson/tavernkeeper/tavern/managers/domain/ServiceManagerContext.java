package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.managers.ManagerContext;

/**
 * Interface defining what ServiceSpace can query from its parent ServiceManager
 * 
 * Currently empty but can be extended with methods like:
 * - boolean canAddLectern()
 * - boolean canAddBarrel()
 * 
 * This prevents ServiceSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface ServiceManagerContext extends ManagerContext {
    // Future: validation methods will be added here
}

