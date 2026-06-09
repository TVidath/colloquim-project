
/**
 * Represents a computational task to be offloaded to a Fog Network.
 * Each task has an ID, CPU demand (in million cycles), input size/output size in KB, and deadline.
 * Handles calculation values w.r.t all available fog nodes.
 */
public class Task {

    private int taskId;
    private double cpuDemanded;     // in million cycles
    private double inputSize;       // in KB (task size, randomly assigned)
    private double outputSize;      // in KB (result size, randomly assigned)
    private double deadline;        // in seconds

    // Computed metrics per fog node (indexed by fog node index)
    private double[] transmissionTimes;    // seconds
    private double[] executionTimes;       // seconds
    private double[] receivingTimes;       // seconds
    private double[] offloadingDelays;     // total delay in seconds
    private double[] energies;              // total energy in Joules

    // Normalized values per fog node
    private double[] normalizedDelays;
    private double[] normalizedEnergies;

    // Sum of normalized delay and normalized energy per fog node
    private double[] normSums;

    // Preferred order of fog node indices (sorted from best to worst preference)
    private int[] preferredFogIndices;

    // Urgency values per fog node
    private double[] urgencies;

    // Number of fog nodes that can finish the task before the deadline
    private int prefCount;

    // Task classification
    private String phase;     // "Pre-Surgery" or "Post-Surgery"
    private String severity;  // "Minor" or "Major"

    /**
     * Constructor for Task
     *
     * @param taskId       Unique task identifier
     * @param cpuDemanded  CPU demand in million cycles
     * @param inputSize    Input data size in KB (randomly assigned)
     * @param outputSize   Output data size in KB (randomly assigned)
     * @param deadline     Task deadline in seconds
     */
    public Task(int taskId, double cpuDemanded, double inputSize, double outputSize, double deadline) {
        this.taskId = taskId;
        this.cpuDemanded = cpuDemanded;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.deadline = deadline;
    }

    /**
     * Initializes array metrics w.r.t number of fog nodes.
     */
    public void initFogMetrics(int numFogNodes) {
        this.transmissionTimes = new double[numFogNodes];
        this.executionTimes    = new double[numFogNodes];
        this.receivingTimes    = new double[numFogNodes];
        this.offloadingDelays  = new double[numFogNodes];
        this.energies          = new double[numFogNodes];
        this.normalizedDelays  = new double[numFogNodes];
        this.normalizedEnergies = new double[numFogNodes];
        this.normSums          = new double[numFogNodes];
        this.urgencies         = new double[numFogNodes];
    }

    // ------------------- Getters -------------------

    public int getTaskId()              { return taskId; }
    public double getCpuDemanded()      { return cpuDemanded; }
    public double getInputSize()        { return inputSize; }
    public double getOutputSize()       { return outputSize; }
    public double getDeadline()         { return deadline; }

    public double[] getTransmissionTimes() { return transmissionTimes; }
    public double[] getExecutionTimes()    { return executionTimes; }
    public double[] getReceivingTimes()    { return receivingTimes; }
    public double[] getOffloadingDelays()  { return offloadingDelays; }
    public double[] getEnergies()          { return energies; }
    public double[] getNormalizedDelays()  { return normalizedDelays; }
    public double[] getNormalizedEnergies() { return normalizedEnergies; }
    public double[] getNormSums()          { return normSums; }
    public int[] getPreferredFogIndices()  { return preferredFogIndices; }
    public double[] getUrgencies()         { return urgencies; }
    public int getPrefCount()              { return prefCount; }

    // Index-specific getters
    public double getTransmissionTime(int idx) { return transmissionTimes[idx]; }
    public double getExecutionTime(int idx)    { return executionTimes[idx]; }
    public double getReceivingTime(int idx)    { return receivingTimes[idx]; }
    public double getOffloadingDelay(int idx)  { return offloadingDelays[idx]; }
    public double getEnergy(int idx)           { return energies[idx]; }
    public double getNormalizedDelay(int idx)  { return normalizedDelays[idx]; }
    public double getNormalizedEnergy(int idx) { return normalizedEnergies[idx]; }
    public double getNormSum(int idx)          { return normSums[idx]; }
    public double getUrgency(int idx)          { return urgencies[idx]; }

