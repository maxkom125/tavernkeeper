package maxitoson.tavernkeeper.compat.furniture;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.furniture.Table;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * Recognizes furniture blocks from Another Furniture Mod
 * Uses registry ID matching to avoid compile-time dependency
 */
public class AnotherFurnitureRecognizer implements FurnitureRecognizer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "another_furniture";
    
    @Override
    public boolean canRecognize(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId.getNamespace().equals(MOD_ID);
    }
    
    @Override
    public Object recognizeFurniture(BlockPos pos, BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();
        
        // Another Furniture Mod naming conventions:
        // Chairs: chair, stool, tall_stool
        // Tables: table
        // Note: benches and sofas are not included as they're multi-block and work differently
        
        if (path.contains("chair") || path.contains("stool")) {
            LOGGER.debug("Recognized Another Furniture chair: {} at {}", blockId, pos);
            return new Chair(pos, state);
        } else if (path.contains("table")) {
            LOGGER.debug("Recognized Another Furniture table: {} at {}", blockId, pos);
            return new Table(pos, state);
        }
        
        return null;
    }
    
    @Override
    public String getModId() {
        return MOD_ID;
    }
}

