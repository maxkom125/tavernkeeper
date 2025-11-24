package maxitoson.tavernkeeper.tavern.managers;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.spaces.SleepingManagerContext;
import maxitoson.tavernkeeper.tavern.spaces.SleepingSpace;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/**
 * Manager that handles all SleepingSpace instances in a tavern
 * Each SleepingSpace represents one SLEEPING area
 * 
 * Pattern: Manager extends BaseManager, owns Spaces, Spaces own Areas
 */
public class SleepingManager extends BaseManager<SleepingSpace> implements SleepingManagerContext {
    
    public SleepingManager(TavernContext tavern) {
        super(tavern);
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
}
