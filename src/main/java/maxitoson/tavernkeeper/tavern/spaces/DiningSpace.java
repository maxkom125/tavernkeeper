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
     * Result of scanning for furniture in a dining space
     */
    public static class ScanResult {
        private final int tablesFound;
        private final int tablesRejected;
        private final int chairsFound;
        private final int chairsRejected;
        private final int validChairs;
        
        public ScanResult(int tablesFound, int tablesRejected, int chairsFound, int chairsRejected, int validChairs) {
            this.tablesFound = tablesFound;
            this.tablesRejected = tablesRejected;
            this.chairsFound = chairsFound;
            this.chairsRejected = chairsRejected;
            this.validChairs = validChairs;
        }
        
        public int getTablesFound() { return tablesFound; }
        public int getTablesRejected() { return tablesRejected; }
        public int getChairsFound() { return chairsFound; }
        public int getChairsRejected() { return chairsRejected; }
        public int getValidChairs() { return validChairs; }
        public boolean hadRejectedTables() { return tablesRejected > 0; }
        public boolean hadRejectedChairs() { return chairsRejected > 0; }
    }
    
    /**
     * Scan the area and recognize all dining furniture
     */
    @Override
    public ScanResult scanForFurniture() {
        tables.clear();
        chairs.clear();
        int rejectedTables = 0;
        int rejectedChairs = 0;
        
        Level level = area.getLevel();
        if (level == null) return new ScanResult(0, 0, 0, 0, 0);
        
        DiningManagerContext diningManager = (DiningManagerContext) manager;
        
        // Iterate through all blocks in the area
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            Object furniture = null;
            
            // Check if block is a furniture block
            if (block instanceof StairBlock) {
                furniture = recognizeStair(pos, state);
            }
            // TODO: Add more furniture blocks here

            if (furniture == null) {
                continue;
            }

            // Check if it's a table or chair
            if (furniture instanceof Table table) {
                // It's a table - check if we can add it
                if (diningManager.canAddTable()) {
                    tables.add(table);
                    LOGGER.debug("Added table at {}", pos);
                } else {
                    rejectedTables++;
                    LOGGER.debug("Rejected table at {} - limit reached", pos);
                }
            } else if (furniture instanceof Chair chair) {
                // It's a chair - check if we can add it
                if (diningManager.canAddChair()) {
                    chairs.add(chair);
                    LOGGER.debug("Added chair at {} (will validate)", pos);
                } else {
                    rejectedChairs++;
                    LOGGER.debug("Rejected chair at {} - limit reached", pos);
                }
            }
        }
        
        // After scanning all blocks, validate chairs
        validateChairs();

        LOGGER.info("Scanned DiningSpace: Found {} tables ({} rejected), {} chairs ({} rejected, {} valid)", 
            tables.size(), rejectedTables, chairs.size(), rejectedChairs, getValidChairCount());
            
        return new ScanResult(tables.size(), rejectedTables, chairs.size(), rejectedChairs, getValidChairCount());
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
        
        // If it's a stair block, recognize and add it
        if (state.getBlock() instanceof StairBlock) {
            Object furniture = recognizeStair(pos, state);
            DiningManagerContext diningManager = (DiningManagerContext) manager;
            
            if (furniture instanceof Table table && diningManager.canAddTable()) {
                tables.add(table);
                LOGGER.debug("Added table at {}", pos);
            } else if (furniture instanceof Chair chair && diningManager.canAddChair()) {
                chairs.add(chair);
                LOGGER.debug("Added chair at {}", pos);
            }
        }
        
        // Re-validate all chairs since a table might have been placed/removed
        validateChairs();
    }
    
    /**
     * Recognize a stair block and return the furniture object (Table or Chair)
     * @return Table if upside-down stairs, Chair if normal stairs, null otherwise
     */
    private Object recognizeStair(BlockPos pos, BlockState state) {
        if (!state.hasProperty(StairBlock.HALF)) {
            return null;
        }
        
        Half half = state.getValue(StairBlock.HALF);
        
        if (half == Half.TOP) {
            // Upside-down stairs = Table
            return new Table(pos, state);
        } else {
            // Normal stairs = Chair
            return new Chair(pos, state);
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

