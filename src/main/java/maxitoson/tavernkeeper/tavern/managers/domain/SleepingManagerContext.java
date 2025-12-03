package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.tavern.managers.ManagerContext;

/**
 * Interface defining what SleepingSpace can query from its parent SleepingManager
 * 
 * Currently empty but can be extended with methods like:
 * - boolean canAddBed()
 * - int getRemainingBedCapacity()
 * 
 * This prevents SleepingSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface SleepingManagerContext extends ManagerContext {
    // Future: validation methods will be added here
}

