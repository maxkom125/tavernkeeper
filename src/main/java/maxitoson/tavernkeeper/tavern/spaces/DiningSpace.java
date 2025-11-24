package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.furniture.Table;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single dining area with its tables and chairs
 * Recognizes stairs as chairs (normal) and tables (upside-down)
 * 
 * Pattern: Space owns Area (1:1) and Furniture
 */
public class DiningSpace extends BaseSpace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<Table> tables;
    private final List<Chair> chairs;
    
    public DiningSpace(DiningManagerContext diningManager, TavernArea area) {
        super(diningManager, area);
        this.tables = new ArrayList<>();
        this.chairs = new ArrayList<>();
    }
    
    /**
     * Scan the area and recognize all dining furniture
     */
    @Override
    public void scanForFurniture() {
        tables.clear();
        chairs.clear();
        
        Level level = area.getLevel();
        if (level == null) return;
        
        // Iterate through all blocks in the area
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            
            // Check if it's a stair block
            if (block instanceof StairBlock) {
                recognizeStair(pos, state);
            }
        }
        
        // After scanning all blocks, validate chairs
        validateChairs();

        LOGGER.info("Scanned DiningSpace: Found {} tables and {} chairs ({} valid)", 
            tables.size(), chairs.size(), getValidChairCount());
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("DiningSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        // Remove any existing furniture at this position
        boolean removedTable = tables.removeIf(t -> t.getPosition().equals(pos));
        boolean removedChair = chairs.removeIf(c -> c.getPosition().equals(pos));
        
        if (removedTable) {
            LOGGER.debug("Removed table at {}", pos);
        }
        if (removedChair) {
            LOGGER.debug("Removed chair at {}", pos);
        }
        
        // If it's a stair block, recognize it
        if (state.getBlock() instanceof StairBlock) {
            recognizeStair(pos, state);
        }
        
        // Re-validate all chairs since a table might have been placed/removed
        validateChairs();
    }
    
    /**
     * Recognize a stair as either a table (upside-down) or chair (normal)
     */
    private void recognizeStair(BlockPos pos, BlockState state) {
        if (state.hasProperty(StairBlock.HALF)) {
            Half half = state.getValue(StairBlock.HALF);
            
            if (half == Half.TOP) {
                // Upside-down stairs = Table
                tables.add(new Table(pos, state));
                LOGGER.debug("Added table at {}", pos);
            } else {
                // Normal stairs = Chair
                chairs.add(new Chair(pos, state));
                LOGGER.debug("Added chair at {} (will validate)", pos);
            }
        }
    }
    
    /**
     * Validate that each chair is facing a table
     */
    private void validateChairs() {
        int validCount = 0;
        for (Chair chair : chairs) {
            BlockPos tableMustBePos = chair.getFrontPos();
            boolean hasTable = tables.stream()
                .anyMatch(t -> t.getPosition().equals(tableMustBePos));
            
            chair.setValid(hasTable);
            if (hasTable) {
                validCount++;
                LOGGER.debug("Chair at {} is VALID (faces table at {})", chair.getPosition(), tableMustBePos);
            } else {
                LOGGER.debug("Chair at {} is INVALID (no table at {})", chair.getPosition(), tableMustBePos);
            }
        }
        LOGGER.debug("Chair validation complete: {}/{} valid", validCount, chairs.size());
    }
    
    public List<Table> getTables() {
        return java.util.Collections.unmodifiableList(tables);
    }
    
    public List<Chair> getChairs() {
        return java.util.Collections.unmodifiableList(chairs);
    }
    
    public int getTableCount() {
        return tables.size();
    }
    
    public int getChairCount() {
        return chairs.size();
    }
    
    public int getValidChairCount() {
        return (int) chairs.stream().filter(Chair::isValid).count();
    }
    
    /**
     * Load a DiningSpace from NBT
     */
    public static DiningSpace load(CompoundTag tag, DiningManagerContext manager, ServerLevel level) {
        TavernArea area = loadArea(tag, level);
        DiningSpace space = new DiningSpace(manager, area);
        space.scanForFurniture();
        return space;
    }
}

