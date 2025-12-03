package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.economy.CoinRegistry;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles granting special advancements:
 * - Millionaire advancement (1M coins earned)
 * - Reputation milestones (10, 50, 100, 250, 500, 1000)
 * - Coin collection advancements
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class AdvancementHandler {
    
    // Money milestone
    private static final long MILLIONAIRE_THRESHOLD = 1_000_000L;
    private static final ResourceLocation ADV_MILLIONAIRE = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "millionaire");
    private static final Set<UUID> millionairesAwarded = new HashSet<>();
    
    // Reputation milestones
    private static final int[] REPUTATION_THRESHOLDS = {10, 50, 100, 250, 500, 1000};
    private static final ResourceLocation[] REPUTATION_ADVANCEMENTS = {
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_10"),
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_50"),
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_100"),
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_250"),
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_500"),
        ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "reputation_1000")
    };
    
    // Track highest reputation milestone reached per player
    private static final Map<UUID, Integer> playerHighestReputation = new HashMap<>();
    
    // Coin collection advancements
    private static final ResourceLocation ADV_COPPER = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "first_earning");
    private static final ResourceLocation ADV_IRON = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "iron_collector");
    private static final ResourceLocation ADV_GOLD = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "gold_collector");
    private static final ResourceLocation ADV_DIAMOND = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "diamond_collector");
    private static final ResourceLocation ADV_NETHERITE = ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "netherite_collector");
    
    /**
     * Check and grant coin collection advancement when a coin is added to wallet.
     * Also works in parallel with inventory_changed trigger (for coins in inventory).
     * Only grants if player doesn't already have the advancement.
     */
    public static void checkCoinAdvancement(ServerPlayer player, Item coinItem) {
        // Grant advancement based on coin tier
        int tierIndex = CoinRegistry.getTierIndex(coinItem);
        ResourceLocation advancementId = null;
        
        switch (tierIndex) {
            case 0: // Copper
                advancementId = ADV_COPPER;
                break;
            case 1: // Iron
                advancementId = ADV_IRON;
                break;
            case 2: // Gold
                advancementId = ADV_GOLD;
                break;
            case 3: // Diamond
                advancementId = ADV_DIAMOND;
                break;
            case 4: // Netherite
                advancementId = ADV_NETHERITE;
                break;
        }
        
        if (advancementId != null) {
            // Only grant if player doesn't already have it
            if (!hasAdvancement(player, advancementId)) {
                grantAdvancement(player, advancementId);
            }
        }
    }
    
    /**
     * Check if a player has already completed an advancement
     */
    private static boolean hasAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
        if (holder == null) {
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
    
    /**
     * Check and grant millionaire and reputation advancements to tavern owner when customer payment occurs
     */
    @SubscribeEvent
    public static void onCustomerPayment(CustomerPaymentEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        ServerLevel level = serverPlayer.serverLevel();
        Tavern tavern = Tavern.get(level);
        
        // Check if tavern has an owner
        if (!tavern.hasOwner()) {
            return;
        }
        
        UUID ownerUUID = tavern.getOwnerUUID();
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(ownerUUID);
        
        // Only grant advancements if owner is online
        if (ownerPlayer == null) {
            return;
        }
        
        // Check millionaire advancement
        long totalEarned = tavern.getTotalMoneyEarned();
        if (totalEarned >= MILLIONAIRE_THRESHOLD && !millionairesAwarded.contains(ownerUUID)) {
            grantAdvancement(ownerPlayer, ADV_MILLIONAIRE);
            millionairesAwarded.add(ownerUUID);
        }
        
        // Check reputation advancements
        int currentReputation = tavern.getReputation();
        int highestReached = playerHighestReputation.getOrDefault(ownerUUID, 0);
        
        for (int i = 0; i < REPUTATION_THRESHOLDS.length; i++) {
            int threshold = REPUTATION_THRESHOLDS[i];
            if (currentReputation >= threshold && highestReached < threshold) {
                grantAdvancement(ownerPlayer, REPUTATION_ADVANCEMENTS[i]);
                playerHighestReputation.put(ownerUUID, threshold);
                highestReached = threshold;
            }
        }
    }
    
    /**
     * Grant an advancement to a player by completing all criteria
     */
    private static void grantAdvancement(ServerPlayer player, ResourceLocation advancementId) {
        AdvancementHolder holder = player.server.getAdvancements().get(advancementId);
        if (holder != null) {
            for (String criterion : holder.value().criteria().keySet()) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }
}

