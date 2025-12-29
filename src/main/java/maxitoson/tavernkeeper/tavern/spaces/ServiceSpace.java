package maxitoson.tavernkeeper.tavern.spaces;

import maxitoson.tavernkeeper.TavernKeeperMod;
import maxitoson.tavernkeeper.areas.TavernArea;
import maxitoson.tavernkeeper.tavern.managers.domain.ServiceManagerContext;
import maxitoson.tavernkeeper.tavern.furniture.ServiceBarrel;
import maxitoson.tavernkeeper.tavern.furniture.types.ServiceFurnitureType;
import maxitoson.tavernkeeper.tavern.furniture.ServiceLectern;
import maxitoson.tavernkeeper.tavern.furniture.ServiceReceptionDesk;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a service area where customers order food or sleeping
 * Recognizes Lecterns (food orders), Reception Desks (sleeping), and Barrels (storage)
 */
public class ServiceSpace extends BaseSpace {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<ServiceLectern> lecterns;
    private final List<ServiceReceptionDesk> receptionDesks;
    private final List<ServiceBarrel> barrels;
    
    public ServiceSpace(ServiceManagerContext manager, TavernArea area) {
        super(manager, area);
        this.lecterns = new ArrayList<>();
        this.receptionDesks = new ArrayList<>();
        this.barrels = new ArrayList<>();
    }
    
    /**
     * Result of scanning for furniture in a service space
     */
    public static class ScanResult {
        private final int lecternsFound;
        private final int lecternsRejected;
        private final int receptionDesksFound;
        private final int receptionDesksRejected;
        private final int barrelsFound;
        
        public ScanResult(int lecternsFound, int lecternsRejected, int receptionDesksFound, int receptionDesksRejected, int barrelsFound) {
            this.lecternsFound = lecternsFound;
            this.lecternsRejected = lecternsRejected;
            this.receptionDesksFound = receptionDesksFound;
            this.receptionDesksRejected = receptionDesksRejected;
            this.barrelsFound = barrelsFound;
        }
        
        public int getLecternsFound() { return lecternsFound; }
        public int getLecternsRejected() { return lecternsRejected; }
        public int getReceptionDesksFound() { return receptionDesksFound; }
        public int getReceptionDesksRejected() { return receptionDesksRejected; }
        public int getBarrelsFound() { return barrelsFound; }
        public boolean hadRejectedLecterns() { return lecternsRejected > 0; }
        public boolean hadRejectedReceptionDesks() { return receptionDesksRejected > 0; }
    }
    
    /**
     * Scan the area and recognize all service furniture
     */
    @Override
    public ScanResult scanForFurniture() {
        lecterns.clear();
        receptionDesks.clear();
        barrels.clear();
        
        Level level = area.getLevel();
        if (level == null) return new ScanResult(0, 0, 0, 0, 0);
        
        BlockPos minPos = area.getMinPos();
        BlockPos maxPos = area.getMaxPos();
        
        int rejectedLecterns = 0;
        int rejectedReceptionDesks = 0;
        
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            BlockState state = level.getBlockState(pos);
            int[] rejections = recognizeBlock(pos, state);
            rejectedLecterns += rejections[0];
            rejectedReceptionDesks += rejections[1];
        }
        
        LOGGER.info("Scanned ServiceSpace: Found {} lecterns ({} rejected), {} reception desks ({} rejected), {} barrels", 
            lecterns.size(), rejectedLecterns, receptionDesks.size(), rejectedReceptionDesks, barrels.size());
            
        return new ScanResult(lecterns.size(), rejectedLecterns, receptionDesks.size(), rejectedReceptionDesks, barrels.size());
    }

    @Override
    public void onBlockUpdated(BlockPos pos, BlockState state) {
        LOGGER.debug("ServiceSpace {} updating block at {}. State: {}", area.getName(), pos, state);
        
        // Remove existing furniture at this position
        boolean removedLectern = lecterns.removeIf(l -> l.getPosition().equals(pos));
        boolean removedReceptionDesk = receptionDesks.removeIf(r -> r.getPosition().equals(pos));
        boolean removedBarrel = barrels.removeIf(b -> b.getPosition().equals(pos));
        
        if (removedLectern) {
            LOGGER.debug("Removed lectern at {}", pos);
        }
        if (removedReceptionDesk) {
            LOGGER.debug("Removed reception desk at {}", pos);
        }
        if (removedBarrel) {
            LOGGER.debug("Removed barrel at {}", pos);
        }
        
        // Recognize new block
        recognizeBlock(pos, state);
    }
    
    /**
     * Recognize and add a block as furniture if applicable
     * @return int array [rejectedLecterns, rejectedReceptionDesks]
     */
    private int[] recognizeBlock(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        ServiceManagerContext serviceManager = (ServiceManagerContext) manager;
        int rejectedLecterns = 0;
        int rejectedReceptionDesks = 0;
        
        if (block instanceof LecternBlock) {
            if (serviceManager.canAddFurniture(ServiceFurnitureType.LECTERN)) {
                lecterns.add(new ServiceLectern(pos, state));
                LOGGER.debug("Added lectern at {}", pos);
            } else {
                rejectedLecterns = 1;
                LOGGER.debug("Rejected lectern at {} - limit reached", pos);
            }
        } else if (block == TavernKeeperMod.RECEPTION_DESK.get()) {
            if (serviceManager.canAddFurniture(ServiceFurnitureType.RECEPTION_DESK)) {
                receptionDesks.add(new ServiceReceptionDesk(pos, state));
                LOGGER.debug("Added reception desk at {}", pos);
            } else {
                rejectedReceptionDesks = 1;
                LOGGER.debug("Rejected reception desk at {} - limit reached", pos);
            }
        } else if (block instanceof BarrelBlock) {
            barrels.add(new ServiceBarrel(pos, state));
            LOGGER.debug("Added barrel at {}", pos);
        }
        
        return new int[]{rejectedLecterns, rejectedReceptionDesks};
    }
    
    public List<ServiceLectern> getLecterns() {
        return java.util.Collections.unmodifiableList(lecterns);
    }
    
    public List<ServiceReceptionDesk> getReceptionDesks() {
        return java.util.Collections.unmodifiableList(receptionDesks);
    }
    
    public List<ServiceBarrel> getBarrels() {
        return java.util.Collections.unmodifiableList(barrels);
    }
    
    public int getLecternCount() {
        return lecterns.size();
    }
    
    public int getReceptionDeskCount() {
        return receptionDesks.size();
    }
    
    public int getBarrelCount() {
        return barrels.size();
    }
    
    public static ServiceSpace load(CompoundTag tag, ServiceManagerContext manager, ServerLevel level) {
        TavernArea area = loadArea(tag, level);
        ServiceSpace space = new ServiceSpace(manager, area);
        space.scanForFurniture();
        return space;
    }
}

