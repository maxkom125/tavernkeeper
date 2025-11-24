package maxitoson.tavernkeeper.areas;

import net.minecraft.ChatFormatting;

/**
 * Types of areas that can be defined in a tavern
 */
public enum AreaType {
    DINING("Dining Area", "A place where guests eat meals", ChatFormatting.YELLOW, 0xFFFF00),
    SLEEPING("Sleeping Area", "A place where guests can rest", ChatFormatting.BLUE, 0x5555FF),
    SERVICE("Service Area", "A place where guests order food", ChatFormatting.GREEN, 0x55FF55);
    
    private final String displayName;
    private final String description;
    private final ChatFormatting chatColor;
    private final int renderColor; // RGB color for rendering (0xRRGGBB)
    
    AreaType(String displayName, String description, ChatFormatting chatColor, int renderColor) {
        this.displayName = displayName;
        this.description = description;
        this.chatColor = chatColor;
        this.renderColor = renderColor;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ChatFormatting getChatColor() {
        return chatColor;
    }
    
    public String getColoredName() {
        return chatColor + displayName + ChatFormatting.RESET;
    }
    
    /**
     * Get the RGB color as an integer (0xRRGGBB format)
     */
    public int getColor() {
        return renderColor;
    }
    
    /**
     * Get RGB color components for rendering (0.0 - 1.0)
     */
    public float getRed() {
        return ((renderColor >> 16) & 0xFF) / 255.0f;
    }
    
    public float getGreen() {
        return ((renderColor >> 8) & 0xFF) / 255.0f;
    }
    
    public float getBlue() {
        return (renderColor & 0xFF) / 255.0f;
    }
}

