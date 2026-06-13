/**
 * QuotaDeterminator
 * 
 * Implements Algorithm 1: Minimum Quota Determination (MQD) from the M-DAFTO paper.
 * Computes minimum quotas for Fog Networks proportionately based on their VRU capacity (vruCapacity)
 * relative to the most computationally efficient Fog Network.
 */
public class QuotaDeterminator {

    /**
     * Computes the minimum quotas w.r.t all fog nodes.
     *
     * @param fogNetworks The array of Fog Nodes
     * @param numTasks    The size of the task set (All Tasks or Major Tasks)
     * @param isAllTasks  Whether we are computing for the All Tasks set (true) or Major Tasks set (false)
     */
    public static void computeMinimumQuotas(FogNetwork[] fogNetworks, int numTasks, boolean isAllTasks) {
        if (fogNetworks == null || fogNetworks.length == 0) return;

        // Initialize: l(fj) = 0, for all fj in F
        int[] minQuotas = new int[fogNetworks.length];

        // Find the most computationally efficient FN (highest vruCapacity)
        double lambdaMax = -1.0;
        int maxIndex = -1;
        for (int i = 0; i < fogNetworks.length; i++) {
            double cap = fogNetworks[i].getVruCapacity();
            if (cap > lambdaMax) {
                lambdaMax = cap;
                maxIndex = i;
            }
        }

        if (maxIndex == -1) return;

        // lMax = min(h(fj), floor(|T|/|F|))
        int hMax = fogNetworks[maxIndex].getNumberOfVRUs();
        int lMax = Math.min(hMax, (int) Math.floor((double) numTasks / fogNetworks.length));

        minQuotas[maxIndex] = lMax;

        // For remaining FNs in F \ {fj}
        for (int i = 0; i < fogNetworks.length; i++) {
            if (i == maxIndex) continue;
            double vruCap = fogNetworks[i].getVruCapacity();
            int h = fogNetworks[i].getNumberOfVRUs();
            int lPrime = (int) Math.floor((vruCap / lambdaMax) * lMax);
            minQuotas[i] = Math.min(lPrime, h);
        }

        // Store back
        for (int i = 0; i < fogNetworks.length; i++) {
            if (isAllTasks) {
                fogNetworks[i].setMinQuotaAllTasks(minQuotas[i]);
                fogNetworks[i].setMaxQuotaAllTasks(fogNetworks[i].getNumberOfVRUs());
            } else {
                fogNetworks[i].setMinQuotaMajorTasks(minQuotas[i]);
                fogNetworks[i].setMaxQuotaMajorTasks(null); // Keep empty
            }
        }
    }
}
