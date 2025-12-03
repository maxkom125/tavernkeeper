package maxitoson.tavernkeeper.items;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.events.AdvancementHandler;
import maxitoson.tavernkeeper.events.CustomerPaymentEvent;
import maxitoson.tavernkeeper.tavern.economy.CoinRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Handles automatic coin collection into wallet.
 * Listens for:
 * - ItemEntityPickupEvent: coins picked up from ground
 * - CustomerPaymentEvent: coins received from customer service
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class WalletHandler {

    /**
     * Handle item pickup from ground - redirect coins to wallet
     */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        // Don't pickup if it has a delay (e.g. just dropped by player)
        if (event.getItemEntity().hasPickUpDelay()) {
            return; 
        }
        
        Player player = event.getPlayer();
        ItemStack pickedUpStack = event.getItemEntity().getItem();
        
        if (!CoinRegistry.isCoin(pickedUpStack.getItem())) return;
        
        // Try to add to wallet
        if (tryAddToWallet(player, pickedUpStack)) {
            if (pickedUpStack.isEmpty()) {
                // Show pickup animation
                int originalCount = event.getItemEntity().getItem().getCount();
                player.take(event.getItemEntity(), originalCount);
                event.getItemEntity().discard();
            }
        }
    }

    /**
     * Handle customer payment - try wallet first, fallback to inventory
     */
    @SubscribeEvent
    public static void onCustomerPayment(CustomerPaymentEvent event) {
        Player player = event.getPlayer();
        
        for (ItemStack payment : event.getPaymentStacks()) {
            // Try wallet first, fallback to inventory
            if (!tryAddToWallet(player, payment)) {
                if (!player.addItem(payment)) {
                    player.drop(payment, false);
                }
            }
        }
    }

    /**
     * Try to add coins to player's wallet.
     * Also triggers coin collection advancements on server side.
     * @return true if coins were added to a wallet, false if no wallet found
     */
    public static boolean tryAddToWallet(Player player, ItemStack coinStack) {
        if (!CoinRegistry.isCoin(coinStack.getItem())) return false;
        
        Item coinType = coinStack.getItem();
        
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof WalletItem walletItem) {
                walletItem.addCoin(stack, coinStack);
                
                // Grant coin collection advancement on server side
                if (player instanceof ServerPlayer serverPlayer) {
                    AdvancementHandler.checkCoinAdvancement(serverPlayer, coinType);
                }
                
                return true;
            }
        }
        return false;
    }
}

