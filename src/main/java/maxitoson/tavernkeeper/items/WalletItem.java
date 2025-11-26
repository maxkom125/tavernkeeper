package maxitoson.tavernkeeper.items;

import maxitoson.tavernkeeper.tavern.economy.CoinRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

public class WalletItem extends Item {
    
    public WalletItem(Properties properties) {
        super(properties);
    }
    
    // Handle clicking an item ON the wallet (in inventory)
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack walletStack, ItemStack otherStack, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        if (otherStack.isEmpty()) {
            extractHighestCoin(walletStack, access);
            return true;
        } else if (CoinRegistry.isCoin(otherStack.getItem())) {
            addCoin(walletStack, otherStack);
            return true;
        }
        
        return false;
    }

    // Handle clicking the wallet ON a slot (in inventory)
    @Override
    public boolean overrideStackedOnOther(ItemStack walletStack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }

        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            extractHighestCoin(walletStack, slot);
            return true;
        } else if (CoinRegistry.isCoin(slotStack.getItem())) {
            addCoin(walletStack, slotStack);
            return true;
        }

        return false;
    }
    
    // Helper to add coins from a stack to the wallet
    public void addCoin(ItemStack walletStack, ItemStack coinStack) {
        if (!CoinRegistry.isCoin(coinStack.getItem())) return;
        
        CustomData customData = walletStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        String key = CoinRegistry.getTierKey(coinStack.getItem());
        if (key != null) {
            long current = tag.getLong(key);
            tag.putLong(key, current + coinStack.getCount());
            coinStack.setCount(0); // Consume all
            
            // Auto-convert to higher tier coins
            CoinRegistry.autoConvertTag(tag);
            
            walletStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }
    
    private void extractHighestCoin(ItemStack walletStack, SlotAccess access) {
        CustomData customData = walletStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        // Iterate from highest to lowest tier
        for (int i = CoinRegistry.TIER_COUNT - 1; i >= 0; i--) {
            String key = CoinRegistry.TIER_KEYS[i];
            long count = tag.getLong(key);
            if (count > 0) {
                Item item = CoinRegistry.getItemByIndex(i);
                if (item != null) {
                    int extractCount = (int) Math.min(count, item.getDefaultInstance().getMaxStackSize());
                    ItemStack newStack = new ItemStack(item, extractCount);
                    
                    if (access.set(newStack)) {
                        tag.putLong(key, count - extractCount);
                        walletStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        return;
                    }
                }
            }
        }
    }
    
    private void extractHighestCoin(ItemStack walletStack, Slot targetSlot) {
         CustomData customData = walletStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
         CompoundTag tag = customData.copyTag();
         
         // Iterate from highest to lowest tier
         for (int i = CoinRegistry.TIER_COUNT - 1; i >= 0; i--) {
             String key = CoinRegistry.TIER_KEYS[i];
             long count = tag.getLong(key);
             if (count > 0) {
                 Item item = CoinRegistry.getItemByIndex(i);
                 if (item != null) {
                     int extractCount = (int) Math.min(count, item.getDefaultInstance().getMaxStackSize());
                     ItemStack toPlace = new ItemStack(item, extractCount);
                     
                     if (targetSlot.mayPlace(toPlace)) {
                         targetSlot.set(toPlace);
                         tag.putLong(key, count - extractCount);
                         walletStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                         return;
                     }
                 }
             }
         }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // Add usage tooltip
        tooltipComponents.add(Component.translatable("item.tavernkeeper.wallet.tooltip").withStyle(ChatFormatting.GRAY));
        
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        
        boolean empty = true;
        
        // Display from highest to lowest tier
        for (int i = CoinRegistry.TIER_COUNT - 1; i >= 0; i--) {
            String key = CoinRegistry.TIER_KEYS[i];
            long count = tag.getLong(key);
            if (count > 0) {
                empty = false;
                Item item = CoinRegistry.getItemByIndex(i);
                if (item != null) {
                    tooltipComponents.add(Component.literal("").withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(count + "x "))
                        .append(Component.literal("").withStyle(ChatFormatting.WHITE).append(item.getName(new ItemStack(item)))));
                }
            }
        }
        
        if (empty) {
            tooltipComponents.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
