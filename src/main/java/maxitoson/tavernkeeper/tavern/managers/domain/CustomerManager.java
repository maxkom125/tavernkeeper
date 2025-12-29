package maxitoson.tavernkeeper.tavern.managers.domain;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycle;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import maxitoson.tavernkeeper.events.CustomerPaymentEvent;
import maxitoson.tavernkeeper.tavern.economy.CustomerRequest;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import maxitoson.tavernkeeper.tavern.TavernContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages customer spawning, tracking, and lifecycle
 * 
 * Responsibilities:
 * - Spawn customers at designated spawn points
 * - Track active customers
 * - Enforce customer limits
 * - Manage spawn timing/rates
 */
public class CustomerManager {
    private final TavernContext tavern;
    private final List<UUID> activeCustomers = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    
    private int spawnCooldownTicks = 0; // Countdown until next spawn attempt
    private Optional<BlockPos> waveSpawnPos = Optional.empty(); // Cached spawn position
    
    // Tavern capacity settings (will be upgradeable in the future)
    private int maxCustomers = 10; // Starting capacity, can be upgraded
    private int spawnIntervalMean = 600; // Mean spawn rate (30 seconds)
    private int spawnIntervalStd = 120; // Standard deviation (±6 seconds)
    
    // Upgrade-based multipliers (set by upgrade system)
    private float spawnRateMultiplier;
    
    // Spawn attempt constants (from Raid.java)
    private static final int NUM_SPAWN_ATTEMPTS = 20; // Total attempts to find valid position
    private static final int SPAWN_SEARCH_RADIUS = 32; // Blocks from center to search
    
    /**
     * Result object for customer service attempts.
     * Simple: null = do nothing, non-null = show feedback
     */
    public static class ServiceResult {
        private final CustomerRequest request;
        private final boolean success;
        
        private ServiceResult(CustomerRequest request, boolean success) {
            this.request = request;
            this.success = success;
        }
        
        /** Player served/accepted - show success message */
        public static ServiceResult success(CustomerRequest request) {
            return new ServiceResult(request, true);
        }
        
        /** Player has wrong item - show what customer wants */
        public static ServiceResult wrongFood(CustomerRequest request) {
            return new ServiceResult(request, false);
        }
        
        /** Customer not ready - do nothing (allow other interactions) */
        public static ServiceResult ignored() {
            return new ServiceResult(null, false);
        }
        
        public boolean shouldShowFeedback() { return request != null; }
        public boolean isSuccess() { return success; }
        public CustomerRequest getRequest() { return request; }
    }
    
    public CustomerManager(TavernContext tavern) {
        this.tavern = tavern;
    }
    
    /**
     * Called every server tick to handle customer lifecycle
     * Adapted from Raid.tick() (Raid.java lines 277-430)
     */
    public void tick(ServerLevel level) {
        if (level == null) return;
        
        // Handle spawn cooldown and position searching (from Raid.java lines 315-347)
        if (spawnCooldownTicks > 0) {
            // Check if we need to find/update spawn position (every 5 ticks)
            boolean needsNewPos = !waveSpawnPos.isPresent() && spawnCooldownTicks % 5 == 0;
            if (waveSpawnPos.isPresent() && !level.isPositionEntityTicking(waveSpawnPos.get())) {
                needsNewPos = true; // Position no longer ticking, find new one
            }
            
            if (needsNewPos) {
                waveSpawnPos = getValidSpawnPos(level);
            }
            
            spawnCooldownTicks--;
        }
        
        // Try to spawn if cooldown expired and we can spawn more (from Raid.java lines 366-383)
        if (shouldSpawnCustomer(level)) {
            BlockPos spawnPos = waveSpawnPos.isPresent() ? waveSpawnPos.get() : findRandomSpawnPos(level, NUM_SPAWN_ATTEMPTS);
            if (spawnPos != null) {
                spawnCustomer(level, spawnPos);
            } else {
                // Failed to find position, try again next interval
                resetSpawnCooldown();
            }
        }
    }
    
    /**
     * Check if we should attempt to spawn a customer
     * Adapted from Raid.shouldSpawnGroup() (line 642)
     */
    private boolean shouldSpawnCustomer(ServerLevel level) {
        return spawnCooldownTicks == 0 
            && countActiveCustomers(level) < maxCustomers
            && isTavernOpen();
    }
    
