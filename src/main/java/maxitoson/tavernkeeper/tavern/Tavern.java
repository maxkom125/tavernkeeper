package maxitoson.tavernkeeper.tavern;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.tavern.managers.CustomerManager;
import maxitoson.tavernkeeper.tavern.managers.DiningManager;
import maxitoson.tavernkeeper.tavern.managers.ServiceManager;
import maxitoson.tavernkeeper.tavern.managers.SleepingManager;
import maxitoson.tavernkeeper.tavern.spaces.BaseSpace;
import maxitoson.tavernkeeper.tavern.spaces.DiningSpace;
import maxitoson.tavernkeeper.tavern.spaces.ServiceSpace;
import maxitoson.tavernkeeper.tavern.spaces.SleepingSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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
public class Tavern extends SavedData implements maxitoson.tavernkeeper.tavern.managers.TavernContext {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "tavernkeeper_tavern";
    
    private final DiningManager diningManager;
    private final SleepingManager sleepingManager;
    private final ServiceManager serviceManager;
    private final CustomerManager customerManager;
    private ServerLevel level;
    
    // Tavern open/closed state
    private boolean manuallyOpen = true;  // Default to open
    private BlockPos tavernSignPos = null;  // Position of the tavern sign
    
    public Tavern() {
        this.diningManager = new DiningManager(this);
        this.sleepingManager = new SleepingManager(this);
        this.serviceManager = new ServiceManager(this);
        this.customerManager = new CustomerManager(this);
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
                tavern.loadSpacesIfNeeded(level.registryAccess());
            }
        }
        return tavern;
    }
    
    // ========== Public API: Area Management ==========
    
    /**
     * Create a new dining area
     */
    public DiningSpace createDiningArea(String name, BlockPos minPos, BlockPos maxPos) {
        DiningSpace space = diningManager.addArea(name, minPos, maxPos, level);
        space.scanForFurniture();
        setDirty();
        return space;
    }
    
    /**
     * Create a new sleeping area
     */
    public SleepingSpace createSleepingArea(String name, BlockPos minPos, BlockPos maxPos) {
        SleepingSpace space = sleepingManager.addArea(name, minPos, maxPos, level);
        space.scanForFurniture();
        setDirty();
        return space;
    }

    /**
     * Create a new service area
     */
    public ServiceSpace createServiceArea(String name, BlockPos minPos, BlockPos maxPos) {
        ServiceSpace space = serviceManager.addArea(name, minPos, maxPos, level);
        space.scanForFurniture();
        setDirty();
        return space;
    }
    
    /**
     * Create an area of specified type
     */
    public BaseSpace createArea(String name, AreaType type, BlockPos minPos, BlockPos maxPos) {
        return switch (type) {
            case DINING -> createDiningArea(name, minPos, maxPos);
            case SLEEPING -> createSleepingArea(name, minPos, maxPos);
            case SERVICE -> createServiceArea(name, minPos, maxPos);
        };
    }
    
    /**
     * Delete an area by ID
     */
    public boolean deleteArea(UUID id) {
        boolean removed = diningManager.removeSpace(id) || 
                          sleepingManager.removeSpace(id) || 
                          serviceManager.removeSpace(id);
        if (removed) {
            setDirty();
        }
        return removed;
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
    
    // ========== Tavern State Queries (for CustomerManager) ==========

    /**
     *  Check if tavern is open for business (has service areas with lecterns AND manually open)
     */
    public boolean isOpen() {
        return manuallyOpen 
            && serviceManager.getTotalLecternCount() > 0
            && !getAllSpaces().isEmpty();
    }
    
    /**
     * Set tavern sign position and update its text
     * Destroys the old tavern sign if one exists
     * 
     * @param pos The position of the new tavern sign
     * @param player The player setting the sign (for feedback), can be null
     */
    public void setTavernSign(BlockPos pos, net.minecraft.world.entity.player.Player player) {
        // Check if already the tavern sign
        if (tavernSignPos != null && tavernSignPos.equals(pos)) {
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§6[Tavern Keeper] §rThis is already your tavern sign!"
                ));
            }
            return;
        }
        
        // Destroy old sign if it exists at a different position
        if (tavernSignPos != null && level != null) {
            net.minecraft.world.level.block.state.BlockState oldState = level.getBlockState(tavernSignPos);
            if (oldState.getBlock() instanceof net.minecraft.world.level.block.SignBlock) {
                level.destroyBlock(tavernSignPos, true); // true = drop items
                LOGGER.info("Destroyed old tavern sign at {}", tavernSignPos);
            }
        }
        
        // Set new sign position and update its text
        this.tavernSignPos = pos;
        updateSignText();
        setDirty();
        
        // Provide feedback to player
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Tavern Keeper] §rThis sign is now your tavern sign! Right-click to toggle open/closed."
            ));
            player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        }
        
        LOGGER.info("Set tavern sign at {}", pos);
    }
    
    /**
     * Toggle tavern open/closed state
     * Called when player clicks on the tavern sign
     * 
     * @param player The player toggling the state (for feedback)
     */
    public void toggleOpenClosed(net.minecraft.world.entity.player.Player player) {
        this.manuallyOpen = !this.manuallyOpen;
        updateSignText();
        setDirty();
        
        // Provide feedback to player
        String status = isOpen() ? "§aOPEN" : "§cCLOSED";
        player.displayClientMessage(
            net.minecraft.network.chat.Component.literal("§6[Tavern Keeper] §rTavern is now " + status),
            true
        );
        player.playSound(net.minecraft.sounds.SoundEvents.WOODEN_DOOR_CLOSE, 1.0F, 1.0F);
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
    
    /**
     * Get a random tavern center point (random lectern position)
     * This provides CustomerManager with spawn center
     */
    public BlockPos getTavernCenter() {
        List<BlockPos> allLecterns = new ArrayList<>();
        
        // Collect all lecterns from all service spaces
        for (var space : serviceManager.getSpaces()) {
            ServiceSpace serviceSpace = (ServiceSpace) space;
            for (maxitoson.tavernkeeper.tavern.furniture.ServiceLectern lectern : serviceSpace.getLecterns()) {
                allLecterns.add(lectern.getPosition());
            }
        }
        
        if (allLecterns.isEmpty()) {
            return null;
        }
        
        // Return random lectern
        return allLecterns.get(new java.util.Random().nextInt(allLecterns.size()));
    }
    
    // ========== Tavern Queries for AI Behaviors ==========
    
    /**
     * Find nearest available chair for a customer
     * Delegates to DiningManager
     */
    public java.util.Optional<maxitoson.tavernkeeper.tavern.furniture.Chair> findNearestAvailableChair(
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
    public java.util.Optional<maxitoson.tavernkeeper.tavern.furniture.ServiceLectern> findNearestLectern(
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
        diningManager.save(tag, registries);
        sleepingManager.save(tag, registries);
        serviceManager.save(tag, registries);
        customerManager.save(tag, registries);
        
        // Save tavern open/closed state
        tag.putBoolean("manuallyOpen", manuallyOpen);
        
        // Save tavern sign position
        if (tavernSignPos != null) {
            tag.putLong("tavernSignPos", tavernSignPos.asLong());
        }
        
        return tag;
    }

    
    public static Tavern load(CompoundTag tag, HolderLookup.Provider registries) {
        Tavern tavern = new Tavern();
        tavern.loadedData = tag;  // Store for later loading when level is set
        return tavern;
    }
    
    private CompoundTag loadedData = null;
    
    /**
     * Load spaces after level is set (called by setLevel)
     */
    private void loadSpacesIfNeeded(HolderLookup.Provider registries) {
        if (loadedData != null && level != null) {
            diningManager.load(loadedData, level, registries);
            sleepingManager.load(loadedData, level, registries);
            serviceManager.load(loadedData, level, registries);
            customerManager.load(loadedData, level, registries);
            
            // Load tavern open/closed state
            if (loadedData.contains("manuallyOpen")) {
                manuallyOpen = loadedData.getBoolean("manuallyOpen");
            }
            
            // Load tavern sign position
            if (loadedData.contains("tavernSignPos")) {
                tavernSignPos = BlockPos.of(loadedData.getLong("tavernSignPos"));
            }
            
            loadedData = null;  // Clear after loading
        }
    }
}

