package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.managers.domain.SleepingManagerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single sleeping area with its beds
 * Currently recognizes vanilla beds
 * 
 * Pattern: Space owns Area (1:1) and Furniture
 */
public class SleepingSpace extends BaseSpace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<BlockPos> beds;
    
    public SleepingSpace(SleepingManagerContext sleepingManager, TavernArea area) {
        super(sleepingManager, area);
        this.beds = new ArrayList<>();
    }
    
    /**
     * Scan the area and recognize all sleeping furniture
     */
    @Override
    public Object scanForFurniture() {
        beds.clear();
        
        Level level = area.getLevel();
        if (level == null) return null;
        
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        LOGGER.debug("Scanning SleepingSpace {} from {} to {}", area.getName(), minPos, maxPos);
        
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            // Check if it's a bed block
            if (block instanceof BedBlock) {
                beds.add(pos.immutable());
                LOGGER.debug("Found existing bed at {}", pos);
            }
        }
        LOGGER.info("Scanned SleepingSpace {}: Found {} beds", area.getName(), beds.size());
        return null;
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("SleepingSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        // Remove existing bed if any
        if (beds.remove(pos)) {
             LOGGER.debug("Removed bed part at {}", pos);
        }
        
        // Add if it's a bed
        if (state.getBlock() instanceof BedBlock) {
            beds.add(pos.immutable());
            LOGGER.debug("Added bed part at {}", pos);
            
            // Also add the connected part (Head/Foot)
            // Because placing a bed places both, but event might only fire for one
            Direction direction = BedBlock.getConnectedDirection(state);
            BlockPos otherPartPos = pos.relative(direction);
            if (!beds.contains(otherPartPos)) {
                beds.add(otherPartPos.immutable());
                LOGGER.debug("Added connected bed part at {}", otherPartPos);
            }
        }
        LOGGER.debug("SleepingSpace {} now has {} beds", area.getName(), beds.size());
    }
    
    @Override
    public void onBlockBroken(BlockPos pos, BlockState oldState) {
        // Remove the broken block
        if (beds.remove(pos)) {
            LOGGER.debug("Broken bed part removed at {}", pos);
        }
        
        // If it was a bed, remove the connected part too
        if (oldState.getBlock() instanceof BedBlock) {
            Direction direction = BedBlock.getConnectedDirection(oldState);
            BlockPos otherPartPos = pos.relative(direction);
            if (beds.remove(otherPartPos)) {
                LOGGER.debug("Connected bed part removed at {}", otherPartPos);
            }
        }
        LOGGER.debug("SleepingSpace {} now has {} beds", area.getName(), beds.size());
    }
    
    public List<BlockPos> getBeds() {
        return java.util.Collections.unmodifiableList(beds);
    }
    
    public int getBedCount() {
        return beds.size();
    }
    
    /**
     * Load a SleepingSpace from NBT
     */
    public static SleepingSpace load(CompoundTag tag, SleepingManagerContext manager, ServerLevel level) {
        TavernArea area = loadArea(tag, level);
        SleepingSpace space = new SleepingSpace(manager, area);
        space.scanForFurniture();
        return space;
    }
}

