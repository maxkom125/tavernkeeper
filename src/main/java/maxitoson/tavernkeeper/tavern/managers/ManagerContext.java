package maxitoson.tavernkeeper.tavern.managers;

/**
 * Base marker interface for all Manager context interfaces
 * 
 * This follows the Interface Segregation Principle (ISP):
 * - Spaces depend on abstractions, not concrete Managers
 * - Each Space type gets its own specific context interface
 * - Prevents Spaces from accessing Manager methods they shouldn't
 * 
 * This is a marker interface that all specific context interfaces extend.
 */
public interface ManagerContext {
    // Marker interface - specific context interfaces will extend this
}

