package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.types.SleepingFurnitureType;
import maxitoson.tavernkeeper.tavern.spaces.SleepingSpace;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager that handles all SleepingSpace instances in a tavern
 * Each SleepingSpace represents one SLEEPING area
 * 
 * Pattern: Manager extends BaseManager, owns Spaces, Spaces own Areas
 */
public class SleepingManager extends BaseDomainManager<SleepingSpace> implements SleepingManagerContext {
    
    // Track which beds are occupied by which customers (not persisted - runtime state)
    private final Map<BlockPos, UUID> occupiedBeds = new HashMap<>();
    
    // Upgrade-based limits (set by upgrade system)
    private int maxBeds;
    
    public SleepingManager(TavernContext tavern) {
        super(tavern);
        this.maxBeds = 2; // Default: Level 1 = 2 beds
    }
    
    @Override
    public AreaType getAreaType() {
        return AreaType.SLEEPING;
    }
    
    @Override
    protected SleepingSpace createSpace(String name, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        TavernArea area = new TavernArea(name, AreaType.SLEEPING, minPos, maxPos, level);
        return new SleepingSpace(this, area);
    }
    
    @Override
    protected SleepingSpace createSpaceFromNBT(CompoundTag tag, ServerLevel level) {
        return SleepingSpace.load(tag, this, level);
    }
    
    /**
     * Get total number of beds across all sleeping spaces
     */
    public int getTotalBedCount() {
        return spaces.values().stream().mapToInt(SleepingSpace::getBedCount).sum();
    }
    
    // ========== Upgrade System ==========
    
    /**
     * Get the current maximum number of beds allowed
     */
    public int getMaxBeds() {
        return maxBeds;
    }
    
    /**
     * Set the maximum number of beds (called by upgrade system)
     */
    public void setMaxBeds(int maxBeds) {
        this.maxBeds = maxBeds;
    }
    
    // ========== Bed Management ==========
    /**
     * Check if a bed is available (not occupied by another customer)
     */
    public boolean isBedAvailable(BlockPos bedPos) {
        return !occupiedBeds.containsKey(bedPos);
    }
    
    /**
     * Reserve a bed for a customer
     * Returns true if successfully reserved, false if already occupied
     */
    public boolean reserveBed(BlockPos bedPos, UUID customerId) {
        if (occupiedBeds.containsKey(bedPos)) {
            return false;
        }
        occupiedBeds.put(bedPos, customerId);
        return true;
    }
    
    /**
     * Release a bed when customer is done
     */
    public void releaseBed(BlockPos bedPos) {
        occupiedBeds.remove(bedPos);
    }
    
    /**
     * Get the customer occupying a bed, or null if not occupied
     */
    public UUID getBedOccupant(BlockPos bedPos) {
        return occupiedBeds.get(bedPos);
    }
    
    /**
     * Check if a bed exists at the given position
     * Queries all sleeping spaces to see if they have a bed at this position
     * @return true if any sleeping space has a bed at this position
     */
    public boolean hasBedAt(BlockPos bedPos) {
        return spaces.values().stream()
            .flatMap(space -> space.getBeds().stream())
            .anyMatch(pos -> pos.equals(bedPos));
    }
    
    /**
     * Check if a bed is reserved by a specific customer
     * @return true if bed is reserved by this customer, false if not reserved or reserved by another
     */
    public boolean isBedReservedBy(BlockPos bedPos, UUID customerId) {
        UUID occupant = occupiedBeds.get(bedPos);
        return occupant != null && occupant.equals(customerId);
    }
    
    /**
     * Find nearest available bed for a customer
     * Manager knows how to query its own spaces
     */
    public Optional<BlockPos> findNearestAvailableBed(
            BlockPos from, double maxDistance) {
        return spaces.values().stream()
            .flatMap(space -> space.getBeds().stream())
            .filter(bedPos -> isBedAvailable(bedPos))
            .filter(bedPos -> {
                double distSq = bedPos.distSqr(from);
                return distSq <= maxDistance * maxDistance;
            })
            .min(java.util.Comparator.comparingDouble(bedPos -> 
                bedPos.distSqr(from)
            ));
    }
    
    /**
     * Check if more furniture of the given type can be added based on current limits
     * Implements SleepingManagerContext interface
     * 
     * @param type the type of furniture to check
     * @return true if furniture can be added, false if limit reached
     */
    @Override
    public boolean canAddFurniture(SleepingFurnitureType type) {
        return switch (type) {
            case BED -> getTotalBedCount() < maxBeds;
        };
    }
}
