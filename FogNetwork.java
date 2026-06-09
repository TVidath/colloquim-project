
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

    /**
     * Constructor for FogNetwork
     *
     * @param fogId             Unique fog node identifier
     * @param name              Descriptive name (e.g., "FogNode_A")
     * @param totalCpuCapacity  Total CPU in MIPS (constant, specified)
     * @param numberOfVRUs      Number of VRUs on this fog node
     */
    public FogNetwork(int fogId, String name, double totalCpuCapacityGHz, int numberOfVRUs) {
        this.fogId = fogId;
        this.name = name;
        this.totalCpuGHz = totalCpuCapacityGHz;
        this.numberOfVRUs = numberOfVRUs;
        this.vruCapacity = (totalCpuCapacityGHz * 1000.0) / numberOfVRUs;   // Derived in million cycles/sec per VRU
    }

    // ------------------- Getters -------------------

    public int getFogId()               { return fogId; }
    public String getName()             { return name; }
    public double getTotalCpuCapacity() { return totalCpuGHz; }
    public int getNumberOfVRUs()        { return numberOfVRUs; }
    public double getVruCapacity()      { return vruCapacity; }

    // ------------------- Display -------------------

    @Override
    public String toString() {
        return String.format(
            "FogNetwork[ID=%d | Name=%-12s | CPU=%.2f GHz | VRUs=%3d | VRU Capacity=%.2f Mcycles/s/VRU]",
            fogId, name, totalCpuGHz, numberOfVRUs, vruCapacity
        );
    }
}
