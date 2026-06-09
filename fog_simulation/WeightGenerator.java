package fog_simulation;

import java.util.Random;

/**
 * WeightGenerator
 *
 * Generates weights w1, w2, w3, w4 for the urgency calculation.
 * Currently returns random values normalized to sum to 1.0.
 * Can be updated later with other weight selection algorithms.
 */
public class WeightGenerator {

    /**
     * Generates and returns w1, w2, w3, w4 as a double array of length 4.
     * The weights sum to 1.0.
     *
     * @param rand The Random instance to generate random numbers
     * @return double[] containing {w1, w2, w3, w4}
     */
    public static double[] generateWeights(Random rand) {
        double w1 = rand.nextDouble();
        double w2 = rand.nextDouble();
        double w3 = rand.nextDouble();
        double w4 = rand.nextDouble();
        
        double sum = w1 + w2 + w3 + w4;
        if (sum > 0.0) {
            w1 /= sum;
            w2 /= sum;
            w3 /= sum;
            w4 /= sum;
        } else {
            w1 = 0.25;
            w2 = 0.25;
            w3 = 0.25;
            w4 = 0.25;
        }
        
        return new double[]{ w1, w2, w3, w4 };
    }
}
