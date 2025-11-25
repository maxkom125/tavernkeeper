package maxitoson.tavernkeeper.tavern.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a customer's food request with pricing
 * Contains item, amount, and payment information
 */
public class FoodRequest {
    private final Item requestedItem;
    private final int requestedAmount;
    private final Price price;
    
    public FoodRequest(Item item, int amount, Price price) {
        this.requestedItem = item;
        this.requestedAmount = amount;
        this.price = price;
    }
    
    public Item getRequestedItem() {
        return requestedItem;
    }
    
    public int getRequestedAmount() {
        return requestedAmount;
    }
    
    public Price getPrice() {
        return price;
    }
    
    /**
     * Check if the given ItemStack satisfies this request
     */
    public boolean isSatisfiedBy(ItemStack stack) {
        return stack.is(requestedItem) && stack.getCount() >= requestedAmount;
    }
    
    /**
     * Get display name for chat messages
     */
    public String getDisplayName() {
        return requestedAmount + " " + requestedItem.getDescription().getString();
    }
}

