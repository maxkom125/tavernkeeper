package maxitoson.tavernkeeper.tavern.upgrades;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for formatting upgrade information for display
 * UI layer - converts UpgradeDetails (data) into formatted Components (display)
 * 
 * Pattern: Centralized formatting for consistency across commands and event handlers
 */
public class UpgradeFormatter {
    
    /**
     * Format a complete upgrade info display with borders
     * Used by /tavern upgrade command
     */
    public static List<Component> formatUpgradeInfo(UpgradeDetails details) {
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.literal("§6╔═══════════════════════════════╗"));
        lines.add(Component.literal("§6║       §e⬆ Tavern Upgrades §e⬆       §6║"));
        lines.add(Component.literal("§6╠═══════════════════════════════╣"));
        
        // Current level
        lines.add(Component.literal(
            String.format("§6║ §rCurrent Level: §e%s", details.getCurrentLevel().getDisplayName())
        ));
        
        // Current benefits
        lines.add(Component.literal("§6║ §rCurrent Capacity:"));
        lines.add(Component.literal(
            String.format("§6║   §r• Max Tables: §e%d", details.getCurrentMaxTables())
        ));
        lines.add(Component.literal(
            String.format("§6║   §r• Max Chairs: §e%d", details.getCurrentMaxChairs())
        ));
        lines.add(Component.literal("§6║ §rTavern Reputation Effects:"));
        lines.add(Component.literal(
            String.format("§6║   §r• Customer Attraction: §e%.0f%%", details.getCurrentSpawnRate() * 100)
        ));
        lines.add(Component.literal(
            String.format("§6║   §r• Customer Generosity: §e%.0f%%", details.getCurrentPaymentMultiplier() * 100)
        ));
        
        lines.add(Component.literal("§6╠═══════════════════════════════╣"));
        
        // Next level info or max level
        if (details.hasNextLevel()) {
            lines.addAll(formatNextLevelInfo(details));
        } else {
            lines.add(Component.literal("§6║"));
            lines.add(Component.literal("§6║ §e§l✨ MAXIMUM PRESTIGE ACHIEVED! ✨"));
            lines.add(Component.literal("§6║ §7Your tavern has reached its full potential!"));
        }
        
        lines.add(Component.literal("§6╚═══════════════════════════════╝"));
        
