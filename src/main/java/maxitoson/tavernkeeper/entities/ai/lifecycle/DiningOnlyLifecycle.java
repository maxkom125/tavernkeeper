package maxitoson.tavernkeeper.entities.ai.lifecycle;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Simple dining lifecycle: Eat and leave
 * Journey: Lectern → Chair → Leave
 * 
 */
public class DiningOnlyLifecycle implements CustomerLifecycle {
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
            case EATING -> CustomerState.LEAVING;
            default -> {
                LOGGER.warn("DINING_ONLY lifecycle in unexpected state: {}", current);
                yield CustomerState.LEAVING;
            }
        };
    }
    
    @Override
    public LifecycleType getType() {
        return LifecycleType.DINING_ONLY;
    }
    
    @Override
    public String getDescription() {
        return "Dining Only (Food → Leave)";
    }
}

