/**
 * ============================================================================
 *  OffloadingCalculator
 * ============================================================================
 *
 *  Computes delay and energy metrics for offloading a task to a fog node.
 *
 *  Formulas:
 *    1. Transmission Time = InputSize (KB) / UplinkRate (KB/s)       → seconds
 *    2. Execution Time    = CpuDemand (Mcycles) / VruCapacity (Mcycles/s) → seconds
 *    3. Receiving Time    = OutputSize (KB) / DownlinkRate (KB/s)    → seconds
 *    4. Offloading Delay  = TransmissionTime + ExecutionTime + ReceivingTime
 *    5. Energy (Joules)   = (TxPower × TxTime) + (ExecPower × ExecTime)
 *                           + (RxPower × RxTime)
 *
 *  Network & Power constants are defined below.
 * ============================================================================
 */
public class OffloadingCalculator {

    // ================================================================
    //  NETWORK CONSTANTS
    // ================================================================

    /**
     * Uplink data rate in KB/s.
     * 20 MHz = 20 × 10^6 bits/s ÷ 8 = 2.5 × 10^6 bytes/s = 2500 KB/s
     */
    public static final double UPLINK_DATA_RATE_KBPS = 2500.0;

    /**
     * Downlink data rate in KB/s.
     * Same bandwidth as uplink: 20 MHz = 2500 KB/s
     */
    public static final double DOWNLINK_DATA_RATE_KBPS = 2500.0;

    // ================================================================
    //  POWER CONSTANTS (Watts)
    // ================================================================

    /** Transmission power — device side (W) */
    public static final double TRANSMISSION_POWER = 0.5;

    /** Execution power — fog node side (W) */
    public static final double EXECUTION_POWER = 1.0;

    /** Receiving power — device side (W) */
    public static final double RECEIVING_POWER = 0.5;

    // ================================================================
    //  INDIVIDUAL COMPUTATIONS
    // ================================================================

    /**
     * Transmission Time = Input Size (KB) / Uplink Rate (KB/s) → seconds
     */
    public static double computeTransmissionTime(double inputSizeKB) {
        return inputSizeKB / UPLINK_DATA_RATE_KBPS;
    }

    /**
     * Execution Time = CPU Demanded (Mcycles) / VRU Capacity (Mcycles/s) → seconds
     */
    public static double computeExecutionTime(double cpuDemandedMcycles,
                                               double vruCapacityMcyclesPerSec) {
        return cpuDemandedMcycles / vruCapacityMcyclesPerSec;
    }

    /**
     * Receiving Time = Output Size (KB) / Downlink Rate (KB/s) → seconds
     */
    public static double computeReceivingTime(double outputSizeKB) {
        return outputSizeKB / DOWNLINK_DATA_RATE_KBPS;
    }

    /**
     * Offloading Delay = Transmission Time + Execution Time + Receiving Time
     */
    public static double computeOffloadingDelay(double txTime, double execTime,
                                                 double rxTime) {
        return txTime + execTime + rxTime;
    }

    /**
     * Energy (J) = TxPower × TxTime + ExecPower × ExecTime + RxPower × RxTime
     */
    public static double computeEnergy(double txTime, double execTime,
                                        double rxTime) {
        return (TRANSMISSION_POWER * txTime)
             + (EXECUTION_POWER    * execTime)
             + (RECEIVING_POWER    * rxTime);
    }

    // ================================================================
    //  BATCH COMPUTATION
    // ================================================================

    /**
     * Computes all offloading metrics (delay + energy) for one task
     * across ALL available fog networks, and stores results in the task.
     *
     * @param task        The task to compute metrics for
     * @param fogNetworks Array of all available fog nodes
     */
    public static void computeAndStore(Task task, FogNetwork[] fogNetworks) {
        task.initFogMetrics(fogNetworks.length);

        for (int i = 0; i < fogNetworks.length; i++) {
            FogNetwork fog = fogNetworks[i];

            double txTime   = computeTransmissionTime(task.getInputSize());
            double execTime = computeExecutionTime(task.getCpuDemanded(), fog.getVruCapacity());
            double rxTime   = computeReceivingTime(task.getOutputSize());
            double delay    = computeOffloadingDelay(txTime, execTime, rxTime);
            double energy   = computeEnergy(txTime, execTime, rxTime);

            task.setTransmissionTime(i, txTime);
            task.setExecutionTime(i, execTime);
            task.setReceivingTime(i, rxTime);
            task.setOffloadingDelay(i, delay);
            task.setEnergy(i, energy);
        }
    }
}
