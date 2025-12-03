package maxitoson.tavernkeeper.tavern.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class for sign-related operations.
 * Handles both regular signs and hanging signs (ceiling/wall-mounted).
 */
public class SignHelper {
    
    /**
     * Check if a block is any type of sign (regular or hanging).
     */
    public static boolean isAnySign(BlockState state) {
        return state.getBlock() instanceof SignBlock ||
               state.getBlock() instanceof CeilingHangingSignBlock ||
               state.getBlock() instanceof WallHangingSignBlock;
    }
    
    /**
     * Update sign text at the given position.
     * Works for both regular signs and hanging signs.
     * 
     * @param level The server level
     * @param pos The position of the sign
     * @param line1 Text for line 1 (0-indexed)
     * @param line2 Text for line 2
     * @param line3 Text for line 3
     * @param line4 Text for line 4
     */
    public static void updateSignText(ServerLevel level, BlockPos pos, 
                                     Component line1, Component line2, 
                                     Component line3, Component line4) {
        var blockEntity = level.getBlockEntity(pos);
        
        // Both SignBlockEntity and HangingSignBlockEntity extend SignBlockEntity
        if (blockEntity instanceof SignBlockEntity signEntity) {
            var frontText = signEntity.getFrontText();
            frontText = frontText.setMessage(0, line1);
            frontText = frontText.setMessage(1, line2);
            frontText = frontText.setMessage(2, line3);
            frontText = frontText.setMessage(3, line4);
            
            signEntity.setText(frontText, true);
            signEntity.setChanged();
            
            // Sync to clients
            level.sendBlockUpdated(pos, 
                level.getBlockState(pos), 
                level.getBlockState(pos), 
                3);
        }
    }
}

