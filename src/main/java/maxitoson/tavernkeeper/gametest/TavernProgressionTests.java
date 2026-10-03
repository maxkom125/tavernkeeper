package maxitoson.tavernkeeper.gametest;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.EnumMap;
import java.util.Map;

import static maxitoson.tavernkeeper.gametest.TavernTestSupport.*;

/**
 * Statistics, level-ups, ownership and save/load of the Tavern.
 */
@GameTestHolder(TavernKeeperMod.MODID)
@PrefixGameTestTemplate(false)
public class TavernProgressionTests {

    @GameTest(template = FLAT_7, batch = "progress_sale")
    public static void saleUpdatesStatistics(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        tavern.recordSale(50);
        tavern.recordSale(7);
        helper.assertValueEqual(tavern.getTotalMoneyEarned(), 57L, "money earned");
        helper.assertValueEqual(tavern.getTotalCustomersServed(), 2, "customers served");
        helper.assertValueEqual(tavern.getReputation(), 2, "reputation (+1 per customer)");
        helper.succeed();
    }

    /** Level 2 needs BOTH 100 reputation and 500 copper; its benefits reach every manager. */
    @GameTest(template = FLAT_7, batch = "progress_level_up")
    public static void levelUpRequiresBothThresholdsAndAppliesBenefits(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        TavernUpgrade level2 = TavernUpgrade.LEVEL_2;

        tavern.adjustMoney(level2.getMoneyRequired());
        tavern.adjustReputation(level2.getReputationRequired() - 1);
        helper.assertTrue(tavern.getCurrentUpgrade() == TavernUpgrade.LEVEL_1, "Upgraded with 1 reputation missing");

        tavern.adjustReputation(1);
        helper.assertTrue(tavern.getCurrentUpgrade() == level2, "Should be level 2, is " + tavern.getCurrentUpgrade());
        helper.assertValueEqual(tavern.getDiningManager().getMaxTables(), level2.getMaxTables(), "max tables");
        helper.assertValueEqual(tavern.getDiningManager().getMaxChairs(), level2.getMaxChairs(), "max chairs");
        helper.assertValueEqual(tavern.getSleepingManager().getMaxBeds(), level2.getMaxBeds(), "max beds");
        helper.assertValueEqual(tavern.getEconomyManager().getPaymentMultiplierValue(), level2.getPaymentMultiplier(), "payment multiplier");
        helper.assertValueEqual(tavern.getCustomerManager().getSpawnRateMultiplier(), level2.getSpawnRateMultiplier(), "spawn multiplier");
        helper.succeed();
    }

    /** One statistics change only moves one level, but the next change continues to the top. */
    @GameTest(template = FLAT_7, batch = "progress_max_level")
    public static void levelsClimbToMaxAndStop(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        upgradeTo(tavern, TavernUpgrade.LEVEL_3);
        tavern.adjustReputation(100_000);
        tavern.adjustMoney(1_000_000);
        helper.assertTrue(tavern.getCurrentUpgrade() == TavernUpgrade.LEVEL_3, "Should stay at max level");
        helper.assertTrue(TavernUpgrade.LEVEL_3.getNextLevel() == null, "LEVEL_3 should be the last level");
        helper.succeed();
    }

    @GameTest(template = FLAT_7, batch = "progress_customer_death")
    public static void customerDeathCostsReputation(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        tavern.adjustReputation(30);
        CustomerEntity customer = spawnCustomer(helper, new BlockPos(3, 1, 3), LifecycleType.DINING_ONLY);
        customer.kill();
        helper.assertValueEqual(tavern.getReputation(), 10, "reputation after a customer died (-20)");
        helper.succeed();
    }

    /** First area claims ownership; areas are auto-numbered; deleting the last area releases ownership. */
    @GameTest(template = FLAT_15, batch = "progress_ownership")
    public static void ownershipAndAreaNumbering(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        Player alice = helper.makeMockPlayer(GameType.SURVIVAL);
        Player bob = helper.makeMockPlayer(GameType.SURVIVAL);

        var first = tavern.createArea(AreaType.DINING,
                helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(4, 3, 4)), alice);
        var second = tavern.createArea(AreaType.DINING,
                helper.absolutePos(new BlockPos(6, 1, 0)), helper.absolutePos(new BlockPos(10, 3, 4)), bob);
        helper.assertTrue(first.becameOwner(), "First area creator should become owner");
        helper.assertFalse(second.becameOwner(), "Second creator must not take over");
        helper.assertTrue(tavern.isOwner(alice.getUUID()), "Owner should be the first creator");
        helper.assertValueEqual(first.getCreatedArea().getName(), "#1", "first area name");
        helper.assertValueEqual(second.getCreatedArea().getName(), "#2", "second area name");

        tavern.deleteArea(second.getCreatedArea().getId());
        var lastDeletion = tavern.deleteArea(first.getCreatedArea().getId());
        helper.assertTrue(lastDeletion.wasOwnershipLost(), "Deleting the last area should release ownership");
        helper.assertFalse(tavern.hasOwner(), "Tavern should have no owner");

