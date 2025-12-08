package maxitoson.tavernkeeper.entities.ai.behavior;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.ServiceReceptionDesk;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Moves sleeping customers to the nearest reception desk in a service area
 * After reaching reception, transitions to WAITING_RECEPTION state
 */
public class MoveToReceptionDesk extends MoveToTargetBehavior {
    private static final int REACHED_DISTANCE = 2;
    
    public MoveToReceptionDesk(float speedModifier) {
        super(speedModifier);
    }
    
    @Override
    protected boolean isInCorrectState(CustomerEntity customer) {
        // Only for customers looking for reception desk
        return customer.getCustomerState() == CustomerState.FINDING_RECEPTION;
    }
    
    @Override
    protected Optional<BlockPos> findTarget(TavernContext tavern, BlockPos from, double maxDistance) {
        return tavern.findNearestReceptionDesk(from, maxDistance)
                     .map(ServiceReceptionDesk::getPosition);
    }
    
    @Override
    protected void onTargetReached(ServerLevel level, CustomerEntity customer, BlockPos target) {
        customer.setTargetPosition(target);
        customer.transitionToNextState(level);
    }
    
    @Override
    protected String getTargetName() {
        return "reception desk";
    }
    
    @Override
    protected int getReachedDistance() {
        return REACHED_DISTANCE;
    }
}

