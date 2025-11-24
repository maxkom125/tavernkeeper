package maxitoson.tavernkeeper.entities;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Represents a customer's food request
 * Currently hardcoded to carrots, but structured for future expansion
 */
public class FoodRequest {
    private final Item requestedItem;
    private final int requestedAmount;
    
    // For future: could add price, satisfaction value, etc.
    
    public FoodRequest(Item item, int amount) {
        this.requestedItem = item;
        this.requestedAmount = amount;
    }
    
    /**
     * Create a default food request (currently always carrots)
     * TODO: In the future, this could be randomized or based on tavern menu
     */
    public static FoodRequest createDefault() {
        return new FoodRequest(Items.CARROT, 3);
    }
    
    public Item getRequestedItem() {
        return requestedItem;
    }
    
    public int getRequestedAmount() {
        return requestedAmount;
    }
    
    /**
     * Check if the given ItemStack satisfies this request
     */
    public boolean isSatisfiedBy(ItemStack stack) {
        return stack.is(requestedItem) && stack.getCount() >= requestedAmount;
    }
    
    /**
     * Create an ItemStack representing this request (for display purposes)
     */
    public ItemStack toDisplayStack() {
        return new ItemStack(requestedItem, requestedAmount);
    }
    
    /**
     * Get display name for chat messages
     */
    public String getDisplayName() {
        return requestedAmount + " " + requestedItem.getDescription().getString();
    }
}

