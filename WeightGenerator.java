import java.util.Random;

/**
 * WeightGenerator
 *
 * Generates weights w1, w2, w3, w4 for the urgency calculation.
 * Implements Analytic Hierarchy Process (AHP) weight generation from the M-DAFTO paper:
 *  - Algorithm 3 (ACW): AHP-Based Criteria Weight
 *  - Algorithm 4 (PMC): Pairwise-Comparison Matrix Computation
 *  - Algorithm 5 (CWD): Criteria Weight Determination
 *  - Algorithm 6 (CC): Consistency Check
 */
public class WeightGenerator {

    // Diagnostic fields to inspect the generated AHP properties
    public static double[][] pairwiseMatrix;
    public static double lambdaMax;
    public static double consistencyIndex;
    public static double consistencyRatio;
    public static int iterations;

    /**
     * Algorithm 3: AHP-Based Criteria Weight (ACW)
     * Generates and returns criteria weights w1, w2, w3, w4 as a double array.
     * Keeps regenerating the pairwise matrix until it passes the AHP consistency check (CR <= 0.1).
     *
     * @param rand The Random instance to generate random numbers
     * @return double[] containing {w1, w2, w3, w4}
     */
    public static double[] generateWeights(Random rand) {
        boolean flag = false;
        double[] W = null;
        double[][] P = null;
        int iters = 0;

        // Loop until consistency check passes (flag == true), with a safeguard of 100 iterations
        while (!flag && iters < 100) {
            iters++;
            P = pmc(rand);
            W = cwd(P);
            flag = cc(P, W);
        }

        // Save diagnostics
        pairwiseMatrix = P;
        iterations = iters;

        return W;
    }

    /**
     * Algorithm 4: Pairwise-Comparison Matrix Computation (PMC)
     * Generates relative importances randomly on Saaty's judgment scale {1/9, ..., 9}.
     */
    private static double[][] pmc(Random rand) {
        double[] v = new double[4];
        // Generate random perceived importances in range [1, 9] to construct a near-consistent matrix
        for (int i = 0; i < 4; i++) {
            v[i] = 1.0 + rand.nextDouble() * 8.0;
        }

        double[][] P = new double[4][4];
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                if (x == y) {
                    P[x][y] = 1.0;
                } else if (x < y) {
                    P[x][y] = getSaatyScale(v[x] / v[y]);
                } else {
                    P[x][y] = 1.0 / P[y][x];
                }
            }
        }
        return P;
    }

    /**
     * Maps a ratio of importances to Saaty's linear judgment scale value.
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

    /**
     * Algorithm 5: Criteria Weight Determination (CWD)
     * Row-averages the column-normalized pairwise comparison matrix.
     */
    private static double[] cwd(double[][] P) {
        int n = P.length;
        double[][] P_bar = new double[n][n];

        // Column-wise normalization
        for (int y = 0; y < n; y++) {
            double colSum = 0.0;
            for (int x = 0; x < n; x++) {
                colSum += P[x][y];
            }
            for (int x = 0; x < n; x++) {
                P_bar[x][y] = P[x][y] / (colSum == 0.0 ? 1.0 : colSum);
            }
        }

        // Row-wise average
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

    /**
     * Algorithm 6: Consistency Check (CC)
     * Verifies if the consistency ratio (CR) is <= 0.1.
     * Uses Random Index (RI) = 0.90 for n = 4 criteria.
     */
    private static boolean cc(double[][] P, double[] W) {
        int n = P.length;
        double[][] P_prime = new double[n][n];

        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                P_prime[x][y] = P[x][y] * W[y];
            }
        }

        double[] w_prime = new double[n];
        double sumWPrime = 0.0;
        for (int x = 0; x < n; x++) {
            double rowSum = 0.0;
            for (int y = 0; y < n; y++) {
                rowSum += P_prime[x][y];
            }
            w_prime[x] = rowSum / (W[x] == 0.0 ? 0.0001 : W[x]);
            sumWPrime += w_prime[x];
        }

        lambdaMax = sumWPrime / n;
        consistencyIndex = (lambdaMax - n) / (n - 1);

        double RI = 0.90; // Random Index value for n=4
        consistencyRatio = consistencyIndex / RI;

        return (consistencyRatio <= 0.1);
    }
}
