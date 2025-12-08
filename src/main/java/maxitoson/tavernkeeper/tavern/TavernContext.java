package maxitoson.tavernkeeper.tavern;

import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import maxitoson.tavernkeeper.tavern.furniture.Chair;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import maxitoson.tavernkeeper.tavern.furniture.ServiceReceptionDesk;
import net.minecraft.core.BlockPos;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface defining what components can query from Tavern
 * 
 * This follows the Interface Segregation Principle (ISP):
 * - Components (Managers, Entities, AI Behaviors) depend on an abstraction, not the concrete Tavern
 * - Compile-time enforcement: Components can ONLY call these methods
 * - Each component gets exactly what it needs, no more
 * 
 * Benefits:
 * - Prevents direct access to Tavern internals
 * - Prevents components from accessing other components (getDiningManager() not exposed)
 * - Clear contract: "This is what you can ask from Tavern"
 * - Easier to test: Mock this interface instead of entire Tavern
 * 
 * Used by:
 * - Managers (to access tavern state and record statistics)
 * - Entities (to adjust reputation and notify players)
 * - AI Behaviors (to find and reserve resources like chairs, beds, lecterns)
 */
public interface TavernContext {
    /**
     * Check if tavern is open for business
     * Used by CustomerManager to determine if customers should spawn
     */
    boolean isOpen();
    
    /**
     * Get a random center point for the tavern (e.g., random lectern)
     * Used by CustomerManager to determine where customers spawn around
     */
    BlockPos getTavernCenter();
    
    // ========== Statistics Queries ==========
    
    /**
     * Get total money earned by the tavern (in copper coins)
     */
    long getTotalMoneyEarned();
    
    /**
     * Get current tavern reputation
     */
    int getReputation();
    
    /**
     * Get total customers served
     */
    int getTotalCustomersServed();
    
    // ========== Statistics Recording ==========
    
    /**
     * Record a sale (customer payment)
     * Updates statistics and checks for upgrade unlocks
     * 
     * @param copperAmount Amount in copper coins
     */
    void recordSale(int copperAmount);
    
    /**
     * Adjust tavern reputation (positive to increase, negative to decrease)
     * Triggers upgrade checks and persistence
     * 
     * @param amount Amount to change (can be positive or negative)
     */
    void adjustReputation(int amount);
    
    // ========== Server Access ==========
    
    /**
     * Get the server level for broadcasting messages
     * Used by entities to send messages to all players
     */
    net.minecraft.server.level.ServerLevel getLevel();
    
    // ========== AI Behavior Queries ==========
    // Methods used by customer AI behaviors to find and reserve resources
    
    /**
     * Find nearest available chair for a customer
     * Used by FindSeat behavior
     */
    Optional<Chair> findNearestAvailableChair(BlockPos from, double maxDistance);
    
    /**
     * Reserve a chair for a customer
     * Used by FindSeat behavior
     * @return true if successfully reserved, false if already occupied
     */
    boolean reserveChair(BlockPos chairPos, UUID customerId);
    
    /**
     * Release a chair reservation
     * Used by FindSeat and EatAtChair behaviors when customer is done
     */
    void releaseChair(BlockPos chairPos);
    
    /**
     * Check if a chair exists at the given position
     * Used by EatAtChair to detect if chair was destroyed
     * @return true if position has a valid chair (any type), false otherwise
     */
    boolean hasChairAt(BlockPos chairPos);
    
    /**
     * Find nearest service lectern
     * Used by MoveToLectern behavior
     */
    Optional<ServiceLectern> findNearestLectern(BlockPos from, double maxDistance);
    
    /**
     * Find nearest reception desk
     * Used by MoveToReceptionDesk behavior
     */
    Optional<ServiceReceptionDesk> findNearestReceptionDesk(BlockPos from, double maxDistance);
    
    /**
     * Find nearest available bed for a customer
     * Used by FindBed behavior
     */
    Optional<BlockPos> findNearestAvailableBed(BlockPos from, double maxDistance);
    
    /**
     * Reserve a bed for a customer
     * Used by FindBed behavior
     * @return true if successfully reserved, false if already occupied
     */
    boolean reserveBed(BlockPos bedPos, UUID customerId);
    
    /**
     * Release a bed reservation
     * Used by FindBed and SleepInBed behaviors when customer is done
     */
    void releaseBed(BlockPos bedPos);
    
    /**
     * Check if a bed exists at the given position
     * Used by SleepInBed to detect if bed was destroyed
     * @return true if position has a valid bed (any type), false otherwise
     */
    boolean hasBedAt(BlockPos bedPos);
    
    /**
     * Check if a bed is reserved by a specific customer
     * Used by SleepInBed to verify reservation
     * @return true if bed is reserved by this customer, false otherwise
     */
    boolean isBedReservedBy(BlockPos bedPos, UUID customerId);
    
    /**
     * Check if a chair is reserved by a specific customer
     * Used by EatAtChair to verify reservation
     * @return true if chair is reserved by this customer, false otherwise
     */
    boolean isChairReservedBy(BlockPos chairPos, UUID customerId);
    
    /**
     * Create a new food request for a customer
     * Used by WaitAtLectern behavior when customer places order
     */
    FoodRequest createFoodRequest();
    
    /**
     * Create a new sleeping request for a customer
     * Used by WaitAtReceptionDesk behavior when customer requests room
     */
    SleepingRequest createSleepingRequest();
}

