package maxitoson.tavernkeeper.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import maxitoson.tavernkeeper.entities.ai.behavior.CustomerGoalPackages;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
import maxitoson.tavernkeeper.entities.ai.LifecycleType;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycle;
import maxitoson.tavernkeeper.entities.ai.lifecycle.CustomerLifecycleFactory;
import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.tavern.Tavern;
import maxitoson.tavernkeeper.tavern.TavernContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import maxitoson.tavernkeeper.tavern.economy.CustomerRequest;
import maxitoson.tavernkeeper.tavern.economy.FoodRequest;
import maxitoson.tavernkeeper.tavern.economy.SleepingRequest;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
// import net.minecraft.world.entity.npc.Villager; // not used, just for reference
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * Customer entity that visits the tavern
 * Lifecycle: Spawn -> Queue at Lectern -> Get Food -> Sit at Chair -> Leave
 * 
 * Extends AbstractVillager so zombies/skeletons will attack customers
 * Uses custom Brain system (not Villager's profession/trading system)
 */
public class CustomerEntity extends AbstractVillager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // Entity attributes - similar to Villager constants (Villager line 116)
    public static final float SPEED_MODIFIER = 0.9F; // Speed multiplier for AI behaviors
    public static final double MOVEMENT_SPEED = 0.5; // Base movement speed attribute
    public static final double MAX_HEALTH = 20.0;    // Health points
    public static final double FOLLOW_RANGE = 48.0;  // How far entity can detect targets
    
    private CustomerLifecycle lifecycle = null; // Manages customer's journey through tavern
    
    // Customer state tracking (lifecycle will set correct initial state)
    private CustomerState customerState = CustomerState.LEAVING;
    private CustomerState stateBeforePanic = null;
    private net.minecraft.core.BlockPos targetPosition = null; // Target position (lectern, reception desk, chair, or bed)
    private CustomerRequest request = null; // Customer's current request (food, sleeping, etc.)
    private net.minecraft.core.BlockPos spawnPosition = null; // Where customer spawned (for returning when leaving)
    
    // Panic tracking - cumulative total time across all panic episodes
    private long totalPanicTicks = 0; // Total ticks spent in panic (accumulates across episodes)
    private static final long MAX_PANIC_DURATION = 1200; // 60 seconds (20 ticks per second)
    
    // Brain configuration - similar to Villager (line 131-132)
    // Note: We add NEAREST_HOSTILE for panic behavior
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES;
    private static final ImmutableList<SensorType<? extends Sensor<? super CustomerEntity>>> SENSOR_TYPES;
    
    public CustomerEntity(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(false); // Don't pick up items like villagers do
        
        // Enable navigation (same as Villager line 142-143)
        ((net.minecraft.world.entity.ai.navigation.GroundPathNavigation)this.getNavigation()).setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        
    }
    
    @Override
    public boolean canBeLeashed() {
        return false; // Can't be leashed like villagers
    }
    
    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity entity) {
        // Make customers not attack each other or players
        return entity instanceof CustomerEntity || entity instanceof net.minecraft.world.entity.player.Player || super.isAlliedTo(entity);
    }
    
    @Override
    public boolean canTakeItem(net.minecraft.world.item.ItemStack stack) {
        // Customers don't pick up items from ground
        return false;
    }
    
    // AbstractVillager abstract methods - customers don't trade
    @Override
    protected void updateTrades() {
        // Customers don't have trades - leave empty
    }
    
    @Override
    protected void rewardTradeXp(net.minecraft.world.item.trading.MerchantOffer offer) {
        // Customers don't trade - leave empty
    }
    
    // AgeableMob abstract method - customers don't breed
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null; // Customers don't breed
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Brain<CustomerEntity> getBrain() {
        return (Brain<CustomerEntity>) super.getBrain();
    }
    
    // Similar to Villager.brainProvider() (line 152-154)
    @Override
    protected Brain.Provider<CustomerEntity> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }
    
    // Similar to Villager.makeBrain() (line 156-160)
    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        Brain<CustomerEntity> brain = this.brainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }
    
    // Similar to Villager.registerBrainGoals() (line 169-191)
    private void registerBrainGoals(Brain<CustomerEntity> brain) {
        float speedModifier = SPEED_MODIFIER; // Use class constant for behavior speed
        
        // Add activities with their behavior packages (same order as Villager)
        brain.addActivity(Activity.CORE, CustomerGoalPackages.getCorePackage(speedModifier));
        brain.addActivity(Activity.REST, CustomerGoalPackages.getRestPackage(speedModifier));
        brain.addActivity(Activity.IDLE, CustomerGoalPackages.getIdlePackage(speedModifier));
        brain.addActivity(Activity.PANIC, CustomerGoalPackages.getPanicPackage(speedModifier));
        
        // Set up activity priorities (same as Villager line 187-189)
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
        
        // Note: We skip brain.updateActivityFromSchedule() because customers don't have schedules
        // They will leave at night via custom logic (not schedule-based)
        
        LOGGER.info("Customer brain initialized with {} activities", 4);
    }
    
    // Similar to Villager.createAttributes() (line 201-203)
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }
    
    // Similar to Villager.customServerAiStep() (line 209-247)
    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("customerBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        
        // Track cumulative panic time across all panic episodes
        if (isCurrentlyPanicking()) {
            totalPanicTicks++;
            
            // Check if total panic time exceeds maximum (60 seconds)
            if (totalPanicTicks >= MAX_PANIC_DURATION) {
                // Customer has been panicking for too long (cumulative) - trigger event
                LOGGER.info("Customer {} exceeded max total panic time: {} ticks ({}s), despawning", 
                    this.getId(), totalPanicTicks, totalPanicTicks / 20);
                
                decreaseReputationAndNotify(-5, "A customer was terrified for too long near your tavern! (-5 reputation)");
                this.discard();
            }
        }
        
        // Call parent LAST - same as Villager line 246
        super.customServerAiStep();
    }
    
    // Customer state management
    public CustomerState getCustomerState() {
        return customerState;
    }
    
    public void setCustomerState(CustomerState state) {
        if (this.customerState != state) {
            LOGGER.info("Customer {} state change: {} -> {}", 
                this.getId(), this.customerState, state);
            this.customerState = state;
        }
    }
    
    /**
     * Store current state before panicking, so we can restore it later
     */
    public void saveStateBeforePanic() {
        this.stateBeforePanic = this.customerState;
        LOGGER.debug("Customer {} saved state before panic: {}", this.getId(), this.stateBeforePanic);
    }
    
    // Panic time tracking - cumulative across all panic episodes
    public long getTotalPanicTicks() {
        return totalPanicTicks;
    }
    
    public boolean isCurrentlyPanicking() {
        return this.getBrain().isActive(Activity.PANIC);
    }
    
    /**
     * Decrease tavern reputation and broadcast message to players.
     * Uses TavernContext to avoid direct dependency on Tavern implementation.
     */
    private void decreaseReputationAndNotify(int reputationChange, String message) {
        if (this.level() instanceof ServerLevel serverLevel) {
            TavernContext tavern = Tavern.get(serverLevel);
            if (tavern != null) {
                tavern.adjustReputation(reputationChange);
                
                // Broadcast message to all players via context
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(message), 
                    false
                );
            }
        }
    }
    
    // Target position management (lectern, chair, etc.)
    public net.minecraft.core.BlockPos getTargetPosition() {
        return targetPosition;
    }
    
    public void setTargetPosition(net.minecraft.core.BlockPos pos) {
        this.targetPosition = pos;
    }
    
    public CustomerState getStateBeforePanic() {
        return stateBeforePanic;
    }
    
    // Request management - unified interface for all request types
    public CustomerRequest getRequest() {
        return request;
    }
    
    public void setRequest(CustomerRequest request) {
        this.request = request;
    }
    
    // Type-safe convenience getters (return null if wrong type)
    public FoodRequest getFoodRequest() {
        return request instanceof FoodRequest ? (FoodRequest) request : null;
    }
    
    public SleepingRequest getSleepingRequest() {
        return request instanceof SleepingRequest ? (SleepingRequest) request : null;
    }
    
    // Customer type management - derived from lifecycle (single source of truth)
    /**
     * Get customer type - derived from lifecycle
     * Returns DINING_ONLY as default if no lifecycle is set
     */
    public LifecycleType getLifecycleType() {
        return lifecycle != null ? lifecycle.getType() : LifecycleType.DINING_ONLY;
    }
    
    // Lifecycle management (brain-like layer)
    public CustomerLifecycle getLifecycle() {
        return lifecycle;
    }
    
    /**
     * Set the customer's lifecycle - determines their entire journey
     * This should be called once at spawn time
     */
    public void setLifecycle(CustomerLifecycle lifecycle) {
        this.lifecycle = lifecycle;
        this.customerState = lifecycle.getInitialState();
        LOGGER.info("Customer {} assigned lifecycle: {} ({})", 
            this.getId(), lifecycle.getType(), lifecycle.getDescription());
    }
    
    /**
     * Transition to the next state based on lifecycle rules
     * Call this when a state completes (e.g., finished eating, woke up)
     */
    public void transitionToNextState(net.minecraft.server.level.ServerLevel level) {
        if (lifecycle == null) {
            LOGGER.error("Customer {} has no lifecycle, defaulting to LEAVING", this.getId());
            setCustomerState(CustomerState.LEAVING);
            return;
        }
        
        CustomerState currentState = this.customerState;
        CustomerState nextState = lifecycle.getNextState(currentState, level);
        
        setCustomerState(nextState);
        LOGGER.debug("Customer {} transitioned from {} to {}", this.getId(), currentState, nextState);
    }
    
    // Spawn position management
    public net.minecraft.core.BlockPos getSpawnPosition() {
        return spawnPosition;
    }
    
    public void setSpawnPosition(net.minecraft.core.BlockPos pos) {
        this.spawnPosition = pos;
    }
    
    // NBT serialization - save customer data when world is saved
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        
        // Save spawn position
        if (this.spawnPosition != null) {
            tag.putInt("SpawnX", this.spawnPosition.getX());
            tag.putInt("SpawnY", this.spawnPosition.getY());
            tag.putInt("SpawnZ", this.spawnPosition.getZ());
        }
        
        // Save customer state
        tag.putString("CustomerState", this.customerState.name());
        
        // Save lifecycle type (to recreate lifecycle on load)
        if (this.lifecycle != null) {
            tag.putString("LifecycleType", this.lifecycle.getType().name());
        }
        
        // Save target position if exists
        if (this.targetPosition != null) {
            tag.putInt("TargetX", this.targetPosition.getX());
            tag.putInt("TargetY", this.targetPosition.getY());
            tag.putInt("TargetZ", this.targetPosition.getZ());
        }
        
        // Save panic tracking (cumulative total)
        tag.putLong("TotalPanicTicks", this.totalPanicTicks);
    }
    
    // NBT deserialization - load customer data when world is loaded
    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        // Load spawn position
        if (tag.contains("SpawnX")) {
            this.spawnPosition = new net.minecraft.core.BlockPos(
                tag.getInt("SpawnX"),
                tag.getInt("SpawnY"),
                tag.getInt("SpawnZ")
            );
            LOGGER.debug("Loaded customer spawn position: {}", this.spawnPosition);
        }
        
        // Load customer state
        if (tag.contains("CustomerState")) {
            try {
                this.customerState = CustomerState.valueOf(tag.getString("CustomerState"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid customer state in NBT: {}, defaulting to LEAVING", tag.getString("CustomerState"));
                this.customerState = CustomerState.LEAVING;
            }
        }
        
        // Load and recreate lifecycle from type
        if (tag.contains("LifecycleType")) {
            try {
                LifecycleType type = LifecycleType.valueOf(tag.getString("LifecycleType"));
                this.lifecycle = CustomerLifecycleFactory.fromType(type);
                LOGGER.debug("Recreated lifecycle {} for customer {}", type, this.getId());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid lifecycle type in NBT: {}, defaulting to DINING_ONLY", 
                    tag.getString("LifecycleType"));
                this.lifecycle = CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY);
            }
        } else if (tag.contains("CustomerType")) {
            // Backward compatibility: old saves have CustomerType instead of LifecycleType
            // TODO: Remove this in the next commit
            try {
                LifecycleType type = LifecycleType.valueOf(tag.getString("CustomerType"));
                this.lifecycle = CustomerLifecycleFactory.fromType(type);
                LOGGER.debug("Migrated old CustomerType {} to lifecycle for customer {}", type, this.getId());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid customer type in NBT: {}, defaulting to DINING_ONLY", 
                    tag.getString("CustomerType"));
                this.lifecycle = CustomerLifecycleFactory.fromType(LifecycleType.DINING_ONLY);
            }
        }
        
        // Load target position if exists
        if (tag.contains("TargetX")) {
            this.targetPosition = new net.minecraft.core.BlockPos(
                tag.getInt("TargetX"),
                tag.getInt("TargetY"),
                tag.getInt("TargetZ")
            );
        }
        
        // Load panic tracking (cumulative total)
        if (tag.contains("TotalPanicTicks")) {
            this.totalPanicTicks = tag.getLong("TotalPanicTicks");
        }
    }
    
    /**
     * Called when entity is added to the world
     * This is where we set spawn position for NEW entities (not loaded from NBT)
     */
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        
        // Set spawn position only on server side and only if not already loaded from NBT
        if (!this.level().isClientSide && getSpawnPosition() == null) {
            setSpawnPosition(this.blockPosition());
            LOGGER.info("Customer {} set new spawn position: {}", this.getId(), getSpawnPosition());
        }
    }
    
    // ========================================
    // SITTING HELPERS
    // ========================================
    
    /**
     * Make this customer sit at the specified position
     * 
     * @param pos The block position to sit at
     * @param duration How long to sit (in ticks)
     * @param facing Direction to face while sitting (or null)
     * @return true if successfully sat down, false if position was occupied
     */
    public boolean sitDown(BlockPos pos, int duration, Direction facing) {
        return SittingEntity.sitDown(pos, this, duration, TavernKeeperMod.SITTING.get(), facing);
    }
    
    /**
     * Check if this customer is currently sitting
     * 
     * @return true if riding a SittingEntity
     */
    public boolean isSitting() {
        return this.getVehicle() instanceof SittingEntity;
    }
    
    /**
     * Get the sitting entity if currently sitting
     * 
     * @return The SittingEntity, or null if not sitting
     */
    public SittingEntity getSittingEntity() {
        if (this.getVehicle() instanceof SittingEntity sitting) {
            return sitting;
        }
        return null;
    }
    
    /**
     * Force the customer to stand up early
     * Does nothing if not currently sitting
     */
    public void standUp() {
        SittingEntity sitting = getSittingEntity();
        if (sitting != null) {
            sitting.setMaxLifeTime(0); // Trigger immediate dismount
            LOGGER.debug("Customer {} standing up", this.getId());
        }
    }
    
    // Similar to Villager.startSleeping() (line 939-944)
    @Override
    public void startSleeping(net.minecraft.core.BlockPos pos) {
        super.startSleeping(pos);
        // Note: We don't track LAST_SLEPT memory like Villagers do
        // Clear walk target to prevent movement while sleeping
        this.brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        this.brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }
    
    // Similar to Villager.stopSleeping() (line 946-949)
    @Override
    public void stopSleeping() {
        super.stopSleeping();
        // Note: We don't track LAST_WOKEN memory like Villagers do
        
        // If bed was broken (not a normal wake-up), transition customer to appropriate state
        if (this.getCustomerState() == CustomerState.SLEEPING) {
            // Bed was broken while sleeping - need to find a new bed or leave
            if (this.getLifecycle() != null) {
                // Go back to finding bed state
                this.setCustomerState(CustomerState.FINDING_BED);
                LOGGER.info("Customer {} woken up by bed breaking, looking for new bed", this.getId());
            } else {
                this.setCustomerState(CustomerState.LEAVING);
            }
        }
    }
    
    /**
     * Determine what state to transition to after panic, based on saved state
     * This centralizes the state transition logic for easier maintenance
     */
    public CustomerState getStateAfterPanic() {
        CustomerState savedState = getStateBeforePanic();
        if (savedState == null) {
            // No saved state - return to lifecycle's initial state
            LOGGER.warn("Customer {} has no saved state, defaulting to {}", this.getId(), CustomerState.LEAVING);
            return lifecycle != null ? lifecycle.getInitialState() : CustomerState.LEAVING;
        }
        
        return switch (savedState) {
            // Customer was finding or waiting at lectern → go back to finding lectern
            case FINDING_LECTERN, WAITING_SERVICE -> CustomerState.FINDING_LECTERN;
            
            // Customer was finding or waiting at reception → go back to finding reception
            case FINDING_RECEPTION, WAITING_RECEPTION -> CustomerState.FINDING_RECEPTION;
            
            // Customer was finding or eating at chair → go back to finding chair
            case FINDING_SEAT, EATING -> CustomerState.FINDING_SEAT;
            
            // Customer was finding bed or sleeping → go back to finding bed
            case FINDING_BED, SLEEPING -> CustomerState.FINDING_BED;
            
            // Customer was leaving → continue leaving
            case LEAVING -> CustomerState.LEAVING;
        };
    }
    
    // Similar to Villager.sendDebugPackets() (line 934-937)
    @Override
    protected void sendDebugPackets() {
        super.sendDebugPackets();
        // DebugPackets.sendEntityBrain(this); // Enable for debugging
    }
    
    // Static initialization - similar to Villager static block (line 956-971)
    static {
        MEMORY_TYPES = ImmutableList.of(
            // Navigation
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET,
            MemoryModuleType.PATH,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, // Required by MoveToTargetSink line 34
            
            // Panic-related (required for VillagerCalmDown and SetWalkTargetAwayFrom)
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY,
            MemoryModuleType.NEAREST_HOSTILE, // Required for CustomerPanicTrigger
            
            // Entity awareness
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_PLAYERS,
            MemoryModuleType.NEAREST_VISIBLE_PLAYER
        );
        
        SENSOR_TYPES = ImmutableList.of(
            SensorType.NEAREST_LIVING_ENTITIES, // Detects all nearby entities
            SensorType.NEAREST_PLAYERS,         // Detects players specifically
            SensorType.HURT_BY,                 // Detects when hurt (populates HURT_BY)
            SensorType.VILLAGER_HOSTILES        // Detects hostile mobs (populates NEAREST_HOSTILE)
        );
    }
}
