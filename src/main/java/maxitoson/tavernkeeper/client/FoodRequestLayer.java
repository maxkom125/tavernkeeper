package maxitoson.tavernkeeper.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Custom render layer that displays the food request (carrots, etc.) floating above the customer's head
 * Only shows when customer is in WAITING_SERVICE state
 */
public class FoodRequestLayer extends RenderLayer<CustomerEntity, VillagerModel<CustomerEntity>> {
    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;
    private final Font font;
    
    public FoodRequestLayer(RenderLayerParent<CustomerEntity, VillagerModel<CustomerEntity>> parent,
                           net.minecraft.client.renderer.entity.ItemRenderer itemRenderer,
                           Font font) {
        super(parent);
        this.itemRenderer = itemRenderer;
        this.font = font;
    }
    
    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                      CustomerEntity customer, float limbSwing, float limbSwingAmount,
                      float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        
        // Read food request from HEAD slot (auto-synced by Minecraft)
        ItemStack displayStack = customer.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (displayStack.isEmpty()) {
            return;
        }
        
        poseStack.pushPose();
        
        // Position above head, closer (negative Y due to entity coordinate space)
        poseStack.translate(0.0, -1.2, 0.0);
        
        // Gentle bobbing animation
        float bobOffset = (float) Math.sin((ageInTicks + partialTicks) * 0.1) * 0.05F;
        poseStack.translate(0.0, bobOffset, 0.0);
        
        // Flip it right-side up (rotate 180° on X axis)
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        
        // Slow rotation for visibility (Y axis)
        float rotation = (ageInTicks + partialTicks) / 20.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 57.29578F));
        
        // Nice visible scale
        poseStack.scale(1.0F, 1.0F, 1.0F);
        
        // Render the item
        itemRenderer.renderStatic(
            displayStack,
            ItemDisplayContext.FIXED,
            15728880, // Full bright
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            customer.level(),
            customer.getId()
        );
        
        // Render the quantity text with the item
        if (displayStack.getCount() > 1) {
            Component text = Component.literal(String.valueOf(displayStack.getCount()));
            float textScale = 0.04F;
            float textWidth = -font.width(text) / 2.0F;
            
            // Render front side (rotates with carrot)
            poseStack.pushPose();
            poseStack.translate(0.1, -0.3, 0.1); // Above the item
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F)); // Flip text right-side up
            poseStack.scale(textScale, textScale, textScale);
            font.drawInBatch(text, textWidth, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x000000, 15728880);
            poseStack.popPose();
            
            // Render back side (180° on Y axis)
            poseStack.pushPose();
            poseStack.translate(-0.1, -0.3, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F)); // Flip text right-side up
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F)); // Flip to back
            poseStack.scale(textScale, textScale, textScale);
            font.drawInBatch(text, textWidth, 0, 0xFFFFFF, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x000000, 15728880);
            poseStack.popPose();
        }
        
        poseStack.popPose();
    }
}

