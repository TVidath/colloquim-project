package fog_simulation;

/**
 * Normalizer
 *
 * Performs global Min-Max normalization of delay and energy values
 * across all tasks and all fog nodes, mapping each value to the range [0, 1].
 *
 * Formula:
 *   normalizedValue = (value - min) / (max - min)
 *
 * If all values are identical (max == min), normalized value = 0.
 *
 * Also computes the sum of normalized delay and normalized energy per task/fog node,
 * and triggers preference sorting for each task.
 */
public class Normalizer {

    /**
     * Normalizes delay and energy values globally across all tasks and fog nodes.
     * Stores normalized values and sums back into each Task object.
     *
     * @param tasks        Array of tasks with computed delay and energy w.r.t fog nodes
     * @param numFogNodes  The number of available fog nodes
     */
    public static void normalize(Task[] tasks, int numFogNodes) {

        // ---- Find global min and max for Delay & Energy across all tasks and fog nodes ----
        double minDelay = Double.MAX_VALUE, maxDelay = -Double.MAX_VALUE;
        double minEnergy = Double.MAX_VALUE, maxEnergy = -Double.MAX_VALUE;

        for (Task t : tasks) {
            for (int f = 0; f < numFogNodes; f++) {
                double delay = t.getOffloadingDelay(f);
                double energy = t.getEnergy(f);
                if (delay < minDelay)  minDelay  = delay;
                if (delay > maxDelay)  maxDelay  = delay;
                if (energy < minEnergy) minEnergy = energy;
                if (energy > maxEnergy) maxEnergy = energy;
            }
        }

        double delayRange  = (maxDelay  - minDelay)  == 0 ? 1 : (maxDelay  - minDelay);
        double energyRange = (maxEnergy - minEnergy) == 0 ? 1 : (maxEnergy - minEnergy);

        // ---- Normalize and store (apply severity weight: Minor=0.5, Major=1.0) ----
        for (Task t : tasks) {
            String severity = t.getSeverity();
            double weight = 1.0;
            if (severity != null && severity.equalsIgnoreCase("Minor")) {
                weight = 0.5;
            }

            for (int f = 0; f < numFogNodes; f++) {
                double nd = (t.getOffloadingDelay(f) - minDelay)  / delayRange;
                double ne = (t.getEnergy(f)          - minEnergy) / energyRange;

                nd = nd * weight;
                ne = ne * weight;

                t.setNormalizedDelay(f, nd);
                t.setNormalizedEnergy(f, ne);

                // Sum of normalized delay and normalized energy
                double sum = nd + ne;
                t.setNormSum(f, sum);
            }

            // Sort the preferred order of fog nodes for this task (least sum first)
            t.computePreferredFogOrder(numFogNodes);
        }
    }
}
