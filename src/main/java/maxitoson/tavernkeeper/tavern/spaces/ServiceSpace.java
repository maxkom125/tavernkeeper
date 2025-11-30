package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.furniture.ServiceBarrel;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service area where customers order food
 * Recognizes Lecterns (ordering stations) and Barrels (food storage)
 */
public class ServiceSpace extends BaseSpace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<ServiceLectern> lecterns;
    private final List<ServiceBarrel> barrels;
    
    public ServiceSpace(ServiceManagerContext manager, TavernArea area) {
        super(manager, area);
        this.lecterns = new ArrayList<>();
        this.barrels = new ArrayList<>();
    }
    
    @Override
    public Object scanForFurniture() {
        lecterns.clear();
        barrels.clear();
        
        Level level = area.getLevel();
        if (level == null) return null;
        
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            recognizeBlock(pos, state);
        }
        
        LOGGER.info("Scanned ServiceSpace: Found {} lecterns and {} barrels", lecterns.size(), barrels.size());
        return null;
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("ServiceSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        // Remove existing furniture at this position
        boolean removedLectern = lecterns.removeIf(l -> l.getPosition().equals(pos));
        boolean removedBarrel = barrels.removeIf(b -> b.getPosition().equals(pos));
        
        if (removedLectern) {
            LOGGER.debug("Removed lectern at {}", pos);
        }
        if (removedBarrel) {
            LOGGER.debug("Removed barrel at {}", pos);
        }
        
        // Recognize new block
        recognizeBlock(pos, state);
    }
    
    private void recognizeBlock(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LecternBlock) {
            lecterns.add(new ServiceLectern(pos, state));
            LOGGER.debug("Added lectern at {}", pos);
        } else if (block instanceof BarrelBlock) {
            barrels.add(new ServiceBarrel(pos, state));
            LOGGER.debug("Added barrel at {}", pos);
        }
    }
    
    public List<ServiceLectern> getLecterns() {
        return java.util.Collections.unmodifiableList(lecterns);
    }
    
    public List<ServiceBarrel> getBarrels() {
        return java.util.Collections.unmodifiableList(barrels);
    }
    
    public int getLecternCount() {
        return lecterns.size();
    }
    
    public int getBarrelCount() {
        return barrels.size();
    }
    
    public static ServiceSpace load(CompoundTag tag, ServiceManagerContext manager, ServerLevel level) {
        TavernArea area = loadArea(tag, level);
        ServiceSpace space = new ServiceSpace(manager, area);
        space.scanForFurniture();
        return space;
    }
}

