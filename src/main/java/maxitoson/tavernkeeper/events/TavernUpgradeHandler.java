package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles tavern upgrade events
 * Broadcasts upgrade messages to all players
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class TavernUpgradeHandler {
    
    /**
     * Broadcast upgrade message to all players when tavern upgrades
     */
    @SubscribeEvent
    public static void onTavernUpgraded(TavernUpgradedEvent event) {
        // Get tavern to query current stats
        Tavern tavern = Tavern.get(event.getServerLevel());
        
        // Create messages
        Component upgradeMessage = Component.literal(
            "§6[Tavern Keeper] §r§aTavern upgraded to " + event.getNewTavernLevel().getDisplayName() + "!"
        );
        Component statsMessage = Component.literal(
            "§7  Max Tables: §f" + tavern.getDiningManager().getMaxTables() + 
            " §7| Payment: §f" + (int)(tavern.getEconomyManager().getPaymentMultiplierValue() * 100) + "%" +
            " §7| Spawn Rate: §f" + (int)(tavern.getCustomerManager().getSpawnRateMultiplier() * 100) + "%"
        );
        
        // Broadcast to all players
        for (ServerPlayer player : event.getServerLevel().players()) {
            player.sendSystemMessage(upgradeMessage);
            player.sendSystemMessage(statsMessage);
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}

