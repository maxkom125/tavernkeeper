package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.managers.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.spaces.DiningManagerContext;
import maxitoson.tavernkeeper.tavern.spaces.DiningSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manager that handles all DiningSpace instances in a tavern
 * Each DiningSpace represents one DINING area
 * 
 * Pattern: Manager extends BaseManager, owns Spaces, Spaces own Areas
 */
public class DiningManager extends BaseDomainManager<DiningSpace> implements DiningManagerContext {
    
    // Track which customer is occupying which chair
    private final Map<BlockPos, UUID> occupiedChairs = new HashMap<>();
    
    // Upgrade-based limits (set by upgrade system)
    private int maxTables;
    private int maxChairs;
    
    public DiningManager(TavernContext tavern) {
        super(tavern);
    }
    
    @Override
    public AreaType getAreaType() {
        return AreaType.DINING;
    }
    
    @Override
    protected DiningSpace createSpace(String name, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        TavernArea area = new TavernArea(name, AreaType.DINING, minPos, maxPos, level);
        return new DiningSpace(this, area);
    }
    
    @Override
    protected DiningSpace createSpaceFromNBT(CompoundTag tag, ServerLevel level) {
        return DiningSpace.load(tag, this, level);
    }
    
    /**
     * Get total number of tables across all dining spaces
     */
    public int getTotalTableCount() {
        return spaces.values().stream().mapToInt(DiningSpace::getTableCount).sum();
    }
    
    /**
     * Get total number of chairs across all dining spaces
     */
    public int getTotalChairCount() {
        return spaces.values().stream().mapToInt(DiningSpace::getChairCount).sum();
    }
    
    // ========== Upgrade System ==========
    
    /**
     * Get the current maximum number of tables allowed
     */
    public int getMaxTables() {
        return maxTables;
    }
    
    /**
     * Set the maximum number of tables (called by upgrade system)
     */
    public void setMaxTables(int maxTables) {
        this.maxTables = maxTables;
    }
    
    /**
     * Get the current maximum number of chairs allowed
     */
    public int getMaxChairs() {
        return maxChairs;
    }
    
    /**
     * Set the maximum number of chairs (called by upgrade system)
     */
    public void setMaxChairs(int maxChairs) {
        this.maxChairs = maxChairs;
    }
    
    /**
     * Check if more tables can be added based on current upgrade level
     * Implements DiningManagerContext interface
     */
    @Override
    public boolean canAddTable() {
        return getTotalTableCount() < maxTables;
    }
    
    /**
     * Check if more chairs can be added based on current upgrade level
     * Implements DiningManagerContext interface
     */
    @Override
    public boolean canAddChair() {
        return getTotalChairCount() < maxChairs;
    }
    
    /**
     * Check if a chair is available (not occupied by another customer)
     */
    public boolean isChairAvailable(BlockPos chairPos) {
        return !occupiedChairs.containsKey(chairPos);
    }
    
    /**
     * Reserve a chair for a customer
     * Returns true if successfully reserved, false if already occupied
     */
    public boolean reserveChair(BlockPos chairPos, UUID customerId) {
        if (occupiedChairs.containsKey(chairPos)) {
            return false;
        }
        occupiedChairs.put(chairPos, customerId);
        return true;
    }
    
    /**
     * Release a chair when customer is done
     */
    public void releaseChair(BlockPos chairPos) {
        occupiedChairs.remove(chairPos);
    }
    
    /**
     * Get the customer occupying a chair, or null if not occupied
     */
    public UUID getChairOccupant(BlockPos chairPos) {
        return occupiedChairs.get(chairPos);
    }
    
    /**
     * Find nearest available chair for a customer
     * Manager knows how to query its own spaces
     */
    public java.util.Optional<Chair> findNearestAvailableChair(
            BlockPos from, double maxDistance) {
        return spaces.values().stream()
            .flatMap(space -> space.getChairs().stream())
            .filter(Chair::isValid)
            .filter(chair -> isChairAvailable(chair.getPosition()))
            .filter(chair -> {
                double distSq = chair.getPosition().distSqr(from);
                return distSq <= maxDistance * maxDistance;
            })
            .min(java.util.Comparator.comparingDouble(chair -> 
                chair.getPosition().distSqr(from)
            ));
    }
}

