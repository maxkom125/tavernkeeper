package maxitoson.tavernkeeper.events;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.items.MarkingCane;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.Tavern.ToggleResult;
import maxitoson.tavernkeeper.tavern.economy.CustomerRequest;
import maxitoson.tavernkeeper.tavern.managers.domain.CustomerManager.ServiceResult;
import maxitoson.tavernkeeper.tavern.utils.SignHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles player interactions with blocks, entities, and items.
 * 
 * Responsibilities:
 * - Marking cane right-click (area selection, tavern sign)
 * - Marking cane left-click (area deletion)
 * - Customer serving (player right-clicks customer with food)
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class PlayerInteractionHandler {
    // private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handle right-click on block - HIGH PRIORITY to intercept sign interactions.
     * Routes to MarkingCane for area selection or Tavern for sign toggling.
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack heldItem = player.getItemInHand(event.getHand());
        net.minecraft.core.BlockPos pos = event.getPos();
        net.minecraft.world.level.Level level = event.getLevel();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        // Client-side: prevent sign editor from opening if holding marking cane
        if (level.isClientSide) {
            if (SignHelper.isAnySign(state) && heldItem.getItem() instanceof MarkingCane) {
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
            return;
        }
        
        // Server-side routing logic
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
        Tavern tavern = Tavern.get(serverLevel);
        
        // Route 1: Holding marking cane + clicking sign → delegate to MarkingCane
        if (heldItem.getItem() instanceof MarkingCane && SignHelper.isAnySign(state)) {
            MarkingCane.handleSignClick(serverLevel, pos, player);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
        
        // Route 2: Clicking on tavern sign (without marking cane) → delegate to Tavern
        if (tavern.isTavernSign(pos)) {
            ToggleResult result = tavern.toggleOpenClosed();
            
            // UI layer interprets result and displays feedback
            if (result.isNowOpen()) {
                if (result.isTavernReady()) {
                    player.displayClientMessage(
                        Component.literal("§6[Tavern Keeper] §rTavern is now §aOPEN"),
                        true
                    );
                } else {
                    // Sign is OPEN but missing requirements
                    String issuesStr = String.join(", ", result.getIssues());
                    player.displayClientMessage(
                        Component.literal("§6[Tavern Keeper] §rSign is §aOPEN§r but tavern not ready: §c" + issuesStr),
                        true
                    );
                }
            } else {
                player.displayClientMessage(
                    Component.literal("§6[Tavern Keeper] §rTavern is now §cCLOSED"),
                    true
                );
            }
            player.playSound(SoundEvents.WOODEN_DOOR_CLOSE, 1.0F, 1.0F);
            
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
    }
    
    /**
     * Handle left-click on block with Marking Cane.
     * Used for clearing selection or deleting areas.
     */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.getMainHandItem().getItem() instanceof MarkingCane) {
            if (!player.level().isClientSide) {
                MarkingCane.handleLeftClick(player, event.getPos(), 
                    (net.minecraft.server.level.ServerLevel) player.level());
            }
            event.setCanceled(true); // Prevent block breaking
        }
    }
    
    /**
     * Handle player right-clicking customer to serve them or accept sleeping payment.
     * UI layer: Routes to manager and displays feedback based on result.
     */
    @SubscribeEvent
    public static void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        
        if (event.getTarget() instanceof CustomerEntity customer) {
            Player player = event.getEntity();
            ItemStack heldItem = player.getItemInHand(event.getHand());
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
            Tavern tavern = Tavern.get(serverLevel);
            
            // Delegate to manager for business logic (handles both food and sleeping)
            ServiceResult result = tavern.handlePlayerServe(player, customer, heldItem);
            
            // UI layer: Display feedback based on result
            if (result.shouldShowFeedback()) {
                CustomerRequest request = result.getRequest();
                
                if (result.isSuccess()) {
                    // Success - show what was served/accepted and payment
                    String message = customer.getCustomerState() == CustomerState.FINDING_SEAT 
                        ? "Served customer " + request.getDisplayName() + "! +" + request.getPrice().getDisplayName()
                        : "Received " + request.getPrice().getDisplayName() + " for " + request.getDisplayName() + "!";
                    
                    player.displayClientMessage(Component.literal(message), true);
                    customer.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
                    player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                } else {
                    // Wrong food - show what customer wants
                    player.displayClientMessage(
                        Component.literal("Customer wants " + request.getDisplayName() + "!"),
                        true
                    );
                    customer.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
                }
                event.setCanceled(true);
            }
            // If no feedback needed, customer not ready - allow other interactions
        }
    }
}

