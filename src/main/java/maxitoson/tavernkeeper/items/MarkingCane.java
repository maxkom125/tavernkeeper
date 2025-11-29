package maxitoson.tavernkeeper.items;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.spaces.BaseSpace;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarkingCane extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Store selections per player (UUID -> AreaSelection)
    private static final Map<UUID, AreaSelection> PLAYER_SELECTIONS = new HashMap<>();
    
    // Track areas pending deletion (player UUID -> area UUID)
    private static final Map<UUID, UUID> PENDING_DELETIONS = new HashMap<>();
    
    public MarkingCane(Properties properties) {
        super(properties);
    }
    
    // Add tooltip showing instructions
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Show instructions
        tooltip.add(Component.literal("§7Shift + Scroll: Change mode").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7Right-click: Set corners (auto-saves)").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7Left-click: Clear selection or delete area").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("§7  (Click area twice to delete)").withStyle(ChatFormatting.GRAY));
    }
    
    // Right-click on block - sets first position, then second position and auto-saves, OR designates tavern sign
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        
        if (player == null) return InteractionResult.PASS;
        
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        
        LOGGER.info("MarkingCane.useOn called - pos: {}, block: {}, isClientSide: {}", 
            pos, state.getBlock().getClass().getSimpleName(), level.isClientSide);
        
        // NOTE: Sign clicks are intercepted by event handler (HIGH priority) to prevent editor
        // The event handler calls handleSignClick() directly
        // So signs will never reach this code path
        
        // Normal area selection flow - server side only
        if (!level.isClientSide) {
            UUID playerId = player.getUUID();
            
            // Right-click cancels pending deletion
            PENDING_DELETIONS.remove(playerId);
            
            // Normal flow: area selection
            AreaSelection selection = PLAYER_SELECTIONS.computeIfAbsent(playerId, k -> new AreaSelection());
            
            // If first position not set, set it
            if (selection.getPos1() == null) {
                selection.setPos1(pos);
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Marking Cane] §rFirst position set to §e%d, %d, %d", 
                        pos.getX(), pos.getY(), pos.getZ())
                ));
            } 
            // Otherwise, set second position and auto-save
            else {
                selection.setPos2(pos);
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Marking Cane] §rSecond position set to §e%d, %d, %d", 
                        pos.getX(), pos.getY(), pos.getZ())
                ));
                
                // Calculate and show selection info
                int[] size = selection.getSize();
                int volume = selection.getVolume();
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Marking Cane] §rArea selected: §e%d×%d×%d §r(%d blocks)", 
                        size[0], size[1], size[2], volume)
                ));
                
                // Auto-save using current mode
                autoSaveArea(player, (net.minecraft.server.level.ServerLevel) level);
            }
        }
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * Handle right-click on a sign with marking cane to designate it as tavern sign
     * Delegates to Tavern for business logic, handles UI display
     */
    public static void handleSignClick(net.minecraft.server.level.ServerLevel serverLevel,
                                       BlockPos pos, Player player) {
        LOGGER.info("MarkingCane: Designating sign at {} as tavern sign", pos);
        
        // Delegate to Tavern for business logic
        Tavern tavern = Tavern.get(serverLevel);
        maxitoson.tavernkeeper.tavern.Tavern.SignSetResult result = tavern.setTavernSign(pos);
        
        // UI layer interprets result
        if (result.wasAlreadySet()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Marking Cane] §rThis is already your tavern sign!"
            ));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§6[Marking Cane] §rThis sign is now your tavern sign! Right-click to toggle open/closed."
            ));
            player.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        }
    }
    
    /**
     * Auto-save area with the current mode and auto-generated name
     */
    private void autoSaveArea(Player player, net.minecraft.server.level.ServerLevel level) {
        // Get current mode from player
        AreaType currentMode = maxitoson.tavernkeeper.areas.AreaMode.getMode(player);
        // Delegate to main save method
        saveArea(player, currentMode, level);
    }
    
    public static AreaSelection getSelection(Player player) {
        return PLAYER_SELECTIONS.get(player.getUUID());
    }
    
    public static void clearSelection(Player player) {
        PLAYER_SELECTIONS.remove(player.getUUID());
        PENDING_DELETIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§6[Marking Cane] §rSelection cleared"));
    }
    
    /**
     * Get the UUID of the area pending deletion for a player
     */
    public static UUID getPendingDeletion(Player player) {
        return PENDING_DELETIONS.get(player.getUUID());
    }
    
    /**
     * Handle left-click: delete area (2-step) or clear selection
     */
    public static void handleLeftClick(Player player, BlockPos pos, net.minecraft.server.level.ServerLevel level) {
        UUID playerId = player.getUUID();
        AreaSelection selection = PLAYER_SELECTIONS.get(playerId);
        
        // If actively selecting an area, don't trigger deletion - just clear selection
        if (selection != null && selection.getPos1() != null) {
            clearSelection(player);
            return;
        }
        
        Tavern tavern = Tavern.get(level);
        BaseSpace spaceAtPos = tavern.getSpaceAt(pos);
        
        if (spaceAtPos != null) {
            // Clicked on a saved area
            TavernArea areaAtPos = spaceAtPos.getArea();
            UUID pendingAreaId = PENDING_DELETIONS.get(playerId);
            
            if (pendingAreaId != null && pendingAreaId.equals(areaAtPos.getId())) {
                // Second click on same area - delete it
                Tavern.DeletionResult result = tavern.deleteArea(areaAtPos.getId());
                
                if (result != null) {
                    TavernArea deletedArea = result.getDeletedArea();
                    player.sendSystemMessage(Component.literal(
                        String.format("§6[Marking Cane] §rDeleted %s %s", 
                            deletedArea.getType().getColoredName(), deletedArea.getName())
                    ));
                    
                    // Notify if they lost ownership
                    if (result.wasOwnershipLost()) {
                        player.sendSystemMessage(Component.literal(
                            "§6[Marking Cane] §eTavern is now unclaimed (last area deleted)"
                        ));
                    }
                    
                    // Sync to all players
                    tavern.syncAreasToAllClients();
                }
                
                // Clear pending deletion
                PENDING_DELETIONS.remove(playerId);
            } else {
                // First click on area - mark for deletion
                PENDING_DELETIONS.put(playerId, areaAtPos.getId());
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Marking Cane] §eClick again to delete %s %s", 
                        areaAtPos.getType().getColoredName(), areaAtPos.getName())
                ));
            }
        } else {
            // Not clicking on area - clear selection and pending deletion
            clearSelection(player);
        }
    }
    
    /**
     * Save the current selection as a tavern area with auto-generated name.
     * Used by both marking cane and commands.
     * 
     * @param player The player creating the area
     * @param type The area type
     * @param level The server level
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveArea(Player player, AreaType type, net.minecraft.server.level.ServerLevel level) {
        AreaSelection selection = getSelection(player);
        if (selection == null || !selection.isComplete()) {
            player.sendSystemMessage(Component.literal("§c[Marking Cane] §rNo area selected! Select two positions first."));
            return false;
        }
        
        Tavern tavern = Tavern.get(level);
        
        Tavern.CreationResult result = tavern.createArea(type, selection.getMinPos(), selection.getMaxPos(), player);
        TavernArea area = result.getCreatedArea();
        
        // Notify if player became owner
        if (result.becameOwner()) {
            player.sendSystemMessage(Component.literal(
                "§6[Marking Cane] §aYou are now the tavern owner!"
            ));
        }
        
        // Show success message
        player.sendSystemMessage(Component.literal(
            String.format("§6[Marking Cane] §rSaved %s (§e%d blocks§r)", 
                type.getColoredName() + " " + area.getName(), area.getVolume())
        ));
        
        // Sync to all players
        tavern.syncAreasToAllClients();
        
        // Clear selection
        clearSelection(player);
        return true;
    }
    
    /**
     * Stores a player's area selection using two BlockPos points.
     * Can be converted to Minecraft's native AABB (Axis-Aligned Bounding Box) for area operations.
     */
    public static class AreaSelection {
        private BlockPos pos1;
        private BlockPos pos2;
        
        public void setPos1(BlockPos pos) {
            this.pos1 = pos;
        }
        
        public void setPos2(BlockPos pos) {
            this.pos2 = pos;
        }
        
        public BlockPos getPos1() {
            return pos1;
        }
        
        public BlockPos getPos2() {
            return pos2;
        }
        
        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }
        
        /**
         * Creates a Minecraft AABB (Axis-Aligned Bounding Box) from the selection.
         * AABB is the standard Minecraft class for defining rectangular areas.
         * @return AABB representing the selected area, or null if selection is incomplete
         */
        public AABB createAABB() {
            if (!isComplete()) return null;
            BlockPos min = getMinPos();
            BlockPos max = getMaxPos();
            // AABB uses double coordinates, and we add 1 to max to include the full block
            return new AABB(min.getX(), min.getY(), min.getZ(), 
                           max.getX() + 1, max.getY() + 1, max.getZ() + 1);
        }
        
        /**
         * Get the size of the selection in blocks [X, Y, Z]
         */
        public int[] getSize() {
            if (!isComplete()) return new int[]{0, 0, 0};
            
            int sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
            int sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
            int sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
            
            return new int[]{sizeX, sizeY, sizeZ};
        }
        
        public int getVolume() {
            int[] size = getSize();
            return size[0] * size[1] * size[2];
        }
        
        public BlockPos getMinPos() {
            if (!isComplete()) return null;
            
            return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
            );
        }
        
        public BlockPos getMaxPos() {
            if (!isComplete()) return null;
            
            return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
            );
        }
        
        /**
         * Check if a position is within the selected area
         */
        public boolean contains(BlockPos pos) {
            AABB box = createAABB();
            return box != null && box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        }
    }
}

