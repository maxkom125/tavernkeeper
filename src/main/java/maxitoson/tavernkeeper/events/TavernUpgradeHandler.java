package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.upgrades.UpgradeFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles tavern upgrade events
 * Broadcasts upgrade messages to all players
 * UI layer - formats and displays upgrade notifications
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class TavernUpgradeHandler {
    
    /**
     * Broadcast upgrade message to all players when tavern upgrades
     */
    @SubscribeEvent
    public static void onTavernUpgraded(TavernUpgradedEvent event) {
        // Format messages using old and new upgrade levels from event
        for (ServerPlayer player : event.getServerLevel().players()) {
            // Send formatted notification showing old → new values
            for (Component line : UpgradeFormatter.formatUpgradeNotification(
                    event.getOldTavernLevel(), 
                    event.getNewTavernLevel())) {
                player.sendSystemMessage(line);
            }
            
            // Play level-up sound
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}

