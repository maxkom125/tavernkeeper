package maxitoson.tavernkeeper.tavern;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.network.NetworkHandler;
import maxitoson.tavernkeeper.network.SyncAreasPacket;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.managers.domain.BaseDomainManager;
import maxitoson.tavernkeeper.tavern.managers.domain.CustomerManager;
import maxitoson.tavernkeeper.tavern.managers.domain.DiningManager;
import maxitoson.tavernkeeper.tavern.managers.domain.ServiceManager;
import maxitoson.tavernkeeper.tavern.managers.domain.SleepingManager;
import maxitoson.tavernkeeper.tavern.managers.system.AdvancementManager;
import maxitoson.tavernkeeper.tavern.managers.system.EconomyManager;
import maxitoson.tavernkeeper.tavern.managers.system.UpgradeManager;
import maxitoson.tavernkeeper.tavern.managers.domain.CustomerManager.ServiceResult;
import maxitoson.tavernkeeper.tavern.spaces.BaseSpace;
import maxitoson.tavernkeeper.tavern.spaces.ServiceSpace;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Stream;

/**
 * Aggregate root for the entire tavern domain
 * Manages all tavern facilities (dining, sleeping, etc.) and coordinates persistence
 * 
 * Pattern: Tavern (SavedData) → Managers → Spaces → Areas + Furniture
 * 
 * Implements TavernContext to provide controlled access to managers
 */
public class Tavern extends SavedData implements TavernContext {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "tavernkeeper_tavern";
    
    private final DiningManager diningManager;
    private final SleepingManager sleepingManager;
    private final ServiceManager serviceManager;
    private final CustomerManager customerManager;
    private final EconomyManager economyManager;
    private final UpgradeManager upgradeManager;
    private final AdvancementManager advancementManager;
    private final TavernStatistics statistics;
    private ServerLevel level;
    
    // Tavern ownership (set when first area is created)
    private UUID ownerUUID = null;
    private String ownerName = null;
    
    // Tavern open/closed state
    private boolean manuallyOpen = true;  // Default to open
    private BlockPos tavernSignPos = null;  // Position of the tavern sign
    
    public Tavern() {
        this.statistics = new TavernStatistics();
        this.upgradeManager = new UpgradeManager(this);
        this.diningManager = new DiningManager(this);
        this.sleepingManager = new SleepingManager(this);
        this.serviceManager = new ServiceManager(this);
        this.customerManager = new CustomerManager(this);
        this.economyManager = new EconomyManager(this);
        this.advancementManager = new AdvancementManager();
        
        // Apply default upgrade to all managers (single source of truth)
        applyCurrentUpgradeToAllManagers();
    }
    
    public void setLevel(ServerLevel level) {
        this.level = level;
    }
    
    public ServerLevel getLevel() {
        return level;
    }
    
