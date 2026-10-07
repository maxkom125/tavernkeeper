package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.spaces.DiningSpace;
import maxitoson.tavernkeeper.tavern.spaces.ServiceSpace;
import maxitoson.tavernkeeper.tavern.spaces.SleepingSpace;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Furniture scanning: what counts as a table/chair/lectern/desk/bed, chair validity rules,
 * and upgrade-level limits. No AI involved - these finish in one tick.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class FurnitureRecognitionTests {

    private static final BlockPos MIN = new BlockPos(0, 2, 0);
    private static final BlockPos MAX_15 = new BlockPos(14, 4, 14);

    private static DiningSpace onlyDiningSpace(Tavern tavern) {
        return tavern.getDiningManager().getSpaces().iterator().next();
    }

    private static Chair chairAt(GameTestHelper helper, Tavern tavern, BlockPos rel) {
        BlockPos abs = helper.absolutePos(rel);
        return tavern.getChairAt(abs).orElseThrow(() -> new AssertionError("No chair recognized at " + rel));
    }

    @GameTest(template = FLAT_15, batch = "furniture_chair_validity")
    public static void chairIsValidOnlyWhenFacingTableWithAirAbove(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        // Only 2 tables: that's the level-1 limit, a third would be rejected and skew the result
        // facing the table -> valid
        BlockPos good = placeTableWithChair(helper, new BlockPos(2, 2, 2), Direction.SOUTH);
        // facing the table but a block on top -> invalid
        BlockPos covered = placeTableWithChair(helper, new BlockPos(10, 2, 2), Direction.SOUTH);
        helper.setBlock(covered.above(), Blocks.OAK_PLANKS);
        // right next to that table but facing away from it -> invalid
        BlockPos facingAway = new BlockPos(10, 2, 1);
        placeChair(helper, facingAway, Direction.NORTH);
        // no table at all -> invalid
        BlockPos lonely = new BlockPos(2, 2, 10);
        placeChair(helper, lonely, Direction.NORTH);
        tavern.createDiningArea("dining", helper.absolutePos(MIN), helper.absolutePos(MAX_15));

        helper.assertTrue(chairAt(helper, tavern, good).isValid(), "Chair facing a table should be valid");
        helper.assertFalse(chairAt(helper, tavern, facingAway).isValid(), "Chair facing away should be invalid");
        helper.assertFalse(chairAt(helper, tavern, covered).isValid(), "Chair with a block above should be invalid");
        helper.assertFalse(chairAt(helper, tavern, lonely).isValid(), "Chair without a table should be invalid");
        helper.assertValueEqual(onlyDiningSpace(tavern).getValidChairCount(), 1, "valid chairs");
        helper.succeed();
    }

    /** Level 1 allows 2 tables and 8 chairs in total; the rest are reported as rejected. */
    @GameTest(template = FLAT_15, batch = "furniture_dining_limits")
    public static void diningLimitsAtLevel1(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        for (int x = 2; x <= 10; x += 4) {
            placeTable(helper, new BlockPos(x, 2, 2));
        }
        for (int x = 1; x <= 9; x++) {
            placeChair(helper, new BlockPos(x, 2, 10), Direction.NORTH);
        }
        var result = (DiningSpace.ScanResult) tavern.createDiningArea("dining",
                helper.absolutePos(MIN), helper.absolutePos(MAX_15)).getScanResult();

        helper.assertValueEqual(result.getTablesFound(), TavernUpgrade.LEVEL_1.getMaxTables(), "tables found");
        helper.assertValueEqual(result.getTablesRejected(), 1, "tables rejected");
        helper.assertValueEqual(result.getChairsFound(), TavernUpgrade.LEVEL_1.getMaxChairs(), "chairs found");
        helper.assertValueEqual(result.getChairsRejected(), 1, "chairs rejected");
        helper.succeed();
    }

    /** Only one lectern and one reception desk count - across ALL service areas. */
    @GameTest(template = FLAT_15, batch = "furniture_service_limits")
    public static void oneLecternAndOneReceptionDeskPerTavern(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.LECTERN);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.LECTERN);
        helper.setBlock(new BlockPos(6, 2, 2), TavernKeeperMod.RECEPTION_DESK.get());
        helper.setBlock(new BlockPos(8, 2, 2), Blocks.BARREL);
        helper.setBlock(new BlockPos(2, 2, 12), Blocks.LECTERN);
        helper.setBlock(new BlockPos(4, 2, 12), TavernKeeperMod.RECEPTION_DESK.get());

        var first = (ServiceSpace.ScanResult) tavern.createServiceArea("a",
                helper.absolutePos(new BlockPos(0, 2, 0)), helper.absolutePos(new BlockPos(14, 4, 5))).getScanResult();
        helper.assertValueEqual(first.getLecternsFound(), 1, "lecterns in first area");
        helper.assertValueEqual(first.getLecternsRejected(), 1, "lecterns rejected in first area");
        helper.assertValueEqual(first.getReceptionDesksFound(), 1, "desks in first area");
        helper.assertValueEqual(first.getBarrelsFound(), 1, "barrels in first area");

        var second = (ServiceSpace.ScanResult) tavern.createServiceArea("b",
                helper.absolutePos(new BlockPos(0, 2, 9)), helper.absolutePos(new BlockPos(14, 4, 14))).getScanResult();
        helper.assertValueEqual(second.getLecternsFound(), 0, "lecterns in second area");
        helper.assertValueEqual(second.getLecternsRejected(), 1, "lecterns rejected in second area");
        helper.assertValueEqual(second.getReceptionDesksRejected(), 1, "desks rejected in second area");
        helper.succeed();
    }

    /** Beds: none allowed at level 1, two at level 2. */
    @GameTest(template = FLAT_15, batch = "furniture_bed_limits")
    public static void bedLimitsFollowTavernLevel(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        for (int x = 2; x <= 10; x += 4) {
            placeBed(helper, new BlockPos(x, 2, 4), Direction.NORTH);
        }
        var atLevel1 = (SleepingSpace.ScanResult) tavern.createSleepingArea("beds",
                helper.absolutePos(MIN), helper.absolutePos(MAX_15)).getScanResult();
        helper.assertValueEqual(atLevel1.getBedsFound(), 0, "beds accepted at level 1");
        helper.assertValueEqual(atLevel1.getBedsRejected(), 3, "beds rejected at level 1");

        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        tavern.scanAndRecognize();
        helper.assertValueEqual(tavern.getSleepingManager().getTotalBedCount(),
                TavernUpgrade.LEVEL_2.getMaxBeds(), "beds accepted at level 2");
        helper.succeed();
    }

    /**
     * Regression: levelling up applied the new limits but didn't rescan existing areas,
     * so beds placed at level 1 stayed rejected after reaching level 2.
     */
    @GameTest(template = FLAT_15, batch = "furniture_upgrade_rescans")
    public static void upgradeMakesAlreadyPlacedBedsUsable(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        placeBed(helper, new BlockPos(2, 2, 4), Direction.NORTH);
        placeBed(helper, new BlockPos(6, 2, 4), Direction.NORTH);
        sleepingArea(helper, tavern, MIN, MAX_15);
        helper.assertValueEqual(tavern.getSleepingManager().getTotalBedCount(), 0, "beds at level 1");

        upgradeTo(tavern, TavernUpgrade.LEVEL_2); // no manual rescan
        helper.assertValueEqual(tavern.getSleepingManager().getTotalBedCount(), 2, "beds right after level-up");
        helper.assertTrue(tavern.findNearestAvailableBed(helper.absolutePos(new BlockPos(4, 2, 8)), 48).isPresent(),
                "Customers should be able to find the beds after level-up");
        helper.succeed();
    }

    /** Breaking an accepted bed lets a previously rejected bed take its slot. */
    @GameTest(template = FLAT_15, batch = "furniture_bed_rescan")
    public static void breakingBedAdmitsPreviouslyRejectedBed(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        upgradeTo(tavern, TavernUpgrade.LEVEL_2); // max 2 beds
        BlockPos[] heads = new BlockPos[3];
        for (int i = 0; i < 3; i++) {
            heads[i] = placeBed(helper, new BlockPos(2 + 4 * i, 2, 4), Direction.NORTH);
        }
        sleepingArea(helper, tavern, MIN, MAX_15);
        SleepingSpace space = tavern.getSleepingManager().getSpaces().iterator().next();
        helper.assertValueEqual(space.getBedCount(), 2, "beds before break");

        BlockPos brokenHead = space.getBeds().get(0);
        BlockPos rel = toRelative(helper, brokenHead);
        BlockState oldState = helper.getLevel().getBlockState(brokenHead);
        // BreakEvent fires while the block is still there - mirror WorldUpdateHandler
        space.onBlockBroken(brokenHead, oldState);
        helper.setBlock(rel, Blocks.AIR);
        helper.setBlock(rel.south(), Blocks.AIR);

        helper.assertValueEqual(space.getBedCount(), 2, "beds after break");
        helper.assertFalse(space.getBeds().contains(brokenHead), "Broken bed is still registered");
        helper.succeed();
    }

    /** Areas thinner than 3 blocks are stretched so furniture + headroom fit. */
    @GameTest(template = FLAT_7, batch = "furniture_area_height")
    public static void flatAreaIsExtendedToThreeBlocks(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        BlockPos min = helper.absolutePos(new BlockPos(0, 2, 0));
        BlockPos max = helper.absolutePos(new BlockPos(6, 2, 6));
        TavernArea area = tavern.createDiningArea("flat", min, max).getArea();
        helper.assertValueEqual(area.getMaxPos().getY(), min.getY() + 2, "area max Y");
        helper.succeed();
    }
}
