package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a table in the dining hall
 * Currently recognized as upside-down stairs
 */
public class Table {
    private final BlockPos position;
    private final BlockState blockState;
    
    public Table(BlockPos position, BlockState blockState) {
        this.position = position.immutable();
        this.blockState = blockState;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public BlockState getBlockState() {
        return blockState;
    }
}

