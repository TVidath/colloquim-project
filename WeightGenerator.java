import java.util.Random;

/**
 * ============================================================================
 *  WeightGenerator
 * ============================================================================
 *
 *  Generates urgency weights w1, w2, w3, w4 using the Analytic Hierarchy
 *  Process (AHP) from the M-DAFTO paper.
 *
 *  Implements four algorithms:
 *    - Algorithm 3 (ACW) : AHP-Based Criteria Weight — main loop
 *    - Algorithm 4 (PMC) : Pairwise-Comparison Matrix Computation
 *    - Algorithm 5 (CWD) : Criteria Weight Determination
 *    - Algorithm 6 (CC)  : Consistency Check (CR ≤ 0.1)
 *
 *  The result is encapsulated in a WeightResult object that contains
 *  both the weights and all AHP diagnostic information.
 * ============================================================================
 */
public class WeightGenerator {

    // ================================================================
    //  RESULT CONTAINER
    // ================================================================

    /**
     * Holds the generated weights and all AHP diagnostic information.
     */
    public static class WeightResult {
        public final double[] weights;           // [w1, w2, w3, w4]
        public final double[][] pairwiseMatrix;  // 4×4 pairwise comparison matrix P
        public final double lambdaMax;           // Maximum eigenvalue
        public final double consistencyIndex;    // CI = (lambdaMax - n) / (n - 1)
        public final double consistencyRatio;    // CR = CI / RI
        public final int iterations;             // Number of iterations to converge

        public WeightResult(double[] weights, double[][] pairwiseMatrix,
                            double lambdaMax, double consistencyIndex,
                            double consistencyRatio, int iterations) {
            this.weights          = weights;
            this.pairwiseMatrix   = pairwiseMatrix;
            this.lambdaMax        = lambdaMax;
            this.consistencyIndex = consistencyIndex;
            this.consistencyRatio = consistencyRatio;
            this.iterations       = iterations;
        }
    }

    // ================================================================
    //  ALGORITHM 3: ACW — AHP-Based Criteria Weight (Main Entry Point)
    // ================================================================

    // Diagnostic fields updated during generation (used by CC algorithm)
    private static double lambdaMaxVal;
    private static double ciVal;
    private static double crVal;

    /**
     * Generates AHP-consistent urgency weights.
     * Keeps regenerating the pairwise matrix until the consistency
     * check passes (CR ≤ 0.1), with a safeguard of 100 iterations.
     *
     * @param rand  Random number generator instance
     * @return      WeightResult containing weights + diagnostics
     */
    public static WeightResult generateWeights(Random rand) {
        boolean flag = false;
        double[] W   = null;
        double[][] P = null;
        int iters    = 0;

        // Loop until consistency check passes
        while (!flag && iters < 100) {
            iters++;
            P    = pmc(rand);     // Algorithm 4: Generate pairwise matrix
            W    = cwd(P);        // Algorithm 5: Compute weights
            flag = cc(P, W);      // Algorithm 6: Check consistency
        }

        return new WeightResult(W, P, lambdaMaxVal, ciVal, crVal, iters);
    }

    // ================================================================
    //  ALGORITHM 4: PMC — Pairwise-Comparison Matrix Computation
    // ================================================================

    /**
     * Generates a 4×4 pairwise comparison matrix using Saaty's scale.
     * Random perceived importances in [1, 9] create a near-consistent matrix.
     */
    private static double[][] pmc(Random rand) {
        // Generate random importance values for 4 criteria
        double[] v = new double[4];
        for (int i = 0; i < 4; i++) {
            v[i] = 1.0 + rand.nextDouble() * 8.0;
        }

        // Build pairwise comparison matrix
        double[][] P = new double[4][4];
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if (x == y) {
                    P[x][y] = 1.0;                             // Diagonal = 1
                } else if (x < y) {
                    P[x][y] = getSaatyScale(v[x] / v[y]);      // Upper triangle
                } else {
                    P[x][y] = 1.0 / P[y][x];                   // Lower = 1/Upper
                }
            }
        }
        return P;
    }

    /**
     * Maps a ratio of importances to the nearest Saaty scale value.
     * Scale: {1, 2, 3, 4, 5, 6, 7, 8, 9} and their reciprocals.
     */
    private static double getSaatyScale(double ratio) {
        double[] scale = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0};
        if (ratio >= 1.0) {
            int idx = (int) Math.round(ratio) - 1;
            if (idx > 8) idx = 8;
            return scale[idx];
        } else {
            double invRatio = 1.0 / ratio;
            int idx = (int) Math.round(invRatio) - 1;
            if (idx > 8) idx = 8;
            return 1.0 / scale[idx];
        }
    }

    // ================================================================
    //  ALGORITHM 5: CWD — Criteria Weight Determination
    // ================================================================

    /**
     * Computes criteria weights by:
     *  1. Column-normalizing the pairwise matrix
     *  2. Row-averaging the normalized matrix
     *
     * @param P  4×4 pairwise comparison matrix
     * @return   Normalized weight array [w1, w2, w3, w4]
     */
    private static double[] cwd(double[][] P) {
        int n = P.length;
        double[][] P_bar = new double[n][n];

        // Step 1: Column-wise normalization
        for (int y = 0; y < n; y++) {
            double colSum = 0.0;
            for (int x = 0; x < n; x++) {
                colSum += P[x][y];
            }
            for (int x = 0; x < n; x++) {
                P_bar[x][y] = P[x][y] / (colSum == 0.0 ? 1.0 : colSum);
            }
        }

        // Step 2: Row-wise average → final weights
        double[] W = new double[n];
        for (int x = 0; x < n; x++) {
            double rowSum = 0.0;
            for (int y = 0; y < n; y++) {
                rowSum += P_bar[x][y];
            }
            W[x] = rowSum / n;
        }

        return W;
    }

    // ================================================================
    //  ALGORITHM 6: CC — Consistency Check
    // ================================================================

    /**
     * Verifies consistency of the pairwise comparison matrix.
     * Uses Random Index (RI) = 0.90 for n = 4 criteria.
     *
     * @param P  4×4 pairwise comparison matrix
     * @param W  Weight array from CWD
     * @return   true if CR ≤ 0.1 (consistent), false otherwise
     */
    private static boolean cc(double[][] P, double[] W) {
        int n = P.length;

        // Compute P' = P × W (element-wise: P[x][y] * W[y])
        double[][] P_prime = new double[n][n];
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                P_prime[x][y] = P[x][y] * W[y];
            }
        }

        // Compute w'[x] = rowSum(P'[x]) / W[x]
        double sumWPrime = 0.0;
        for (int x = 0; x < n; x++) {
            double rowSum = 0.0;
            for (int y = 0; y < n; y++) {
                rowSum += P_prime[x][y];
            }
            double wPrime = rowSum / (W[x] == 0.0 ? 0.0001 : W[x]);
            sumWPrime += wPrime;
        }

        // Lambda Max, CI, CR
        lambdaMaxVal = sumWPrime / n;
        ciVal = (lambdaMaxVal - n) / (n - 1);

        double RI = 0.90;    // Random Index for n = 4
        crVal = ciVal / RI;

        return (crVal <= 0.1);
    }
}
