/**
 * ============================================================================
 *  QuotaDeterminator
 * ============================================================================
 *
 *  Implements Algorithm 1: Minimum Quota Determination (MQD) from M-DAFTO.
 *
 *  Computes minimum quotas for each Fog Network proportionately based on
 *  their VRU capacity relative to the most computationally efficient node.
 *
 *  Algorithm Steps:
 *    1. Initialize: l(fj) = 0 for all fog nodes
 *    2. Find the most efficient FN (highest VRU capacity = λ_max)
 *    3. lMax = min(h(fj), floor(|T| / |F|))
 *       where h(fj) = number of VRUs, |T| = task count, |F| = fog count
 *    4. For other nodes: l' = floor((vruCap / λ_max) × lMax), capped at VRUs
 *    5. Max quota is set to numberOfVRUs for all nodes
 * ============================================================================
 */
public class QuotaDeterminator {

    /**
     * Computes minimum and maximum quotas for all fog nodes.
     *
     * @param fogNetworks  Array of fog nodes
     * @param numTasks     Size of the task set (all tasks or major tasks)
     * @param isAllTasks   true = computing for All Tasks set,
     *                     false = computing for Major Tasks set
     */
    public static void computeMinimumQuotas(FogNetwork[] fogNetworks,
                                             int numTasks, boolean isAllTasks) {
        if (fogNetworks == null || fogNetworks.length == 0) return;

        int numFogs = fogNetworks.length;
        int[] minQuotas = new int[numFogs];

        // ── Step 1: Find the most efficient FN (highest VRU capacity) ──
        double lambdaMax = -1.0;
        int maxIndex = -1;

        for (int i = 0; i < numFogs; i++) {
            double cap = fogNetworks[i].getVruCapacity();
            if (cap > lambdaMax) {
                lambdaMax = cap;
                maxIndex  = i;
            }
        }

        if (maxIndex == -1) return;

        // ── Step 2: Compute lMax for the most efficient node ──
        // lMax = min(VRUs, floor(|T| / |F|))
        int hMax = fogNetworks[maxIndex].getNumberOfVRUs();
        int lMax = Math.min(hMax, (int) Math.floor((double) numTasks / numFogs));

        minQuotas[maxIndex] = lMax;

        // ── Step 3: Compute proportional quotas for remaining nodes ──
        for (int i = 0; i < numFogs; i++) {
            if (i == maxIndex) continue;

            double vruCap = fogNetworks[i].getVruCapacity();
            int h = fogNetworks[i].getNumberOfVRUs();

            // l' = floor((vruCap / lambdaMax) × lMax), capped at VRUs
            int lPrime = (int) Math.floor((vruCap / lambdaMax) * lMax);
            minQuotas[i] = Math.min(lPrime, h);
        }

        // ── Step 4: Store quotas back into fog nodes ──
        for (int i = 0; i < numFogs; i++) {
            if (isAllTasks) {
                fogNetworks[i].setMinQuotaAllTasks(minQuotas[i]);
                fogNetworks[i].setMaxQuotaAllTasks(fogNetworks[i].getNumberOfVRUs());
            } else {
                fogNetworks[i].setMinQuotaMajorTasks(minQuotas[i]);
                fogNetworks[i].setMaxQuotaMajorTasks(fogNetworks[i].getNumberOfVRUs());
            }
        }
    }
}
