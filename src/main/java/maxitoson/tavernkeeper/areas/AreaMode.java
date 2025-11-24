package maxitoson.tavernkeeper.areas;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the current area mode (type) for each player.
 * Players can cycle through modes using Shift + Scroll.
 */
public class AreaMode {
    
    // Store current mode per player
    private static final Map<UUID, AreaType> PLAYER_MODES = new HashMap<>();
    
    /**
     * Get the current mode for a player
     */
    public static AreaType getMode(Player player) {
        return PLAYER_MODES.getOrDefault(player.getUUID(), AreaType.DINING); // Default to DINING
    }
    
    /**
     * Set the mode for a player
     */
    public static void setMode(Player player, AreaType mode) {
        PLAYER_MODES.put(player.getUUID(), mode);
    }
    
    /**
     * Cycle to the next mode (called when scrolling up)
     */
    public static AreaType cycleNext(Player player) {
        AreaType current = getMode(player);
        AreaType[] types = AreaType.values();
        int nextIndex = (current.ordinal() + 1) % types.length;
        AreaType next = types[nextIndex];
        setMode(player, next);
        return next;
    }
    
    /**
     * Cycle to the previous mode (called when scrolling down)
     */
    public static AreaType cyclePrevious(Player player) {
        AreaType current = getMode(player);
        AreaType[] types = AreaType.values();
        int prevIndex = (current.ordinal() - 1 + types.length) % types.length;
        AreaType prev = types[prevIndex];
        setMode(player, prev);
        return prev;
    }
    
    /**
     * Clear mode for a player (on logout)
     */
    public static void clearMode(Player player) {
        PLAYER_MODES.remove(player.getUUID());
    }
}

