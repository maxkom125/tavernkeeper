package maxitoson.tavernkeeper.entities.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.FoodRequest;
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
        // Create food request (currently always carrots, but ready for expansion)
        FoodRequest request = FoodRequest.createDefault();
        customer.setFoodRequest(request);
        
        // Put emerald in main hand - will be rendered in crossed arms position by CrossedArmsItemLayer
        ItemStack emerald = new ItemStack(net.minecraft.world.item.Items.EMERALD, 1);
        customer.setItemSlot(EquipmentSlot.MAINHAND, emerald);
        
        // Put the requested food in HEAD slot - will be rendered floating above by FoodRequestLayer
        customer.setItemSlot(EquipmentSlot.HEAD, request.toDisplayStack());
        
        LOGGER.info("Customer {} waiting at lectern for {} (holding emerald, showing {} above head)", 
            customer.getId(), request.getDisplayName(), request.getDisplayName());
    }
    
    @Override
    protected void stop(ServerLevel level, CustomerEntity customer, long gameTime) {
        // Remove items
        customer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        customer.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        LOGGER.debug("Customer {} stopped waiting at lectern", customer.getId());
    }
}

