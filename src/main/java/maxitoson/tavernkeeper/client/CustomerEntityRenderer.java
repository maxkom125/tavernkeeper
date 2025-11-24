package maxitoson.tavernkeeper.client;

import maxitoson.tavernkeeper.entities.CustomerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for CustomerEntity that uses the vanilla Villager model and textures
 * with additional layers to show held/worn items
 */
public class CustomerEntityRenderer extends MobRenderer<CustomerEntity, VillagerModel<CustomerEntity>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = 
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    
    public CustomerEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        
        // Add layer to render items in crossed arms position (like villagers do with emeralds)
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
        
        // Add custom layer to render food request floating above head (reads from HEAD slot)
        this.addLayer(new FoodRequestLayer(this, context.getItemRenderer(), context.getFont()));
    }
    
    @Override
    public ResourceLocation getTextureLocation(CustomerEntity entity) {
        return VILLAGER_BASE_SKIN;
    }
}

