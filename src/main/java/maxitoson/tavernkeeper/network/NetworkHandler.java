package maxitoson.tavernkeeper.network;

import maxitoson.tavernkeeper.TavernKeeperMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles network communication between server and client
 */
public class NetworkHandler {
    
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TavernKeeperMod.MODID)
            .versioned("1.0");
        
        registrar.playToClient(
            SyncAreasPacket.TYPE,
            SyncAreasPacket.STREAM_CODEC,
            SyncAreasPacket::handle
        );
    }
    
    /**
     * Send a packet to a specific player
     */
    public static void sendToPlayer(SyncAreasPacket packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
    
    /**
     * Send a packet to all players
     */
    public static void sendToAllPlayers(SyncAreasPacket packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}
