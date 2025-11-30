package maxitoson.tavernkeeper.events;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.AreaCommand;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.network.NetworkHandler;
import maxitoson.tavernkeeper.network.SyncAreasPacket;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

/**
 * Handles tavern lifecycle events and server events.
 * 
 * Responsibilities:
 * - Server startup initialization
 * - Tavern ticking (customer spawning, etc.)
 * - Player join (welcome message, area sync)
 * - Command registration
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class TavernLifecycleHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handle server startup.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
    
    /**
     * Handle level tick for tavern lifecycle.
     * Only ticks Overworld to handle customer spawning and management.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        // Only tick if it's server-side and Overworld
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel 
            && serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            Tavern tavern = Tavern.get(serverLevel);
            tavern.tick();
        }
    }
    
    /**
     * Welcome message when player joins and sync tavern areas to their client.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        player.sendSystemMessage(Component.literal("§6[Tavern Keeper] §rWelcome to your tavern! Your journey as a keeper begins! 🍺"));
        LOGGER.info("Player {} joined - mod is working!", player.getName().getString());
        
        // Sync areas to the joining player
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.minecraft.server.level.ServerLevel level = serverPlayer.serverLevel();
            Tavern tavern = Tavern.get(level);
            // Convert spaces to TavernArea for network packet
            java.util.List<TavernArea> areas = tavern.getAllSpaces().stream()
                .map(space -> space.getArea())
                .toList();
            NetworkHandler.sendToPlayer(new SyncAreasPacket(areas), serverPlayer);
        }
    }
    
    /**
     * Register commands.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        AreaCommand.register(event.getDispatcher());
        LOGGER.info("Registered tavern area commands");
    }
}

