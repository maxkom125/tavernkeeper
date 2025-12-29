package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.furniture.types.SleepingFurnitureType;
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
     * Result of scanning for furniture in a sleeping space
     */
    public static class ScanResult {
        private final int bedsFound;
        private final int bedsRejected;
        
        public ScanResult(int bedsFound, int bedsRejected) {
            this.bedsFound = bedsFound;
            this.bedsRejected = bedsRejected;
        }
        
        public int getBedsFound() { return bedsFound; }
        public int getBedsRejected() { return bedsRejected; }
        public boolean hadRejectedBeds() { return bedsRejected > 0; }
    }
    
    /**
     * Scan the area and recognize all sleeping furniture
     */
    @Override
    public ScanResult scanForFurniture() {
        return scanForFurnitureExcluding(null);
    }
    
    /**
     * Scan the area and recognize all sleeping furniture, excluding a specific position
     * @param excludePos position to exclude from scanning (e.g., a bed being broken)
     */
    private ScanResult scanForFurnitureExcluding(BlockPos excludePos) {
        beds.clear();
        
        Level level = area.getLevel();
        if (level == null) return new ScanResult(0, 0);
        
        SleepingManagerContext sleepingManager = (SleepingManagerContext) manager;
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        LOGGER.debug("Scanning SleepingSpace {} from {} to {}", area.getName(), minPos, maxPos);
        
        int rejectedBeds = 0;
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            // Skip the excluded position
            if (excludePos != null && pos.equals(excludePos)) {
                continue;
            }
            
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            // Check if it's a bed block - only add HEAD part (not FOOT)
            // This ensures each physical bed is counted once
            if (block instanceof BedBlock) {
                BedPart part = state.getValue(BedBlock.PART);
                if (part == BedPart.HEAD) {
                    if (sleepingManager.canAddFurniture(SleepingFurnitureType.BED)) {
                        beds.add(pos.immutable());
                        LOGGER.debug("Found bed HEAD at {}", pos);
                    } else {
                        rejectedBeds++;
                        LOGGER.debug("Rejected bed at {} - limit reached", pos);
                    }
                }
            }
        }
        LOGGER.info("Scanned SleepingSpace {}: Found {} beds ({} rejected)", 
            area.getName(), beds.size(), rejectedBeds);
            
        return new ScanResult(beds.size(), rejectedBeds);
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("SleepingSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        SleepingManagerContext sleepingManager = (SleepingManagerContext) manager;
        
        // Remove existing bed if any (might be either HEAD or FOOT position)
        // When a bed is placed/broken, we need to handle both parts
        boolean removedBed = beds.remove(pos);
        
        if (removedBed) {
            LOGGER.debug("Removed bed at {}", pos);
        }
        
        // Add if it's a bed - but ONLY if it's the HEAD part
        // This ensures each physical bed is stored once at the HEAD position
        if (state.getBlock() instanceof BedBlock) {
            BedPart part = state.getValue(BedBlock.PART);
            
            if (part == BedPart.HEAD) {
                // This is the HEAD - check limit and store it
                if (sleepingManager.canAddFurniture(SleepingFurnitureType.BED)) {
                    beds.add(pos.immutable());
                    LOGGER.debug("Added bed HEAD at {}", pos);
                } else {
                    LOGGER.debug("Rejected bed at {} - limit reached", pos);
                }
            } else {
                // This is the FOOT - find and store the HEAD position instead
                Direction direction = BedBlock.getConnectedDirection(state);
                BlockPos headPos = pos.relative(direction);
                if (!beds.contains(headPos) && sleepingManager.canAddFurniture(SleepingFurnitureType.BED)) {
                    beds.add(headPos.immutable());
                    LOGGER.debug("Added bed HEAD at {} (from FOOT at {})", headPos, pos);
                } else if (!sleepingManager.canAddFurniture(SleepingFurnitureType.BED)) {
                    LOGGER.debug("Rejected bed at {} - limit reached", headPos);
                }
            }
        } else if (removedBed) {
            // We removed a bed and it's not being replaced with another bed
            // Rescan to pick up any previously rejected beds
            LOGGER.debug("Bed removed at {} - rescanning area", pos);
            scanForFurniture();
            return;
        }
        LOGGER.debug("SleepingSpace {} now has {} beds", area.getName(), beds.size());
    }
    
    @Override
    public void onBlockBroken(BlockPos pos, BlockState oldState) {
        // When a bed is broken, we need to handle both HEAD and FOOT parts
        if (oldState.getBlock() instanceof BedBlock) {
            BedPart part = oldState.getValue(BedBlock.PART);
            BlockPos headPos;
            
            if (part == BedPart.HEAD) {
                // Broken block is HEAD - remove it directly
                headPos = pos;
                if (beds.remove(pos)) {
                    LOGGER.debug("Broken bed HEAD removed at {}", pos);
                }
            } else {
                // Broken block is FOOT - find and remove the HEAD position
                Direction direction = BedBlock.getConnectedDirection(oldState);
                headPos = pos.relative(direction);
                if (beds.remove(headPos)) {
                    LOGGER.debug("Broken bed HEAD removed at {} (FOOT broken at {})", headPos, pos);
                }
            }
            
            // After removing, rescan to pick up previously rejected beds
            // BUT: exclude the HEAD position since the bed is still in the world during this event
            LOGGER.debug("Bed broken at {} - rescanning area (excluding HEAD at {})", pos, headPos);
            scanForFurnitureExcluding(headPos);
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

