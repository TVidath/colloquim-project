/**
 * ============================================================================
 *  Task — Data Model
 * ============================================================================
 *
 *  Represents a single computational task to be offloaded to a Fog Network.
 *
 *  This is a PURE DATA MODEL — it only holds fields, getters, setters,
 *  and array initialization. All computation logic (urgency, preference
 *  ordering, etc.) lives in separate calculator classes.
 *
 *  Fields:
 *    ── Identity ──
 *    - taskId              : Unique task identifier
 *
 *    ── Task Parameters ──
 *    - cpuDemanded         : CPU demand in million cycles
 *    - inputSize           : Input data size in KB
 *    - outputSize          : Output/result data size in KB
 *    - deadline            : Task deadline in seconds
 *
 *    ── Classification ──
 *    - phase               : "Pre-Surgery" or "Post-Surgery"
 *    - severity            : "Minor" or "Major"
 *
 *    ── Per-Fog-Node Metrics (indexed by fog node index) ──
 *    - transmissionTimes[] : Transmission time to each fog node (seconds)
 *    - executionTimes[]    : Execution time on each fog node (seconds)
 *    - receivingTimes[]    : Receiving time from each fog node (seconds)
 *    - offloadingDelays[]  : Total delay per fog node (seconds)
 *    - energies[]          : Total energy per fog node (Joules)
 *    - normalizedDelays[]  : Min-Max normalized delay per fog node [0, 1]
 *    - normalizedEnergies[]: Min-Max normalized energy per fog node [0, 1]
 *    - normSums[]          : Sum of normalized delay + energy per fog node
 *    - urgencies[]         : Urgency value per fog node
 *
 *    ── Derived ──
 *    - preferredFogIndices[] : Fog node indices sorted best-to-worst
 *    - prefCount             : Number of fog nodes that meet the deadline
 * ============================================================================
 */
public class Task {

    // ── Identity ──
    private int taskId;

    // ── Task Parameters ──
    private double cpuDemanded;     // million cycles
    private double inputSize;       // KB
    private double outputSize;      // KB
    private double deadline;        // seconds

    // ── Classification ──
    private String phase;           // "Pre-Surgery" or "Post-Surgery"
    private String severity;        // "Minor" or "Major"

    // ── Per-Fog-Node Metrics ──
    private double[] transmissionTimes;
    private double[] executionTimes;
    private double[] receivingTimes;
    private double[] offloadingDelays;
    private double[] energies;
    private double[] normalizedDelays;
    private double[] normalizedEnergies;
    private double[] normSums;
    private double[] urgencies;

    // ── Derived ──
    private int[]  preferredFogIndices;
    private int    prefCount;

    // ================================================================
    //  CONSTRUCTOR
    // ================================================================

    /**
     * Creates a new Task with the given parameters.
     *
     * @param taskId       Unique task identifier
     * @param cpuDemanded  CPU demand in million cycles
     * @param inputSize    Input data size in KB
     * @param outputSize   Output/result data size in KB
     * @param deadline     Task deadline in seconds
     */
    public Task(int taskId, double cpuDemanded, double inputSize,
                double outputSize, double deadline) {
        this.taskId      = taskId;
        this.cpuDemanded = cpuDemanded;
        this.inputSize   = inputSize;
        this.outputSize  = outputSize;
        this.deadline    = deadline;
    }

    // ================================================================
    //  ARRAY INITIALIZATION
    // ================================================================

    /**
     * Allocates all per-fog-node metric arrays.
     * Must be called before setting any per-fog-node values.
     *
     * @param numFogNodes Number of fog nodes in the simulation
     */
    public void initFogMetrics(int numFogNodes) {
        this.transmissionTimes  = new double[numFogNodes];
        this.executionTimes     = new double[numFogNodes];
        this.receivingTimes     = new double[numFogNodes];
        this.offloadingDelays   = new double[numFogNodes];
        this.energies           = new double[numFogNodes];
        this.normalizedDelays   = new double[numFogNodes];
        this.normalizedEnergies = new double[numFogNodes];
        this.normSums           = new double[numFogNodes];
        this.urgencies          = new double[numFogNodes];
    }

