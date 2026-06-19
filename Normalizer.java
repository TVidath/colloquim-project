/**
 * ============================================================================
 *  Normalizer
 * ============================================================================
 *
 *  Performs global Min-Max normalization of delay and energy values
 *  across ALL tasks and ALL fog nodes, mapping each value to [0, 1].
 *
 *  Formula:
 *    normalizedValue = (value - globalMin) / (globalMax - globalMin)
 *
 *  Special handling:
 *    - If all values are identical (max == min), range defaults to 1
 *      to avoid division by zero.
 *    - Minor-severity tasks get a 0.5 weight reduction on their
 *      normalized values.
 *
 *  After normalization, this class also:
 *    - Computes the sum of normalized delay + energy per task/fog node
 *    - Triggers preference ordering for each task via PreferenceRanker
 * ============================================================================
 */
public class Normalizer {

    /**
     * Normalizes delay and energy values globally across all tasks and fog nodes.
     * Stores normalized values, sums, and fog preference order back into each Task.
     *
     * @param tasks        Array of tasks with computed delay and energy
     * @param numFogNodes  Number of fog nodes in the simulation
     */
    public static void normalize(Task[] tasks, int numFogNodes) {

        // ── Step 1: Find global min/max for delay and energy ──
        double minDelay  = Double.MAX_VALUE, maxDelay  = -Double.MAX_VALUE;
        double minEnergy = Double.MAX_VALUE, maxEnergy = -Double.MAX_VALUE;

        for (Task t : tasks) {
            for (int f = 0; f < numFogNodes; f++) {
                double delay  = t.getOffloadingDelay(f);
                double energy = t.getEnergy(f);

                if (delay  < minDelay)  minDelay  = delay;
                if (delay  > maxDelay)  maxDelay  = delay;
                if (energy < minEnergy) minEnergy = energy;
                if (energy > maxEnergy) maxEnergy = energy;
            }
        }

        double delayRange  = (maxDelay  - minDelay)  == 0 ? 1 : (maxDelay  - minDelay);
        double energyRange = (maxEnergy - minEnergy) == 0 ? 1 : (maxEnergy - minEnergy);

        // ── Step 2: Normalize each value and compute sums ──
        for (Task t : tasks) {
            // Severity weight: Minor tasks get 0.5 reduction, Major tasks get 1.0
            String severity = t.getSeverity();
            double weight = 1.0;
            if (severity != null && severity.equalsIgnoreCase("Minor")) {
                weight = 0.5;
            }

            for (int f = 0; f < numFogNodes; f++) {
                double nd = (t.getOffloadingDelay(f) - minDelay)  / delayRange;
                double ne = (t.getEnergy(f)          - minEnergy) / energyRange;

                // Apply severity weight
                nd = nd * weight;
                ne = ne * weight;

                t.setNormalizedDelay(f, nd);
                t.setNormalizedEnergy(f, ne);

                // Sum of normalized delay + normalized energy
                t.setNormSum(f, nd + ne);
            }

            // ── Step 3: Compute preferred fog order for this task ──
            PreferenceRanker.computePreferredFogOrder(t, numFogNodes);
        }
    }
}
