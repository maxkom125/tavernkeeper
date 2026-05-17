package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerStateTransitionTests {

    private static final String FLAT_PLATFORM = "gametest/flat_7x5x7";

    @SuppressWarnings("unchecked")
    private static CustomerEntity spawnCustomer(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(
                (EntityType<CustomerEntity>) (EntityType<?>) TavernKeeperMod.CUSTOMER.get(), pos);
    }

    /** DINING_ONLY: FINDING_LECTERN→WAITING_SERVICE→FINDING_SEAT→EATING→LEAVING */
    @GameTest(template = FLAT_PLATFORM)
    public static void customerDiningAllTransitions(GameTestHelper helper) {
        ServerLevel level = (ServerLevel) helper.getLevel();
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY));

        CustomerState[] expected = {
                CustomerState.WAITING_SERVICE,
                CustomerState.FINDING_SEAT,
                CustomerState.EATING,
                CustomerState.LEAVING
        };
        for (CustomerState expectedState : expected) {
            customer.transitionToNextState(level);
            helper.assertTrue(customer.getCustomerState() == expectedState,
                    "Expected " + expectedState + ", got: " + customer.getCustomerState());
        }
        helper.succeed();
    }

    /** SLEEPING_ONLY: FINDING_RECEPTION→WAITING_RECEPTION→FINDING_BED→SLEEPING→LEAVING */
    @GameTest(template = FLAT_PLATFORM)
    public static void customerSleepingOnlyAllTransitions(GameTestHelper helper) {
        ServerLevel level = (ServerLevel) helper.getLevel();
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.SLEEPING_ONLY));

        CustomerState[] expected = {
                CustomerState.WAITING_RECEPTION,
                CustomerState.FINDING_BED,
                CustomerState.SLEEPING,
                CustomerState.LEAVING
        };
        for (CustomerState expectedState : expected) {
            customer.transitionToNextState(level);
            helper.assertTrue(customer.getCustomerState() == expectedState,
                    "Expected " + expectedState + ", got: " + customer.getCustomerState());
        }
        helper.succeed();
    }

    /** FULL_SERVICE: 8 transitions from FINDING_LECTERN through sleeping to LEAVING */
    @GameTest(template = FLAT_PLATFORM)
    public static void customerFullServiceAllTransitions(GameTestHelper helper) {
        ServerLevel level = (ServerLevel) helper.getLevel();
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.FULL_SERVICE));

        CustomerState[] expected = {
                CustomerState.WAITING_SERVICE,
                CustomerState.FINDING_SEAT,
                CustomerState.EATING,
                CustomerState.FINDING_RECEPTION,
                CustomerState.WAITING_RECEPTION,
                CustomerState.FINDING_BED,
                CustomerState.SLEEPING,
                CustomerState.LEAVING
        };
        for (CustomerState expectedState : expected) {
            customer.transitionToNextState(level);
            helper.assertTrue(customer.getCustomerState() == expectedState,
                    "Expected " + expectedState + ", got: " + customer.getCustomerState());
        }
        helper.succeed();
    }
}
