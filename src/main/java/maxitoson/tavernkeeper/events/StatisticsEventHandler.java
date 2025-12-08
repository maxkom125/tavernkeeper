package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles statistics tracking for tavern operations
 * Automatically records sales, updates reputation, tracks customers served, etc.
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class StatisticsEventHandler {
    
    /**
     * When a customer pays for service, automatically record the sale
     * This ensures we track revenue, customers served, and reputation
     */
    @SubscribeEvent
    public static void onCustomerPayment(CustomerPaymentEvent event) {
        if (event.getPlayer().level() instanceof ServerLevel level) {
            Tavern tavern = Tavern.get(level);
            
            // Automatically record sale in tavern statistics
            // This updates: money earned, customers served, reputation
            // Also triggers upgrade checks and persistence
            tavern.recordSale(event.getRequest().getPrice().getCopperValue());
        }
    }
}