    /**
     * Get the Tavern instance for a world
     */
    public static Tavern get(ServerLevel level) {
        Tavern tavern = level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                Tavern::new,
                (tag, registries) -> {
                    Tavern t = new Tavern();
                    t.loadedData = tag;  // Store for deferred loading
                    return t;
                },
                null
            ),
            DATA_NAME
        );
        if (tavern.level != level) {
            tavern.setLevel(level);
            // Trigger deferred loading if needed
            if (tavern.loadedData != null) {
                tavern.loadTavernData(level.registryAccess());
            }
        }
        return tavern;
    }
    
    // ========== Owner Management ==========
    
    /**
     * Set the tavern owner (called when first area is created)
     */
    public void setOwner(UUID ownerUUID, String ownerName) {
        if (this.ownerUUID == null) {
            this.ownerUUID = ownerUUID;
            this.ownerName = ownerName;
            setDirty();
            LOGGER.info("Tavern owner set to {} ({})", ownerName, ownerUUID);
        }
    }
    
    /**
     * Get the tavern owner UUID
     */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }
    
    /**
     * Get the tavern owner name
     */
    public String getOwnerName() {
        return ownerName;
    }
    
    /**
     * Check if a player is the tavern owner
     */
    public boolean isOwner(UUID playerUUID) {
        return ownerUUID != null && ownerUUID.equals(playerUUID);
    }
    
    /**
     * Check if tavern has an owner
     */
    public boolean hasOwner() {
        return ownerUUID != null;
    }
    
    // ========== Public API: Area Management ==========
    
    /**
     * Create a new dining area
     */
    /**
     * Create a new dining area and scan for furniture
     * @return AddSpaceResult with area and scan information
     */
    public BaseDomainManager.AddSpaceResult createDiningArea(String name, BlockPos minPos, BlockPos maxPos) {
        BaseDomainManager.AddSpaceResult result = diningManager.addSpace(name, minPos, maxPos, level);
        setDirty();
        return result;
    }
    
    /**
     * Create a new sleeping area
     */
    /**
     * Create a new sleeping area and scan for furniture
     * @return AddSpaceResult with area and scan information
     */
    public BaseDomainManager.AddSpaceResult createSleepingArea(String name, BlockPos minPos, BlockPos maxPos) {
        BaseDomainManager.AddSpaceResult result = sleepingManager.addSpace(name, minPos, maxPos, level);
        setDirty();
        return result;
    }

    /**
     * Create a new service area
     */
    /**
     * Create a new service area and scan for furniture
     * @return AddSpaceResult with area and scan information
     */
    public BaseDomainManager.AddSpaceResult createServiceArea(String name, BlockPos minPos, BlockPos maxPos) {
        BaseDomainManager.AddSpaceResult result = serviceManager.addSpace(name, minPos, maxPos, level);
        setDirty();
        return result;
    }
    
    /**
     * Create an area of specified type with auto-generated name.
     * Automatically assigns ownership to the player if this is the first area.
     * 
     * @param type The area type
     * @param minPos Minimum corner position
     * @param maxPos Maximum corner position
     * @param player The player creating the area
     * @return CreationResult containing the created area and ownership info
     */
    public CreationResult createArea(AreaType type, BlockPos minPos, BlockPos maxPos, Player player) {
        // Check if player will become owner
        boolean becameOwner = !hasOwner();
        
        // Claim ownership if this is the first area
        if (becameOwner) {
            setOwner(player.getUUID(), player.getName().getString());
        }
        
        // Generate auto-name
        int nextNumber = getNextCounter(type);
        String name = "#" + nextNumber;
        
        // Create the area and get result (includes area + scan result)
        BaseDomainManager.AddSpaceResult addResult = switch (type) {
            case DINING -> createDiningArea(name, minPos, maxPos);
            case SLEEPING -> createSleepingArea(name, minPos, maxPos);
            case SERVICE -> createServiceArea(name, minPos, maxPos);
        };
        
        return new CreationResult(addResult.getArea(), becameOwner, addResult.getScanResult());
    }
    
    /**
     * Result of area creation operation.
     * Contains information about the created area and ownership changes.
     */
    public static class CreationResult {
        private final TavernArea createdArea;
        private final boolean becameOwner;
        private final Object scanResult; // ScanResult from specific space type
        
        public CreationResult(TavernArea createdArea, boolean becameOwner, Object scanResult) {
            this.createdArea = createdArea;
            this.becameOwner = becameOwner;
            this.scanResult = scanResult;
        }
        
        public TavernArea getCreatedArea() {
            return createdArea;
        }
        
        public boolean becameOwner() {
            return becameOwner;
        }
        
        public Object getScanResult() {
            return scanResult;
        }
    }
    
    /**
     * Delete an area by ID and return information about what changed.
     * Clears tavern owner if this was the last area.
     * 
     * @param id The UUID of the area to delete
     * @return DeletionResult containing deletion info, or null if area not found
     */
    public DeletionResult deleteArea(UUID id) {
        // Find the area before deletion to get its info
        BaseSpace spaceToDelete = getSpace(id);
        if (spaceToDelete == null) {
            return null;
        }
        
        TavernArea area = spaceToDelete.getArea();
        boolean wasLastArea = getAllSpaces().size() == 1;
        
        // Perform deletion
        boolean removed = diningManager.removeSpace(id) || 
                          sleepingManager.removeSpace(id) || 
                          serviceManager.removeSpace(id);
        
        if (removed) {
            // Check if this was the last area and clear owner
            if (getAllSpaces().isEmpty()) {
                clearOwner();
            }
            setDirty();
            
            return new DeletionResult(area, wasLastArea && !hasOwner());
        }
        
        return null;
    }
    
    /**
     * Sync all areas to connected clients.
     * UI layers should call this after area modifications.
     */
    public void syncAreasToAllClients() {
        java.util.List<TavernArea> areas = getAllSpaces().stream()
            .map(BaseSpace::getArea)
            .toList();
        NetworkHandler.sendToAllPlayers(new SyncAreasPacket(areas));
    }
    
    /**
     * Result of area deletion operation.
     * Contains all information UI layers need for notifications.
     */
    public static class DeletionResult {
        private final TavernArea deletedArea;
        private final boolean ownershipLost;
        
        public DeletionResult(TavernArea deletedArea, boolean ownershipLost) {
            this.deletedArea = deletedArea;
            this.ownershipLost = ownershipLost;
        }
        
        public TavernArea getDeletedArea() {
            return deletedArea;
        }
        
        public boolean wasOwnershipLost() {
            return ownershipLost;
        }
    }
    
    /**
     * Result object for tavern sign setting
     */
    public static class SignSetResult {
        private final boolean alreadySet;
        private final boolean destroyed;
        
        public SignSetResult(boolean alreadySet, boolean destroyed) {
            this.alreadySet = alreadySet;
            this.destroyed = destroyed;
        }
        
        public boolean wasAlreadySet() {
            return alreadySet;
        }
        
        public boolean wasOldSignDestroyed() {
            return destroyed;
        }
    }
    
    /**
     * Result object for toggling tavern open/closed
     */
    public static class ToggleResult {
        private final boolean nowOpen;
        private final boolean tavernReady;
        private final java.util.List<String> issues;
        
        public ToggleResult(boolean nowOpen, boolean tavernReady, java.util.List<String> issues) {
            this.nowOpen = nowOpen;
            this.tavernReady = tavernReady;
            this.issues = issues;
        }
        
        public boolean isNowOpen() {
            return nowOpen;
        }
        
        public boolean isTavernReady() {
            return tavernReady;
        }
        
        public java.util.List<String> getIssues() {
            return issues;
        }
    }
    
    /**
     * Clear the tavern owner (e.g., when last area is deleted)
     */
    private void clearOwner() {
        if (ownerUUID != null) {
            LOGGER.info("Clearing tavern owner {} ({})", ownerName, ownerUUID);
            ownerUUID = null;
            ownerName = null;
            setDirty();
        }
    }
    
    /**
     * Get all areas (from all managers)
     */
    public Collection<BaseSpace> getAllSpaces() {
        return Stream.concat(
            Stream.concat(
                diningManager.getSpaces().stream(),
                sleepingManager.getSpaces().stream()
            ),
            serviceManager.getSpaces().stream()
        ).map(s -> (BaseSpace) s).toList();
    }
    
    /**
     * Get space by ID
     */
    public BaseSpace getSpace(UUID id) {
        BaseSpace space = diningManager.getSpace(id);
        if (space != null) return space;
        space = sleepingManager.getSpace(id);
        if (space != null) return space;
        return serviceManager.getSpace(id);
    }
    
    /**
     * Get the space at a specific position
     */
    public BaseSpace getSpaceAt(BlockPos pos) {
        BaseSpace space = diningManager.getSpaceAt(pos);
        if (space != null) return space;
        space = sleepingManager.getSpaceAt(pos);
        if (space != null) return space;
        return serviceManager.getSpaceAt(pos);
    }
    
    /**
     * Get all spaces at a position (unmodifiable list)
     */
    public List<BaseSpace> getSpacesAt(BlockPos pos) {
        List<BaseSpace> result = new ArrayList<>();
        result.addAll(diningManager.getSpacesAt(pos));
        result.addAll(sleepingManager.getSpacesAt(pos));
        result.addAll(serviceManager.getSpacesAt(pos));
        return java.util.Collections.unmodifiableList(result);
    }
    
    /**
     * Get spaces that intersect with a bounding box
     */
    public List<BaseSpace> getIntersectingSpaces(AABB box) {
        List<BaseSpace> result = new ArrayList<>();
        result.addAll(diningManager.getIntersectingSpaces(box));
        result.addAll(sleepingManager.getIntersectingSpaces(box));
        result.addAll(serviceManager.getIntersectingSpaces(box));
        return java.util.Collections.unmodifiableList(result);
    }
    
    /**
     * Get next counter for auto-naming areas
     */
    public int getNextCounter(AreaType type) {
        return switch (type) {
            case DINING -> diningManager.getNextCounter();
            case SLEEPING -> sleepingManager.getNextCounter();
            case SERVICE -> serviceManager.getNextCounter();
        };
    }
    
    // ========== Manager Access ==========
    
    public DiningManager getDiningManager() {
        return diningManager;
    }
    
    public SleepingManager getSleepingManager() {
        return sleepingManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }
    
    public CustomerManager getCustomerManager() {
        return customerManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }
    
    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }
    
    // ========== Tavern State Queries (for CustomerManager) ==========

    /**
     *  Check if tavern is open for business (has service areas with lecterns AND manually open)
     * Note: Please change toggleOpenClosed if you change this method
     */
    @Override
    public boolean isOpen() {
        return manuallyOpen 
            && serviceManager.getTotalLecternCount() > 0
            && !getAllSpaces().isEmpty();
    }

    /**
     * Get a random tavern center point (random lectern position)
     * This provides CustomerManager with spawn center
     */
    @Override
    public BlockPos getTavernCenter() {
        List<BlockPos> allLecterns = new ArrayList<>();
        
        // Collect all lecterns from all service spaces
        for (var space : serviceManager.getSpaces()) {
            ServiceSpace serviceSpace = (ServiceSpace) space;
            for (ServiceLectern lectern : serviceSpace.getLecterns()) {
                allLecterns.add(lectern.getPosition());
            }
        }
        
        if (allLecterns.isEmpty()) {
            return null;
        }
        
        // Return random lectern
        return allLecterns.get(new java.util.Random().nextInt(allLecterns.size()));
    }
    
    /**
     * Handle player serving a customer with food.
     * Delegates to CustomerManager for business logic.
     * 
     * @param player The player serving the customer
     * @param customer The customer being served
     * @param heldItem The item the player is holding
     * @return ServiceResult with outcome details for UI layer
     */
    public ServiceResult handlePlayerServe(
            net.minecraft.world.entity.player.Player player,
            CustomerEntity customer,
            net.minecraft.world.item.ItemStack heldItem) {
        return customerManager.handlePlayerServe(player, customer, heldItem);
    }
        
    /**
     * Create a food request for a customer.
     * Delegates to EconomyManager.
     * Used by customer AI when they arrive at a lectern.
     * 
     * @return A randomly generated food request
     */
    public FoodRequest createFoodRequest() {
        return economyManager.createFoodRequest();
    }
    
    // ========== Statistics (TavernContext interface) ==========
    
    @Override
    public long getTotalMoneyEarned() {
        return statistics.getTotalMoneyEarned();
    }
    
    @Override
    public int getReputation() {
        return statistics.getReputation();
    }
    
    @Override
    public int getTotalCustomersServed() {
        return statistics.getTotalCustomersServed();
    }
    
    @Override
    public void recordSale(int copperAmount) {
        // Update statistics through internal helper (ensures upgrade check)
        modifyStatistics(() -> {
            statistics.addMoney(copperAmount);
            statistics.incrementCustomersServed();
            statistics.addReputation(1);  // +1 reputation per customer served
        });
    }
    
    /**
     * Adjust tavern reputation (positive to increase, negative to decrease)
     * Triggers upgrade checks and persistence
     * 
     * @param amount Amount to change (can be positive or negative)
     */
    @Override
    public void adjustReputation(int amount) {
        modifyStatistics(() -> {
            statistics.addReputation(amount);
        });
    }
    
    /**
     * Adjust tavern total money earned
     * Triggers upgrade checks and persistence
     * 
     * @param amount Amount to add to total earned (typically positive)
     */
    public void adjustMoney(int amount) {
        modifyStatistics(() -> {
            statistics.addMoney(amount);
        });
    }
    
    /**
     * Modify statistics and automatically check for upgrades
     * 
     * CRITICAL: All statistics modifications MUST go through this method!
     * Never call statistics.addX() directly - this ensures:
     * - setDirty() is always called
     * - Upgrade checks happen automatically
     * - Persistence works correctly
     * 
     * Example usage:
     * <pre>
     * modifyStatistics(() -> {
     *     statistics.addMoney(amount);
     *     statistics.addReputation(1);
     * });
     * </pre>
     * 
     * @param modification The statistics modification to perform
     */
    private void modifyStatistics(Runnable modification) {
        modification.run();
        setDirty();
        checkForUpgrades();
    }
    
    // ========== Upgrades ==========
        
    /**
     * Check if upgrades are available and apply them
     * Called automatically after statistics changes
     */
    private void checkForUpgrades() {
        if (level == null) return;
        
        TavernUpgrade newTavernLevel = upgradeManager.checkAndAutoUpgrade(level);
        if (newTavernLevel != null) {
            // Apply upgrade to all managers
            applyCurrentUpgradeToAllManagers();
            setDirty();
        }
    }
    
    /**
     * Get current upgrade level
     */
    public TavernUpgrade getCurrentUpgrade() {
        return upgradeManager.getCurrentUpgrade();
    }
    
    /**
     * Apply current upgrade level to all managers
     * Called during initialization, loading, and after upgrade purchase
     */
    private void applyCurrentUpgradeToAllManagers() {
        TavernUpgrade currentTavernLevel = upgradeManager.getCurrentUpgrade();
        
        // Apply to all managers
        currentTavernLevel.applyToDiningManager(diningManager);
        currentTavernLevel.applyToCustomerManager(customerManager);
        currentTavernLevel.applyToEconomyManager(economyManager);
        // Future: currentTavernLevel.applyToSleepingManager(sleepingManager);
        
        LOGGER.info("Applied upgrade {} (maxTables: {}, paymentMult: {}x, spawnMult: {}x)", 
            currentTavernLevel.getDisplayName(), 
            diningManager.getMaxTables(),
            economyManager.getPaymentMultiplierValue(),
            customerManager.getSpawnRateMultiplier());
    }

    // ========== Tavern Sign Management (Tavern Open/Closed State) ==========
    /**
     * Set tavern sign position and update its text
     * Destroys the old tavern sign if one exists
     * 
     * @param pos The position of the new tavern sign
     * @param player The player setting the sign (for feedback), can be null
     */
    public SignSetResult setTavernSign(BlockPos pos) {
        // Check if already the tavern sign
        if (tavernSignPos != null && tavernSignPos.equals(pos)) {
            return new SignSetResult(true, false);
        }
        
        // Destroy old sign if it exists at a different position
        boolean destroyedOld = false;
        if (tavernSignPos != null && level != null) {
            net.minecraft.world.level.block.state.BlockState oldState = level.getBlockState(tavernSignPos);
            if (oldState.getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
                level.destroyBlock(tavernSignPos, true); // true = drop items
                LOGGER.info("Destroyed old tavern sign at {}", tavernSignPos);
                destroyedOld = true;
            }
        }
        
        // Set new sign position and update its text
        this.tavernSignPos = pos;
        updateSignText();
        setDirty();
        
        LOGGER.info("Set tavern sign at {}", pos);
        return new SignSetResult(false, destroyedOld);
    }
    
    /**
     * Toggle tavern open/closed state
     * Called when player clicks on the tavern sign
     * Returns a ToggleResult for UI layer to interpret
     * 
     * @return ToggleResult with outcome details
     */
    public ToggleResult toggleOpenClosed() {
        this.manuallyOpen = !this.manuallyOpen;
        updateSignText();
        setDirty();
        
        // Determine tavern status and issues
        boolean tavernReady = isOpen();
        java.util.List<String> issues = new java.util.ArrayList<>();
        
        if (manuallyOpen && !tavernReady) {
            // Sign is OPEN but missing requirements
            if (getAllSpaces().isEmpty()) {
                issues.add("No areas defined");
            }
            if (serviceManager.getTotalLecternCount() == 0) {
                issues.add("No lecterns in service areas");
            }
        }
        
        return new ToggleResult(manuallyOpen, tavernReady, issues);
    }
    
    /**
     * Update the sign text to show current state
     */
    private void updateSignText() {
        if (tavernSignPos == null || level == null) {
            return;
        }
        
        // Get the sign block entity
        var blockEntity = level.getBlockEntity(tavernSignPos);
        if (blockEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity signEntity) {
            // Update the sign text
            var frontText = signEntity.getFrontText();
            frontText = frontText.setMessage(0, net.minecraft.network.chat.Component.literal(""));
            frontText = frontText.setMessage(1, net.minecraft.network.chat.Component.literal(
                manuallyOpen ? "§a§lOPEN" : "§c§lCLOSED"
            ));
            frontText = frontText.setMessage(2, net.minecraft.network.chat.Component.literal(""));
            frontText = frontText.setMessage(3, net.minecraft.network.chat.Component.literal(""));
            
            signEntity.setText(frontText, true);
            signEntity.setChanged();
            
            // Sync to clients
            level.sendBlockUpdated(tavernSignPos, 
                level.getBlockState(tavernSignPos), 
                level.getBlockState(tavernSignPos), 
                3);
        }
    }
    
    /**
     * Get the current tavern sign position
     */
    public BlockPos getTavernSignPos() {
        return tavernSignPos;
    }
    
    /**
     * Check if a position is the tavern sign
     */
    public boolean isTavernSign(BlockPos pos) {
        return tavernSignPos != null && tavernSignPos.equals(pos);
    }
    
    /**
     * Clear the tavern sign reference (e.g., when sign is broken)
     */
    public void clearTavernSign() {
        this.tavernSignPos = null;
        setDirty();
    }
    
    /**
     * Get the manually-set open state (used for messaging)
     */
    public boolean isManuallyOpen() {
        return manuallyOpen;
    }

    // ========== Tavern Queries for AI Behaviors ==========
    
    /**
     * Find nearest available chair for a customer
     * Delegates to DiningManager
     */
    public java.util.Optional<Chair> findNearestAvailableChair(
            BlockPos from, double maxDistance) {
        return diningManager.findNearestAvailableChair(from, maxDistance);
    }
    
    /**
     * Reserve a chair for a customer
     * Encapsulates DiningManager access for AI behaviors
     */
    public boolean reserveChair(BlockPos chairPos, java.util.UUID customerId) {
        return diningManager.reserveChair(chairPos, customerId);
    }
    
    /**
     * Release a chair reservation
     * Encapsulates DiningManager access for AI behaviors
     */
    public void releaseChair(BlockPos chairPos) {
        diningManager.releaseChair(chairPos);
    }
    
    /**
     * Find nearest service lectern
     * Delegates to ServiceManager
     */
    public java.util.Optional<ServiceLectern> findNearestLectern(
            BlockPos from, double maxDistance) {
        return serviceManager.findNearestLectern(from, maxDistance);
    }
    
    // ========== Scanning ==========
    
    /**
     * Scan all areas and recognize all furniture
     */
    public void scanAndRecognize() {
        diningManager.scanAll();
        sleepingManager.scanAll();
        serviceManager.scanAll();
    }
    
    // ========== Lifecycle / Spawning ==========
    
    /**
     * Called every server tick to handle tavern lifecycle
     */
    public void tick() {
        if (level == null) return;
        
        // Delegate to CustomerManager
        customerManager.tick(level);
    }
    
    // ========== Persistence ==========
    
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        // Save managers
        diningManager.save(tag, registries);
        sleepingManager.save(tag, registries);
        serviceManager.save(tag, registries);
        customerManager.save(tag, registries);
        
        // Save statistics and upgrades
        statistics.save(tag);
        upgradeManager.save(tag);
        advancementManager.save(tag);
        
        // Save tavern metadata
        saveTavernMetadata(tag);
        
        return tag;
    }
    
    private void saveTavernMetadata(CompoundTag tag) {
        // Save tavern owner
        if (ownerUUID != null) {
            tag.putUUID("ownerUUID", ownerUUID);
            tag.putString("ownerName", ownerName);
        }
        
        // Save tavern open/closed state
        tag.putBoolean("manuallyOpen", manuallyOpen);
        
        // Save tavern sign position
        if (tavernSignPos != null) {
            tag.putLong("tavernSignPos", tavernSignPos.asLong());
        }
    }

    
    public static Tavern load(CompoundTag tag, HolderLookup.Provider registries) {
        Tavern tavern = new Tavern();
        tavern.loadedData = tag;  // Store for later loading when level is set
        return tavern;
    }
    
    private CompoundTag loadedData = null;
    
    /**
     * Load tavern data after level is set
     */
    private void loadTavernData(HolderLookup.Provider registries) {
        if (loadedData != null && level != null) {
            // Load managers
            diningManager.load(loadedData, level, registries);
            sleepingManager.load(loadedData, level, registries);
            serviceManager.load(loadedData, level, registries);
            customerManager.load(loadedData, level, registries);
            
            // Load statistics and upgrades
            statistics.load(loadedData);
            upgradeManager.load(loadedData);
            advancementManager.load(loadedData);
            
            // Apply current upgrade to managers after loading
            applyCurrentUpgradeToAllManagers();
            
            // Load tavern metadata
            loadTavernMetadata();
            
            loadedData = null;  // Clear after loading
        }
    }
    
    private void loadTavernMetadata() {
        // Load tavern owner
        if (loadedData.contains("ownerUUID")) {
            ownerUUID = loadedData.getUUID("ownerUUID");
            ownerName = loadedData.getString("ownerName");
            LOGGER.info("Loaded tavern owner: {} ({})", ownerName, ownerUUID);
        }
        
        // Load tavern open/closed state
        if (loadedData.contains("manuallyOpen")) {
            manuallyOpen = loadedData.getBoolean("manuallyOpen");
        }
        
        // Load tavern sign position
        if (loadedData.contains("tavernSignPos")) {
            tavernSignPos = BlockPos.of(loadedData.getLong("tavernSignPos"));
        }
    }
}
