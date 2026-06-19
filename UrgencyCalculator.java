/**
 * ============================================================================
 *  UrgencyCalculator
 * ============================================================================
 *
 *  Computes the urgency value for each task with respect to each fog node.
 *
 *  Urgency Formula:
 *    U(ti, fj) = w1 × 1/(Deadline - Delay)
 *              + w2 × 1/pref(ti)
 *              + w3 × 1/Energy
 *              + w4 × γ(ti)
 *
 *  Where:
 *    - w1, w2, w3, w4  : AHP-generated weights (sum to 1.0)
 *    - Deadline - Delay : Time slack (clamped to 0.001 if ≤ 0)
 *    - pref(ti)         : Number of fog nodes that can meet the task's deadline
 *    - Energy           : Energy consumption (clamped to 0.001 if ≤ 0)
 *    - γ(ti)            : Critical score = 1.5 for Major, 1.0 for Minor
 *                         (Formula: 1 + c × α, where c=1 if Major else 0, α=0.5)
 * ============================================================================
 */
public class UrgencyCalculator {

    // ================================================================
    //  BATCH COMPUTATION
    // ================================================================

    /**
     * Computes urgencies for ALL tasks across all fog nodes.
     *
     * @param tasks  Array of all tasks
     * @param w1     Weight for delay difference (inverse)
     * @param w2     Weight for preference count (inverse)
     * @param w3     Weight for energy (inverse)
     * @param w4     Weight for severity/critical score
     */
    public static void computeAllUrgencies(Task[] tasks,
                                            double w1, double w2,
                                            double w3, double w4) {
        for (Task task : tasks) {
            computeUrgency(task, w1, w2, w3, w4);
        }
    }

    // ================================================================
    //  SINGLE TASK COMPUTATION
    // ================================================================

    /**
     * Computes urgency values for one task across all fog nodes.
     * Also calculates pref count (number of fog nodes meeting deadline).
     *
     * @param task  The task to compute urgency for
     * @param w1    Weight for delay difference
     * @param w2    Weight for preference count
     * @param w3    Weight for energy
     * @param w4    Weight for severity
     */
    public static void computeUrgency(Task task,
                                       double w1, double w2,
                                       double w3, double w4) {
        // Step 1: Calculate pref count (fog nodes that meet deadline)
        calculatePrefCount(task);

        double invPref = (task.getPrefCount() == 0) ? 1.0 : (1.0 / task.getPrefCount());

        // Step 2: Determine critical score based on severity
        //   Major: γ = 1 + 1 × 0.5 = 1.5
        //   Minor: γ = 1 + 0 × 0.5 = 1.0
        double criticalScore = 1.0;
        String severity = task.getSeverity();
        if (severity != null && severity.equalsIgnoreCase("Major")) {
            criticalScore = 1.5;
        }

        // Step 3: Compute urgency for each fog node
        double[] delays   = task.getOffloadingDelays();
        double[] energies = task.getEnergies();
        double   deadline = task.getDeadline();

        for (int f = 0; f < delays.length; f++) {
            // Safe division: clamp to 0.001 if deadline ≤ delay
            double delayDiff    = deadline - delays[f];
            double invDelayDiff = 1.0 / Math.max(delayDiff, 0.001);

            // Safe division: clamp to 0.001 if energy ≤ 0
            double invEnergy = 1.0 / Math.max(energies[f], 0.001);

            double urgency = w1 * invDelayDiff
                           + w2 * invPref
                           + w3 * invEnergy
                           + w4 * criticalScore;

            task.setUrgency(f, urgency);
        }
    }

    // ================================================================
    //  PREF COUNT
    // ================================================================

    /**
     * Counts how many fog nodes can complete the task before its deadline.
     * Stores the result in the task's prefCount field.
     *
     * @param task  The task to evaluate
     * @return      Number of fog nodes meeting the deadline
     */
    public static int calculatePrefCount(Task task) {
        double[] delays  = task.getOffloadingDelays();
        double   deadline = task.getDeadline();

        int count = 0;
        for (int f = 0; f < delays.length; f++) {
            if (delays[f] <= deadline) {
                count++;
            }
        }

        task.setPrefCount(count);
        return count;
    }
}
