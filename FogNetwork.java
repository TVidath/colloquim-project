/**
 * ============================================================================
 *  FogNetwork — Data Model
 * ============================================================================
 *
 *  Represents a single Fog Network node in the IoT-Fog architecture.
 *
 *  Each fog node has:
 *    ── Identity ──
 *    - fogId            : Unique fog node identifier
 *    - name             : Descriptive name (e.g., "FogNode_1")
 *
 *    ── Hardware Specs ──
 *    - totalCpuGHz      : Total CPU capacity in GHz
 *    - numberOfVRUs     : Number of Virtual Resource Units
 *    - vruCapacity      : Derived: (GHz * 1000) / VRUs = million cycles/sec per VRU
 *
 *    ── Quotas (computed by QuotaDeterminator) ──
 *    - minQuotaAllTasks  : Minimum tasks this node must accept (all tasks)
 *    - maxQuotaAllTasks  : Maximum tasks this node can accept (all tasks) = VRUs
 *    - minQuotaMajorTasks: Minimum Major tasks this node must accept
 *    - maxQuotaMajorTasks: Maximum Major tasks this node can accept = VRUs
 *
 *  The VRU capacity is used to compute execution time:
 *    Execution Time = CPU Demanded (million cycles) / VRU Capacity (million cycles/sec per VRU)
 * ============================================================================
 */
public class FogNetwork {

    // ── Identity ──
    private int    fogId;
    private String name;

    // ── Hardware Specs ──
    private double totalCpuGHz;     // Total CPU capacity in GHz
    private int    numberOfVRUs;    // Number of Virtual Resource Units
    private double vruCapacity;     // Derived: million cycles/sec per VRU

    // ── Quotas (set by QuotaDeterminator) ──
    private int     minQuotaAllTasks;
    private int     maxQuotaAllTasks;
    private int     minQuotaMajorTasks;
    private Integer maxQuotaMajorTasks;   // Set to numberOfVRUs

    // ================================================================
    //  CONSTRUCTOR
    // ================================================================

    /**
     * Creates a new FogNetwork node.
     *
     * @param fogId                Unique fog node identifier
     * @param name                 Descriptive name (e.g., "FogNode_1")
     * @param totalCpuCapacityGHz  Total CPU capacity in GHz
     * @param numberOfVRUs         Number of Virtual Resource Units
     */
    public FogNetwork(int fogId, String name, double totalCpuCapacityGHz, int numberOfVRUs) {
        this.fogId        = fogId;
        this.name         = name;
        this.totalCpuGHz  = totalCpuCapacityGHz;
        this.numberOfVRUs = numberOfVRUs;

        // Derived: Convert GHz to million cycles/sec, then divide by VRUs
        // Example: 8 GHz = 8000 million cycles/sec → 8000 / 400 VRUs = 20 Mcycles/s/VRU
        this.vruCapacity = (totalCpuCapacityGHz * 1000.0) / numberOfVRUs;
    }

    // ================================================================
    //  GETTERS — Identity & Hardware
    // ================================================================

    public int    getFogId()           { return fogId; }
    public String getName()            { return name; }
    public double getTotalCpuCapacity() { return totalCpuGHz; }
    public int    getNumberOfVRUs()    { return numberOfVRUs; }
    public double getVruCapacity()     { return vruCapacity; }

    // ================================================================
    //  GETTERS / SETTERS — Quotas
    // ================================================================

    public int  getMinQuotaAllTasks()                  { return minQuotaAllTasks; }
    public void setMinQuotaAllTasks(int val)            { this.minQuotaAllTasks = val; }

    public int  getMaxQuotaAllTasks()                  { return maxQuotaAllTasks; }
    public void setMaxQuotaAllTasks(int val)            { this.maxQuotaAllTasks = val; }

    public int     getMinQuotaMajorTasks()              { return minQuotaMajorTasks; }
    public void    setMinQuotaMajorTasks(int val)       { this.minQuotaMajorTasks = val; }

    public Integer getMaxQuotaMajorTasks()              { return maxQuotaMajorTasks; }
    public void    setMaxQuotaMajorTasks(Integer val)   { this.maxQuotaMajorTasks = val; }

    // ================================================================
    //  DISPLAY
    // ================================================================

    @Override
    public String toString() {
        return String.format(
            "FogNetwork[ID=%d | Name=%-12s | CPU=%.2f GHz | VRUs=%3d "
          + "| VRU Capacity=%.2f Mcycles/s/VRU]",
            fogId, name, totalCpuGHz, numberOfVRUs, vruCapacity
        );
    }
}
