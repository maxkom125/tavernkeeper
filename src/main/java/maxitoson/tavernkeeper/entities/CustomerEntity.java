package maxitoson.tavernkeeper.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import maxitoson.tavernkeeper.entities.ai.behavior.CustomerGoalPackages;
import maxitoson.tavernkeeper.entities.ai.CustomerState;
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
    
    // Customer state tracking
    private CustomerState customerState = CustomerState.FINDING_QUEUE;
    private CustomerState stateBeforePanic = null;
    private net.minecraft.core.BlockPos targetPosition = null; // Target position (lectern or chair)
    private maxitoson.tavernkeeper.entities.FoodRequest foodRequest = null; // What food customer wants
    private net.minecraft.core.BlockPos spawnPosition = null; // Where customer spawned (for returning when leaving)
    
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
        float speedModifier = 0.5F; // Base speed for behaviors (same as Villager line 116)
        
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
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }
    
    // Similar to Villager.customServerAiStep() (line 209-247)
    @Override
    protected void customServerAiStep() {
        this.level().getProfiler().push("customerBrain");
        this.getBrain().tick((ServerLevel) this.level(), this);
        this.level().getProfiler().pop();
        
        // TODO: Add customer-specific logic here (like Villager has trading/restocking)
        
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
    
    // Food request management
    public maxitoson.tavernkeeper.entities.FoodRequest getFoodRequest() {
        return foodRequest;
    }
    
    public void setFoodRequest(maxitoson.tavernkeeper.entities.FoodRequest request) {
        this.foodRequest = request;
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
        
        // Save target position if exists
        if (this.targetPosition != null) {
            tag.putInt("TargetX", this.targetPosition.getX());
            tag.putInt("TargetY", this.targetPosition.getY());
            tag.putInt("TargetZ", this.targetPosition.getZ());
        }
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
                LOGGER.warn("Invalid customer state in NBT: {}", tag.getString("CustomerState"));
                this.customerState = CustomerState.FINDING_QUEUE;
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
    
    /**
     * Determine what state to transition to after panic, based on saved state
     * This centralizes the state transition logic for easier maintenance
     */
    public CustomerState getStateAfterPanic() {
        CustomerState savedState = getStateBeforePanic();
        if (savedState == null) {
            return CustomerState.FINDING_QUEUE;
        }
        
        return switch (savedState) {
            // Customer was finding or waiting at lectern → go back to finding lectern
            case FINDING_QUEUE, WAITING_SERVICE -> CustomerState.FINDING_QUEUE;
            
            // Customer was finding or eating at chair → go back to finding chair
            case FINDING_SEAT, EATING -> CustomerState.FINDING_SEAT;
            
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
