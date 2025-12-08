package maxitoson.tavernkeeper.entities.ai;

/**
 * Type of lifecycle/journey the customer will follow
 * Decided at spawn time and determines entire customer journey
 * Maps to CustomerLifecycle implementations
 */
public enum LifecycleType {
    DINING_ONLY,        // Customer wants food only (lectern → eat → leave)
    SLEEPING_ONLY,      // Customer wants sleeping only (reception → sleep → leave)
    FULL_SERVICE        // Customer wants both (lectern → eat → reception → sleep → leave)
}

