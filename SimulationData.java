
/**
 * SimulationData
 *
 * Central container that holds all simulation arrays.
 * By passing this object to the M-MOORA algorithm phase,
 * you get direct access to all arrays via their references.
 *
 * Arrays held:
 *  - tasks[]                 : all Task objects
 *  - fogNetworks[]           : all FogNetwork objects
 *  - normDelayArray[][]      : normalized delay w.r.t each fog node [taskIndex][fogIndex]
 *  - normEnergyArray[][]     : normalized energy w.r.t each fog node [taskIndex][fogIndex]
 *  - normSumArray[][]        : sum of normalized delay and energy [taskIndex][fogIndex]
 *  - preferredFogIndices[][] : preferred order of fog node indices [taskIndex][fogRankIndex]
 *  - preferredTasksPerFog[][] : preferred order of task indices for each fog node [fogIndex][taskRankIndex]
 *  - weights[]               : weights used in urgency calculations [w1, w2, w3, w4]
 */
public class SimulationData {

    private Task[]       tasks;
    private FogNetwork[] fogNetworks;
    private double[][]   normDelayArray;
    private double[][]   normEnergyArray;
    private double[][]   normSumArray;
    private int[][]      preferredFogIndices;
    private int[][]      preferredTasksPerFog;
    private double[]     weights;

    public SimulationData(Task[] tasks, FogNetwork[] fogNetworks,
                          double[][] normDelayArray, double[][] normEnergyArray,
                          double[][] normSumArray, int[][] preferredFogIndices,
                          int[][] preferredTasksPerFog, double[] weights) {
        this.tasks                = tasks;
        this.fogNetworks          = fogNetworks;
        this.normDelayArray       = normDelayArray;
        this.normEnergyArray      = normEnergyArray;
        this.normSumArray         = normSumArray;
        this.preferredFogIndices  = preferredFogIndices;
        this.preferredTasksPerFog = preferredTasksPerFog;
        this.weights              = weights;
    }

    // ------------------- Getters -------------------

    public Task[]       getTasks()                { return tasks; }
    public FogNetwork[] getFogNetworks()          { return fogNetworks; }
    public double[][]   getNormDelayArray()       { return normDelayArray; }
    public double[][]   getNormEnergyArray()      { return normEnergyArray; }
    public double[][]   getNormSumArray()         { return normSumArray; }
    public int[][]      getPreferredFogIndices()  { return preferredFogIndices; }
    public int[][]      getPreferredTasksPerFog() { return preferredTasksPerFog; }
    public double[]     getWeights()              { return weights; }

    /**
     * Convenience: get normalized delay for a specific task and fog node
     */
    public double getNormDelay(int taskIndex, int fogIndex) {
        return normDelayArray[taskIndex][fogIndex];
    }

    /**
     * Convenience: get normalized energy for a specific task and fog node
     */
    public double getNormEnergy(int taskIndex, int fogIndex) {
        return normEnergyArray[taskIndex][fogIndex];
    }

    /**
     * Convenience: get sum of norm delay and energy for a specific task and fog node
     */
    public double getNormSum(int taskIndex, int fogIndex) {
        return normSumArray[taskIndex][fogIndex];
    }

    /**
     * Convenience: get preferred fog node indices for a specific task
     */
    public int[] getPreferredFogIndices(int taskIndex) {
        return preferredFogIndices[taskIndex];
    }

    /**
     * Convenience: get preferred task order indices for a specific fog node
     */
    public int[] getPreferredTasks(int fogIndex) {
        return preferredTasksPerFog[fogIndex];
    }
}