        return lines;
    }
    
    /**
     * Format next level information with requirements and benefits
     */
    private static List<Component> formatNextLevelInfo(UpgradeDetails details) {
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.literal(
            String.format("§6║ §rNext Level: §e%s %s", 
                details.getNextLevel().getDisplayName(),
                details.canUpgrade() ? "§a§l✓ READY TO UPGRADE!" : "")
        ));
        
        lines.add(Component.literal("§6║ §rRequirements to Unlock:"));
        
        // Money requirement
        boolean moneyMet = details.isMoneyRequirementMet();
        if (moneyMet) {
            lines.add(Component.literal(
                String.format("§6║   §a✓ §rEarnings: §e%d§r/§e%d §rcopper §a✓",
                    details.getCurrentMoney(),
                    details.getRequiredMoney())
            ));
        } else {
            long remaining = details.getRequiredMoney() - details.getCurrentMoney();
            lines.add(Component.literal(
                String.format("§6║   §c✗ §rEarnings: §e%d§r/§e%d §rcopper §7(%d more needed)",
                    details.getCurrentMoney(),
                    details.getRequiredMoney(),
                    remaining)
            ));
        }
        
        // Reputation requirement
        boolean repMet = details.isReputationRequirementMet();
        if (repMet) {
            lines.add(Component.literal(
                String.format("§6║   §a✓ §rReputation: §e%d§r/§e%d §a✓",
                    details.getCurrentReputation(),
                    details.getRequiredReputation())
            ));
        } else {
            int remaining = details.getRequiredReputation() - details.getCurrentReputation();
            lines.add(Component.literal(
                String.format("§6║   §c✗ §rReputation: §e%d§r/§e%d §7(%d more needed)",
                    details.getCurrentReputation(),
                    details.getRequiredReputation(),
                    remaining)
            ));
        }
        
        // Show what's new in next level
        lines.add(Component.literal("§6║ §rWhat's New:"));
        lines.addAll(formatUpgradeChanges(details));
        
        if (details.canUpgrade()) {
            lines.add(Component.literal("§6║"));
            lines.add(Component.literal(
                "§6║ §a§l✨ Your tavern will upgrade automatically! ✨"
            ));
            lines.add(Component.literal(
                "§6║ §7Keep serving customers to trigger the upgrade!"
            ));
        }
        
        return lines;
    }
    
    /**
     * Format what changes between current and next level
     * Shows increases/improvements with game-oriented descriptions
     */
    private static List<Component> formatUpgradeChanges(UpgradeDetails details) {
        List<Component> lines = new ArrayList<>();
        
        // Max Tables change
        if (details.getNextMaxTables() != null && 
            details.getNextMaxTables() > details.getCurrentMaxTables()) {
            lines.add(Component.literal(
                String.format("§6║   §r• Max Tables: §7%d §r→ §e%d §a(+%d)",
                    details.getCurrentMaxTables(),
                    details.getNextMaxTables(),
                    details.getNextMaxTables() - details.getCurrentMaxTables())
            ));
        }
        
        // Max Chairs change
        if (details.getNextMaxChairs() != null && 
            details.getNextMaxChairs() > details.getCurrentMaxChairs()) {
            lines.add(Component.literal(
                String.format("§6║   §r• Max Chairs: §7%d §r→ §e%d §a(+%d)",
                    details.getCurrentMaxChairs(),
                    details.getNextMaxChairs(),
                    details.getNextMaxChairs() - details.getCurrentMaxChairs())
            ));
        }
        
        // Spawn Rate change
        if (details.getNextSpawnRate() != null && 
            details.getNextSpawnRate() > details.getCurrentSpawnRate()) {
            float increasePercent = (details.getNextSpawnRate() - details.getCurrentSpawnRate()) * 100;
            lines.add(Component.literal(
                String.format("§6║   §a✓ §rYour tavern attracts more customers §e(+%.0f%%)",
                    increasePercent)
            ));
        }
        
        // Payment change
        if (details.getNextPaymentMultiplier() != null && 
            details.getNextPaymentMultiplier() > details.getCurrentPaymentMultiplier()) {
            float increasePercent = (details.getNextPaymentMultiplier() - details.getCurrentPaymentMultiplier()) * 100;
            lines.add(Component.literal(
                String.format("§6║   §a✓ §rCustomers pay more generously §e(+%.0f%%)",
                    increasePercent)
            ));
        }
        
        return lines;
    }
    
    /**
     * Format a simple upgrade notification (for broadcasts)
     * Used when tavern auto-upgrades
     */
    public static List<Component> formatUpgradeNotification(TavernUpgrade oldLevel, TavernUpgrade newLevel) {
        List<Component> lines = new ArrayList<>();
        
        lines.add(Component.literal(
            "§6✨ [Tavern Keeper] §r§a§lTavern upgraded to " + 
            newLevel.getDisplayName() + "! §r§a✨"
        ));
        
        // Show capacity changes (old → new)
        lines.add(Component.literal(
            String.format("§7  Capacity: §f%d §7→ §a%d §7tables, §f%d §7→ §a%d §7chairs",
                oldLevel.getMaxTables(),
                newLevel.getMaxTables(),
                oldLevel.getMaxChairs(),
                newLevel.getMaxChairs())
        ));
        
        // Show reputation effects (old → new bonus)
        float oldPaymentBonus = (oldLevel.getPaymentMultiplier() - 1.0f) * 100;
        float newPaymentBonus = (newLevel.getPaymentMultiplier() - 1.0f) * 100;
        float oldSpawnBonus = (oldLevel.getSpawnRateMultiplier() - 1.0f) * 100;
        float newSpawnBonus = (newLevel.getSpawnRateMultiplier() - 1.0f) * 100;
        
        lines.add(Component.literal(
            String.format("§7  Reputation: Customers pay §f+%.0f%% §7→ §a+%.0f%% §7more, visit §f+%.0f%% §7→ §a+%.0f%% §7more often",
                oldPaymentBonus,
                newPaymentBonus,
                oldSpawnBonus,
                newSpawnBonus)
        ));
        
        return lines;
    }
}

