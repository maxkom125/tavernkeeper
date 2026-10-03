package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.behavior.FindSeat;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Consumer;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Real-movement tests: a seat-seeking customer must physically walk to its chair -
 * no getting stuck on furniture, no big detours, and its chair reservation must hold for the whole walk.
 * Once within {@link FindSeat#REACHED_DISTANCE} the customer is placed onto the seat (by design,
 * chairs are often hard to path onto), so "arrived" means "walked into that radius".
 *
 * Layout: 15x15 stone floor (y=0), customers walk at y=1, dining area covers the whole floor.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerNavigationTests {

    /** Open floor: straight walk across the room. */
    @GameTest(template = FLAT_15, batch = "nav_open_floor", timeoutTicks = 400)
    public static void customerWalksStraightToChair(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chair = placeTableWithChair(helper, new BlockPos(13, 2, 7), Direction.WEST);
        runSeatingScenario(helper, tavern, chair, new BlockPos(1, 2, 7));
    }

    /**
     * The table stands between the customer and the chair, extended into a counter so that no spot
     * inside the sit radius is reachable from the customer's side: the customer has to walk AROUND
     * the counter and sit down from the chair's side.
     */
    @GameTest(template = FLAT_15, batch = "nav_table_between", timeoutTicks = 400)
    public static void customerWalksAroundTableToChair(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        // Table at x=7, chair on its east side (x=8); customer starts west of the table
        BlockPos chair = placeTableWithChair(helper, new BlockPos(7, 2, 7), Direction.EAST);
        // Counter: fences continue the table along x=7, so the chair side is only reachable around its ends
        for (int z = 3; z <= 11; z++) {
            if (z != 7) {
                helper.setBlock(new BlockPos(7, 2, z), Blocks.OAK_FENCE);
            }
        }
        // (6,2,7) is the only near-side spot within the sit radius. Block it with a solid 2-high wall:
        // a lone fence post is thin enough to stand next to inside that block, and a 1-high block
        // would let the customer climb onto the table from the near side.
        helper.setBlock(new BlockPos(6, 2, 7), Blocks.STONE_BRICKS);
        helper.setBlock(new BlockPos(6, 3, 7), Blocks.STONE_BRICKS);
        runSeatingScenario(helper, tavern, chair, new BlockPos(2, 2, 7),
                arrival -> helper.assertTrue(arrival.getX() >= 8,
                        "Customer sat down from " + arrival + ", on the near side of the table"));
    }

    /** A row of fences (can't be jumped) blocks the direct line; the customer goes around its end. */
    @GameTest(template = FLAT_15, batch = "nav_fence_row", timeoutTicks = 500)
    public static void customerWalksAroundFenceRowToChair(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        for (int z = 3; z <= 11; z++) {
            helper.setBlock(new BlockPos(7, 2, z), Blocks.OAK_FENCE);
        }
        BlockPos chair = placeTableWithChair(helper, new BlockPos(13, 2, 7), Direction.WEST);
        runSeatingScenario(helper, tavern, chair, new BlockPos(2, 2, 7));
    }

    /** A 2-high wall splits the room with a single gap at the far edge: long detour, long walk. */
    @GameTest(template = FLAT_15, batch = "nav_wall_gap", timeoutTicks = 700)
    public static void customerFindsGapInWallToReachChair(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        for (int z = 0; z <= 12; z++) {
            helper.setBlock(new BlockPos(7, 2, z), Blocks.STONE_BRICKS);
            helper.setBlock(new BlockPos(7, 3, z), Blocks.STONE_BRICKS);
        }
        BlockPos chair = placeTableWithChair(helper, new BlockPos(13, 2, 2), Direction.WEST);
        runSeatingScenario(helper, tavern, chair, new BlockPos(2, 2, 2));
    }

    /**
     * Common checks for "customer walks to a chair and sits":
     *  - not stuck / no big detour (WalkTracker)
     *  - never walks towards the chair without holding its reservation (regression: the move behavior
     *    used to time out after 40 ticks and release the chair while the customer kept walking)
     *  - walked into the sit radius itself (wasn't placed onto the chair from further away)
     *  - ends up EATING, riding a seat on that exact chair
     */
    private static void runSeatingScenario(GameTestHelper helper, Tavern tavern, BlockPos relChair, BlockPos relSpawn) {
        runSeatingScenario(helper, tavern, relChair, relSpawn, arrival -> {});
    }

    /** @param arrivalCheck extra check (call helper.fail / assert) on the RELATIVE position the customer last walked on before sitting */
    private static void runSeatingScenario(GameTestHelper helper, Tavern tavern, BlockPos relChair, BlockPos relSpawn,
                                           Consumer<BlockPos> arrivalCheck) {
        diningArea(helper, tavern, new BlockPos(0, 2, 0), new BlockPos(14, 4, 14));
        BlockPos chair = helper.absolutePos(relChair);
        helper.assertTrue(tavern.getDiningManager().getSpaces().iterator().next().getValidChairCount() == 1,
                "Expected exactly one valid chair in the layout");
        CustomerEntity customer = spawnSeatSeeker(helper, relSpawn);

        WalkTracker walk = new WalkTracker(helper, customer, CustomerState.FINDING_SEAT, chair);
        boolean[] arrivalChecked = {false};
        helper.onEachTick(() -> {
            walk.tick();
            if (!arrivalChecked[0] && customer.getCustomerState() == CustomerState.EATING) {
                // Checked once, right when the customer sits down, so a wrong arrival fails with its
                // real reason instead of being masked by a later "not eating yet" timeout message
                arrivalChecked[0] = true;
                BlockPos arrival = helper.relativePos(walk.lastWalkingPos());
                int reach = FindSeat.REACHED_DISTANCE;
                if (walk.lastWalkingPos().distSqr(chair) > reach * reach) {
                    helper.fail("Customer sat down from " + arrival + ", outside the " + reach + "-block sit radius");
                }
                arrivalCheck.accept(arrival);
            }
            // A customer may drop a reservation when a walk attempt fails and re-reserve later;
            // what must never happen is walking towards a chair it doesn't hold.
            if (customer.getCustomerState() == CustomerState.FINDING_SEAT && chair.equals(walkTargetOf(customer))
                    && !isOccupiedBy(tavern, chair, customer)) {
                helper.fail("Customer walks to a chair it hasn't reserved (tick " + helper.getTick()
                        + ", at " + helper.relativePos(customer.blockPosition()) + ")");
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(customer.getCustomerState() == CustomerState.EATING,
                    "Customer not eating yet, state: " + customer.getCustomerState());
            BlockPos seat = seatOf(helper, customer);
            helper.assertTrue(chair.equals(seat), "Customer sat on " + seat + " instead of " + chair);
            helper.assertTrue(isOccupiedBy(tavern, chair, customer), "Seated customer doesn't hold the chair");
            helper.assertTrue(arrivalChecked[0], "Arrival was never checked");
        });
    }
}
