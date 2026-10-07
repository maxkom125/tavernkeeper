package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.managers.domain.DiningManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Several customers competing for chairs: one chair = one customer, chairs are released after eating,
 * and chairs that aren't usable are never handed out.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class ChairReservationTests {

    private static final BlockPos AREA_MIN = new BlockPos(0, 2, 0);
    private static final BlockPos AREA_MAX = new BlockPos(14, 4, 14);

    @GameTest(template = FLAT_15, batch = "seat_two_customers", timeoutTicks = 400)
    public static void twoCustomersTakeDifferentChairs(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chairA = helper.absolutePos(placeTableWithChair(helper, new BlockPos(11, 2, 4), Direction.WEST));
        BlockPos chairB = helper.absolutePos(placeTableWithChair(helper, new BlockPos(11, 2, 10), Direction.WEST));
        diningArea(helper, tavern, AREA_MIN, AREA_MAX);

        CustomerEntity first = spawnSeatSeeker(helper, new BlockPos(2, 2, 6));
        CustomerEntity second = spawnSeatSeeker(helper, new BlockPos(2, 2, 8));

        helper.succeedWhen(() -> {
            helper.assertTrue(first.getCustomerState() == CustomerState.EATING, "First customer not eating");
            helper.assertTrue(second.getCustomerState() == CustomerState.EATING, "Second customer not eating");
            BlockPos seatA = seatOf(helper, first);
            BlockPos seatB = seatOf(helper, second);
            helper.assertFalse(seatA.equals(seatB), "Both customers sat on the same chair " + seatA);
            helper.assertTrue((seatA.equals(chairA) && seatB.equals(chairB)) || (seatA.equals(chairB) && seatB.equals(chairA)),
                    "Customers should sit on the two chairs, got " + seatA + " and " + seatB);
            helper.assertTrue(isOccupiedBy(tavern, seatA, first) && isOccupiedBy(tavern, seatB, second),
                    "Each chair must be reserved by the customer sitting on it");
        });
    }

    @GameTest(template = FLAT_15, batch = "seat_one_chair_two_customers", timeoutTicks = 400)
    public static void extraCustomerWaitsWhenAllChairsTaken(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chair = helper.absolutePos(placeTableWithChair(helper, new BlockPos(11, 2, 7), Direction.WEST));
        diningArea(helper, tavern, AREA_MIN, AREA_MAX);

        CustomerEntity first = spawnSeatSeeker(helper, new BlockPos(2, 2, 5));
        CustomerEntity second = spawnSeatSeeker(helper, new BlockPos(2, 2, 9));

        helper.onEachTick(() -> {
            if (first.isSitting() && second.isSitting()) {
                helper.fail("Two customers are sitting although there is only one chair");
            }
        });
        // Eating takes 200 ticks, so at tick 180 nobody has freed the chair yet
        helper.runAtTickTime(180, () -> {
            boolean firstEats = first.getCustomerState() == CustomerState.EATING;
            boolean secondEats = second.getCustomerState() == CustomerState.EATING;
            helper.assertTrue(firstEats ^ secondEats, "Exactly one customer should be eating, states: "
                    + first.getCustomerState() + " / " + second.getCustomerState());
            CustomerEntity waiting = firstEats ? second : first;
            CustomerEntity eating = firstEats ? first : second;
            helper.assertTrue(waiting.getCustomerState() == CustomerState.FINDING_SEAT,
                    "Customer without a chair should keep looking, got: " + waiting.getCustomerState());
            helper.assertTrue(isOccupiedBy(tavern, chair, eating), "Chair should belong to the eating customer");
            helper.succeed();
        });
    }

    /** After the 200-tick meal the chair is free again and the customer moves on to LEAVING. */
    @GameTest(template = FLAT_7, batch = "seat_released_after_meal", timeoutTicks = 400)
    public static void chairIsReleasedAfterEating(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chair = helper.absolutePos(placeTableWithChair(helper, new BlockPos(3, 2, 3), Direction.EAST));
        diningArea(helper, tavern, new BlockPos(0, 2, 0), new BlockPos(6, 4, 6));
        DiningManager dining = tavern.getDiningManager();

        CustomerEntity customer = spawnSeatSeeker(helper, new BlockPos(1, 2, 1));
        boolean[] ate = {false};
        helper.onEachTick(() -> {
            if (customer.getCustomerState() == CustomerState.EATING) {
                ate[0] = true;
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(ate[0], "Customer never started eating");
            helper.assertTrue(customer.isRemoved() || customer.getCustomerState() == CustomerState.LEAVING,
                    "Dining-only customer should leave after eating, got: " + customer.getCustomerState());
            helper.assertTrue(dining.isChairAvailable(chair), "Chair still reserved after the customer finished");
            helper.assertFalse(customer.isSitting(), "Customer still sitting after the meal");
        });
    }

    /** A chair that doesn't face a table is not a seat: nobody may sit on it. */
    @GameTest(template = FLAT_7, batch = "seat_invalid_chair", timeoutTicks = 200)
    public static void chairNotFacingTableIsNeverUsed(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        placeTable(helper, new BlockPos(3, 2, 3));
        BlockPos chair = new BlockPos(4, 2, 3);
        placeChair(helper, chair, Direction.EAST); // east side of the table, but facing away from it
        diningArea(helper, tavern, new BlockPos(0, 2, 0), new BlockPos(6, 4, 6));

        CustomerEntity customer = spawnSeatSeeker(helper, new BlockPos(1, 2, 1));
        helper.runAtTickTime(150, () -> {
            helper.assertTrue(customer.getCustomerState() == CustomerState.FINDING_SEAT,
                    "Customer should keep looking, got: " + customer.getCustomerState());
            helper.assertFalse(customer.isSitting(), "Customer sat on an invalid chair");
            helper.assertTrue(tavern.getDiningManager().isChairAvailable(helper.absolutePos(chair)),
                    "Invalid chair must never be reserved");
            helper.succeed();
        });
    }
}
