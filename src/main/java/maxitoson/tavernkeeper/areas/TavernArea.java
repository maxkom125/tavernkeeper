package maxitoson.tavernkeeper.areas;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Represents a defined area in a tavern (dining, sleeping, etc.)
 */
public class TavernArea {
    private final UUID id;
    private String name;
    private AreaType type;
    private BlockPos minPos;
    private BlockPos maxPos;
    private AABB boundingBox;
    private ServerLevel level;  // The world this area exists in
    
    // Constructor for new area
    public TavernArea(String name, AreaType type, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.type = type;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.level = level;
        this.boundingBox = createAABB();
    }
    
    // Constructor for loading from NBT
    public TavernArea(UUID id, String name, AreaType type, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.minPos = minPos;
        this.maxPos = maxPos;
        this.level = level;
        this.boundingBox = createAABB();
    }
    
    private AABB createAABB() {
        return new AABB(minPos.getX(), minPos.getY(), minPos.getZ(),
                       maxPos.getX() + 1, maxPos.getY() + 1, maxPos.getZ() + 1);
    }
    
    // Getters
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public AreaType getType() {
        return type;
    }
    
    public BlockPos getMinPos() {
        return minPos;
    }
    
    public BlockPos getMaxPos() {
        return maxPos;
    }
    
    public AABB getBoundingBox() {
        return boundingBox;
    }
    
    public ServerLevel getLevel() {
        return level;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setLevel(ServerLevel level) {
        this.level = level;
    }
    
    public void setType(AreaType type) {
        this.type = type;
    }
    
    // Utility methods
    public boolean contains(BlockPos pos) {
        return boundingBox.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
    
    public int[] getSize() {
        int sizeX = maxPos.getX() - minPos.getX() + 1;
        int sizeY = maxPos.getY() - minPos.getY() + 1;
        int sizeZ = maxPos.getZ() - minPos.getZ() + 1;
        return new int[]{sizeX, sizeY, sizeZ};
    }
    
    public int getVolume() {
        int[] size = getSize();
        return size[0] * size[1] * size[2];
    }
    
    // NBT serialization
    public CompoundTag save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putString("type", type.name());
        tag.putInt("minX", minPos.getX());
        tag.putInt("minY", minPos.getY());
        tag.putInt("minZ", minPos.getZ());
        tag.putInt("maxX", maxPos.getX());
        tag.putInt("maxY", maxPos.getY());
        tag.putInt("maxZ", maxPos.getZ());
        return tag;
    }
    
    public static TavernArea load(CompoundTag tag, ServerLevel level) {
        UUID id = tag.getUUID("id");
        String name = tag.getString("name");
        AreaType type = AreaType.valueOf(tag.getString("type"));
        BlockPos minPos = new BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"));
        BlockPos maxPos = new BlockPos(tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"));
        return new TavernArea(id, name, type, minPos, maxPos, level);
    }
    
    @Override
    public String toString() {
        return String.format("%s '%s' at [%s to %s]", 
            type.getDisplayName(), name, minPos.toShortString(), maxPos.toShortString());
    }
}

