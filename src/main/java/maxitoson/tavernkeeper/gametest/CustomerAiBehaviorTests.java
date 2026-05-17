package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Behavioral AI tests — customers physically navigate to furniture and transition states.
 *
 * FURNITURE LIMITS (level 1): MAX_LECTERNS=1, MAX_RECEPTION_DESKS=1, maxBeds=0, maxTables=2, maxChairs=8.
 * All tests share one Tavern per ServerLevel and run concurrently — each furniture type must be
 * registered by EXACTLY ONE test:
 *   - customerMovesToLecternAndWaits  → owns the 1 lectern
 *   - customerFindsSeatAndEats        → owns 1 table + 1 chair
 *   - customerMovesToReceptionDesk    → owns the 1 reception desk
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class CustomerAiBehaviorTests {

    private static final String FLAT_PLATFORM = "gametest/flat_7x5x7";

    @SuppressWarnings("unchecked")
    private static CustomerEntity spawnCustomer(GameTestHelper helper, BlockPos pos) {
        return helper.spawn(
                (EntityType<CustomerEntity>) (EntityType<?>) TavernKeeperMod.CUSTOMER.get(), pos);
    }

    /**
     * Customer navigates to a lectern and transitions to WAITING_SERVICE.
     * Owns: the 1 allowed lectern (MAX_LECTERNS = 1 at level 1).
     */
    @GameTest(template = FLAT_PLATFORM, timeoutTicks = 200)
    public static void customerMovesToLecternAndWaits(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 1, 3), Blocks.LECTERN.defaultBlockState());
        ServerLevel level = (ServerLevel) helper.getLevel();
        Tavern tavern = Tavern.get(level);
        tavern.createServiceArea("svc",
                helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(6, 4, 6)));

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY));

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(
                    customer.getCustomerState() == CustomerState.WAITING_SERVICE,
                    "Customer should reach lectern (WAITING_SERVICE), got: " + customer.getCustomerState()
            );
            helper.succeed();
        });
    }

    /**
     * Customer navigates to a chair adjacent to a table and transitions to EATING.
     * Table at (3,1,4) upside-down stair; chair at (3,1,5) FACING=SOUTH → getFacing()=NORTH toward table.
     * Owns: 1 table + 1 chair.
     */
    @GameTest(template = FLAT_PLATFORM, timeoutTicks = 200)
    public static void customerFindsSeatAndEats(GameTestHelper helper) {
        helper.setBlock(new BlockPos(3, 1, 4),
                Blocks.OAK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.HALF, Half.TOP)
                        .setValue(StairBlock.FACING, Direction.SOUTH));
        helper.setBlock(new BlockPos(3, 1, 5),
                Blocks.OAK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.HALF, Half.BOTTOM)
                        .setValue(StairBlock.FACING, Direction.SOUTH));

        ServerLevel level = (ServerLevel) helper.getLevel();
        Tavern tavern = Tavern.get(level);
        tavern.createDiningArea("dining",
                helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(6, 4, 6)));

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY));
        customer.setCustomerState(CustomerState.FINDING_SEAT);

        helper.runAfterDelay(100, () -> {
            helper.assertTrue(
                    customer.getCustomerState() == CustomerState.EATING,
                    "Customer should find a seat and enter EATING, got: " + customer.getCustomerState()
            );
            helper.succeed();
        });
    }

    /**
     * Customer navigates to a reception desk and leaves FINDING_RECEPTION.
     * At level 1, WaitAtReceptionDesk auto-transitions to FINDING_BED (maxBeds=0, no sleeping request created).
     * Owns: the 1 allowed reception desk (MAX_RECEPTION_DESKS = 1 at level 1).
     */
    @GameTest(template = FLAT_PLATFORM, timeoutTicks = 200)
    public static void customerMovesToReceptionDesk(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 1, 3), TavernKeeperMod.RECEPTION_DESK.get().defaultBlockState());
        ServerLevel level = (ServerLevel) helper.getLevel();
        Tavern tavern = Tavern.get(level);
        tavern.createServiceArea("svc-reception",
                helper.absolutePos(new BlockPos(1, 1, 1)),
                helper.absolutePos(new BlockPos(6, 4, 6)));

        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 2, 3));
        customer.setLifecycle(CustomerLifecycleFactory.fromType(LifecycleType.SLEEPING_ONLY));

        helper.runAfterDelay(100, () -> {
            CustomerState state = customer.getCustomerState();
            helper.assertTrue(
                    state != CustomerState.FINDING_RECEPTION,
                    "Customer should have reached reception desk (left FINDING_RECEPTION), got: " + state
            );
            helper.succeed();
        });
    }
}
