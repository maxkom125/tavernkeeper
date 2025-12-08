package maxitoson.tavernkeeper.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Reception Desk block for customers to request sleeping services
 * Uses enchanting table texture and similar properties
 */
public class ReceptionDeskBlock extends Block {
    // Same shape as enchanting table (from EnchantingTableBlock)
    protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);
    
    public ReceptionDeskBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

