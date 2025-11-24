package maxitoson.tavernkeeper.areas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import maxitoson.tavernkeeper.items.MarkingCane;
import maxitoson.tavernkeeper.network.NetworkHandler;
import maxitoson.tavernkeeper.network.SyncAreasPacket;
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
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> saveArea(ctx, AreaType.DINING))
                    )
                )
                .then(Commands.literal("sleeping")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> saveArea(ctx, AreaType.SLEEPING))
                    )
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
            String name = StringArgumentType.getString(ctx, "name");
            
            if (MarkingCane.saveArea(player, name, type)) {
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
                TavernArea area = toDelete.getArea();
                tavern.deleteArea(area.getId());
                player.sendSystemMessage(Component.literal(
                    String.format("§6[Tavern Area] §rDeleted %s '%s'", 
                        area.getType().getColoredName(), name)
                ));
                
                // Sync areas to all players
                java.util.List<TavernArea> areas = tavern.getAllSpaces().stream()
                    .map(BaseSpace::getArea)
                    .toList();
                NetworkHandler.sendToAllPlayers(new SyncAreasPacket(areas));
                
                return 1;
            } else {
                player.sendSystemMessage(Component.literal(
                    "§c[Tavern Area] §rArea '" + name + "' not found!"
                ));
                return 0;
            }
        }
        return 0;
    }
}

