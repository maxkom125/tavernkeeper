package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * Shared fixture for GameTests. Not a test holder itself.
 *
 * ISOLATION: the Tavern is a per-level SavedData shared by every test running in the same level.
 * Any test that touches the Tavern must:
 *   1. use its own batch:  @GameTest(batch = "<unique name>", ...)  - batches run one after another,
 *      tests inside one batch run at the same time;
 *   2. call {@link #freshTavern(GameTestHelper)} first.
 *
 * All positions passed to these helpers are RELATIVE to the test structure (like GameTestHelper).
 * The structure sits one block above the structure block, so the flat templates' stone floor is at
 * y=1: stand customers and place furniture at y=2.
 */
public final class TavernTestSupport {

    public static final String FLAT_7 = "gametest/flat_7x5x7";
    public static final String FLAT_15 = "gametest/flat_15x5x15";

    private TavernTestSupport() {}

    // ========== Tavern ==========

    /**
     * Replace the level's Tavern with a brand-new one (no areas, no reservations, LEVEL_1, zero stats)
     * and remove customers left over from earlier batches.
     * The tavern is CLOSED so the spawner doesn't drop random customers into the test.
     */
    public static Tavern freshTavern(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (CustomerEntity leftover : level.getEntities(TavernKeeperMod.CUSTOMER.get(), e -> true)) {
            leftover.discard();
        }
        level.getDataStorage().set(Tavern.DATA_NAME, new Tavern());
        Tavern tavern = Tavern.get(level);
        if (tavern.isManuallyOpen()) {
            tavern.toggleOpenClosed();
        }
        return tavern;
    }

    /** Raise statistics until the tavern auto-upgrades to the given level. */
    public static void upgradeTo(Tavern tavern, TavernUpgrade target) {
        tavern.adjustReputation(Math.max(0, target.getReputationRequired() - tavern.getReputation()));
        tavern.adjustMoney((int) Math.max(0, target.getMoneyRequired() - tavern.getTotalMoneyEarned()));
        // checkAndAutoUpgrade() moves one level per statistics change
        while (tavern.getCurrentUpgrade().ordinal() < target.ordinal()) {
            tavern.adjustMoney(0);
        }
    }

    public static void diningArea(GameTestHelper helper, Tavern tavern, BlockPos min, BlockPos max) {
        tavern.createDiningArea("dining", helper.absolutePos(min), helper.absolutePos(max));
    }

    public static void serviceArea(GameTestHelper helper, Tavern tavern, BlockPos min, BlockPos max) {
        tavern.createServiceArea("service", helper.absolutePos(min), helper.absolutePos(max));
    }

    public static void sleepingArea(GameTestHelper helper, Tavern tavern, BlockPos min, BlockPos max) {
        tavern.createSleepingArea("sleeping", helper.absolutePos(min), helper.absolutePos(max));
    }

    // ========== Furniture (vanilla stairs: TOP half = table, BOTTOM half = chair) ==========

    public static void placeTable(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF, Half.TOP)
                .setValue(StairBlock.FACING, Direction.NORTH));
    }

    /**
     * Place a chair whose seat faces {@code towardTable}. A chair is only valid
     * when a table is directly in front of it and there is air above it.
     */
    public static void placeChair(GameTestHelper helper, BlockPos pos, Direction towardTable) {
        // Stairs FACING points at the backrest, so the sitter faces the opposite way
        helper.setBlock(pos, Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.HALF, Half.BOTTOM)
                .setValue(StairBlock.FACING, towardTable.getOpposite()));
    }

    /** Table at {@code tablePos} with a chair on its {@code chairSide}, facing the table. */
    public static BlockPos placeTableWithChair(GameTestHelper helper, BlockPos tablePos, Direction chairSide) {
        placeTable(helper, tablePos);
        BlockPos chairPos = tablePos.relative(chairSide);
        placeChair(helper, chairPos, chairSide.getOpposite());
        return chairPos;
    }

    /** Place a bed: FOOT at {@code footPos}, HEAD one block towards {@code headDirection}. Returns HEAD. */
    public static BlockPos placeBed(GameTestHelper helper, BlockPos footPos, Direction headDirection) {
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, headDirection);
        BlockPos headPos = footPos.relative(headDirection);
        helper.setBlock(headPos, bed.setValue(BedBlock.PART, BedPart.HEAD));
        helper.setBlock(footPos, bed.setValue(BedBlock.PART, BedPart.FOOT));
        return headPos;
    }

    // ========== Customers ==========

    @SuppressWarnings("unchecked")
    public static CustomerEntity spawnCustomer(GameTestHelper helper, BlockPos pos, LifecycleType type) {
        CustomerEntity customer = helper.spawn(
                (EntityType<CustomerEntity>) (EntityType<?>) TavernKeeperMod.CUSTOMER.get(), pos);
        customer.setLifecycle(CustomerLifecycleFactory.fromType(type));
        return customer;
    }

    /** Spawn a dining customer that has already been served and is looking for a seat. */
    public static CustomerEntity spawnSeatSeeker(GameTestHelper helper, BlockPos pos) {
        CustomerEntity customer = spawnCustomer(helper, pos, LifecycleType.DINING_ONLY);
        customer.setCustomerState(CustomerState.FINDING_SEAT);
        return customer;
    }

    public static boolean isOccupiedBy(Tavern tavern, BlockPos absChairPos, CustomerEntity customer) {
        UUID occupant = tavern.getDiningManager().getChairOccupant(absChairPos);
        return occupant != null && occupant.equals(customer.getUUID());
    }

    // ========== Navigation checks ==========

    /**
     * Records a customer's walk to a target and fails the test if the walk goes wrong.
     * Register with {@code helper.onEachTick(tracker::tick)}.
     *
     * Checks, every tick while the customer is in {@code walkingState}:
     *  - not stuck: it must move at least {@link #STUCK_MIN_MOVE} blocks in any {@link #STUCK_WINDOW} ticks
     *  - efficient: distance walked stays within {@link #DETOUR_FACTOR} x the pathfinder's own route
     *    (+ {@link #DETOUR_SLACK} blocks)
     * And remembers the last position before the state changed, so tests can assert where the
     * customer actually stood when it "arrived" (see {@link #lastWalkingPos()}).
     */
    public static final class WalkTracker {
        public static final int STUCK_WINDOW = 50;
        public static final double STUCK_MIN_MOVE = 0.3;
        public static final double DETOUR_FACTOR = 1.5;
        public static final double DETOUR_SLACK = 3.0;

        private final GameTestHelper helper;
        private final CustomerEntity customer;
        private final CustomerState walkingState;
        private final BlockPos target;
        private double routeLength = -1; // computed once the customer stands on the ground
        private final Deque<Vec3> window = new ArrayDeque<>();
        private Vec3 last;
        private double walked;
        private BlockPos lastWalkingPos;

        public WalkTracker(GameTestHelper helper, CustomerEntity customer, CustomerState walkingState, BlockPos absTarget) {
            this.helper = helper;
            this.customer = customer;
            this.walkingState = walkingState;
            this.target = absTarget;
            this.last = customer.position();
            this.lastWalkingPos = customer.blockPosition();
        }

        public void tick() {
            if (customer.isRemoved() || customer.getCustomerState() != walkingState || customer.isPassenger()) {
                return;
            }
            if (routeLength < 0) {
                // Vanilla navigation refuses to plan a path while the mob is still in the air
                if (!customer.onGround()) {
                    return;
                }
                Path path = customer.getNavigation().createPath(target, 1);
                if (path == null || !path.canReach()) {
                    helper.fail("Pathfinder found no route from " + helper.relativePos(customer.blockPosition())
                            + " to " + helper.relativePos(target));
                }
                routeLength = length(path);
                last = customer.position();
            }
            Vec3 now = customer.position();
            walked += horizontalDistance(now, last);
            last = now;
            lastWalkingPos = customer.blockPosition();

            window.addLast(now);
            if (window.size() > STUCK_WINDOW) {
                window.removeFirst();
                if (horizontalDistance(window.peekFirst(), now) < STUCK_MIN_MOVE) {
                    helper.fail("Customer stuck at " + helper.relativePos(customer.blockPosition())
                            + " for " + STUCK_WINDOW + " ticks while " + walkingState);
                }
            }

            double budget = routeLength * DETOUR_FACTOR + DETOUR_SLACK;
            if (walked > budget) {
                helper.fail(String.format("Customer detoured: walked %.1f blocks, route is %.1f (budget %.1f)",
                        walked, routeLength, budget));
            }
        }

        /** Block position the customer stood on during its last tick in {@code walkingState}. */
        public BlockPos lastWalkingPos() {
            return lastWalkingPos;
        }

        public double walked() {
            return walked;
        }

        private static double length(Path path) {
            double total = 0;
            for (int i = 1; i < path.getNodeCount(); i++) {
                total += Math.sqrt(path.getNodePos(i).distSqr(path.getNodePos(i - 1)));
            }
            return total;
        }

        private static double horizontalDistance(Vec3 a, Vec3 b) {
            double dx = a.x - b.x;
            double dz = a.z - b.z;
            return Math.sqrt(dx * dx + dz * dz);
        }
    }
}
