package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Behavioral AI tests - customers physically navigate to furniture and transition states.
 * Every test owns its batch and starts from {@link TavernTestSupport#freshTavern}, so furniture limits
 * (1 lectern, 1 reception desk at level 1) never leak between tests.
 * Spawn points are far enough away that each customer has to actually walk (reach radius is 1-2 blocks).
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerAiBehaviorTests {

    /** Customer walks to the lectern, then waits there with a food request. */
    @GameTest(template = FLAT_15, batch = "ai_lectern", timeoutTicks = 300)
    public static void customerMovesToLecternAndWaits(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos lectern = new BlockPos(12, 2, 7);
        helper.setBlock(lectern, Blocks.LECTERN);
        serviceArea(helper, tavern, new BlockPos(9, 2, 4), new BlockPos(14, 4, 10));

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(2, 2, 7), LifecycleType.DINING_ONLY);
        WalkTracker walk = new WalkTracker(helper, customer, CustomerState.FINDING_LECTERN, helper.absolutePos(lectern));
        helper.onEachTick(walk::tick);

        helper.succeedWhen(() -> {
            helper.assertTrue(customer.getCustomerState() == CustomerState.WAITING_SERVICE,
                    "Customer should reach lectern (WAITING_SERVICE), got: " + customer.getCustomerState());
            helper.assertTrue(customer.getFoodRequest() != null, "Waiting customer has no food request");
            helper.assertTrue(customer.blockPosition().closerThan(helper.absolutePos(lectern), 2.5),
                    "Customer is waiting too far from the lectern: " + toRelative(helper, customer.blockPosition()));
        });
    }

    /** Served customer walks to the only valid chair and starts eating. */
    @GameTest(template = FLAT_15, batch = "ai_seat", timeoutTicks = 300)
    public static void customerFindsSeatAndEats(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos chair = placeTableWithChair(helper, new BlockPos(11, 2, 7), Direction.SOUTH);
        diningArea(helper, tavern, new BlockPos(0, 2, 0), new BlockPos(14, 4, 14));

        CustomerEntity customer = spawnSeatSeeker(helper, new BlockPos(2, 2, 7));

        helper.succeedWhen(() -> {
            helper.assertTrue(customer.getCustomerState() == CustomerState.EATING,
                    "Customer should find a seat and enter EATING, got: " + customer.getCustomerState());
            helper.assertTrue(customer.isSitting(), "Eating customer should be sitting");
            helper.assertTrue(isOccupiedBy(tavern, helper.absolutePos(chair), customer),
                    "Eating customer should hold its chair reservation");
        });
    }

    /**
     * Sleeping customer walks to the reception desk. At level 1 sleeping is not offered
     * (no sleeping request), so the customer gives up and leaves.
     */
    @GameTest(template = FLAT_15, batch = "ai_reception", timeoutTicks = 300)
    public static void customerMovesToReceptionDeskAndLeavesAtLevel1(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos desk = new BlockPos(12, 2, 7);
        helper.setBlock(desk, TavernKeeperMod.RECEPTION_DESK.get());
        serviceArea(helper, tavern, new BlockPos(9, 2, 4), new BlockPos(14, 4, 10));

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(2, 2, 7), LifecycleType.SLEEPING_ONLY);
        boolean[] reachedDesk = {false};
        helper.onEachTick(() -> {
            if (customer.getCustomerState() != CustomerState.FINDING_RECEPTION) {
                reachedDesk[0] = true;
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(reachedDesk[0], "Customer never left FINDING_RECEPTION");
            helper.assertTrue(customer.getRequest() == null, "No sleeping request should exist at level 1");
            helper.assertTrue(customer.isRemoved() || customer.getCustomerState() == CustomerState.LEAVING,
                    "Level-1 sleeping customer should leave, got: " + customer.getCustomerState());
        });
    }

    /** With no lectern anywhere, a dining customer keeps looking instead of inventing a target. */
    @GameTest(template = FLAT_7, batch = "ai_no_lectern", timeoutTicks = 100)
    public static void customerWithoutLecternKeepsSearching(GameTestHelper helper) {
        freshTavern(helper);
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3), LifecycleType.DINING_ONLY);

        helper.runAfterDelay(80, () -> {
            helper.assertTrue(customer.getCustomerState() == CustomerState.FINDING_LECTERN,
                    "Customer should still be FINDING_LECTERN, got: " + customer.getCustomerState());
            FoodRequest request = customer.getFoodRequest();
            helper.assertTrue(request == null, "Customer got a food request without a lectern");
            helper.succeed();
        });
    }
}
