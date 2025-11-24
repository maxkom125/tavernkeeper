package maxitoson.tavernkeeper.tavern.furniture;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Represents a barrel in the service area
 * Used as food storage for serving customers
 */
public class ServiceBarrel {
    private final BlockPos position;
    private final BlockState blockState;
    
    public ServiceBarrel(BlockPos position, BlockState blockState) {
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

