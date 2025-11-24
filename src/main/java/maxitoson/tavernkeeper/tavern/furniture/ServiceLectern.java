package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a lectern in the service area
 * Used as a queue point/ordering station for customers
 */
public class ServiceLectern {
    private final BlockPos position;
    private final BlockState blockState;
    
    public ServiceLectern(BlockPos position, BlockState blockState) {
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

