package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Behavior for customer waiting at lectern for service
 */
public class WaitAtLectern extends Behavior<CustomerEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public WaitAtLectern() {
        // No memory requirements, no time limit
        super(ImmutableMap.of(), Integer.MAX_VALUE); // Run indefinitely
    }
    
    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, CustomerEntity customer) {
        // Only run if customer is waiting for service
        return customer.getCustomerState() == CustomerState.WAITING_SERVICE;
    }
    
    @Override
    protected boolean canStillUse(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Keep waiting while in WAITING_SERVICE state
        return customer.getCustomerState() == CustomerState.WAITING_SERVICE;
    }
    
    @Override
    protected void start(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Get food request from tavern's economy manager
        Tavern tavern = Tavern.get(level);
        FoodRequest request = tavern.getEconomyManager().createFoodRequest();
        customer.setFoodRequest(request);
        
        // Set rendered items using request data
        customer.setItemSlot(EquipmentSlot.MAINHAND, request.getPrice().toHighestTierStack());
        customer.setItemSlot(EquipmentSlot.HEAD, 
            new ItemStack(request.getRequestedItem(), request.getRequestedAmount()));
        
        LOGGER.info("Customer {} waiting at lectern for {} (holding {}, showing {} above head)", 
            customer.getId(), request.getDisplayName(), 
            request.getPrice().getDisplayName(), request.getDisplayName());
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Remove items
        customer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        customer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        LOGGER.debug("Customer {} stopped waiting at lectern", customer.getId());
    }
}

