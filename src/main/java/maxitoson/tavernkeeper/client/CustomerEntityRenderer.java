package maxitoson.tavernkeeper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.SittingEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Renderer for CustomerEntity that uses a custom VillagerModel with sitting support.
 * 
 * The standard VillagerModel extends ListModel (not HumanoidModel), so it doesn't
 * have a built-in "riding" flag for sitting poses. We use SittingVillagerModel
 * which manually rotates legs when sitting.
 */
public class CustomerEntityRenderer extends MobRenderer<CustomerEntity, SittingVillagerModel<CustomerEntity>> {
    private static final ResourceLocation VILLAGER_BASE_SKIN = 
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    
    public CustomerEntityRenderer(EntityRendererProvider.Context context) {
        // Use our custom SittingVillagerModel instead of standard VillagerModel
        super(context, new SittingVillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        
        // Add layer to render items in crossed arms position (like villagers do with emeralds)
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
        
        // Add custom layer to render food request floating above head (reads from HEAD slot)
        this.addLayer(new FoodRequestLayer(this, context.getItemRenderer(), context.getFont()));
    }
    
    @Override
    public void render(
            @NotNull CustomerEntity customer,
            float entityYaw,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight) {
        
        // Tell our custom model whether to show sitting pose
        // Only show sitting pose when riding OUR SittingEntity (chairs)
        // For boats/minecarts, let them handle positioning with default pose
        boolean isOnChair = customer.getVehicle() instanceof SittingEntity;
        this.model.setSitting(isOnChair);
        
        super.render(customer, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }
    
    @Override
    public ResourceLocation getTextureLocation(CustomerEntity entity) {
        return VILLAGER_BASE_SKIN;
    }
}

