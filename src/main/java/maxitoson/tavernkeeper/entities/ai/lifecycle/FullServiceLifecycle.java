package maxitoson.tavernkeeper.entities.ai.lifecycle;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Full service lifecycle: Eat then sleep
 * Journey: Lectern → Chair → Reception → Bed → Leave
 * 
 * Customer eats first, then goes to reception desk for sleeping
 */
public class FullServiceLifecycle implements CustomerLifecycle {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public CustomerState getInitialState() {
        return CustomerState.FINDING_LECTERN;
    }
    
    @Override
    public CustomerState getNextState(CustomerState current, ServerLevel level) {
        return switch (current) {
            case FINDING_LECTERN -> CustomerState.WAITING_SERVICE;
            case WAITING_SERVICE -> CustomerState.FINDING_SEAT;
            case FINDING_SEAT -> CustomerState.EATING;
            case EATING -> CustomerState.FINDING_RECEPTION; // After eating, find reception desk
            case FINDING_RECEPTION -> CustomerState.WAITING_RECEPTION;
            case WAITING_RECEPTION -> CustomerState.FINDING_BED;
            case FINDING_BED -> CustomerState.SLEEPING;
            case SLEEPING -> CustomerState.LEAVING;
            default -> {
                LOGGER.warn("FULL_SERVICE lifecycle in unexpected state: {}", current);
                yield CustomerState.LEAVING;
            }
        };
    }
    
    @Override
    public LifecycleType getType() {
        return LifecycleType.FULL_SERVICE;
    }
    
    @Override
    public String getDescription() {
        return "Full Service (Food → Reception → Bed → Leave)";
    }
}

