package maxitoson.tavernkeeper.areas;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.items.MarkingCane;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Renders tavern area boundaries when player is holding a Marking Cane
 * Also renders all saved areas (synced from server)
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID, value = Dist.CLIENT)
public class AreaRenderer {
    
    // Client-side cache of saved areas (synced from server)
    private static final Map<UUID, TavernArea> CLIENT_AREAS = new HashMap<>();
    
    /**
     * Update the client-side cache of areas (called when sync packet is received)
     */
    public static void updateClientAreas(java.util.List<TavernArea> areas) {
        CLIENT_AREAS.clear();
        for (TavernArea area : areas) {
            CLIENT_AREAS.put(area.getId(), area);
        }
    }
    
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        
        if (player == null || !(player.level() instanceof net.minecraft.client.multiplayer.ClientLevel)) {
            return;
        }
        
        // Only render when holding Marking Cane
        if (!(player.getMainHandItem().getItem() instanceof MarkingCane) &&
            !(player.getOffhandItem().getItem() instanceof MarkingCane)) {
            return;
        }
        
        PoseStack poseStack = new PoseStack();
        
        // Render current selection
        renderCurrentSelection(poseStack, player);
        
        // Render all saved areas (synced from server)
        renderSavedAreas(poseStack);
    }
    
    private static void renderCurrentSelection(PoseStack poseStack, Player player) {
        MarkingCane.AreaSelection selection = MarkingCane.getSelection(player);
        if (selection == null) {
            return;
        }
        
        AABB box = null;
        
        if (selection.isComplete()) {
            // Both corners selected - show final box
            box = selection.createAABB();
        } else if (selection.getPos1() != null) {
            // Only first corner selected - show live preview to cursor position
            // Use Minecraft's built-in hitResult that tracks what the player is looking at
            net.minecraft.world.phys.HitResult hitResult = Minecraft.getInstance().hitResult;
            if (hitResult != null && hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hitResult;
                net.minecraft.core.BlockPos pos2 = blockHit.getBlockPos();
                net.minecraft.core.BlockPos pos1 = selection.getPos1();
                
                // Create temporary box from pos1 to cursor position
                int minX = Math.min(pos1.getX(), pos2.getX());
                int minY = Math.min(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());
                
                box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            }
        }
        
        if (box == null) {
            return;
        }
        
        // Get current mode color (dynamically updates when player scrolls to change mode)
        AreaType currentMode = AreaMode.getMode(player);
        float red = currentMode.getRed();
        float green = currentMode.getGreen();
        float blue = currentMode.getBlue();
        
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        
        // Render selection in current mode color
        LevelRenderer.renderLineBox(poseStack, consumer, box, red, green, blue, 1.0f);
        
        poseStack.popPose();
        bufferSource.endBatch();
    }
    
    /**
     * Render all saved areas (synced from server)
     */
    private static void renderSavedAreas(PoseStack poseStack) {
        if (CLIENT_AREAS.isEmpty()) {
            return;
        }
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        // Get pending deletion area (if any)
        UUID pendingDeletionId = MarkingCane.getPendingDeletion(player);
        
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        
        for (TavernArea area : CLIENT_AREAS.values()) {
            AABB box = area.getBoundingBox();
            
            // Render pending deletion in red, otherwise use type color
            if (pendingDeletionId != null && pendingDeletionId.equals(area.getId())) {
                // Red for pending deletion
                LevelRenderer.renderLineBox(poseStack, consumer, box, 
                    1.0f, 0.0f, 0.0f, 1.0f);
            } else {
                // Normal type color
                AreaType type = area.getType();
                LevelRenderer.renderLineBox(poseStack, consumer, box, 
                    type.getRed(), type.getGreen(), type.getBlue(), 0.8f);
            }
        }
        
        poseStack.popPose();
        bufferSource.endBatch();
    }
}

