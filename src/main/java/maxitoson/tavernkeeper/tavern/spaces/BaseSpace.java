package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.areas.TavernArea;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base class for all space types
 * A Space owns an Area (1:1) and will own furniture within that area
 * 
 * Pattern: Space = Area + Furniture + Behavior
 */
public abstract class BaseSpace {
    protected final ManagerContext manager;
    protected final TavernArea area;
    
    public BaseSpace(ManagerContext manager, TavernArea area) {
        this.manager = manager;
        this.area = area;
    }
    
    /**
     * Scan the area and recognize all furniture
     * Each space type implements its own furniture recognition logic
     * @return ScanResult specific to the space type, or null if no special result
     */
    public abstract Object scanForFurniture();

    /**
     * Handle a block update at a specific position
     * Called when a block is placed in the area
     */
    public abstract void onBlockUpdated(BlockPos pos, BlockState state);

    /**
     * Handle a block being broken at a specific position
     * Called when a block is broken in the area, providing the old state
     */
    public void onBlockBroken(BlockPos pos, BlockState oldState) {
        // Default implementation just treats it as an update to AIR
        // Subclasses can override for more complex logic (like multi-block furniture)
        onBlockUpdated(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }
    
    /**
     * Get the area this space represents
     */
    public TavernArea getArea() {
        return area;
    }
    
    /**
     * Get the level this space is in
     */
    public Level getLevel() {
        return area.getLevel();
    }
    
    /**
     * Check if this space contains a position
     */
    public boolean contains(BlockPos pos) {
        return area.contains(pos);
    }
    
    /**
     * Save space state to NBT
     * Default implementation saves the area, subclasses can override to save furniture
     */
    public void save(CompoundTag tag) {
        area.save(tag);
    }
    
    /**
     * Load space from NBT
     * Subclasses should implement static load methods that call this
     */
    protected static TavernArea loadArea(CompoundTag tag, ServerLevel level) {
        return TavernArea.load(tag, level);
    }
}

