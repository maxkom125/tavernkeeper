package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Represents a chair in the dining hall
 * Supports vanilla stairs and modded furniture (e.g., Macaw's Furniture)
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
     * For Macaw's Furniture: FACING points forward, so we return it directly
     * Returns DOWN if the block doesn't have a facing property
     */
    public Direction getFacing() {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
        
        // Handle vanilla stairs
        if (blockState.getBlock() instanceof StairBlock && blockState.hasProperty(StairBlock.FACING)) {
            // For stairs, FACING points to the ascending part (backrest)
            // So the direction a player faces when sitting is the opposite
            return blockState.getValue(StairBlock.FACING).getOpposite();
        }
        
        // Handle Macaw's Furniture chairs
        if (blockId.getNamespace().equals("mcwfurnitures")) {
            // Try to find a FACING property
            for (var property : blockState.getProperties()) {
                if (property instanceof DirectionProperty dirProp && property.getName().equals("facing")) {
                    // Macaw's chairs: FACING points forward (where player looks)
                    return blockState.getValue(dirProp);
                }
            }
        }
        
        // Handle Another Furniture Mod chairs
        if (blockId.getNamespace().equals("another_furniture")) {
            // Try to find a FACING property
            for (var property : blockState.getProperties()) {
                if (property instanceof DirectionProperty dirProp && property.getName().equals("facing")) {
                    // AFM chairs: FACING points forward (where player looks)
                    return blockState.getValue(dirProp);
                }
            }
        }
        
        // Handle other modded furniture here as needed
        
        return Direction.DOWN;
    }
    
    /**
     * Get the position in front of the chair (where a table should be)
     */
    public BlockPos getFrontPos() {
        return position.relative(getFacing());
    }
}
