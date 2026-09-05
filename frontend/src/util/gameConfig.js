/**
 * Game balance constants shared with the backend.
 *
 * These mirror `src/main/java/com/villagevandals/vandals/gameconfig/GameDefaults.java` and must be
 * kept in step with it by hand — nothing enforces that at build time. Client-side bounds that
 * disagree with the server's produce requests the server rejects, which is exactly what happened
 * when the training quantity control allowed up to 999 while the server accepted at most 50.
 */

/** GameDefaults.VANDAL_FOOD_COST */
export const VANDAL_FOOD_COST = 50

/** GameDefaults.VANDAL_IRON_COST */
export const VANDAL_IRON_COST = 30

/** GameDefaults.VANDAL_HP */
export const VANDAL_HP = 4

/** GameDefaults.VANDAL_DAMAGE */
export const VANDAL_DAMAGE = 1

/** GameDefaults.TRAINING_DURATION_SECONDS, in milliseconds */
export const TRAINING_DURATION_MS = 5000

/** GameDefaults.MAX_TRAINING_BATCH_SIZE — the server rejects anything above this */
export const MAX_TRAINING_BATCH_SIZE = 50

/** The server rejects a quantity below this */
export const MIN_TRAINING_BATCH_SIZE = 1
