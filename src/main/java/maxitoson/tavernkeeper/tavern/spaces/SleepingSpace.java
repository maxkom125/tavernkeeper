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
import net.minecraft.world.level.block.state.properties.BedPart;
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
            
            // Check if it's a bed block - only add HEAD part (not FOOT)
            // This ensures each physical bed is counted once
            if (block instanceof BedBlock) {
                BedPart part = state.getValue(BedBlock.PART);
                if (part == BedPart.HEAD) {
                    beds.add(pos.immutable());
                    LOGGER.debug("Found bed HEAD at {}", pos);
                }
            }
        }
        LOGGER.info("Scanned SleepingSpace {}: Found {} beds", area.getName(), beds.size());
        return null;
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("SleepingSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        // Remove existing bed if any (might be either HEAD or FOOT position)
        // When a bed is placed/broken, we need to handle both parts
        if (beds.remove(pos)) {
             LOGGER.debug("Removed bed at {}", pos);
        }
        
        // Add if it's a bed - but ONLY if it's the HEAD part
        // This ensures each physical bed is stored once at the HEAD position
        if (state.getBlock() instanceof BedBlock) {
            BedPart part = state.getValue(BedBlock.PART);
            
            if (part == BedPart.HEAD) {
                // This is the HEAD - store it
                beds.add(pos.immutable());
                LOGGER.debug("Added bed HEAD at {}", pos);
            } else {
                // This is the FOOT - find and store the HEAD position instead
                Direction direction = BedBlock.getConnectedDirection(state);
                BlockPos headPos = pos.relative(direction);
                if (!beds.contains(headPos)) {
                    beds.add(headPos.immutable());
                    LOGGER.debug("Added bed HEAD at {} (from FOOT at {})", headPos, pos);
                }
            }
        }
        LOGGER.debug("SleepingSpace {} now has {} beds", area.getName(), beds.size());
    }
    
    @Override
    public void onBlockBroken(BlockPos pos, BlockState oldState) {
        // When a bed is broken, remove the HEAD position from our list
        // (since we only store HEAD positions, not FOOT)
        if (oldState.getBlock() instanceof BedBlock) {
            BedPart part = oldState.getValue(BedBlock.PART);
            
            if (part == BedPart.HEAD) {
                // Broken block is HEAD - remove it directly
                if (beds.remove(pos)) {
                    LOGGER.debug("Broken bed HEAD removed at {}", pos);
                }
            } else {
                // Broken block is FOOT - find and remove the HEAD position
                Direction direction = BedBlock.getConnectedDirection(oldState);
                BlockPos headPos = pos.relative(direction);
                if (beds.remove(headPos)) {
                    LOGGER.debug("Broken bed HEAD removed at {} (FOOT broken at {})", headPos, pos);
                }
            }
        } else {
            // Not a bed, just remove position (shouldn't happen, but safe fallback)
            LOGGER.warn("SleepingSpace {} broken block at {} is not a bed", area.getName(), pos);
            beds.remove(pos);
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

