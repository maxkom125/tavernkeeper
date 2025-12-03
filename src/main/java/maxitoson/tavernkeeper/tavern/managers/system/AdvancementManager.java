package maxitoson.tavernkeeper.tavern.managers.system;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages tavern advancement tracking and persistence.
 * Tracks which players have received specific one-time rewards/milestones
 * to prevent duplicate grants after server restarts.
 * 
 * Pattern: Owned by Tavern
 * Category: System Manager (meta-game state, no world interaction)
 */
public class AdvancementManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Advancement tracking (persisted to prevent duplicate advancement grants after restart)
    private final Set<UUID> millionairesAwarded = new HashSet<>();
    private final Map<UUID, Integer> playerHighestReputation = new HashMap<>();
    
    public AdvancementManager() {
    }
    
    /**
     * Check if millionaire advancement has been awarded to a player
     */
    public boolean hasMillionaireAward(UUID playerUUID) {
        return millionairesAwarded.contains(playerUUID);
    }
    
    /**
     * Mark millionaire advancement as awarded to a player
     */
    public void markMillionaireAwarded(UUID playerUUID) {
        millionairesAwarded.add(playerUUID);
    }
    
    /**
     * Get the highest reputation milestone reached by a player
     * @return The highest milestone (0 if player has no milestones)
     */
    public int getPlayerHighestReputation(UUID playerUUID) {
        return playerHighestReputation.getOrDefault(playerUUID, 0);
    }
    
    /**
     * Update the highest reputation milestone for a player
     */
    public void setPlayerHighestReputation(UUID playerUUID, int milestone) {
        playerHighestReputation.put(playerUUID, milestone);
    }
    
    // ========== Persistence ==========
    
    public void save(CompoundTag tag) {
        if (!millionairesAwarded.isEmpty()) {
            long[] millionaireArray = new long[millionairesAwarded.size() * 2];
            int i = 0;
            for (UUID uuid : millionairesAwarded) {
                millionaireArray[i++] = uuid.getMostSignificantBits();
                millionaireArray[i++] = uuid.getLeastSignificantBits();
            }
            tag.putLongArray("millionairesAwarded", millionaireArray);
        }
        
        if (!playerHighestReputation.isEmpty()) {
            CompoundTag repTag = new CompoundTag();
            for (Map.Entry<UUID, Integer> entry : playerHighestReputation.entrySet()) {
                repTag.putInt(entry.getKey().toString(), entry.getValue());
            }
            tag.put("playerHighestReputation", repTag);
        }
    }
    
    public void load(CompoundTag tag) {
        if (tag.contains("millionairesAwarded")) {
            long[] millionaireArray = tag.getLongArray("millionairesAwarded");
            millionairesAwarded.clear();
            for (int i = 0; i < millionaireArray.length; i += 2) {
                UUID uuid = new UUID(millionaireArray[i], millionaireArray[i + 1]);
                millionairesAwarded.add(uuid);
            }
            LOGGER.info("Loaded {} millionaire awards", millionairesAwarded.size());
        }
        
        if (tag.contains("playerHighestReputation")) {
            CompoundTag repTag = tag.getCompound("playerHighestReputation");
            playerHighestReputation.clear();
            for (String key : repTag.getAllKeys()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    playerHighestReputation.put(uuid, repTag.getInt(key));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid UUID in playerHighestReputation: {}", key);
                }
            }
            LOGGER.info("Loaded reputation tracking for {} players", playerHighestReputation.size());
        }
    }
}

