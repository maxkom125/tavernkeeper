package maxitoson.tavernkeeper.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Reception Desk block for customers to request sleeping services
 * Based on lectern structure with directional facing
 */
public class ReceptionDeskBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Simple collision shape: full block base + small book on top
    private static final VoxelShape BASE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape BOOK = Block.box(4.0, 16.0, 3.0, 12.0, 18.0, 13.0);
    protected static final VoxelShape SHAPE = Shapes.or(BASE, BOOK);
    
    public ReceptionDeskBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

