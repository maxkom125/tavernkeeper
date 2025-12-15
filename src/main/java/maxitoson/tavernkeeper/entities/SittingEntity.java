package maxitoson.tavernkeeper.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Invisible vehicle entity that customers ride to sit on chairs.
 */
public class SittingEntity extends Entity {
    /**
     * The lifetime in ticks of the entity. Entity will dismount at 0
     */
    private int maxLifeTime = 100;
    
    /**
     * The original sitting position (for dismount location)
     */
    private BlockPos sittingPos = BlockPos.ZERO;
    
    /** Tracks occupied positions per dimension */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> existingSittingEntities = new HashMap<>();
    
    public SittingEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.setInvisible(true);
        this.noPhysics = true;
        this.setNoGravity(true);
    }
    
    @Override
    public boolean hurt(DamageSource source, float amount) { return false; }
    
    @Override
    public boolean isPickable() { return false; }
    
    @Override
    public boolean shouldRiderSit() { return true; }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    
    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        existingSittingEntities.getOrDefault(level().dimension(), new HashSet<>()).remove(sittingPos);
    }
    
    @Override
    public void tick() {
        if (this.level().isClientSide) {
            return; // Server-side only
        }
        
        // Check if still has passengers and lifetime not expired
        if (!this.isVehicle() || maxLifeTime-- < 0) {
            // Eject any remaining passengers
            if (getPassengers().size() > 0) {
                this.ejectPassengers();
            }
            this.discard();
        }
    }
    
    /**
     * Position passengers relative to the SittingEntity (which is at seat surface).
     * Offset the rider DOWN so their butt lands on the seat.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float scale) {
        // Offset rider down so butt lands at seat level (SittingEntity position)
        // Negative Y moves rider down;
        return new Vec3(0, -0.55 * scale, 0);
    }
    
    @NotNull
    @Override
    public Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        final BlockPos start = sittingPos.equals(BlockPos.ZERO) ? blockPosition().above() : sittingPos;
        BlockPos dismountPos = start.above();
        return new Vec3(dismountPos.getX() + 0.5, dismountPos.getY(), dismountPos.getZ() + 0.5);
    }
    
    public void setMaxLifeTime(int maxLifeTime) { this.maxLifeTime = maxLifeTime; }
    
    public void setSittingPos(BlockPos pos) { this.sittingPos = pos; }
    
    public BlockPos getSittingPos() { return sittingPos; }
    
    public static boolean isSittingPosOccupied(BlockPos pos, Level level) {
        return existingSittingEntities
            .computeIfAbsent(level.dimension(), k -> new HashSet<>())
            .contains(pos);
    }
    
    /**
     * Make an entity sit down at the specified position
     * Creates an invisible SittingEntity and makes the entity ride it
     * 
     * @param pos The block position to sit at
     * @param entity The entity that should sit
     * @param maxLifeTime How long they can sit (in ticks)
     * @return true if successfully sat down, false if position was already occupied
     */
    public static boolean sitDown(BlockPos pos, Mob entity, int maxLifeTime, 
                                  EntityType<SittingEntity> sittingEntityType, Direction facing) {
        if (entity.getVehicle() != null) return true;
        
        // Check if position is already occupied
        if (existingSittingEntities.getOrDefault(entity.level().dimension(), new HashSet<>()).contains(pos)) {
            return false; // Position taken
        }
        
        // Mark position as occupied
        existingSittingEntities
            .computeIfAbsent(entity.level().dimension(), k -> new HashSet<>())
            .add(pos);
        
        SittingEntity sittingEntity = sittingEntityType.create(entity.level());
        if (sittingEntity == null) return false;
        
        BlockState state = entity.level().getBlockState(pos);
        double seatHeight = findSeatHeight(state, entity.level(), pos);
        
        entity.getNavigation().stop();
        
        sittingEntity.setPos(pos.getX() + 0.5, pos.getY() + seatHeight, pos.getZ() + 0.5);
        sittingEntity.setMaxLifeTime(maxLifeTime);
        sittingEntity.setSittingPos(pos);
        
        entity.level().addFreshEntity(sittingEntity);
        entity.startRiding(sittingEntity);
        
        if (facing != null && facing != Direction.DOWN && facing != Direction.UP) {
            float yaw = facing.toYRot();
            entity.setYRot(yaw);
            entity.setYHeadRot(yaw);
            entity.setYBodyRot(yaw);
        }
        
        return true;
    }
    
    /** Find the seat surface height from block collision shapes */
    private static double findSeatHeight(BlockState state, Level level, BlockPos pos) {
        double seatHeight = 0.5;
        List<AABB> shapes = state.getCollisionShape(level, pos).toAabbs();
        
        if (shapes.isEmpty()) return seatHeight;
        
        // Find lowest horizontal surface (seat, not backrest)
        double lowestSurface = 1.0;
        for (AABB box : shapes) {
            double surfaceArea = (box.maxX - box.minX) * (box.maxZ - box.minZ);
            if (surfaceArea > 0.2 && box.maxY < lowestSurface && box.maxY > 0.1) {
                lowestSurface = box.maxY;
            }
        }
        
        if (lowestSurface < 1.0) {
            return lowestSurface;
        }
        
        // Fallback: highest point
        for (AABB box : shapes) {
            if (box.maxY > seatHeight) seatHeight = box.maxY;
        }
        return seatHeight;
    }
    
}

