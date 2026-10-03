package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.economy.CoinRegistry;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.Price;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Coins, prices and request generation. Pure logic, but coins are registered items,
 * so these run as GameTests (the game is bootstrapped) and finish in one tick.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class EconomyTests {

    @GameTest(template = FLAT_7)
    public static void copperValueBreaksDownIntoTiers(GameTestHelper helper) {
        var breakdown = CoinRegistry.getFullBreakdown(10232); // 1 gold, 2 iron, 32 copper
        helper.assertValueEqual(breakdown.getAmount(0), 32, "copper");
        helper.assertValueEqual(breakdown.getAmount(1), 2, "iron");
        helper.assertValueEqual(breakdown.getAmount(2), 1, "gold");
        helper.assertValueEqual(breakdown.getAmount(3), 0, "diamond");
        helper.assertValueEqual(breakdown.getAmount(4), 0, "netherite");

        Price price = new Price(10232);
        long total = 0;
        for (ItemStack stack : price.toItemStacks()) {
            total += CoinRegistry.toCopperValue(stack.getItem(), stack.getCount());
        }
        helper.assertValueEqual(total, 10232L, "value of paid-out coin stacks");
        ItemStack shown = price.toHighestTierStack();
        helper.assertTrue(shown.is(TavernKeeperMod.GOLD_COIN.get()) && shown.getCount() == 1,
                "Highest-tier display should be 1 gold coin, was " + shown);
        helper.succeed();
    }

    @GameTest(template = FLAT_7)
    public static void coinConversionRates(GameTestHelper helper) {
        helper.assertValueEqual(new Price(TavernKeeperMod.IRON_COIN.get(), 5).getCopperValue(), 500, "5 iron");
        helper.assertValueEqual(new Price(TavernKeeperMod.NETHERITE_COIN.get(), 1).getCopperValue(), 100_000_000, "1 netherite");
        helper.assertTrue(CoinRegistry.isCoin(TavernKeeperMod.COPPER_COIN.get()), "Copper coin is a coin");
        helper.assertFalse(CoinRegistry.isCoin(Items.GOLD_INGOT), "Gold ingot is not a coin");
        helper.succeed();
    }

    /** Wallet auto-convert: 100 of a tier become 1 of the next, cascading upwards. */
    @GameTest(template = FLAT_7)
    public static void walletAutoConvertCascades(GameTestHelper helper) {
        CompoundTag wallet = new CompoundTag();
        wallet.putLong("copper", 250);
        wallet.putLong("iron", 99);
        CoinRegistry.autoConvertTag(wallet);
        helper.assertValueEqual(wallet.getLong("copper"), 50L, "copper");
        helper.assertValueEqual(wallet.getLong("iron"), 1L, "iron");
        helper.assertValueEqual(wallet.getLong("gold"), 1L, "gold");
        helper.succeed();
    }

    /** Carrot orders: 1-8 carrots, ~6.6 copper each, and level 2 pays more for the same order. */
    @GameTest(template = FLAT_7, batch = "economy_food_prices")
    public static void foodRequestsArePricedByQualityAndLevel(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        Map<Integer, Integer> level1Prices = samplePrices(helper, tavern);
        helper.assertTrue(level1Prices.size() >= 6, "Expected most amounts 1-8 to appear, got " + level1Prices.keySet());

        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        Map<Integer, Integer> level2Prices = samplePrices(helper, tavern);
        for (var entry : level2Prices.entrySet()) {
            Integer before = level1Prices.get(entry.getKey());
            // x1.1 is truncated to whole copper, so a single carrot (7 -> 7.7) gains nothing
            boolean shouldGain = entry.getKey() >= 2;
            if (before != null && (shouldGain ? entry.getValue() <= before : entry.getValue() < before)) {
                helper.fail("Level 2 should pay more for " + entry.getKey()
                        + " carrots: " + before + " -> " + entry.getValue());
            }
        }
        helper.succeed();
    }

    private static Map<Integer, Integer> samplePrices(GameTestHelper helper, Tavern tavern) {
        Map<Integer, Integer> priceByAmount = new HashMap<>();
        for (int i = 0; i < 500; i++) {
            FoodRequest request = tavern.createFoodRequest();
            int amount = request.getRequestedAmount();
            int price = request.getPrice().getCopperValue();
            helper.assertTrue(request.getRequestedItem() == Items.CARROT, "Only carrots are on the menu");
            helper.assertTrue(amount >= 1 && amount <= 8, "Carrot amount out of range: " + amount);
            helper.assertTrue(price >= 6 * amount, "Price too low: " + price + " for " + amount);
            Integer previous = priceByAmount.put(amount, price);
            helper.assertTrue(previous == null || previous == price, "Same order, different price: " + previous + " vs " + price);
        }
        return priceByAmount;
    }

    /** Rooms cost 5-10 iron and are only offered from level 2. */
    @GameTest(template = FLAT_7, batch = "economy_room_prices")
    public static void sleepingRequestsNeedLevel2(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        helper.assertTrue(tavern.createSleepingRequest() == null, "No rooms at level 1");

        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        for (int i = 0; i < 200; i++) {
            SleepingRequest request = tavern.createSleepingRequest();
            int copper = request.getPrice().getCopperValue();
            helper.assertTrue(copper >= 500 && copper <= 1000 && copper % 100 == 0,
                    "Room price should be 5-10 whole iron coins, was " + copper + " copper");
        }
        helper.succeed();
    }
}
