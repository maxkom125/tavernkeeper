package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a reception desk in a service area
 * Used for customers requesting sleeping services
 */
public class ServiceReceptionDesk {
    private final BlockPos position;
    private final BlockState blockState;
    
    public ServiceReceptionDesk(BlockPos position, BlockState blockState) {
        this.position = position;
        this.blockState = blockState;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public BlockState getBlockState() {
        return blockState;
    }
}

