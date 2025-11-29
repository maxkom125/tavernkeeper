package maxitoson.tavernkeeper.areas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import maxitoson.tavernkeeper.items.MarkingCane;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.spaces.BaseSpace;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Commands for managing tavern areas
 */
public class AreaCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tavernarea")
            .then(Commands.literal("save")
                .then(Commands.literal("dining")
                    .executes(ctx -> saveArea(ctx, AreaType.DINING))
                )
                .then(Commands.literal("sleeping")
                    .executes(ctx -> saveArea(ctx, AreaType.SLEEPING))
                )
            )
            .then(Commands.literal("list")
                .executes(AreaCommand::listAreas)
            )
            .then(Commands.literal("clear")
                .executes(AreaCommand::clearSelection)
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(AreaCommand::deleteArea)
                )
            )
        );
    }
    
    private static int saveArea(CommandContext<CommandSourceStack> ctx, AreaType type) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            if (MarkingCane.saveArea(player, type, player.serverLevel())) {
                return 1;
            } else {
                player.sendSystemMessage(Component.literal("§c[Tavern Area] §rFailed to save area. Make sure you've selected an area first!"));
                return 0;
            }
        }
        return 0;
    }
    
    private static int listAreas(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            Collection<BaseSpace> spaces = tavern.getAllSpaces();
            
            // Show tavern owner info
            if (tavern.hasOwner()) {
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Tavern Area] §rOwner: §e%s", tavern.getOwnerName())
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§6[Tavern Area] §rNo owner yet (create an area to claim ownership)"
                ));
            }
            
            if (spaces.isEmpty()) {
                player.sendSystemMessage(Component.literal("§6[Tavern Area] §rNo areas defined yet."));
            } else {
                player.sendSystemMessage(Component.literal("§6[Tavern Area] §rDefined areas:"));
                for (BaseSpace space : spaces) {
                    TavernArea area = space.getArea();
                    player.sendSystemMessage(Component.literal(
                        String.format("  - %s '%s' (%d blocks)", 
                            area.getType().getColoredName(), 
                            area.getName(), 
                            area.getVolume())
                    ));
                }
            }
            return 1;
        }
        return 0;
    }
    
    private static int clearSelection(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            MarkingCane.clearSelection(player);
            return 1;
        }
        return 0;
    }
    
    private static int deleteArea(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            String name = StringArgumentType.getString(ctx, "name");
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            
            // Find space by area name
            BaseSpace toDelete = null;
            for (BaseSpace space : tavern.getAllSpaces()) {
                if (space.getArea().getName().equals(name)) {
                    toDelete = space;
                    break;
                }
            }
            
            if (toDelete != null) {
                Tavern.DeletionResult result = tavern.deleteArea(toDelete.getArea().getId());
                
                if (result != null) {
                    TavernArea deletedArea = result.getDeletedArea();
                    player.sendSystemMessage(Component.literal(
                        String.format("§6[Tavern Area] §rDeleted %s '%s'", 
                            deletedArea.getType().getColoredName(), deletedArea.getName())
                    ));
                    
                    // Notify if they lost ownership
                    if (result.wasOwnershipLost()) {
                        player.sendSystemMessage(Component.literal(
                            "§6[Tavern Area] §eTavern is now unclaimed (last area deleted)"
                        ));
                    }
                    
                    // Sync areas to all players
                    tavern.syncAreasToAllClients();
                    
                    return 1;
                }
            }
            
            player.sendSystemMessage(Component.literal(
                "§c[Tavern Area] §rArea '" + name + "' not found!"
            ));
            return 0;
        }
        return 0;
    }
}

