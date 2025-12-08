package maxitoson.tavernkeeper.entities.ai.lifecycle;

import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import net.minecraft.server.level.ServerLevel;

/**
 * Defines a customer's journey through the tavern
 * 
 * Each lifecycle type defines:
 * - Initial state when spawning
 * - State transition rules
 * - Which behaviors should execute
 */
public interface CustomerLifecycle {
    
    /**
     * Get the initial state when customer spawns
     * This determines where the customer goes first
     */
    CustomerState getInitialState();
    
    /**
     * Determine next state based on current state completion
     * This is the core lifecycle logic - defines the customer's journey
     * 
     * @param current The state that just completed
     * @param level The server level (for context-aware decisions if needed)
     * @return The next state to transition to
     */
    CustomerState getNextState(CustomerState current, ServerLevel level);
    
    /**
     * Get lifecycle type identifier
     * Maps lifecycle back to LifecycleType enum for compatibility
     */
    LifecycleType getType();
    
    /**
     * Get human-readable description for logging
     */
    String getDescription();
}

