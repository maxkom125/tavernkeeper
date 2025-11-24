package maxitoson.tavernkeeper.tavern.managers;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
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
public class DiningManager extends BaseManager<DiningSpace> implements DiningManagerContext {
    
    // Track which customer is occupying which chair
    private final Map<BlockPos, UUID> occupiedChairs = new HashMap<>();
    
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
    public java.util.Optional<maxitoson.tavernkeeper.tavern.furniture.Chair> findNearestAvailableChair(
            BlockPos from, double maxDistance) {
        return spaces.values().stream()
            .flatMap(space -> space.getChairs().stream())
            .filter(maxitoson.tavernkeeper.tavern.furniture.Chair::isValid)
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

