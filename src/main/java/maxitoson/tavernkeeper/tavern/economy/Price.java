package maxitoson.tavernkeeper.tavern.economy;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a price stored as copper-equivalent value.
 * Uses CoinRegistry for all conversion logic.
 */
public class Price {
    
    /** Max safe price: ~21 netherite worth of copper (int max = 2,147,483,647) */
    public static final int MAX_COPPER_VALUE = Integer.MAX_VALUE;
    
    private final int copperValue; // Base value in copper equivalent
    
    /**
     * Create a price from any coin type - converts to copper equivalent internally
     */
    public Price(Item currency, int amount) {
        this.copperValue = CoinRegistry.toCopperValue(currency, amount);
    }
    
    /**
     * Get the copper equivalent value
     */
    public int getCopperValue() {
        return copperValue;
    }
    
    /**
     * Create a single ItemStack with the highest tier coin (for visual display).
     * 
     * Example: 10232 copper -> 1 Gold Coin
     */
    public ItemStack toHighestTierStack() {
        CoinRegistry.CoinBreakdown breakdown = CoinRegistry.getHighestTierBreakdown(copperValue);
        return new ItemStack(breakdown.coin(), breakdown.amount());
    }
    
    /**
     * Create ItemStacks representing full breakdown of this price.
     * Returns list of stacks for each tier that has coins.
     * 
     * Example: 10232 copper -> [1 Gold, 2 Iron, 32 Copper]
     */
    public List<ItemStack> toItemStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        CoinRegistry.FullBreakdown breakdown = CoinRegistry.getFullBreakdown(copperValue);
        
        // Add stacks from highest to lowest tier (for nicer inventory order)
        for (int i = CoinRegistry.TIER_COUNT - 1; i >= 0; i--) {
            int amount = breakdown.getAmount(i);
            if (amount > 0) {
                Item coin = CoinRegistry.getItemByIndex(i);
                stacks.add(new ItemStack(coin, amount));
            }
        }
        
        return stacks;
    }
    
    /**
     * Get display name showing full breakdown (e.g., "1 Gold Coin + 2 Iron Coin + 32 Copper Coin")
     */
    public String getDisplayName() {
        return CoinRegistry.formatAsDisplay(copperValue);
    }
}

