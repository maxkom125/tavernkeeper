package maxitoson.tavernkeeper.compat.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Interface for recognizing furniture blocks from different mods
 * Each mod compatibility implementation provides its own recognition logic
 */
public interface FurnitureRecognizer {
    /**
     * Check if this recognizer can handle the given block
     * @param state The block state to check
     * @return true if this recognizer knows this block
     */
    boolean canRecognize(BlockState state);
    
    /**
     * Recognize a block and return the furniture object (Table or Chair)
     * @param pos Block position
     * @param state Block state
     * @return Table, Chair, or null if not furniture
     */
    Object recognizeFurniture(BlockPos pos, BlockState state);
    
    /**
     * Get the mod ID this recognizer is for
     */
    String getModId();
}

