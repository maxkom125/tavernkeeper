package maxitoson.tavernkeeper.entities.ai;

/**
 * Customer lifecycle states
 */
public enum CustomerState {
    FINDING_LECTERN,    // Looking for a lectern to order food
    WAITING_SERVICE,    // Standing at lectern waiting for food
    FINDING_SEAT,       // Looking for a valid chair to sit
    EATING,             // Sitting and eating
    FINDING_RECEPTION,  // Looking for reception desk to pay for sleep
    WAITING_RECEPTION,  // Standing at reception desk waiting to pay for sleep
    FINDING_BED,        // Looking for a bed to sleep
    SLEEPING,           // Lying in bed sleeping
    LEAVING             // Walking away to despawn
}

