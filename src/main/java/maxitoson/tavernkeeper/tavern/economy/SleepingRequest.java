package maxitoson.tavernkeeper.tavern.economy;

/**
 * Represents a customer's sleeping request with pricing
 * Simpler than FoodRequest - only contains payment information
 */
public class SleepingRequest implements CustomerRequest {
    private final Price price;
    
    public SleepingRequest(Price price) {
        this.price = price;
    }
    
    @Override
    public Price getPrice() {
        return price;
    }
    
    /**
     * Get display name for chat messages
     */
    @Override
    public String getDisplayName() {
        return "Sleeping for " + price.getDisplayName();
    }
}