    // ================================================================
    //  GETTERS — Task Parameters
    // ================================================================

    public int    getTaskId()      { return taskId; }
    public double getCpuDemanded() { return cpuDemanded; }
    public double getInputSize()   { return inputSize; }
    public double getOutputSize()  { return outputSize; }
    public double getDeadline()    { return deadline; }

    // ================================================================
    //  GETTERS / SETTERS — Classification
    // ================================================================

    public String getPhase()                { return phase; }
    public void   setPhase(String phase)    { this.phase = phase; }

    public String getSeverity()             { return severity; }
    public void   setSeverity(String sev)   { this.severity = sev; }

    // ================================================================
    //  GETTERS / SETTERS — Per-Fog-Node Arrays (full array access)
    // ================================================================

    public double[] getTransmissionTimes()  { return transmissionTimes; }
    public double[] getExecutionTimes()     { return executionTimes; }
    public double[] getReceivingTimes()     { return receivingTimes; }
    public double[] getOffloadingDelays()   { return offloadingDelays; }
    public double[] getEnergies()           { return energies; }
    public double[] getNormalizedDelays()   { return normalizedDelays; }
    public double[] getNormalizedEnergies() { return normalizedEnergies; }
    public double[] getNormSums()           { return normSums; }
    public double[] getUrgencies()          { return urgencies; }

    // ================================================================
    //  GETTERS / SETTERS — Per-Fog-Node Arrays (single index access)
    // ================================================================

    public double getTransmissionTime(int i)  { return transmissionTimes[i]; }
    public double getExecutionTime(int i)     { return executionTimes[i]; }
    public double getReceivingTime(int i)     { return receivingTimes[i]; }
    public double getOffloadingDelay(int i)   { return offloadingDelays[i]; }
    public double getEnergy(int i)            { return energies[i]; }
    public double getNormalizedDelay(int i)   { return normalizedDelays[i]; }
    public double getNormalizedEnergy(int i)  { return normalizedEnergies[i]; }
    public double getNormSum(int i)           { return normSums[i]; }
    public double getUrgency(int i)           { return urgencies[i]; }

    public void setTransmissionTime(int i, double v)  { transmissionTimes[i] = v; }
    public void setExecutionTime(int i, double v)     { executionTimes[i] = v; }
    public void setReceivingTime(int i, double v)     { receivingTimes[i] = v; }
    public void setOffloadingDelay(int i, double v)   { offloadingDelays[i] = v; }
    public void setEnergy(int i, double v)            { energies[i] = v; }
    public void setNormalizedDelay(int i, double v)   { normalizedDelays[i] = v; }
    public void setNormalizedEnergy(int i, double v)  { normalizedEnergies[i] = v; }
    public void setNormSum(int i, double v)           { normSums[i] = v; }
    public void setUrgency(int i, double v)           { urgencies[i] = v; }

    // ================================================================
    //  GETTERS / SETTERS — Derived Fields
    // ================================================================

    public int[]  getPreferredFogIndices()              { return preferredFogIndices; }
    public void   setPreferredFogIndices(int[] indices) { this.preferredFogIndices = indices; }

    public int    getPrefCount()            { return prefCount; }
    public void   setPrefCount(int val)     { this.prefCount = val; }

    // ================================================================
    //  DISPLAY
    // ================================================================

    @Override
    public String toString() {
        return String.format(
            "Task[ID=%2d | CPU=%.1f Mcycles | Input=%.2f KB | Output=%.2f KB "
          + "| Deadline=%.2f s | %s | %s]",
            taskId, cpuDemanded, inputSize, outputSize, deadline,
            (phase    == null ? "-" : phase),
            (severity == null ? "-" : severity)
        );
    }
}
