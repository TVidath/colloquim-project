
/**
 * Represents a Fog Network node.
 *
 * Each fog node has:
 *  - A unique ID and name
 *  - Total CPU capacity (in MIPS)
 *  - Number of Virtual Resource Units (VRUs)
 *  - Derived VRU capacity = totalCpuCapacity / numberOfVRUs
 *
 * The VRU capacity is used to compute the execution time for each task:
 *     Execution Time = CPU Demanded (MIPS) / VRU Capacity (MIPS per VRU)
 */
public class FogNetwork {

    private int fogId;
    private String name;
    private double totalCpuGHz;         // in GHz
    private int numberOfVRUs;           // Number of Virtual Resource Units
    private double vruCapacity;         // million cycles/sec per VRU = (GHz * 1000) / VRUs

    // Computed quotas for matching (All Tasks & Major Tasks sets)
    private int minQuotaAllTasks;
    private int maxQuotaAllTasks;
    private int minQuotaMajorTasks;
    private Integer maxQuotaMajorTasks; // Kept empty as per user request

    /**
     * Constructor for FogNetwork
     *
     * @param fogId             Unique fog node identifier
     * @param name              Descriptive name (e.g., "FogNode_A")
     * @param totalCpuCapacityGHz Total CPU in GHz
     * @param numberOfVRUs      Number of VRUs on this fog node
     */
    public FogNetwork(int fogId, String name, double totalCpuCapacityGHz, int numberOfVRUs) {
        this.fogId = fogId;
        this.name = name;
        this.totalCpuGHz = totalCpuCapacityGHz;
        this.numberOfVRUs = numberOfVRUs;
        this.vruCapacity = (totalCpuCapacityGHz * 1000.0) / numberOfVRUs;   // Derived in million cycles/sec per VRU
    }

    // ------------------- Getters & Setters -------------------

    public int getFogId()               { return fogId; }
    public String getName()             { return name; }
    public double getTotalCpuCapacity() { return totalCpuGHz; }
    public int getNumberOfVRUs()        { return numberOfVRUs; }
    public double getVruCapacity()      { return vruCapacity; }

    public int getMinQuotaAllTasks()                { return minQuotaAllTasks; }
    public void setMinQuotaAllTasks(int val)        { this.minQuotaAllTasks = val; }

    public int getMaxQuotaAllTasks()                { return maxQuotaAllTasks; }
    public void setMaxQuotaAllTasks(int val)        { this.maxQuotaAllTasks = val; }

    public int getMinQuotaMajorTasks()              { return minQuotaMajorTasks; }
    public void setMinQuotaMajorTasks(int val)      { this.minQuotaMajorTasks = val; }

    public Integer getMaxQuotaMajorTasks()          { return maxQuotaMajorTasks; }
    public void setMaxQuotaMajorTasks(Integer val)  { this.maxQuotaMajorTasks = val; }

    // ------------------- Display -------------------

    @Override
    public String toString() {
        return String.format(
            "FogNetwork[ID=%d | Name=%-12s | CPU=%.2f GHz | VRUs=%3d | VRU Capacity=%.2f Mcycles/s/VRU]",
            fogId, name, totalCpuGHz, numberOfVRUs, vruCapacity
        );
    }
}
