package maxitoson.tavernkeeper.items;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.network.NetworkHandler;
import maxitoson.tavernkeeper.network.SyncAreasPacket;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarkingCane extends Item {
    
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
    
    // Right-click on block - sets first position, then second position and auto-saves
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        
        if (player == null) return InteractionResult.PASS;
        
        if (!level.isClientSide) {
            UUID playerId = player.getUUID();
            
            // Right-click cancels pending deletion
            PENDING_DELETIONS.remove(playerId);
            
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
     * Auto-save area with the current mode and auto-generated name
     */
    private void autoSaveArea(Player player, net.minecraft.server.level.ServerLevel level) {
        AreaSelection selection = getSelection(player);
        if (selection == null || !selection.isComplete()) {
            return;
        }
        
        // Get current mode (imported from AreaMode)
        maxitoson.tavernkeeper.areas.AreaType currentMode = 
            maxitoson.tavernkeeper.areas.AreaMode.getMode(player);
        
        // Generate auto-name with number
        Tavern tavern = Tavern.get(level);
        String autoName = generateAutoName(tavern, currentMode);
        
        // Save the area through Tavern (aggregate root)
        BaseSpace space = tavern.createArea(autoName, currentMode, 
            selection.getMinPos(), selection.getMaxPos());
        TavernArea area = space.getArea();
        
        player.sendSystemMessage(Component.literal(
            String.format("§6[Marking Cane] §rSaved %s (§e%d blocks§r)", 
                currentMode.getColoredName() + " " + autoName, area.getVolume())
        ));
        
        // Sync areas to all players
        java.util.List<TavernArea> areas = tavern.getAllSpaces().stream()
            .map(BaseSpace::getArea)
            .toList();
        NetworkHandler.sendToAllPlayers(new SyncAreasPacket(areas));
        
        // Clear selection
        clearSelection(player);
    }
    
    /**
     * Generate auto-name like "Dining #1", "Sleeping #2"
     * Uses an ever-incrementing counter per type
     */
    private String generateAutoName(Tavern tavern, 
                                    maxitoson.tavernkeeper.areas.AreaType type) {
        int nextNumber = tavern.getNextCounter(type);
        return "#" + nextNumber;
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
                String areaName = areaAtPos.getName();
                AreaType areaType = areaAtPos.getType();
                tavern.deleteArea(areaAtPos.getId());
                
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Marking Cane] §rDeleted %s %s", 
                        areaType.getColoredName(), areaName)
                ));
                
                // Sync to all players
                java.util.List<TavernArea> areas = tavern.getAllSpaces().stream()
                    .map(BaseSpace::getArea)
                    .toList();
                NetworkHandler.sendToAllPlayers(new SyncAreasPacket(areas));
                
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
     * Save the current selection as a tavern area
     */
    public static boolean saveArea(Player player, String name, AreaType type) {
        AreaSelection selection = getSelection(player);
        if (selection == null || !selection.isComplete()) {
            player.sendSystemMessage(Component.literal("§c[Marking Cane] §rNo area selected! Select two positions first."));
            return false;
        }
        
        if (!player.level().isClientSide && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Tavern tavern = Tavern.get(serverLevel);
            BaseSpace space = tavern.createArea(name, type, selection.getMinPos(), selection.getMaxPos());
            TavernArea area = space.getArea();
            
            player.sendSystemMessage(Component.literal(
                String.format("§6[Marking Cane] §rSaved %s '%s' (%d blocks)", 
                    type.getColoredName(), name, area.getVolume())
            ));
            
            // Clear selection after saving
            clearSelection(player);
            return true;
        }
        return false;
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

