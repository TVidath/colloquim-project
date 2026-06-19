import java.util.Random;

/**
 * ============================================================================
 *  FogNetworkGenerator
 * ============================================================================
 *
 *  Generates an array of FogNetwork objects with randomly assigned parameters.
 *
 *  For each fog node, the following are randomly generated within configured ranges:
 *    - CPU capacity : [CPU_CAP_MIN_GHZ, CPU_CAP_MAX_GHZ] GHz
 *    - VRU count    : [VRU_MIN, VRU_MAX] integer
 *
 *  All range values come from SimulationConfig.
 * ============================================================================
 */
public class FogNetworkGenerator {

    /**
     * Generates an array of fog network nodes with random parameters.
     *
     * @param rand  Random number generator instance
     * @return      Array of NUM_FOG_NODES fog nodes with random hardware specs
     */
    public static FogNetwork[] generate(Random rand) {
        int numNodes = SimulationConfig.NUM_FOG_NODES;
        FogNetwork[] fogNetworks = new FogNetwork[numNodes];

        for (int i = 0; i < numNodes; i++) {
            double cpuGHz = randomInRange(rand, SimulationConfig.CPU_CAP_MIN_GHZ,
                                                SimulationConfig.CPU_CAP_MAX_GHZ);
            int vrus = randomIntInRange(rand, SimulationConfig.VRU_MIN,
                                              SimulationConfig.VRU_MAX);

            fogNetworks[i] = new FogNetwork(
                i + 1,                      // fogId (1-based)
                "FogNode_" + (i + 1),       // name
                cpuGHz,                     // CPU capacity in GHz
                vrus                        // number of VRUs
            );
        }

        return fogNetworks;
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    /** Returns a random double uniformly distributed in [min, max]. */
    private static double randomInRange(Random rand, double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    /** Returns a random integer uniformly distributed in [min, max] (inclusive). */
    private static int randomIntInRange(Random rand, int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }
}
