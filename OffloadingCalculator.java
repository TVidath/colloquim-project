
/**
 * OffloadingCalculator
 *
 * Utility class that computes:
 *  1. Transmission Time   = inputSizeKB   / uplinkDataRateKBps
 *  2. Execution Time      = cpuDemandedMillionCycles / vruCapacityMillionCyclesPerSec
 *  3. Receiving Time      = outputSizeKB  / downlinkDataRateKBps
 *  4. Offloading Delay    = Transmission Time + Execution Time + Receiving Time
 *  5. Energy              = (txPower * txTime) + (execPower * execTime) + (rxPower * rxTime)
 *
 * Data rates and power values are defined as constants here.
 */
public class OffloadingCalculator {

    // -------- Network Constants (KB/s) --------
    // 20 MHz = 20 * 10^6 bits/s / 8 = 2.5 * 10^6 bytes/s = 2500 KB/s
    public static final double UPLINK_DATA_RATE_KBPS   = 2500.0;   // KB/s (20 MHz)

    /** Downlink data rate in KB/s */
    public static final double DOWNLINK_DATA_RATE_KBPS = 2500.0;   // KB/s (20 MHz)

    // -------- Power Constants (Watts) --------
    /** Transmission power in Watts (device side) */
    public static final double TRANSMISSION_POWER = 0.5; // W (as requested)

    /** Execution power in Watts */
    public static final double EXECUTION_POWER    = 1.0; // W (leave as before)

    /** Receiving power in Watts */
    public static final double RECEIVING_POWER    = 0.5; // W

    // ------------------------------------------------
    //  Step 1: Compute individual time components
    // ------------------------------------------------

    /**
     * Transmission Time = Input Size (KB) / Uplink Rate (KB/s)  → seconds
     */
    public static double computeTransmissionTime(double inputSizeKB) {
        return inputSizeKB / UPLINK_DATA_RATE_KBPS;
    }

    /**
     * Execution Time = CPU Demanded (million cycles) / VRU Capacity (million cycles/sec)  → seconds
     */
    public static double computeExecutionTime(double cpuDemandedMillionCycles, double vruCapacityMillionCyclesPerSec) {
        return cpuDemandedMillionCycles / vruCapacityMillionCyclesPerSec;
    }

    /**
     * Receiving Time = Output Size (KB) / Downlink Rate (KB/s)  → seconds
     */
    public static double computeReceivingTime(double outputSizeKB) {
        return outputSizeKB / DOWNLINK_DATA_RATE_KBPS;
    }

    // ------------------------------------------------
    //  Step 2: Offloading Delay
    // ------------------------------------------------

    /**
     * Offloading Delay = Transmission Time + Execution Time + Receiving Time
     */
    public static double computeOffloadingDelay(double txTime, double execTime, double rxTime) {
        return txTime + execTime + rxTime;
    }

    // ------------------------------------------------
    //  Step 3: Energy
    // ------------------------------------------------

    /**
     * Energy (J) = TxPower * TxTime + ExecPower * ExecTime + RxPower * RxTime
     */
    public static double computeEnergy(double txTime, double execTime, double rxTime) {
        return (TRANSMISSION_POWER * txTime)
             + (EXECUTION_POWER    * execTime)
             + (RECEIVING_POWER    * rxTime);
    }

    // ------------------------------------------------
    //  Convenience: compute all metrics for a task
    //  w.r.t all available fog networks and store results
    // ------------------------------------------------

    /**
     * Fills all offloading metrics (delay + energy) into the task
     * for all available fog networks.
     *
     * @param task        The task to compute metrics for
     * @param fogNetworks The array of available fog networks
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
