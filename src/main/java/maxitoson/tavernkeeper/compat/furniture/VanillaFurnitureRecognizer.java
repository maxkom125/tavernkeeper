package maxitoson.tavernkeeper.compat.furniture;

import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.furniture.Table;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Recognizes vanilla Minecraft furniture (stairs as chairs/tables)
 */
public class VanillaFurnitureRecognizer implements FurnitureRecognizer {
    
    @Override
    public boolean canRecognize(BlockState state) {
        return state.getBlock() instanceof StairBlock;
    }
    
    @Override
    public Object recognizeFurniture(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof StairBlock)) {
            return null;
        }
        
        if (!state.hasProperty(StairBlock.HALF)) {
            return null;
        }
        
        Half half = state.getValue(StairBlock.HALF);
        
        if (half == Half.TOP) {
            // Upside-down stairs = Table
            return new Table(pos, state);
        } else {
            // Normal stairs = Chair
            return new Chair(pos, state);
        }
    }
    
    @Override
    public String getModId() {
        return "minecraft";
    }
}

