package maxitoson.tavernkeeper.tavern.managers.system;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.events.TavernUpgradedEvent;
import maxitoson.tavernkeeper.tavern.TavernContext;
import maxitoson.tavernkeeper.tavern.upgrades.TavernUpgrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Manages tavern upgrade state and unlocking
 * Handles checking requirements and applying upgrades to managers
 * 
 * Pattern: Owned by Tavern, queries TavernContext for statistics
 * Category: System Manager (meta-game state, no world interaction)
 */
public class UpgradeManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final TavernContext tavern;
    private TavernUpgrade currentUpgrade;
    
    public UpgradeManager(TavernContext tavern) {
        this.tavern = tavern;
        this.currentUpgrade = TavernUpgrade.LEVEL_1;
    }
    
    /**
     * Get current upgrade level
     */
    public TavernUpgrade getCurrentUpgrade() {
        return currentUpgrade;
    }
    
    /**
     * Check if next upgrade is available (requirements met)
     */
    public boolean canUpgradeToNext() {
        TavernUpgrade next = currentUpgrade.getNextLevel();
        return next != null && next.meetsRequirements(tavern);
    }
    
    /**
     * Get the next upgrade level if available
     */
    public TavernUpgrade getNextUpgrade() {
        return currentUpgrade.getNextLevel();
    }
    
    /**
     * Check if new upgrades have been unlocked and automatically upgrade
     * Called after statistics change
     * Fires TavernUpgradedEvent if upgrade occurs
     * 
     * @param serverLevel The server level (for firing event)
     * @return the new upgrade level if upgraded, null otherwise
     */
    public TavernUpgrade checkAndAutoUpgrade(ServerLevel serverLevel) {
        if (!canUpgradeToNext()) {
            return null;
        }
        
        TavernUpgrade oldTavernLevel = currentUpgrade;
        TavernUpgrade newTavernLevel = upgrade();
        
        if (newTavernLevel != null) {
            LOGGER.info("Tavern automatically upgraded: {} -> {}", 
                oldTavernLevel.getDisplayName(), newTavernLevel.getDisplayName());
            
            // Fire event for UI/achievements/other systems
            NeoForge.EVENT_BUS.post(
                new TavernUpgradedEvent(serverLevel, oldTavernLevel, newTavernLevel)
            );
        }
        
        return newTavernLevel;
    }
    
    /**
     * Attempt to upgrade to the next level
     * Returns the new upgrade level if successful, null if failed
     */
    public TavernUpgrade upgrade() {
        if (!canUpgradeToNext()) {
            return null;
        }
        
        TavernUpgrade oldLevel = currentUpgrade;
        currentUpgrade = currentUpgrade.getNextLevel();
        
        LOGGER.info("Tavern upgraded: {} -> {}", oldLevel.getDisplayName(), currentUpgrade.getDisplayName());
        return currentUpgrade;
    }
    
    /**
     * Set upgrade level (used during loading)
     */
    public void setUpgrade(TavernUpgrade upgrade) {
        this.currentUpgrade = upgrade;
    }
    
    // Persistence
    public void save(CompoundTag tag) {
        tag.putString("currentUpgrade", currentUpgrade.name());
    }
    
    public void load(CompoundTag tag) {
        if (tag.contains("currentUpgrade")) {
            try {
                this.currentUpgrade = TavernUpgrade.valueOf(tag.getString("currentUpgrade"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid upgrade level in save data, defaulting to LEVEL_1");
                this.currentUpgrade = TavernUpgrade.LEVEL_1;
            }
        }
    }
}

