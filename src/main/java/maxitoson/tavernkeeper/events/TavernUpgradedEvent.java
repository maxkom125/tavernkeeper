package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Fired when the tavern automatically upgrades to a new level
 * This happens when reputation and money thresholds are met
 * 
 * Listen to this event for:
 * - Achievements/advancements
 * - Statistics tracking
 * - Custom effects/notifications
 */
public class TavernUpgradedEvent extends Event {
    private final ServerLevel serverLevel;
    private final TavernUpgrade oldTavernLevel;
    private final TavernUpgrade newTavernLevel;
    
    public TavernUpgradedEvent(ServerLevel serverLevel, TavernUpgrade oldTavernLevel, TavernUpgrade newTavernLevel) {
        this.serverLevel = serverLevel;
        this.oldTavernLevel = oldTavernLevel;
        this.newTavernLevel = newTavernLevel;
    }
    
    public ServerLevel getServerLevel() {
        return serverLevel;
    }
    
    public TavernUpgrade getOldTavernLevel() {
        return oldTavernLevel;
    }
    
    public TavernUpgrade getNewTavernLevel() {
        return newTavernLevel;
    }
}

