package maxitoson.tavernkeeper.tavern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import maxitoson.tavernkeeper.tavern.upgrades.UpgradeDetails;
import maxitoson.tavernkeeper.tavern.upgrades.UpgradeFormatter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for viewing tavern statistics and upgrades
 * UI layer - displays information from Tavern business layer
 */
public class TavernCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tavern")
            .then(Commands.literal("stats")
                .executes(TavernCommand::showStats)
            )
            .then(Commands.literal("upgrade")
                .executes(TavernCommand::showUpgrade)
            )
            .then(Commands.literal("adjust")
                .then(Commands.literal("reputation")
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(TavernCommand::adjustReputation)
                    )
                )
                .then(Commands.literal("money")
                    .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(TavernCommand::adjustMoney)
                    )
                )
            )
        );
    }
    
    /**
     * Show basic tavern statistics
     */
    private static int showStats(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            
            player.sendSystemMessage(Component.literal("§6╔═══════════════════════════════╗"));
            player.sendSystemMessage(Component.literal("§6║      §e⭐ Tavern Statistics §e⭐      §6║"));
            player.sendSystemMessage(Component.literal("§6╠═══════════════════════════════╣"));
            
            // Owner info
            if (tavern.hasOwner()) {
                player.sendSystemMessage(Component.literal(
                    String.format("§6║ §rOwner: §e%s", tavern.getOwnerName())
                ));
                player.sendSystemMessage(Component.literal(
                    String.format("§6║ §rStatus: %s", 
                        tavern.isManuallyOpen() ? "§a✓ Open" : "§c✗ Closed")
                ));
            } else {
                player.sendSystemMessage(Component.literal("§6║ §rOwner: §7None (unclaimed)"));
            }
            
            player.sendSystemMessage(Component.literal("§6╠═══════════════════════════════╣"));
            
            // Statistics
            player.sendSystemMessage(Component.literal(
                String.format("§6║ §r💰 Total Earned: §e%d §rcopper", 
                    tavern.getTotalMoneyEarned())
            ));
            player.sendSystemMessage(Component.literal(
                String.format("§6║ §r⭐ Reputation: §e%d", 
                    tavern.getReputation())
            ));
            player.sendSystemMessage(Component.literal(
                String.format("§6║ §r👥 Customers Served: §e%d", 
                    tavern.getTotalCustomersServed())
            ));
            
            player.sendSystemMessage(Component.literal("§6╚═══════════════════════════════╝"));
            
            return 1;
        }
        return 0;
    }
    
    /**
     * Show upgrade information
     */
    private static int showUpgrade(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            
            // Get upgrade details from business layer
            UpgradeDetails details = UpgradeDetails.from(tavern);
            
            // Format and display using centralized formatter
            for (Component line : UpgradeFormatter.formatUpgradeInfo(details)) {
                player.sendSystemMessage(line);
            }
            
            return 1;
        }
        return 0;
    }
    
    /**
     * Adjust tavern reputation (useful for testing level-ups and future features)
     */
    private static int adjustReputation(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            
            int amount = IntegerArgumentType.getInteger(ctx, "amount");
            int oldReputation = tavern.getReputation();
            tavern.adjustReputation(amount);
            int newReputation = tavern.getReputation();
            
            String changeSymbol = amount >= 0 ? "+" : "";
            player.sendSystemMessage(Component.literal(
                String.format("§aReputation: §e%d §a→ §e%d §7(%s%d)", 
                    oldReputation, newReputation, changeSymbol, amount)
            ));
            
            return 1;
        }
        return 0;
    }
    
    /**
     * Adjust tavern total money earned (useful for testing level-ups and future features)
     */
    private static int adjustMoney(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            Tavern tavern = Tavern.get(level);
            
            int amount = IntegerArgumentType.getInteger(ctx, "amount");
            long oldMoney = tavern.getTotalMoneyEarned();
            tavern.adjustMoney(amount);
            long newMoney = tavern.getTotalMoneyEarned();
            
            String changeSymbol = amount >= 0 ? "+" : "";
            player.sendSystemMessage(Component.literal(
                String.format("§aMoney Earned: §e%d §a→ §e%d §7(%s%d)", 
                    oldMoney, newMoney, changeSymbol, amount)
            ));
            
            return 1;
        }
        return 0;
    }
}

