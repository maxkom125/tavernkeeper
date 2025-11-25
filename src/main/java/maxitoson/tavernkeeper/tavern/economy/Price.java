package maxitoson.tavernkeeper.tavern.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Represents a price with currency and amount
 * Makes the economy system flexible for different payment types
 */
public class Price {
    private final Item currency;
    private final int amount;
    
    public Price(Item currency, int amount) {
        this.currency = currency;
        this.amount = amount;
    }
    
    public Item getCurrency() {
        return currency;
    }
    
    public int getAmount() {
        return amount;
    }
    
    /**
     * Create an ItemStack representing this price
     */
    public ItemStack toItemStack() {
        return new ItemStack(currency, amount);
    }
    
    /**
     * Get display name for chat messages
     */
    public String getDisplayName() {
        return amount + " " + currency.getDescription().getString();
    }
}

