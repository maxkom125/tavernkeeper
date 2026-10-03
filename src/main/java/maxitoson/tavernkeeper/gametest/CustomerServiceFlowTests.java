package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.economy.CoinRegistry;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import maxitoson.tavernkeeper.tavern.managers.domain.CustomerManager.ServiceResult;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * End-to-end journeys through the real game loop: customers walk, a (mock) player serves them,
 * payment flows through CustomerPaymentEvent into statistics and the player's inventory.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerServiceFlowTests {

    private static final BlockPos LECTERN = new BlockPos(7, 1, 12);

    /** Builds lectern + service area (south side) and one table/chair in a dining area (north side). */
    private static BlockPos buildDiningTavern(GameTestHelper helper, Tavern tavern) {
        helper.setBlock(LECTERN, Blocks.LECTERN);
        serviceArea(helper, tavern, new BlockPos(5, 1, 10), new BlockPos(9, 3, 14));
        BlockPos chair = placeTableWithChair(helper, new BlockPos(7, 1, 3), Direction.SOUTH);
        diningArea(helper, tavern, new BlockPos(0, 1, 0), new BlockPos(14, 3, 8));
        return helper.absolutePos(chair);
    }

    private static Player playerHolding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** Copper value of all coins in the player's inventory. */
    private static long coinValue(Player player) {
        long total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (CoinRegistry.isCoin(stack.getItem())) {
                total += CoinRegistry.toCopperValue(stack.getItem(), stack.getCount());
            }
        }
        return total;
    }

    /** Lectern -> served -> chair -> eat -> leave and despawn. */
    @GameTest(template = FLAT_15, batch = "flow_dining", timeoutTicks = 1000)
    public static void diningCustomerFullJourney(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chair = buildDiningTavern(helper, tavern);
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(2, 1, 12), LifecycleType.DINING_ONLY);
        Player player = playerHolding(helper, new ItemStack(Items.CARROT, 64));
        FoodRequest[] order = {null};
        boolean[] ate = {false};
        helper.onEachTick(() -> ate[0] |= customer.getCustomerState() == CustomerState.EATING);

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(customer.getCustomerState() == CustomerState.WAITING_SERVICE,
                            "Customer not waiting at lectern yet: " + customer.getCustomerState());
                    helper.assertTrue(customer.getFoodRequest() != null, "Customer has no food request yet");
                })
                .thenExecute(() -> {
                    order[0] = customer.getFoodRequest();
                    helper.assertTrue(order[0].getRequestedItem() == Items.CARROT, "Unexpected dish " + order[0].getDisplayName());
                    ServiceResult result = tavern.handlePlayerServe(player, customer, player.getMainHandItem());
                    helper.assertTrue(result.isSuccess(), "Serving the requested food should succeed");

                    int price = order[0].getPrice().getCopperValue();
                    helper.assertValueEqual(player.getMainHandItem().getCount(), 64 - order[0].getRequestedAmount(), "carrots left");
                    helper.assertValueEqual(coinValue(player), (long) price, "coins received");
                    helper.assertValueEqual(tavern.getTotalMoneyEarned(), (long) price, "money earned");
                    helper.assertValueEqual(tavern.getTotalCustomersServed(), 1, "customers served");
                    helper.assertValueEqual(tavern.getReputation(), 1, "reputation");
                    helper.assertTrue(customer.getCustomerState() == CustomerState.FINDING_SEAT,
                            "Served customer should look for a seat, got " + customer.getCustomerState());
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(ate[0], "Customer never ate");
                    helper.assertTrue(customer.isRemoved(), "Customer should despawn after leaving, state: "
                            + customer.getCustomerState());
                    helper.assertTrue(tavern.getDiningManager().isChairAvailable(chair), "Chair not released");
                })
                .thenSucceed();
    }

    /** Wrong food: customer complains, nothing is consumed or paid, customer keeps waiting. */
    @GameTest(template = FLAT_15, batch = "flow_wrong_food", timeoutTicks = 400)
    public static void wrongFoodIsRejected(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        buildDiningTavern(helper, tavern);
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(2, 1, 12), LifecycleType.DINING_ONLY);
        Player player = playerHolding(helper, new ItemStack(Items.BREAD, 64));

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(customer.getFoodRequest() != null, "No food request yet"))
                .thenExecute(() -> {
                    ServiceResult result = tavern.handlePlayerServe(player, customer, player.getMainHandItem());
                    helper.assertFalse(result.isSuccess(), "Bread must not satisfy a carrot order");
                    helper.assertTrue(result.shouldShowFeedback(), "Player should be told what the customer wants");
                    helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "bread left");
                    helper.assertValueEqual(coinValue(player), 0L, "coins received");
                    helper.assertValueEqual(tavern.getTotalCustomersServed(), 0, "customers served");
                })
                .thenIdle(20)
                .thenExecute(() -> helper.assertTrue(customer.getCustomerState() == CustomerState.WAITING_SERVICE,
                        "Customer should still be waiting, got " + customer.getCustomerState()))
                .thenSucceed();
    }

    /** Right-clicking a customer that isn't waiting for service does nothing (other interactions still work). */
    @GameTest(template = FLAT_7, batch = "flow_not_waiting")
    public static void customerNotWaitingIgnoresService(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 1, 3), LifecycleType.DINING_ONLY);
        Player player = playerHolding(helper, new ItemStack(Items.CARROT, 64));

        ServiceResult result = tavern.handlePlayerServe(player, customer, player.getMainHandItem());
        helper.assertFalse(result.shouldShowFeedback(), "FINDING_LECTERN customer must ignore service");
        helper.assertValueEqual(player.getMainHandItem().getCount(), 64, "carrots left");
        helper.assertTrue(customer.getCustomerState() == CustomerState.FINDING_LECTERN, "State must not change");
        helper.succeed();
    }

    /** Level 2: reception desk -> pays for a room -> walks to the bed -> sleeps in it at night. */
    @GameTest(template = FLAT_15, batch = "flow_sleeping", timeoutTicks = 800)
    public static void sleepingCustomerFullJourney(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        helper.getLevel().setDayTime(13000); // night: sleepers only wake up in the morning

        helper.setBlock(new BlockPos(12, 1, 12), TavernKeeperMod.RECEPTION_DESK.get());
        serviceArea(helper, tavern, new BlockPos(10, 1, 10), new BlockPos(14, 3, 14));
        BlockPos bed = helper.absolutePos(placeBed(helper, new BlockPos(12, 1, 3), Direction.NORTH));
        sleepingArea(helper, tavern, new BlockPos(9, 1, 0), new BlockPos(14, 3, 6));
        helper.assertValueEqual(tavern.getSleepingManager().getTotalBedCount(), 1, "beds recognized");

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(2, 1, 7), LifecycleType.SLEEPING_ONLY);
        Player player = playerHolding(helper, ItemStack.EMPTY);
        long moneyBefore = tavern.getTotalMoneyEarned();

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(customer.getCustomerState() == CustomerState.WAITING_RECEPTION,
                            "Customer not waiting at reception yet: " + customer.getCustomerState());
                    helper.assertTrue(customer.getSleepingRequest() != null, "No sleeping request yet");
                })
                .thenExecute(() -> {
                    SleepingRequest request = customer.getSleepingRequest();
                    int price = request.getPrice().getCopperValue();
                    helper.assertTrue(price >= 500 && price <= 1000, "Room should cost 5-10 iron coins, was " + price);
                    ServiceResult result = tavern.handlePlayerServe(player, customer, player.getMainHandItem());
                    helper.assertTrue(result.isSuccess(), "Accepting payment for a room should succeed");
                    helper.assertValueEqual(coinValue(player), (long) price, "coins received");
                    helper.assertValueEqual(tavern.getTotalMoneyEarned(), moneyBefore + price, "money earned");
                    helper.assertTrue(customer.getCustomerState() == CustomerState.FINDING_BED,
                            "Paid customer should look for a bed, got " + customer.getCustomerState());
                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(customer.getCustomerState() == CustomerState.SLEEPING,
                            "Customer not sleeping yet: " + customer.getCustomerState());
                    helper.assertTrue(customer.isSleeping(), "SLEEPING customer is not lying in bed");
                    UUID occupant = tavern.getSleepingManager().getBedOccupant(bed);
                    helper.assertTrue(customer.getUUID().equals(occupant), "Bed not reserved by its sleeper");
                })
                .thenSucceed();
    }
}
