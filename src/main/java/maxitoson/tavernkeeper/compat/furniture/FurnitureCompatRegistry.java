package maxitoson.tavernkeeper.compat.furniture;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry for furniture recognizers from different mods
 * Manages soft dependencies and provides unified furniture recognition
 */
public class FurnitureCompatRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<FurnitureRecognizer> recognizers = new ArrayList<>();
    private static boolean initialized = false;
    
    /**
     * Initialize all available furniture recognizers based on loaded mods
     */
    public static void init() {
        if (initialized) {
            return;
        }
        
        LOGGER.info("Initializing furniture compatibility registry...");
        
        // Always add vanilla recognizer
        recognizers.add(new VanillaFurnitureRecognizer());
        LOGGER.info("Registered vanilla furniture recognizer");
        
        // Add Macaw's Furniture if present
        if (ModList.get().isLoaded("mcwfurnitures")) {
            recognizers.add(new MacawsFurnitureRecognizer());
            LOGGER.info("Registered Macaw's Furniture recognizer");
        }
        
        // Add Another Furniture Mod if present
        if (ModList.get().isLoaded("another_furniture")) {
            recognizers.add(new AnotherFurnitureRecognizer());
            LOGGER.info("Registered Another Furniture Mod recognizer");
        }
        
        // Add more recognizers here as we support more mods
        
        initialized = true;
        LOGGER.info("Furniture compatibility registry initialized with {} recognizers", recognizers.size());
    }
    
    /**
     * Try to recognize a block as furniture using all registered recognizers
     * @param pos Block position
     * @param state Block state
     * @return Table, Chair, or null if not recognized
     */
    public static Object recognizeFurniture(BlockPos pos, BlockState state) {
        for (FurnitureRecognizer recognizer : recognizers) {
            if (recognizer.canRecognize(state)) {
                Object furniture = recognizer.recognizeFurniture(pos, state);
                if (furniture != null) {
                    return furniture;
                }
            }
        }
        return null;
    }
    
    /**
     * Check if any recognizer can handle this block
     */
    public static boolean canRecognize(BlockState state) {
        for (FurnitureRecognizer recognizer : recognizers) {
            if (recognizer.canRecognize(state)) {
                return true;
            }
        }
        return false;
    }
}

