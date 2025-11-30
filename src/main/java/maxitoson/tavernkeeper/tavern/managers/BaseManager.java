package maxitoson.tavernkeeper.tavern.managers;

import maxitoson.tavernkeeper.areas.AreaType;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.spaces.BaseSpace;
// import maxitoson.tavernkeeper.tavern.spaces.ManagerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * Base manager class that handles space collection and area management
 * Provides common functionality for all manager types (Dining, Sleeping, etc.)
 * 
 * Pattern: Manager owns Spaces, Space owns Area (1:1)
 * @param <T> The type of space this manager handles
 */
public abstract class BaseManager<T extends BaseSpace> {
    
    /**
     * Result of adding a space (includes area and scan results)
     */
    public static class AddSpaceResult {
        private final TavernArea area;
        private final Object scanResult;
        
        public AddSpaceResult(TavernArea area, Object scanResult) {
            this.area = area;
            this.scanResult = scanResult;
        }
        
        public TavernArea getArea() {
            return area;
        }
        
        public Object getScanResult() {
            return scanResult;
        }
    }
    
    protected final TavernContext tavern;
    protected final Map<UUID, T> spaces;
    
    // Counter for auto-numbering areas of this type
    private int counter = 0;
    
    public BaseManager(TavernContext tavern) {
        this.tavern = tavern;
        this.spaces = new HashMap<>();
    }
    
    /**
     * Get the area type this manager handles
     */
    public abstract AreaType getAreaType();
    
    /**
     * Create a new space instance
     * Subclasses must implement this to create their specific space type
     */
    protected abstract T createSpace(String name, BlockPos minPos, BlockPos maxPos, ServerLevel level);
    
    /**
     * Add a new space (creates area internally, scans for furniture)
     * @return AddSpaceResult containing area and scan results
     */
    public AddSpaceResult addSpace(String name, BlockPos minPos, BlockPos maxPos, ServerLevel level) {
        // Ensure area is at least 3 blocks tall (for furniture detection)
        int height = maxPos.getY() - minPos.getY() + 1;
        if (height < 3) {
            maxPos = new BlockPos(maxPos.getX(), minPos.getY() + 2, maxPos.getZ());
        }
        
        T space = createSpace(name, minPos, maxPos, level);
        spaces.put(space.getArea().getId(), space);
        
        // Scan for furniture and return result
        Object scanResult = space.scanForFurniture();
        return new AddSpaceResult(space.getArea(), scanResult);
    }
    
    /**
     * Remove a space
     */
    public boolean removeSpace(UUID id) {
        return spaces.remove(id) != null;
    }
    
    /**
     * Get space by ID
     */
    public T getSpace(UUID id) {
        return spaces.get(id);
    }
    
    /**
     * Get all spaces managed by this manager (unmodifiable view)
     */
    public Collection<T> getSpaces() {
        return java.util.Collections.unmodifiableCollection(spaces.values());
    }
    
    /**
     * Get the space that contains the given position
     */
    public T getSpaceAt(BlockPos pos) {
        for (T space : spaces.values()) {
            if (space.getArea().contains(pos)) {
                return space;
            }
        }
        return null;
    }
    
    /**
     * Get all spaces that contain the given position
     */
    public List<T> getSpacesAt(BlockPos pos) {
        List<T> result = new ArrayList<>();
        for (T space : spaces.values()) {
            if (space.getArea().contains(pos)) {
                result.add(space);
            }
        }
        return java.util.Collections.unmodifiableList(result);
    }
    
    /**
     * Get spaces that intersect with a bounding box
     */
    public List<T> getIntersectingSpaces(AABB box) {
        List<T> result = new ArrayList<>();
        for (T space : spaces.values()) {
            if (space.getArea().getBoundingBox().intersects(box)) {
                result.add(space);
            }
        }
        return java.util.Collections.unmodifiableList(result);
    }
    
    /**
     * Check if position is in any space managed by this manager
     */
    public boolean isInAnySpace(BlockPos pos) {
        return getSpaceAt(pos) != null;
    }
    
    /**
     * Scan all spaces for furniture
     */
    public void scanAll() {
        for (T space : spaces.values()) {
            space.scanForFurniture();
        }
    }
    
    /**
     * Get the next counter value for auto-naming
     */
    public int getNextCounter() {
        counter++;
        return counter;
    }
    
    /**
     * Set counter value (used during load)
     */
    protected void setCounter(int value) {
        this.counter = value;
    }
    
    /**
     * Get current counter value
     */
    public int getCounter() {
        return counter;
    }
    
    /**
     * Save manager state to NBT
     * Subclasses should override to save additional data
     */
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag spacesList = new ListTag();
        for (T space : spaces.values()) {
            CompoundTag spaceTag = new CompoundTag();
            space.save(spaceTag);
            spacesList.add(spaceTag);
        }
        tag.put(getAreaType().name().toLowerCase() + "_spaces", spacesList);
        tag.putInt(getAreaType().name().toLowerCase() + "_counter", counter);
    }
    
    /**
     * Load manager state from NBT
     * Subclasses should override to load additional data
     */
    public void load(CompoundTag tag, ServerLevel level, HolderLookup.Provider registries) {
        String prefix = getAreaType().name().toLowerCase();
        
        // Load counter
        if (tag.contains(prefix + "_counter")) {
            counter = tag.getInt(prefix + "_counter");
        }
        
        // Load spaces - delegated to subclass via createSpaceFromNBT
        ListTag spacesList = tag.getList(prefix + "_spaces", Tag.TAG_COMPOUND);
        for (int i = 0; i < spacesList.size(); i++) {
            CompoundTag spaceTag = spacesList.getCompound(i);
            T space = createSpaceFromNBT(spaceTag, level);
            if (space != null) {
                spaces.put(space.getArea().getId(), space);
            }
        }
    }
    
    /**
     * Create a space instance from NBT data
     * Subclasses must implement this to deserialize their space type
     */
    protected abstract T createSpaceFromNBT(CompoundTag tag, ServerLevel level);
}

