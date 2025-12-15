package maxitoson.tavernkeeper.entities.ai.behavior;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

/**
 * Finds and walks to the nearest available chair in a dining area
 * After reaching chair, transitions to EATING state
 * Reserves the chair to prevent other customers from taking it
 */
public class FindSeat extends MoveToTargetBehavior {
    private static final int REACHED_DISTANCE = 2;
    
    public FindSeat(float speedModifier) {
        super(speedModifier);
    }
    
    @Override
    protected boolean isInCorrectState(CustomerEntity customer) {
        return customer.getCustomerState() == CustomerState.FINDING_SEAT;
    }
    
    @Override
    protected Optional<BlockPos> findTarget(TavernContext tavern, BlockPos from, double maxDistance) {
        return tavern.findNearestAvailableChair(from, maxDistance)
                     .map(Chair::getPosition);
    }
    
    @Override
    protected boolean tryReserveTarget(TavernContext tavern, BlockPos target, CustomerEntity customer) {
        return tavern.reserveChair(target, customer.getUUID());
    }
    
    @Override
    protected void releaseTarget(TavernContext tavern, BlockPos target) {
        tavern.releaseChair(target);
    }
    
    @Override
    protected void onTargetReached(ServerLevel level, CustomerEntity customer, BlockPos target) {
        customer.setTargetPosition(target);
        customer.transitionToNextState(level);
    }
    
    @Override
    protected String getTargetName() {
        return "chair";
    }
    
    @Override
    protected int getReachedDistance() {
        return REACHED_DISTANCE;
    }
}
