/**
 * ============================================================================
 *  SimulationData — Data Transfer Object (DTO)
 * ============================================================================
 *  Central container that holds ALL simulation arrays.
 *  Provides direct access to every computed array.
 *
 *  Contents:
 *    ── Core Data ──
 *    - tasks[]                     : All Task objects
 *    - fogNetworks[]               : All FogNetwork objects
 *    - weights[]                   : Urgency weights [w1, w2, w3, w4]
 *
 *    ── Normalized Metrics (per task × per fog node) ──
 *    - normDelayArray[][]          : Normalized delay       [taskIndex][fogIndex]
 *    - normEnergyArray[][]         : Normalized energy      [taskIndex][fogIndex]
 *    - normSumArray[][]            : Sum of norm delay+energy [taskIndex][fogIndex]
 *
 *    ── Preference Rankings ──
 *    - preferredFogIndices[][]     : Fog preferences per task      [taskIndex][rank]
 *    - preferredTasksPerFog[][]    : Task rankings per fog (all)   [fogIndex][rank]
 *    - preferredMajorTasksPerFog[][]: Task rankings per fog (major) [fogIndex][rank]
 *    - preferredMinorTasksPerFog[][]: Task rankings per fog (minor) [fogIndex][rank]
 *
 *    ── Precedence Lists ──
 *    - precedenceListMajor[]       : Major tasks sorted by deadline
 *    - precedenceListMinor[]       : Minor tasks sorted by deadline
 * ============================================================================
 */
public class SimulationData {

    // ── Core Data ──
    private Task[]       tasks;
    private FogNetwork[] fogNetworks;
    private double[]     weights;

    // ── Normalized Metrics ──
    private double[][]   normDelayArray;
    private double[][]   normEnergyArray;
    private double[][]   normSumArray;

    // ── Preference Rankings ──
    private int[][]      preferredFogIndices;
    private int[][]      preferredTasksPerFog;
    private int[][]      preferredMajorTasksPerFog;
    private int[][]      preferredMinorTasksPerFog;

    // ── Precedence Lists ──
    private Task[]       precedenceListMajor;
    private Task[]       precedenceListMinor;

    // ================================================================
    //  CONSTRUCTOR
    // ================================================================

    public SimulationData(Task[] tasks, FogNetwork[] fogNetworks,
                          double[][] normDelayArray, double[][] normEnergyArray,
                          double[][] normSumArray, int[][] preferredFogIndices,
                          int[][] preferredTasksPerFog,
                          int[][] preferredMajorTasksPerFog,
                          int[][] preferredMinorTasksPerFog,
                          double[] weights,
                          Task[] precedenceListMajor,
                          Task[] precedenceListMinor) {
        this.tasks                     = tasks;
        this.fogNetworks               = fogNetworks;
        this.normDelayArray            = normDelayArray;
        this.normEnergyArray           = normEnergyArray;
        this.normSumArray              = normSumArray;
        this.preferredFogIndices       = preferredFogIndices;
        this.preferredTasksPerFog      = preferredTasksPerFog;
        this.preferredMajorTasksPerFog = preferredMajorTasksPerFog;
        this.preferredMinorTasksPerFog = preferredMinorTasksPerFog;
        this.weights                   = weights;
        this.precedenceListMajor       = precedenceListMajor;
        this.precedenceListMinor       = precedenceListMinor;
    }

    // ================================================================
    //  GETTERS — Core Data
    // ================================================================

    public Task[]       getTasks()       { return tasks; }
    public FogNetwork[] getFogNetworks() { return fogNetworks; }
    public double[]     getWeights()     { return weights; }

    // ================================================================
    //  GETTERS — Normalized Metrics (full arrays)
    // ================================================================

    public double[][] getNormDelayArray()  { return normDelayArray; }
    public double[][] getNormEnergyArray() { return normEnergyArray; }
    public double[][] getNormSumArray()    { return normSumArray; }

    // ================================================================
    //  GETTERS — Preference Rankings (full arrays)
    // ================================================================

    public int[][] getPreferredFogIndices()       { return preferredFogIndices; }
    public int[][] getPreferredTasksPerFog()      { return preferredTasksPerFog; }
    public int[][] getPreferredMajorTasksPerFog() { return preferredMajorTasksPerFog; }
    public int[][] getPreferredMinorTasksPerFog() { return preferredMinorTasksPerFog; }

    // ================================================================
    //  GETTERS — Precedence Lists
    // ================================================================

    public Task[] getPrecedenceListMajor() { return precedenceListMajor; }
    public Task[] getPrecedenceListMinor() { return precedenceListMinor; }

    // ================================================================
    //  CONVENIENCE GETTERS — Single Element Access
    // ================================================================

    /** Normalized delay for a specific task and fog node */
    public double getNormDelay(int taskIndex, int fogIndex) {
        return normDelayArray[taskIndex][fogIndex];
    }

    /** Normalized energy for a specific task and fog node */
    public double getNormEnergy(int taskIndex, int fogIndex) {
        return normEnergyArray[taskIndex][fogIndex];
    }

    /** Sum of normalized delay + energy for a specific task and fog node */
    public double getNormSum(int taskIndex, int fogIndex) {
        return normSumArray[taskIndex][fogIndex];
    }

    /** Preferred fog node indices for a specific task */
    public int[] getPreferredFogIndices(int taskIndex) {
        return preferredFogIndices[taskIndex];
    }

    /** Preferred task order for a specific fog node (all tasks) */
    public int[] getPreferredTasks(int fogIndex) {
        return preferredTasksPerFog[fogIndex];
    }

    /** Preferred task order for a specific fog node (major tasks only) */
    public int[] getPreferredMajorTasks(int fogIndex) {
        return preferredMajorTasksPerFog[fogIndex];
    }

    /** Preferred task order for a specific fog node (minor tasks only) */
    public int[] getPreferredMinorTasks(int fogIndex) {
        return preferredMinorTasksPerFog[fogIndex];
    }

    // ================================================================
    //  CONVENIENCE GETTERS — Quota Access
    // ================================================================

    public int getMinQuotaAllTasks(int fogIndex) {
        return fogNetworks[fogIndex].getMinQuotaAllTasks();
    }

    public int getMaxQuotaAllTasks(int fogIndex) {
        return fogNetworks[fogIndex].getMaxQuotaAllTasks();
    }

    public int getMinQuotaMajorTasks(int fogIndex) {
        return fogNetworks[fogIndex].getMinQuotaMajorTasks();
    }

    public Integer getMaxQuotaMajorTasks(int fogIndex) {
        return fogNetworks[fogIndex].getMaxQuotaMajorTasks();
    }
}