    // ------------------- Setters -------------------

    public void setTransmissionTime(int idx, double val) { this.transmissionTimes[idx] = val; }
    public void setExecutionTime(int idx, double val)    { this.executionTimes[idx] = val; }
    public void setReceivingTime(int idx, double val)    { this.receivingTimes[idx] = val; }
    public void setOffloadingDelay(int idx, double val)  { this.offloadingDelays[idx] = val; }
    public void setEnergy(int idx, double val)           { this.energies[idx] = val; }
    public void setNormalizedDelay(int idx, double val)  { this.normalizedDelays[idx] = val; }
    public void setNormalizedEnergy(int idx, double val) { this.normalizedEnergies[idx] = val; }
    public void setNormSum(int idx, double val)          { this.normSums[idx] = val; }
    public void setUrgency(int idx, double val)          { this.urgencies[idx] = val; }
    public void setPrefCount(int val)                    { this.prefCount = val; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    // ------------------- Logic -------------------

    /**
     * Calculates the count of fog nodes that can run the task within its deadline.
     */
    public int calculatePrefCount() {
        int count = 0;
        for (int f = 0; f < offloadingDelays.length; f++) {
            if (offloadingDelays[f] <= deadline) {
                count++;
            }
        }
        this.prefCount = count;
        return count;
    }

    /**
     * Computes the urgency value w.r.t each fog node.
     * Formula:
     *   U = w1 * 1/(deadline - delay) + w2 * 1/pref(t) + w3 * 1/energy + w4 * critical_score
     * 
     * Critical score gamma = 1 + c * alpha, where:
     *   c = 1 if severity is Major else 0
     *   alpha = 0.5
     */
    public void computeUrgencies(double w1, double w2, double w3, double w4) {
        calculatePrefCount();
        
        double invPref = (prefCount == 0) ? 1.0 : (1.0 / prefCount);
        
        double criticalScore = 1.0;
        if (severity != null && severity.equalsIgnoreCase("Major")) {
            criticalScore = 1.5; // 1 + 1 * 0.5
        } else {
            criticalScore = 1.0; // 1 + 0 * 0.5
        }

        for (int f = 0; f < offloadingDelays.length; f++) {
            double delayDiff = deadline - offloadingDelays[f];
            // Safe division: if delayDiff <= 0, clamp to 0.001 (representing high urgency for missed deadline)
            double invDelayDiff = 1.0 / Math.max(delayDiff, 0.001);

            // Safe division for energy: clamp to 0.001 if energy <= 0
            double invEnergy = 1.0 / Math.max(energies[f], 0.001);

            this.urgencies[f] = w1 * invDelayDiff + w2 * invPref + w3 * invEnergy + w4 * criticalScore;
        }
    }

    /**
     * Sorts the fog node indices based on the least normSum (normalized delay + normalized energy).
     */
    public void computePreferredFogOrder(int numFogNodes) {
        this.preferredFogIndices = new int[numFogNodes];
        
        Integer[] indices = new Integer[numFogNodes];
        for (int i = 0; i < numFogNodes; i++) {
            indices[i] = i;
        }

        // Sort indices in ascending order of normSums value (least sum is first)
        java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(normSums[a], normSums[b]);
            }
        });

        for (int i = 0; i < numFogNodes; i++) {
            this.preferredFogIndices[i] = indices[i];
        }
    }

    // ------------------- Display -------------------

    @Override
    public String toString() {
        return String.format(
            "Task[ID=%2d | CPU=%.1f Mcycles | Input=%.2f KB | Output=%.2f KB | Deadline=%.2f s | %s | %s]",
            taskId, cpuDemanded, inputSize, outputSize, deadline,
            (phase == null ? "-" : phase), (severity == null ? "-" : severity)
        );
    }
}