        var third = tavern.createArea(AreaType.DINING,
                helper.absolutePos(new BlockPos(0, 1, 6)), helper.absolutePos(new BlockPos(4, 3, 10)), bob);
        helper.assertValueEqual(third.getCreatedArea().getName(), "#3", "numbers are never reused");
        helper.assertTrue(tavern.isOwner(bob.getUUID()), "Next creator should own the empty tavern");
        helper.succeed();
    }

    /**
     * Save -> load round trip through the real SavedData path.
     * Regression: spaces were re-scanned on load BEFORE the saved level was restored, so
     * furniture above level-1 limits (beds, 3rd+ table) disappeared after every world reload.
     */
    @GameTest(template = FLAT_15, batch = "progress_save_load")
    public static void saveAndLoadKeepsEverything(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Tavern tavern = freshTavern(helper);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        tavern.recordSale(123);

        placeBed(helper, new BlockPos(2, 1, 12), Direction.NORTH);
        placeBed(helper, new BlockPos(6, 1, 12), Direction.NORTH);
        tavern.createArea(AreaType.SLEEPING,
                helper.absolutePos(new BlockPos(0, 1, 9)), helper.absolutePos(new BlockPos(14, 3, 14)), owner);
        for (int x = 2; x <= 10; x += 4) {
            placeTableWithChair(helper, new BlockPos(x, 1, 2), Direction.SOUTH);
        }
        tavern.createArea(AreaType.DINING,
                helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(14, 3, 6)), owner);
        helper.assertValueEqual(tavern.getSleepingManager().getTotalBedCount(), 2, "beds before save");
        helper.assertValueEqual(tavern.getDiningManager().getTotalTableCount(), 3, "tables before save");

        CompoundTag saved = tavern.save(new CompoundTag(), level.registryAccess());
        level.getDataStorage().set(Tavern.DATA_NAME, Tavern.load(saved, level.registryAccess()));
        Tavern reloaded = Tavern.get(level);

        helper.assertFalse(reloaded == tavern, "Expected a freshly loaded instance");
        helper.assertTrue(reloaded.getCurrentUpgrade() == TavernUpgrade.LEVEL_2, "Level lost on reload");
        helper.assertValueEqual(reloaded.getTotalMoneyEarned(), tavern.getTotalMoneyEarned(), "money earned");
        helper.assertValueEqual(reloaded.getReputation(), tavern.getReputation(), "reputation");
        helper.assertValueEqual(reloaded.getTotalCustomersServed(), 1, "customers served");
        helper.assertTrue(reloaded.isOwner(owner.getUUID()), "Owner lost on reload");
        helper.assertFalse(reloaded.isManuallyOpen(), "Closed sign state lost on reload");
        helper.assertValueEqual(reloaded.getAllSpaces().size(), 2, "areas");
        helper.assertValueEqual(reloaded.getSleepingManager().getTotalBedCount(), 2, "beds after reload");
        helper.assertValueEqual(reloaded.getDiningManager().getTotalTableCount(), 3, "tables after reload");
        helper.assertValueEqual(reloaded.getDiningManager().getSpaces().iterator().next().getValidChairCount(), 3,
                "valid chairs after reload");
        helper.assertValueEqual(reloaded.getNextCounter(AreaType.DINING), 2, "dining counter continues");
        helper.succeed();
    }

    /** Spawn mix: level 1 only sends diners; from level 2 all three customer types show up (~70/20/10). */
    @GameTest(template = FLAT_7, batch = "progress_customer_mix")
    public static void customerMixDependsOnLevel(GameTestHelper helper) {
        Tavern tavern = freshTavern(helper);
        RandomSource random = RandomSource.create(42);
        for (int i = 0; i < 200; i++) {
            helper.assertTrue(CustomerLifecycleFactory.create(tavern, random).getType() == LifecycleType.DINING_ONLY,
                    "Level 1 must only spawn dining customers");
        }

        upgradeTo(tavern, TavernUpgrade.LEVEL_2);
        Map<LifecycleType, Integer> counts = new EnumMap<>(LifecycleType.class);
        int rolls = 2000;
        for (int i = 0; i < rolls; i++) {
            counts.merge(CustomerLifecycleFactory.create(tavern, random).getType(), 1, Integer::sum);
        }
        double dining = counts.getOrDefault(LifecycleType.DINING_ONLY, 0) / (double) rolls;
        double sleeping = counts.getOrDefault(LifecycleType.SLEEPING_ONLY, 0) / (double) rolls;
        double full = counts.getOrDefault(LifecycleType.FULL_SERVICE, 0) / (double) rolls;
        helper.assertTrue(Math.abs(dining - 0.7) < 0.05, "Dining share off: " + dining);
        helper.assertTrue(Math.abs(sleeping - 0.2) < 0.05, "Sleeping share off: " + sleeping);
        helper.assertTrue(Math.abs(full - 0.1) < 0.05, "Full-service share off: " + full);
        helper.succeed();
    }
}
