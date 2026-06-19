import java.util.Random;

/**
 * ============================================================================
 *  Main — Simulation Orchestrator
 * ============================================================================
 *  Entry point for the M-DAFTO Fog Computing Offloading Simulation.
 *
 *  This class orchestrates the full pipeline by calling each component
 *  in order. All logic is delegated to specialized classes:
 *
 *    Step 1  : Generate fog networks        → FogNetworkGenerator
 *    Step 2  : Generate AHP weights         → WeightGenerator
 *    Step 3  : Print header + diagnostics   → SimulationPrinter
 *    Step 4  : Generate tasks               → TaskGenerator
 *    Step 5  : Compute delay & energy       → OffloadingCalculator
 *    Step 6  : Normalize metrics            → Normalizer
 *    Step 7  : Compute urgencies            → UrgencyCalculator
 *    Step 8  : Rank preferences             → PreferenceRanker
 *    Step 9  : Build precedence lists       → PreferenceRanker
 *    Step 10 : Compute quotas               → QuotaDeterminator
 *    Step 11 : Assemble SimulationData      → SimulationData
 *    Step 12 : Print results                → SimulationPrinter
 *
 *  Compile : javac *.java
 *  Run     : java Main
 * ============================================================================
 */
public class Main {

    public static void main(String[] args) {

        Random rand = new Random();

        // ── Step 1: Generate Fog Networks ──
        FogNetwork[] fogNetworks = FogNetworkGenerator.generate(rand);

        // ── Step 2: Generate AHP Urgency Weights ──
        WeightGenerator.WeightResult weightResult = WeightGenerator.generateWeights(rand);
        double[] weights = weightResult.weights;
        double w1 = weights[0], w2 = weights[1], w3 = weights[2], w4 = weights[3];

        // ── Step 3: Print Configuration & Diagnostics ──
        SimulationPrinter.printHeader(SimulationConfig.NUM_TASKS,
                                      SimulationConfig.NUM_FOG_NODES, weights);
        SimulationPrinter.printAHPDiagnostics(weightResult);
        SimulationPrinter.printFogNodes(fogNetworks);

        // ── Step 4: Generate Tasks ──
        Task[] tasks = TaskGenerator.generate(rand);

        // ── Step 5: Compute Delay & Energy for all tasks × all fog nodes ──
        for (Task task : tasks) {
            OffloadingCalculator.computeAndStore(task, fogNetworks);
        }

        // ── Step 6: Normalize Metrics Globally ──
        Normalizer.normalize(tasks, fogNetworks.length);

        // ── Step 7: Compute Urgencies ──
        UrgencyCalculator.computeAllUrgencies(tasks, w1, w2, w3, w4);

        // ── Step 8: Rank Task Preferences per Fog Node ──
        int[][] preferredTasksPerFog      = PreferenceRanker.rankAllTasksPerFog(tasks, fogNetworks.length);
        int[][] preferredMajorTasksPerFog = PreferenceRanker.rankMajorTasksPerFog(tasks, fogNetworks.length);
        int[][] preferredMinorTasksPerFog = PreferenceRanker.rankMinorTasksPerFog(tasks, fogNetworks.length);

        // ── Step 9: Build Precedence Lists ──
        Task[] precedenceListMajor = PreferenceRanker.buildMajorPrecedenceList(tasks);
        Task[] precedenceListMinor = PreferenceRanker.buildMinorPrecedenceList(tasks);

        // ── Step 10: Compute Quotas ──
        int majorCount = PreferenceRanker.countMajorTasks(tasks);
        QuotaDeterminator.computeMinimumQuotas(fogNetworks, SimulationConfig.NUM_TASKS, true);
        QuotaDeterminator.computeMinimumQuotas(fogNetworks, majorCount, false);

        SimulationPrinter.printQuotas(fogNetworks);

        // ── Step 11: Extract Normalized Arrays & Assemble SimulationData ──
        int numTasks = SimulationConfig.NUM_TASKS;
        double[][] normDelayArray  = new double[numTasks][fogNetworks.length];
        double[][] normEnergyArray = new double[numTasks][fogNetworks.length];
        double[][] normSumArray    = new double[numTasks][fogNetworks.length];
        int[][]    preferredFogIndices = new int[numTasks][fogNetworks.length];

        for (int i = 0; i < numTasks; i++) {
            normDelayArray[i]      = tasks[i].getNormalizedDelays();
            normEnergyArray[i]     = tasks[i].getNormalizedEnergies();
            normSumArray[i]        = tasks[i].getNormSums();
            preferredFogIndices[i] = tasks[i].getPreferredFogIndices();
        }

        SimulationData simData = new SimulationData(
            tasks, fogNetworks,
            normDelayArray, normEnergyArray, normSumArray, preferredFogIndices,
            preferredTasksPerFog, preferredMajorTasksPerFog, preferredMinorTasksPerFog,
            weights, precedenceListMajor, precedenceListMinor
        );

        // ── Step 12: Print Results ──
        SimulationPrinter.printSampleTasks(tasks, fogNetworks, SimulationConfig.SAMPLE_TASK_COUNT);
        SimulationPrinter.printPreferredTaskPrioritization(simData, tasks, fogNetworks);
        SimulationPrinter.printPrecedenceLists(precedenceListMajor, precedenceListMinor);
        SimulationPrinter.printSummary(simData);
    }
}