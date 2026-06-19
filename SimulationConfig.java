/**
 * ============================================================================
 *  SimulationConfig
 * ============================================================================
 *
 *  Central configuration class that holds ALL simulation constants
 *  and parameter ranges in one place.
 *
 *  Modify values here to change the simulation setup — no need to
 *  touch any other file.
 *
 *  Sections:
 *    1. Task Generation Parameters
 *    2. Fog Network Parameters
 *    3. Network Constants (Bandwidth)
 *    4. Power Constants (Energy)
 * ============================================================================
 */
public class SimulationConfig {

    // ================================================================
    //  1. TASK GENERATION PARAMETERS
    // ================================================================

    /** Total number of tasks to generate */
    static final int NUM_TASKS = 250;

    /** CPU demand range in million cycles */
    static final double CPU_DEMAND_MIN = 210.0;
    static final double CPU_DEMAND_MAX = 480.0;

    /** Input data size range in KB */
    static final double INPUT_SIZE_MIN = 300.0;
    static final double INPUT_SIZE_MAX = 600.0;

    /** Output data size range in KB */
    static final double OUTPUT_SIZE_MIN = 10.0;
    static final double OUTPUT_SIZE_MAX = 20.0;

    /** Task deadline range in seconds */
    static final double DEADLINE_MIN = 15.0;
    static final double DEADLINE_MAX = 25.0;

    // ================================================================
    //  2. FOG NETWORK PARAMETERS
    // ================================================================

    /** Number of fog nodes in the simulation */
    static final int NUM_FOG_NODES = 5;

    /** Fog node CPU capacity range in GHz */
    static final double CPU_CAP_MIN_GHZ = 6.0;
    static final double CPU_CAP_MAX_GHZ = 10.0;

    /** Fog node Virtual Resource Units (VRUs) range */
    static final int VRU_MIN = 200;
    static final int VRU_MAX = 500;

    // ================================================================
    //  3. DISPLAY PARAMETERS
    // ================================================================

    /** Number of sample tasks to print in detailed view */
    static final int SAMPLE_TASK_COUNT = 3;

    /** Number of top-ranked tasks to print per fog node */
    static final int TOP_RANKED_COUNT = 15;
}
