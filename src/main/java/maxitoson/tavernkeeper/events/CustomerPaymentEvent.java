package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.economy.CustomerRequest;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Fired when a customer pays for service (food, sleeping, etc.)
 * Use getPaymentStacks() to get the coin breakdown.
 */
public class CustomerPaymentEvent extends Event {
    private final Player player;
    private final CustomerEntity customer;
    private final CustomerRequest request; // Polymorphic - FoodRequest, SleepingRequest, etc.
    
    public CustomerPaymentEvent(Player player, CustomerEntity customer, CustomerRequest request) {
        this.player = player;
        this.customer = customer;
        this.request = request;
    }
    
    public Player getPlayer() { return player; }
    public CustomerEntity getCustomer() { return customer; }
    public CustomerRequest getRequest() { return request; }
    
    // Type-safe convenience getters (return null if wrong type)
    public FoodRequest getFoodRequest() { 
        return request instanceof FoodRequest ? (FoodRequest) request : null;
    }
    
    public SleepingRequest getSleepingRequest() {
        return request instanceof SleepingRequest ? (SleepingRequest) request : null;
    }
    
    /** Get the payment as ItemStacks (full coin breakdown) */
    public List<ItemStack> getPaymentStacks() {
        return request != null ? request.getPrice().toItemStacks() : List.of();
    }
}

