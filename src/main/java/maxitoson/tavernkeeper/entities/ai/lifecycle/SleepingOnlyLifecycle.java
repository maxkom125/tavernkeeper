package maxitoson.tavernkeeper.entities.ai.lifecycle;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * Simple sleeping lifecycle: Pay and sleep
 * Journey: Reception → Bed → Leave
 * 
 * Customer goes directly to reception desk, pays for room, and sleeps
 */
public class SleepingOnlyLifecycle implements CustomerLifecycle {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @Override
    public CustomerState getInitialState() {
        return CustomerState.FINDING_RECEPTION;
    }
    
    @Override
    public CustomerState getNextState(CustomerState current, ServerLevel level) {
        return switch (current) {
            case FINDING_RECEPTION -> CustomerState.WAITING_RECEPTION;
            case WAITING_RECEPTION -> CustomerState.FINDING_BED;
            case FINDING_BED -> CustomerState.SLEEPING;
            case SLEEPING -> CustomerState.LEAVING;
            default -> {
                LOGGER.warn("SLEEPING_ONLY lifecycle in unexpected state: {}", current);
                yield CustomerState.LEAVING;
            }
        };
    }
    
    @Override
    public LifecycleType getType() {
        return LifecycleType.SLEEPING_ONLY;
    }
    
    @Override
    public String getDescription() {
        return "Sleeping Only (Reception → Bed → Leave)";
    }
}

