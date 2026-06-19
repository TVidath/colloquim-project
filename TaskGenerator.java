import java.util.Random;

/**
 * ============================================================================
 *  TaskGenerator
 * ============================================================================
 *
 *  Generates an array of Task objects with randomly assigned parameters.
 *
 *  For each task, the following are randomly generated within configured ranges:
 *    - CPU demand      : [CPU_DEMAND_MIN, CPU_DEMAND_MAX] million cycles
 *    - Input size      : [INPUT_SIZE_MIN, INPUT_SIZE_MAX] KB
 *    - Output size     : [OUTPUT_SIZE_MIN, OUTPUT_SIZE_MAX] KB
 *    - Deadline        : [DEADLINE_MIN, DEADLINE_MAX] seconds
 *    - Phase           : "Pre-Surgery" or "Post-Surgery" (50/50 random)
 *    - Severity        : "Minor" or "Major" (50/50 random)
 *
 *  All range values come from SimulationConfig.
 * ============================================================================
 */
public class TaskGenerator {

    /**
     * Generates an array of tasks with random parameters.
     *
     * @param rand  Random number generator instance
     * @return      Array of NUM_TASKS tasks with random parameters assigned
     */
    public static Task[] generate(Random rand) {
        int numTasks = SimulationConfig.NUM_TASKS;
        Task[] tasks = new Task[numTasks];

        for (int i = 0; i < numTasks; i++) {
            // Create task with random parameters within configured ranges
            tasks[i] = new Task(
                i + 1,                                                           // taskId (1-based)
                randomInRange(rand, SimulationConfig.CPU_DEMAND_MIN,  SimulationConfig.CPU_DEMAND_MAX),
                randomInRange(rand, SimulationConfig.INPUT_SIZE_MIN,  SimulationConfig.INPUT_SIZE_MAX),
                randomInRange(rand, SimulationConfig.OUTPUT_SIZE_MIN, SimulationConfig.OUTPUT_SIZE_MAX),
                randomInRange(rand, SimulationConfig.DEADLINE_MIN,    SimulationConfig.DEADLINE_MAX)
            );

            // Randomly assign phase and severity
            tasks[i].setPhase(rand.nextBoolean() ? "Pre-Surgery" : "Post-Surgery");
            tasks[i].setSeverity(rand.nextBoolean() ? "Minor" : "Major");
        }

        return tasks;
    }

    // ================================================================
    //  HELPER
    // ================================================================

    /**
     * Returns a random double uniformly distributed in [min, max].
     */
    private static double randomInRange(Random rand, double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }
}
