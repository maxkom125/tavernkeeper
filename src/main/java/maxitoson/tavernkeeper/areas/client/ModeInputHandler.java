package maxitoson.tavernkeeper.areas.client;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.AreaMode;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.items.MarkingCane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Handles Shift + Scroll input for mode switching on the Marking Cane
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID, value = Dist.CLIENT)
public class ModeInputHandler {
    
    private static long lastScrollTime = 0;
    private static final long SCROLL_COOLDOWN_MS = 150; // Debounce rapid scrolling
    
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null) return;
        
        // Only handle when holding Marking Cane
        if (!(player.getMainHandItem().getItem() instanceof MarkingCane) &&
            !(player.getOffhandItem().getItem() instanceof MarkingCane)) {
            return;
        }
        
        // Only handle when Shift is held
        if (!Screen.hasShiftDown()) {
            return;
        }
        
        // Cancel the event IMMEDIATELY to prevent hotbar scrolling
        event.setCanceled(true);
        
        // Debounce rapid scrolling
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScrollTime < SCROLL_COOLDOWN_MS) {
            return;
        }
        lastScrollTime = currentTime;
        
        // Get scroll direction - use the vertical scroll amount
        double scrollDelta = event.getScrollDeltaY();
        
        if (scrollDelta == 0) return;
        
        // Cycle mode based on scroll direction
        AreaType newMode;
        if (scrollDelta > 0) {
            // Scroll up = next mode
            newMode = AreaMode.cycleNext(player);
        } else {
            // Scroll down = previous mode
            newMode = AreaMode.cyclePrevious(player);
        }
        
        // Show feedback in action bar
        player.displayClientMessage(
            Component.literal("§6[Marking Cane] §rMode: " + newMode.getColoredName()),
            true // Action bar
        );
    }
}

