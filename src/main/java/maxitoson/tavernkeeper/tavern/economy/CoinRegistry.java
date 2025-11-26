package maxitoson.tavernkeeper.tavern.economy;

import maxitoson.tavernkeeper.TavernKeeperMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

/**
 * Central registry for coin types and conversion rates.
 * Single source of truth for the tavern economy system.
 */
public final class CoinRegistry {
    
    /** Result of converting a copper value to single-tier breakdown */
    public record CoinBreakdown(Item coin, int amount, int leftoverCopper) {}
    
    /** Full breakdown across all tiers (index 0 = copper, 4 = netherite) */
    public record FullBreakdown(int[] amounts) {
        public int getAmount(int tierIndex) {
            return tierIndex >= 0 && tierIndex < amounts.length ? amounts[tierIndex] : 0;
        }
    }
    
    /** Conversion rate: 100 lower tier = 1 higher tier */
    public static final int CONVERSION_RATE = 100;
    
    /** Number of coin tiers */
    public static final int TIER_COUNT = 5;
    
    /** Tier keys for NBT storage (lowest to highest) */
    public static final String[] TIER_KEYS = {"copper", "iron", "gold", "diamond", "netherite"};
    
    // Cached tier items (lazy init to avoid static init order issues)
    private static Item[] tierItems = null;
    
    // Cached tier values in copper equivalent
    private static int[] tierValues = null;
    
    private CoinRegistry() {} // Prevent instantiation
    
    /**
     * Get all coin items from lowest to highest tier
     */
    public static Item[] getTierItems() {
        if (tierItems == null) {
            tierItems = new Item[] {
                TavernKeeperMod.COPPER_COIN.get(),
                TavernKeeperMod.IRON_COIN.get(),
                TavernKeeperMod.GOLD_COIN.get(),
                TavernKeeperMod.DIAMOND_COIN.get(),
                TavernKeeperMod.NETHERITE_COIN.get()
            };
        }
        return tierItems;
    }
    
    /**
     * Get the copper-equivalent value of each tier (1, 100, 10000, etc.)
     */
    public static int[] getTierValues() {
        if (tierValues == null) {
            tierValues = new int[TIER_COUNT];
            int value = 1;
            for (int i = 0; i < TIER_COUNT; i++) {
                tierValues[i] = value;
                value *= CONVERSION_RATE;
            }
        }
        return tierValues;
    }
    
    /**
     * Check if an item is a coin
     */
    public static boolean isCoin(Item item) {
        for (Item coin : getTierItems()) {
            if (coin == item) return true;
        }
        return false;
    }
    
    /**
     * Get the tier index (0-4) for a coin item, or -1 if not a coin
     */
    public static int getTierIndex(Item item) {
        Item[] items = getTierItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i] == item) return i;
        }
        return -1;
    }
    
    /**
     * Get the tier key (e.g., "copper") for a coin item, or null if not a coin
     */
    public static String getTierKey(Item item) {
        int index = getTierIndex(item);
        return index >= 0 ? TIER_KEYS[index] : null;
    }
    
    /**
     * Get the coin item for a tier key (e.g., "copper" -> Copper Coin)
     */
    public static Item getItemByKey(String key) {
        for (int i = 0; i < TIER_KEYS.length; i++) {
            if (TIER_KEYS[i].equals(key)) {
                return getTierItems()[i];
            }
        }
        return null;
    }
    
    /**
     * Get the coin item for a tier index (0 = copper, 4 = netherite)
     */
    public static Item getItemByIndex(int index) {
        if (index < 0 || index >= TIER_COUNT) return null;
        return getTierItems()[index];
    }
    
    /**
     * Convert any coin amount to copper-equivalent value
     */
    public static int toCopperValue(Item coin, int amount) {
        int tierIndex = getTierIndex(coin);
        if (tierIndex < 0) return amount; // Unknown coin, treat as copper
        return amount * getTierValues()[tierIndex];
    }
    
    /**
     * Get the copper-equivalent value for a tier index
     */
    public static int getTierValue(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIER_COUNT) return 1;
        return getTierValues()[tierIndex];
    }
    
    // ==================== CONVERSION METHODS ====================
    
    /**
     * Auto-convert lower tier coins to higher tier in a wallet NBT tag.
     * 100 of a lower tier = 1 of the next tier.
     * Modifies the tag in place.
     */
    public static void autoConvertTag(CompoundTag tag) {
        // Process from lowest to highest tier (skip last - nothing to convert to)
        for (int i = 0; i < TIER_COUNT - 1; i++) {
            String currentTier = TIER_KEYS[i];
            String nextTier = TIER_KEYS[i + 1];
            
            long currentCount = tag.getLong(currentTier);
            if (currentCount >= CONVERSION_RATE) {
                long convertAmount = currentCount / CONVERSION_RATE;
                long remainder = currentCount % CONVERSION_RATE;
                
                tag.putLong(currentTier, remainder);
                tag.putLong(nextTier, tag.getLong(nextTier) + convertAmount);
            }
        }
    }
    
    /**
     * Get full breakdown of a copper value across ALL tiers.
     * 
     * Example: 10232 copper -> [32, 2, 1, 0, 0] (32 copper, 2 iron, 1 gold)
     */
    public static FullBreakdown getFullBreakdown(int copperValue) {
        int[] amounts = new int[TIER_COUNT];
        int[] values = getTierValues();
        int remaining = copperValue;
        
        // Process from highest to lowest tier
        for (int i = TIER_COUNT - 1; i >= 0; i--) {
            if (remaining >= values[i]) {
                amounts[i] = remaining / values[i];
                remaining = remaining % values[i];
            }
        }
        
        return new FullBreakdown(amounts);
    }
    
    /**
     * Get the highest tier coin for a copper value (for ItemStack creation).
     * Returns the single highest tier with its amount.
     * 
     * Example: 10232 copper -> CoinBreakdown(Gold, 1, 232)
     */
    public static CoinBreakdown getHighestTierBreakdown(int copperValue) {
        int[] values = getTierValues();
        
        // Find highest tier that fits
        for (int i = TIER_COUNT - 1; i >= 0; i--) {
            if (copperValue >= values[i]) {
                int amount = copperValue / values[i];
                int leftover = copperValue % values[i];
                return new CoinBreakdown(getItemByIndex(i), amount, leftover);
            }
        }
        
        // Fallback to copper
        return new CoinBreakdown(getItemByIndex(0), copperValue, 0);
    }
    
    /**
     * Format a copper value as a display string showing ALL tiers.
     * 
     * Example: 10232 copper -> "1 Gold Coin + 2 Iron Coin + 32 Copper Coin"
     */
    public static String formatAsDisplay(int copperValue) {
        FullBreakdown breakdown = getFullBreakdown(copperValue);
        StringBuilder result = new StringBuilder();
        
        // Build string from highest to lowest tier
        for (int i = TIER_COUNT - 1; i >= 0; i--) {
            int amount = breakdown.getAmount(i);
            if (amount > 0) {
                if (result.length() > 0) {
                    result.append(" + ");
                }
                result.append(amount).append(" ").append(getItemByIndex(i).getDescription().getString());
            }
        }
        
        // If somehow empty (0 value), show 0 copper
        if (result.length() == 0) {
            result.append("0 ").append(getItemByIndex(0).getDescription().getString());
        }
        
        return result.toString();
    }
}

