package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.types.ServiceFurnitureType;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import maxitoson.tavernkeeper.tavern.furniture.ServiceReceptionDesk;
import maxitoson.tavernkeeper.tavern.spaces.ServiceSpace;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Manager that handles all ServiceSpace instances in a tavern
 * Each ServiceSpace represents one SERVICE area
 */
public class ServiceManager extends BaseDomainManager<ServiceSpace> implements ServiceManagerContext {
    
    private static final int MAX_LECTERNS = 1;
    private static final int MAX_RECEPTION_DESKS = 1;
    
    public ServiceManager(TavernContext tavern) {
        super(tavern);
    }
    
    @Override
    public AreaType getAreaType() {
        return AreaType.SERVICE;
    }
    
    @Override
    protected ServiceSpace createSpace(String name, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        TavernArea area = new TavernArea(name, AreaType.SERVICE, minPos, maxPos, level);
        return new ServiceSpace(this, area);
    }
    
    @Override
    protected ServiceSpace createSpaceFromNBT(CompoundTag tag, ServerLevel level) {
        return ServiceSpace.load(tag, this, level);
    }
    
    /**
     * Get total number of lecterns across all service spaces
     */
    public int getTotalLecternCount() {
        return spaces.values().stream().mapToInt(ServiceSpace::getLecternCount).sum();
    }
    
    /**
     * Get total number of reception desks across all service spaces
     */
    public int getTotalReceptionDeskCount() {
        return spaces.values().stream().mapToInt(ServiceSpace::getReceptionDeskCount).sum();
    }
    
    /**
     * Get total number of barrels across all service spaces
     */
    public int getTotalBarrelCount() {
        return spaces.values().stream().mapToInt(ServiceSpace::getBarrelCount).sum();
    }
    
    /**
     * Find nearest service lectern
     * Manager knows how to query its own spaces
     */
    public Optional<ServiceLectern> findNearestLectern(
            BlockPos from, double maxDistance) {
        return spaces.values().stream()
            .flatMap(space -> space.getLecterns().stream())
            .filter(lectern -> {
                double distSq = lectern.getPosition().distSqr(from);
                return distSq <= maxDistance * maxDistance;
            })
            .min(java.util.Comparator.comparingDouble(lectern -> 
                lectern.getPosition().distSqr(from)
            ));
    }
    
    /**
     * Find nearest reception desk
     * Manager knows how to query its own spaces
     */
    public Optional<ServiceReceptionDesk> findNearestReceptionDesk(
            BlockPos from, double maxDistance) {
        return spaces.values().stream()
            .flatMap(space -> space.getReceptionDesks().stream())
            .filter(desk -> {
                double distSq = desk.getPosition().distSqr(from);
                return distSq <= maxDistance * maxDistance;
            })
            .min(java.util.Comparator.comparingDouble(desk -> 
                desk.getPosition().distSqr(from)
            ));
    }
    
    /**
     * Check if more furniture of the given type can be added based on current limits
     * Implements ServiceManagerContext interface
     * 
     * @param type the type of furniture to check
     * @return true if furniture can be added, false if limit reached
     */
    @Override
    public boolean canAddFurniture(ServiceFurnitureType type) {
        return switch (type) {
            case LECTERN -> getTotalLecternCount() < MAX_LECTERNS;
            case RECEPTION_DESK -> getTotalReceptionDeskCount() < MAX_RECEPTION_DESKS;
            default -> true; // No limit on other furniture types (e.g., barrels)
        };
    }
}

