package maxitoson.tavernkeeper.client;

import maxitoson.tavernkeeper.TavernKeeperMod;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-side registration for entity renderers
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID, value = Dist.CLIENT)
public class ClientSetup {
    
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Use custom renderer that applies Villager model to our CustomerEntity
        event.registerEntityRenderer(TavernKeeperMod.CUSTOMER.get(), CustomerEntityRenderer::new);
        
        // Register empty renderer for sitting entity (it's invisible anyway)
        event.registerEntityRenderer(TavernKeeperMod.SITTING.get(), NoopRenderer::new);
    }
}

