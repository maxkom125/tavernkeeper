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
 * Recognizes furniture blocks from Macaw's Furniture mod
 * Uses registry ID matching to avoid compile-time dependency
 */
public class MacawsFurnitureRecognizer implements FurnitureRecognizer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "mcwfurnitures";
    
    @Override
    public boolean canRecognize(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return blockId.getNamespace().equals(MOD_ID);
    }
    
    @Override
    public Object recognizeFurniture(BlockPos pos, BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = blockId.getPath();
        
        // Macaw's Furniture uses consistent naming conventions
        // Chairs: contains "chair" in the name
        // Tables: contains "table" or "desk" in the name
        
        if (path.contains("chair") || path.contains("stool")) {
            LOGGER.debug("Recognized Macaw's chair: {} at {}", blockId, pos);
            return new Chair(pos, state);
        } else if (path.contains("table") || path.contains("desk") || path.contains("counter")) {
            LOGGER.debug("Recognized Macaw's table: {} at {}", blockId, pos);
            return new Table(pos, state);
        }
        
        return null;
    }
    
    @Override
    public String getModId() {
        return MOD_ID;
    }
}

