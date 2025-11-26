package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.List;

/**
 * Fired when a customer pays the player for service.
 * Use getPaymentStacks() to get the coin breakdown.
 */
public class CustomerPaymentEvent extends Event {
    private final Player player;
    private final CustomerEntity customer;
    private final FoodRequest request;
    
    public CustomerPaymentEvent(Player player, CustomerEntity customer, FoodRequest request) {
        this.player = player;
        this.customer = customer;
        this.request = request;
    }
    
    public Player getPlayer() { return player; }
    public CustomerEntity getCustomer() { return customer; }
    public FoodRequest getRequest() { return request; }
    
    /** Get the payment as ItemStacks (full coin breakdown) */
    public List<ItemStack> getPaymentStacks() {
        return request.getPrice().toItemStacks();
    }
}

