package maxitoson.tavernkeeper.tavern.managers.domain;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.managers.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import maxitoson.tavernkeeper.tavern.spaces.ServiceManagerContext;
import maxitoson.tavernkeeper.tavern.spaces.ServiceSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Manager that handles all ServiceSpace instances in a tavern
 * Each ServiceSpace represents one SERVICE area
 */
public class ServiceManager extends BaseDomainManager<ServiceSpace> implements ServiceManagerContext {
    
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
     * Get total number of barrels across all service spaces
     */
    public int getTotalBarrelCount() {
        return spaces.values().stream().mapToInt(ServiceSpace::getBarrelCount).sum();
    }
    
    /**
     * Find nearest service lectern
     * Manager knows how to query its own spaces
     */
    public java.util.Optional<ServiceLectern> findNearestLectern(
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
}

