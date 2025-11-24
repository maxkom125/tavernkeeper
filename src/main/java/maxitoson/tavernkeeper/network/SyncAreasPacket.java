package maxitoson.tavernkeeper.network;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.AreaRenderer;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Packet to sync area data from server to client
 */
public record SyncAreasPacket(List<AreaData> areas) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncAreasPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(TavernKeeperMod.MODID, "sync_areas"));
    
    public static final StreamCodec<FriendlyByteBuf, SyncAreasPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buffer, SyncAreasPacket packet) {
            SyncAreasPacket.encode(packet, buffer);
        }

        @Override
        public SyncAreasPacket decode(FriendlyByteBuf buffer) {
            return SyncAreasPacket.decode(buffer);
        }
    };
    
    public SyncAreasPacket(java.util.Collection<TavernArea> areas) {
        this(areas.stream()
            .map(area -> new AreaData(
                area.getId(),
                area.getName(),
                area.getType(),
                area.getMinPos(),
                area.getMaxPos()
            ))
            .toList());
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    /**
     * Encode packet data to buffer
     */
    public static void encode(SyncAreasPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.areas.size());
        for (AreaData data : packet.areas) {
            buffer.writeUUID(data.id);
            buffer.writeUtf(data.name);
            buffer.writeEnum(data.type);
            buffer.writeBlockPos(data.minPos);
            buffer.writeBlockPos(data.maxPos);
        }
    }
    
    /**
     * Decode packet data from buffer
     */
    public static SyncAreasPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readInt();
        List<AreaData> areas = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            UUID id = buffer.readUUID();
            String name = buffer.readUtf();
            AreaType type = buffer.readEnum(AreaType.class);
            BlockPos minPos = buffer.readBlockPos();
            BlockPos maxPos = buffer.readBlockPos();
            
            areas.add(new AreaData(id, name, type, minPos, maxPos));
        }
        
        return new SyncAreasPacket(areas);
    }
    
    /**
     * Handle packet on client side
     */
    public static void handle(SyncAreasPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client-side cache with received areas
            List<TavernArea> clientAreas = new ArrayList<>();
            for (AreaData data : packet.areas) {
                // Client-side areas don't need level reference for rendering
                TavernArea area = new TavernArea(data.id, data.name, data.type, data.minPos, data.maxPos, null);
                clientAreas.add(area);
            }
            
            // Update client-side cache
            AreaRenderer.updateClientAreas(clientAreas);
        });
    }
    
    /**
     * Internal area data for network transmission
     */
    public record AreaData(UUID id, String name, AreaType type, BlockPos minPos, BlockPos maxPos) {}
}
