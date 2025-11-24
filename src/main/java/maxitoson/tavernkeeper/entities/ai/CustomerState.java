package maxitoson.tavernkeeper.entities.ai;

/**
 * Customer lifecycle states
 */
public enum CustomerState {
    FINDING_QUEUE,    // Looking for a service area lectern
    WAITING_SERVICE,  // Standing at lectern waiting for food
    FINDING_SEAT,     // Looking for a valid chair to sit
    EATING,           // Sitting and eating
    LEAVING           // Walking away to despawn
}

