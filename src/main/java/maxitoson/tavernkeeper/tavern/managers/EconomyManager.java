package maxitoson.tavernkeeper.tavern.managers;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.Price;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
// import gold ingot
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import maxitoson.tavernkeeper.TavernKeeperMod;

import java.util.Random;

/**
 * Manager that handles all economy-related operations in the tavern
 * - Generates food requests for customers with dynamic pricing
 * - Determines prices based on food quality (nutrition + saturation)
 * - (Future) Tracks tavern income, manages menu, dynamic pricing
 * 
 * Pattern: Tavern owns EconomyManager, Customers ask EconomyManager what to order
 */
public class EconomyManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Random random = new Random();
    
    public EconomyManager(TavernContext tavern) {
        // Future: Store tavern reference for querying available menu items, etc.
    }
    
    /**
     * Generate a food request for a new customer with smart pricing
     * Currently uses carrots only, but formula is ready for expansion
     * 
     * Price formula: food_quality * amount
     * Amount: Random from 1 to max_amount
     * Max amount decreases linearly with food quality (saturation + nutrition):
     * - Melon Slice (quality 3.2): max 10
     * - Carrot (quality 6.6): max 8
     * - Bread (quality 11): max 6
     * - Cooked Beef (quality 20.8): max 1
     */
    public FoodRequest createFoodRequest() {
        // For now: just carrots, but using smart pricing formula
        // TODO: Randomize from available menu
        Item foodItem = Items.CARROT;
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(foodItem);
        FoodProperties foodProps = foodItem.getFoodProperties(stack, null);
        
        // Get food quality values (nutrition + saturation)
        // Defensive null check in case item doesn't have food properties
        int nutrition = foodProps != null ? foodProps.nutrition() : 1;
        float saturationModifier = foodProps != null ? foodProps.saturation() : 0.0f;
        if (foodProps == null) {
            LOGGER.warn("Food item {} has no food properties, using defaults", foodItem);
        }
        float totalQuality = nutrition + saturationModifier;
        
        // Linear formula, gives: Melon ~10, Carrot ~8, Bread ~5, Steak ~1
        int maxAmount = Math.max(1, Math.round(12 - totalQuality * 0.6f));
        
        // Random amount from 1 to maxAmount
        int amount = 1 + random.nextInt(maxAmount);
        
        // Calculate price: (nutrition + saturation) * amount
        int priceAmount = Math.max(1, (int) Math.ceil(totalQuality * amount));
        Price price = new Price(TavernKeeperMod.COPPER_COIN.get(), priceAmount);
        
        FoodRequest request = new FoodRequest(foodItem, amount, price);
        LOGGER.debug("Created food request: {} for {} (quality: {}, maxAmount: {})", 
            request.getDisplayName(), price.getDisplayName(), totalQuality, maxAmount);
        return request;
    }
    
    /**
     * Process a successful sale
     * (Future) Could track income, statistics, etc.
     */
    public void recordSale(FoodRequest request) {
        LOGGER.info("Tavern sale: {} for {}", 
            request.getDisplayName(), request.getPrice().getDisplayName());
        // TODO: Track total income, best-selling items, etc.
    }
}

