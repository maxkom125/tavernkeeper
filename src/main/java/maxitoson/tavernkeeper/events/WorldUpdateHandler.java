package maxitoson.tavernkeeper.events;

import com.mojang.logging.LogUtils;
import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.entities.CustomerEntity;
import maxitoson.tavernkeeper.tavern.Tavern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;

/**
 * Handles world state changes: block placement, block breaking, entity spawning.
 * 
 * Responsibilities:
 * - Update furniture when blocks are placed/broken in tavern areas
 * - Clear tavern sign references when sign is broken/replaced
 * - Configure mob AI when entities spawn (e.g., skeletons targeting customers)
 */
@EventBusSubscriber(modid = TavernKeeperMod.MODID)
public class WorldUpdateHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Handle block placement to update furniture in tavern areas.
     * Also clears tavern sign if replaced with non-sign block.
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos pos = event.getPos();
            Tavern tavern = Tavern.get(serverLevel);
            
            // If placing a block at the tavern sign position, clear it (sign was replaced)
            if (tavern.isTavernSign(pos) && !(event.getState().getBlock() instanceof net.minecraft.world.level.block.SignBlock)) {
                LOGGER.info("Block placed at tavern sign position {} - clearing tavern sign", pos);
                tavern.clearTavernSign();
            }
            
            // Notify all spaces at this position about the block update
            tavern.getSpacesAt(pos).forEach(space -> space.onBlockUpdated(pos, event.getState()));
        }
    }
    
    /**
     * Handle block breaking to remove furniture from tavern areas.
     * Also clears tavern sign reference when sign is broken.
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.core.BlockPos pos = event.getPos();
            Tavern tavern = Tavern.get(serverLevel);
            
            // If breaking the tavern sign block, clear the reference
            if (tavern.isTavernSign(pos)) {
                LOGGER.info("Tavern sign at {} was broken - clearing tavern sign", pos);
                tavern.clearTavernSign();
            }
            
            // Pass the OLD state to handle multi-block removal correctly
            tavern.getSpacesAt(pos).forEach(space -> space.onBlockBroken(pos, event.getState()));
        }
    }
    
    /**
     * Configure entity AI when entities join the world.
     * Makes skeletons target customers (zombies already target AbstractVillager naturally).
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.AbstractSkeleton skeleton && !event.getLevel().isClientSide()) {
            // Add a goal to target customers (priority 3, same as zombie's villager targeting)
            skeleton.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                skeleton, 
                CustomerEntity.class, 
                true
            ));
        }
    }
}

