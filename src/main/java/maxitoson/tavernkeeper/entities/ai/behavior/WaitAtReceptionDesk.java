package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Behavior for customer waiting at reception desk to pay for sleeping
 * Similar to WaitAtLectern but for sleeping requests
 */
public class WaitAtReceptionDesk extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public WaitAtReceptionDesk() {
        // No memory requirements, no time limit
        super(ImmutableMap.of(), Integer.MAX_VALUE); // Run indefinitely
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is waiting at reception
        return customer.getCustomerState() == CustomerState.WAITING_RECEPTION;
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Keep waiting while in WAITING_RECEPTION state
        return customer.getCustomerState() == CustomerState.WAITING_RECEPTION;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Get sleeping request from tavern -> economy manager
        TavernContext tavern = Tavern.get(level);
        SleepingRequest request = tavern.createSleepingRequest();
        customer.setRequest(request);
        
        // Hold money in hand (no item on head)
        customer.setItemSlot(EquipmentSlot.MAINHAND, request.getPrice().toHighestTierStack());
        
        LOGGER.info("Customer {} waiting at reception desk for sleeping (holding {})", 
            customer.getId(), request.getPrice().getDisplayName());
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Remove items
        customer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        LOGGER.debug("Customer {} stopped waiting at reception desk", customer.getId());
    }
}

