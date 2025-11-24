package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a chair in the dining hall
 * Currently recognized as normal stairs (not upside-down)
 */
public class Chair {
    private final BlockPos position;
    private final BlockState blockState;
    private boolean isValid; // A chair is valid if it faces a table
    
    public Chair(BlockPos position, BlockState blockState) {
        this.position = position.immutable();
        this.blockState = blockState;
        this.isValid = false; // Default to false, validation happens in Space
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public BlockState getBlockState() {
        return blockState;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        this.isValid = valid;
    }
    
    /**
     * Get the direction the chair is facing (where a player would look when sitting)
     * For Stairs: FACING points to the backrest, so we return the OPPOSITE
     * Returns DOWN if the block doesn't have a facing property
     * 
     * TODO: Add support for other chair types (e.g., Macaw's Furniture) here
     */
    public Direction getFacing() {
        if (blockState.getBlock() instanceof StairBlock && blockState.hasProperty(StairBlock.FACING)) {
            // For stairs, FACING points to the ascending part (backrest)
            // So the direction a player faces when sitting is the opposite
            return blockState.getValue(StairBlock.FACING).getOpposite();
        }
        return Direction.DOWN;
    }
    
    /**
     * Get the position in front of the chair (where a table should be)
     */
    public BlockPos getFrontPos() {
        return position.relative(getFacing());
    }
}
