package maxitoson.tavernkeeper.tavern.spaces;

/**
 * Interface defining what DiningSpace can query from its parent DiningManager
 * 
 * Currently empty but can be extended with methods like:
 * - boolean canAddTable()
 * - boolean canAddChair()
 * - int getRemainingTableCapacity()
 * 
 * This prevents DiningSpace from calling:
 * - addSpace(), removeSpace() (structure changes)
 * - scanAll() (manager-level operations)
 * - getSpaces() (accessing other spaces)
 */
public interface DiningManagerContext extends ManagerContext {
    // Future: validation methods will be added here
}

