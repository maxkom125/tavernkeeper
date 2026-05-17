package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerLifecycleInitialStateTests {

    private static final String FLAT_PLATFORM = "gametest/flat_7x5x7";

    @SuppressWarnings("unchecked")
    private static CustomerEntity spawnCustomer(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(
                (EntityType<CustomerEntity>) (EntityType<?>) TavernKeeperMod.CUSTOMER.get(), pos);
    }

    @GameTest(template = FLAT_PLATFORM)
    public static void customerDiningLifecycleInitialState(GameTestHelper helper) {
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY));
        helper.assertTrue(
                customer.getCustomerState() == CustomerState.FINDING_LECTERN,
                "DINING_ONLY should start in FINDING_LECTERN, got: " + customer.getCustomerState()
        );
        helper.succeed();
    }

    @GameTest(template = FLAT_PLATFORM)
    public static void customerSleepingOnlyInitialState(GameTestHelper helper) {
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.SLEEPING_ONLY));
        helper.assertTrue(
                customer.getCustomerState() == CustomerState.FINDING_RECEPTION,
                "SLEEPING_ONLY should start in FINDING_RECEPTION, got: " + customer.getCustomerState()
        );
        helper.succeed();
    }

    @GameTest(template = FLAT_PLATFORM)
    public static void customerFullServiceInitialState(GameTestHelper helper) {
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.FULL_SERVICE));
        helper.assertTrue(
                customer.getCustomerState() == CustomerState.FINDING_LECTERN,
                "FULL_SERVICE should start in FINDING_LECTERN, got: " + customer.getCustomerState()
        );
        helper.succeed();
    }
}
