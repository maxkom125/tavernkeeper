package maxitoson.tavernkeeper.tavern.economy;

/**
 * Base interface for all customer requests (food, sleeping, drinks, etc.)
 * Represents what the customer wants and how much they'll pay
 * 
 * Design: Single source of truth - customer has ONE request at a time
 * Benefits:
 * - Enforces invariant (one request only)
 * - Extensible (easy to add new request types)
 * - Type-safe polymorphism
 */
public interface CustomerRequest {
    
    /**
     * Get the price the customer will pay
     */
    Price getPrice();
    
    /**
     * Get display name for chat messages and UI
     */
    String getDisplayName();
}

