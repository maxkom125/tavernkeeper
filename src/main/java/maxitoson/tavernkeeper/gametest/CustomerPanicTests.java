package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Panic: getting hurt switches the brain to PANIC, and calming down resumes the journey
 * from a sensible state (re-queue / re-seat / re-bed instead of continuing a half-finished step).
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerPanicTests {

    @GameTest(template = FLAT_7)
    public static void stateAfterPanicRestartsTheInterruptedStep(GameTestHelper helper) {
        Map<CustomerState, CustomerState> expected = Map.of(
                CustomerState.FINDING_LECTERN, CustomerState.FINDING_LECTERN,
                CustomerState.WAITING_SERVICE, CustomerState.FINDING_LECTERN,
                CustomerState.FINDING_SEAT, CustomerState.FINDING_SEAT,
                CustomerState.EATING, CustomerState.FINDING_SEAT,
                CustomerState.FINDING_RECEPTION, CustomerState.FINDING_RECEPTION,
                CustomerState.WAITING_RECEPTION, CustomerState.FINDING_RECEPTION,
                CustomerState.FINDING_BED, CustomerState.FINDING_BED,
                CustomerState.SLEEPING, CustomerState.FINDING_BED,
                CustomerState.LEAVING, CustomerState.LEAVING);
        helper.assertValueEqual(expected.size(), CustomerState.values().length, "every state covered");

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 1, 3), LifecycleType.FULL_SERVICE);
        expected.forEach((before, after) -> {
            customer.setCustomerState(before);
            customer.saveStateBeforePanic();
            helper.assertTrue(customer.getStateAfterPanic() == after,
                    "After panicking in " + before + " expected " + after + ", got " + customer.getStateAfterPanic());
        });
        helper.succeed();
    }

    @GameTest(template = FLAT_7, batch = "panic_hurt", timeoutTicks = 100)
    public static void hurtCustomerPanicsAndRemembersWhatItWasDoing(GameTestHelper helper) {
        freshTavern(helper);
        CustomerEntity customer = spawnSeatSeeker(helper, new BlockPos(3, 1, 3));
        customer.hurt(helper.getLevel().damageSources().generic(), 1.0F);

        helper.succeedWhen(() -> {
            helper.assertTrue(customer.isCurrentlyPanicking(), "Hurt customer should panic");
            helper.assertTrue(customer.getStateBeforePanic() == CustomerState.FINDING_SEAT,
                    "Should remember FINDING_SEAT, got " + customer.getStateBeforePanic());
        });
    }
}
