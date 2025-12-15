package maxitoson.tavernkeeper.client;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/**
 * Custom VillagerModel that properly handles sitting pose.
 * 
 * VillagerModel extends ListModel (not HumanoidModel), so it doesn't have
 * the built-in "riding" field that HumanoidModel has. We need to manually
 * rotate the legs when the entity is riding/sitting.
 */
public class SittingVillagerModel<T extends Entity> extends VillagerModel<T> {
    
    // Sitting pose angle (in radians) - ~90 degrees forward for natural sitting
    private static final float SITTING_LEG_ANGLE = -1.4137167F; // ~-81 degrees
    
    private static final float SITTING_LEG_SPREAD = 0.31415927F; // ~18 degrees (PI/10)
    
    // Store reference to leg parts for manipulation
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    
    // Flag to indicate if entity is sitting (set externally before render)
    private boolean isSitting = false;
    
    public SittingVillagerModel(ModelPart root) {
        super(root);
        // VillagerModel uses "right_leg" and "left_leg" for legs
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }
    
    /**
     * Set whether the entity should be shown in sitting pose.
     * Call this before rendering.
     */
    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
    }
    
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // First, let the parent handle normal animation
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        
        // Then override leg positions if sitting
        if (isSitting) {
            // Rotate both legs forward to create sitting pose
            this.rightLeg.xRot = SITTING_LEG_ANGLE;
            this.leftLeg.xRot = SITTING_LEG_ANGLE;
            
            // Slight outward spread for natural sitting look (matches HumanoidModel behavior)
            this.rightLeg.yRot = SITTING_LEG_SPREAD;
            this.leftLeg.yRot = -SITTING_LEG_SPREAD;
        }
    }
}