    /**
     * Get a valid spawn position (with multiple attempts)
     * Adapted from Raid.getValidSpawnPos() (lines 442-451)
     */
    private Optional<BlockPos> getValidSpawnPos(ServerLevel level) {
        for (int attempt = 0; attempt < 3; attempt++) {
            BlockPos pos = findRandomSpawnPos(level, 1);
            if (pos != null) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Find a random spawn position near the tavern center (random lectern)
     * Adapted directly from Raid.findRandomSpawnPos() (lines 686-706)
     * 
     * This is the CORE Raid spawn logic with exact same validation checks
     */
    private BlockPos findRandomSpawnPos(ServerLevel level, int maxTry) {
        // Get center point (random lectern in service area)
        BlockPos center = getTavernCenter();
        if (center == null) {
            return null; // No lecterns = can't spawn
        }
        
        // Get spawn placement type for validation (from Raid.java line 689)
        EntityType<?> customerType = TavernKeeperMod.CUSTOMER.get();
        SpawnPlacementType placementType = SpawnPlacements.getPlacementType(customerType);
        
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        
        // Try maxTry times to find valid position (from Raid.java lines 691-703)
        for (int attempt = 0; attempt < maxTry; attempt++) {
            // Random angle and distance from center (EXACT Raid logic from lines 692-694)
            float angle = level.random.nextFloat() * ((float)Math.PI * 2F); // 2π
            int x = center.getX() + Mth.floor(Mth.cos(angle) * SPAWN_SEARCH_RADIUS) + level.random.nextInt(5);
            int z = center.getZ() + Mth.floor(Mth.sin(angle) * SPAWN_SEARCH_RADIUS) + level.random.nextInt(5);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z); // Get surface height (line 695)
            
            mutablePos.set(x, y, z);
            
            // Validate spawn position (EXACT checks from Raid.java lines 697-700)
            if (level.isLoaded(mutablePos) // Replaced deprecated hasChunksAt
                && level.isPositionEntityTicking(mutablePos)
                && (placementType.isSpawnPositionOk(level, mutablePos, customerType)
                    || level.getBlockState(mutablePos.below()).is(Blocks.SNOW) 
                        && level.getBlockState(mutablePos).isAir())) {
                return mutablePos.immutable();
            }
        }
        
        return null;
    }
    
    /**
     * Get the tavern's center point (random lectern position)
     * This replaces Raid's village center logic
     */
    private BlockPos getTavernCenter() {
        return tavern.getTavernCenter();
    }
    
    /**
     * Actually spawn a customer entity at the given position
     * Adapted from Raid.joinRaid() (lines 604-620)
     */
    private void spawnCustomer(ServerLevel level, BlockPos pos) {
        CustomerEntity customer = TavernKeeperMod.CUSTOMER.get().create(level);
        if (customer == null) {
            resetSpawnCooldown();
            return;
        }
        
        // Position entity (from Raid.java line 612)
        customer.setPos((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5);
        
        // Assign lifecycle (determines entire customer journey)
        // Factory uses probabilities: X% dining, Y% sleeping, Z% full service
        CustomerLifecycle lifecycle = CustomerLifecycleFactory.create(tavern, random);
        customer.setLifecycle(lifecycle);
        
        // Finalize spawn (from Raid.java line 613)
        customer.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        
        // Set on ground (from Raid.java line 615)
        customer.setOnGround(true);
        
        // Add to world (from Raid.java line 616)
        level.addFreshEntityWithPassengers(customer);
        
        // Track customer
        registerCustomer(customer.getUUID());
        
        // Reset for next spawn
        waveSpawnPos = Optional.empty();
        resetSpawnCooldown();
        
        // Notify players
        level.getServer().getPlayerList().getPlayers().forEach(player -> {
            player.sendSystemMessage(
                Component.literal(
                    "§6[Tavern] §rA customer has arrived! (" + countActiveCustomers(level) + "/" + maxCustomers + ")"
                )
            );
        });
    }
    
    /**
     * Reset spawn cooldown with random interval (mean ± std)
     */
    private void resetSpawnCooldown() {
        // Gaussian distribution: mean ± std, adjusted by upgrade multiplier
        int variance = (int)(random.nextGaussian() * spawnIntervalStd);
        int baseInterval = spawnIntervalMean + variance;
        int adjustedInterval = (int)(baseInterval / spawnRateMultiplier);
        spawnCooldownTicks = Math.max(10, adjustedInterval);
    }
    
    /**
     * Count actual customer entities in the world
     * This handles chunk reloading gracefully - if chunks reload with customers,
     * we'll detect them and not over-spawn
     */
    private int countActiveCustomers(ServerLevel level) {
        try {
            // Query all customer entities in the world
            return level.getEntities(
                TavernKeeperMod.CUSTOMER.get(),
                entity -> true // Count all customers
            ).size();
        } catch (Exception e) {
            // Fallback to tracked list if entity type not available
            return activeCustomers.size();
        }
    }
    
    /**
     * Check if tavern is open (has at least one service area with lecterns)
     * This replaces Raid's village check
     */
    public boolean isTavernOpen() {
        return tavern.isOpen();
    }
    
    /**
     * Get count of active customers
     */
    public int getActiveCustomerCount() {
        return activeCustomers.size();
    }
    
    /**
     * Check if we can spawn more customers
     */
    public boolean canSpawnCustomer() {
        return isTavernOpen() && activeCustomers.size() < maxCustomers;
    }
    
    // ========== Capacity Management (for future upgrades) ==========
    
    /**
     * Get current maximum customer capacity
     * TODO: Calculate based on tavern level, number of chairs, etc.
     */
    public int getMaxCustomers() {
        return maxCustomers;
    }
    
    /**
     * Set maximum customer capacity (for upgrades)
     */
    public void setMaxCustomers(int max) {
        this.maxCustomers = Math.max(1, max); // Minimum 1
    }
    
    /**
     * Get current mean spawn interval in ticks
     */
    public int getSpawnIntervalMean() {
        return spawnIntervalMean;
    }
    
    /**
     * Set mean spawn interval (for upgrades that speed up spawning)
     */
    public void setSpawnIntervalMean(int interval) {
        this.spawnIntervalMean = interval;
    }
    
    /**
     * Get spawn interval standard deviation
     */
    public int getSpawnIntervalStd() {
        return spawnIntervalStd;
    }
    
    /**
     * Set spawn interval standard deviation
     */
    public void setSpawnIntervalStd(int std) {
        this.spawnIntervalStd = Math.max(0, std);
    }
    
    /**
     * Track a newly spawned customer
     */
    public void registerCustomer(UUID customerId) {
        if (!activeCustomers.contains(customerId)) {
            activeCustomers.add(customerId);
        }
    }
    
    /**
     * Remove customer from tracking (when despawned)
     */
    public void unregisterCustomer(UUID customerId) {
        activeCustomers.remove(customerId);
    }
    
    // ========== Customer Service ==========
    
    /**
     * Handle player interaction with customer (serving food or accepting sleeping payment)
     * This is the main entry point for player-customer interactions.
     * Returns a ServiceResult for UI layer to interpret.
     * 
     * @param player The player interacting with the customer
     * @param customer The customer being served
     * @param heldItem The item the player is holding
     * @return ServiceResult with outcome details
     */
    public ServiceResult handlePlayerServe(Player player, CustomerEntity customer, ItemStack heldItem) {
        CustomerRequest request = customer.getRequest();
        CustomerState state = customer.getCustomerState();
        
        // Route based on customer state and request type
        if (state == CustomerState.WAITING_SERVICE && request instanceof FoodRequest foodRequest) {
            return handleFoodService(player, customer, foodRequest, heldItem);
        } else if (state == CustomerState.WAITING_RECEPTION && request instanceof SleepingRequest sleepingRequest) {
            return handleSleepingService(player, customer, sleepingRequest);
        } else {
            // Customer not waiting for service or has no request
            return ServiceResult.ignored();
        }
    }
    
    /**
     * Handle food service - player gives food, customer pays and goes to eat
     */
    private ServiceResult handleFoodService(Player player, CustomerEntity customer, 
                                           FoodRequest foodRequest, ItemStack heldItem) {
        // Validate food item
        if (!foodRequest.isSatisfiedBy(heldItem)) {
            return ServiceResult.wrongFood(foodRequest);
        }
        
        // ===== SUCCESS PATH =====
        
        // Remove food from player
        heldItem.shrink(foodRequest.getRequestedAmount());
        
        // Give player the payment and record sale in tavern statistics via event
        NeoForge.EVENT_BUS.post(
            new CustomerPaymentEvent(player, customer, foodRequest)
        );
        
        // Customer received food - transition to next state via lifecycle
        customer.transitionToNextState((net.minecraft.server.level.ServerLevel) customer.level());
        
        LogUtils.getLogger().info("Player {} served customer {} with {} and received {}", 
            player.getName().getString(), customer.getId(), 
            foodRequest.getDisplayName(), foodRequest.getPrice().getDisplayName());
        
        return ServiceResult.success(foodRequest);
    }
    
    /**
     * Handle sleeping service - player accepts payment, customer goes to bed
     */
    private ServiceResult handleSleepingService(Player player, CustomerEntity customer, 
                                               SleepingRequest sleepingRequest) {
        // No item check needed for sleeping - just right-click to accept payment
        
        // ===== SUCCESS PATH =====
        
        // Give player the payment and record sale in tavern statistics via event
        NeoForge.EVENT_BUS.post(
            new CustomerPaymentEvent(player, customer, sleepingRequest)
        );
        
        // Customer transitions to finding bed via lifecycle
        customer.transitionToNextState((net.minecraft.server.level.ServerLevel) customer.level());
        
        LogUtils.getLogger().info("Player {} accepted payment from customer {} for {} and received {}", 
            player.getName().getString(), customer.getId(), 
            sleepingRequest.getDisplayName(), sleepingRequest.getPrice().getDisplayName());
        
        return ServiceResult.success(sleepingRequest);
    }
    
    // ========== Upgrade System ==========
    
    /**
     * Set spawn rate multiplier (called by upgrade system)
     * Higher values = faster spawns
     */
    public void setSpawnRateMultiplier(float multiplier) {
        this.spawnRateMultiplier = multiplier;
    }
    
    /**
     * Get current spawn rate multiplier
     */
    public float getSpawnRateMultiplier() {
        return spawnRateMultiplier;
    }
    
    // ========== Persistence ==========
    
    /**
     * Save customer manager configuration (NOT individual customers - they persist themselves)
     * 
     * Design decision: Only save settings, not runtime state
     * - Customer entities persist through Minecraft's own entity system
     * - countActiveCustomers() queries world directly on load
     * - Spawn position/cooldown can be recalculated fresh
     * - This prevents stale UUID tracking and simplifies persistence
     */
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag customerTag = new CompoundTag();
        
        // Only save configuration (upgradeable settings)
        customerTag.putInt("MaxCustomers", maxCustomers);
        customerTag.putInt("SpawnIntervalMean", spawnIntervalMean);
        customerTag.putInt("SpawnIntervalStd", spawnIntervalStd);
        
        tag.put("CustomerManager", customerTag);
    }
    
    /**
     * Load customer manager configuration
     * Runtime state (cooldown, spawn position, customer list) is reset and recalculated
     */
    public void load(CompoundTag tag, ServerLevel level, HolderLookup.Provider registries) {
        if (!tag.contains("CustomerManager")) {
            return;
        }
        
        CompoundTag customerTag = tag.getCompound("CustomerManager");
        
        // Load configuration
        if (customerTag.contains("MaxCustomers")) {
            maxCustomers = customerTag.getInt("MaxCustomers");
        }
        if (customerTag.contains("SpawnIntervalMean")) {
            spawnIntervalMean = customerTag.getInt("SpawnIntervalMean");
        }
        if (customerTag.contains("SpawnIntervalStd")) {
            spawnIntervalStd = customerTag.getInt("SpawnIntervalStd");
        }
        
        // Runtime state is intentionally reset:
        // - activeCustomers list cleared (will rebuild from world query)
        // - spawnCooldownTicks = 0 (spawn fresh after load)
        // - waveSpawnPos = empty (find new position)
        activeCustomers.clear();
        spawnCooldownTicks = 0;
        waveSpawnPos = Optional.empty();
    }
}

