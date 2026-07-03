import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
 *             :  java Main > output.txt
 * ============================================================================
 */
public class Main {

    public static void main(String[] args) {
        Random rand = new Random();
        int[] scenarios = {250, 500, 1000, 2000};
        int iterations = 10;
        
        ByteArrayOutputStream detailedOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream captureOut = new PrintStream(detailedOutput);

        for (int scenarioTasks : scenarios) {
            SimulationConfig.NUM_TASKS = scenarioTasks;
            
            SimulationMetrics baselineAccumulator = new SimulationMetrics();
            SimulationMetrics proposedAccumulator = new SimulationMetrics();

            for (int iter = 0; iter < iterations; iter++) {
                boolean printDetails = (iter == 0);

                // ── Step 1: Generate Core Data ──
                FogNetwork[] fogNetworks = FogNetworkGenerator.generate(rand);
                Task[] baseTasks = TaskGenerator.generate(rand);

                // ── Step 2: Compute Absolute Delay & Energy (Common) ──
                for (Task task : baseTasks) {
                    OffloadingCalculator.computeAndStore(task, fogNetworks);
                }

                // ── Step 3: Deep Copy Tasks for Independent Pipelines ──
                Task[] baselineTasks = new Task[baseTasks.length];
                Task[] proposedTasks = new Task[baseTasks.length];
                for (int i = 0; i < baseTasks.length; i++) {
                    baselineTasks[i] = baseTasks[i].deepCopy();
                    proposedTasks[i] = baseTasks[i].deepCopy();
                }

                // ================================================================
                //  BASELINE PIPELINE (M-DAFTO)
                // ================================================================
                WeightGenerator.WeightResult baselineWeights = WeightGenerator.generateBaselineWeights(rand);
                Normalizer.normalizeBaseline(baselineTasks, fogNetworks.length);
                UrgencyCalculator.computeBaselineUrgencies(baselineTasks, baselineWeights.weights[0], baselineWeights.weights[1]);
                
                int[][] prefBase = PreferenceRanker.rankAllTasksPerFog(baselineTasks, fogNetworks.length);
                Task[] precBaseMaj = PreferenceRanker.buildMajorPrecedenceList(baselineTasks);
                Task[] precBaseMin = PreferenceRanker.buildMinorPrecedenceList(baselineTasks);
                
                SimulationData baselineSimData = buildSimData(
                    baselineTasks, fogNetworks, baselineWeights.weights, 
                    prefBase, prefBase, prefBase, precBaseMaj, precBaseMin
                );

                // ================================================================
                //  PROPOSED PIPELINE (2-Type MSDA)
                // ================================================================
                WeightGenerator.WeightResult proposedWeights = WeightGenerator.generateWeights(rand);
                Normalizer.normalize(proposedTasks, fogNetworks.length);
                UrgencyCalculator.computeAllUrgencies(
                    proposedTasks, proposedWeights.weights[0], proposedWeights.weights[1], 
                    proposedWeights.weights[2], proposedWeights.weights[3]
                );
                
                int[][] prefProp    = PreferenceRanker.rankAllTasksPerFog(proposedTasks, fogNetworks.length);
                int[][] prefPropMaj = PreferenceRanker.rankMajorTasksPerFog(proposedTasks, fogNetworks.length);
                int[][] prefPropMin = PreferenceRanker.rankMinorTasksPerFog(proposedTasks, fogNetworks.length);
                Task[] precPropMaj  = PreferenceRanker.buildMajorPrecedenceList(proposedTasks);
                Task[] precPropMin  = PreferenceRanker.buildMinorPrecedenceList(proposedTasks);
                
                SimulationData proposedSimData = buildSimData(
                    proposedTasks, fogNetworks, proposedWeights.weights, 
                    prefProp, prefPropMaj, prefPropMin, precPropMaj, precPropMin
                );

                // ── Step 4: Compute Quotas (Based on proposed/common task counts) ──
                int majorCount = PreferenceRanker.countMajorTasks(proposedTasks);
                QuotaDeterminator.computeMinimumQuotas(fogNetworks, SimulationConfig.NUM_TASKS, true);
                QuotaDeterminator.computeMinimumQuotas(fogNetworks, majorCount, false);

                // ── Step 7: Run Algorithms & Capture Results ──
                if (printDetails) {
                    System.setOut(captureOut);
                    System.out.println("\n==========================================================================");
                    System.out.println(" DETAILED RESULTS FOR 1 ITERATION (SCENARIO: " + scenarioTasks + " TASKS)");
                    System.out.println("==========================================================================");
                    System.out.println("\n=================================================");
                    System.out.println(" RUNNING BASELINE M-DAFTO ALGORITHM");
                    System.out.println("=================================================");
                }
                MSDAlgorithm.MatchingResult baselineResult = MSDAlgorithm.matchBaseline(baselineSimData);
                SimulationMetrics baseMetrics = SimulationPrinter.printResults(baselineSimData, baselineResult, true, printDetails);
                baselineAccumulator.add(baseMetrics);

                if (printDetails) {
                    System.out.println("\n=================================================");
                    System.out.println(" RUNNING PROPOSED 2-TYPE MSDA ALGORITHM");
                    System.out.println("=================================================");
                }
                MSDAlgorithm.MatchingResult result = MSDAlgorithm.match(proposedSimData);
                SimulationMetrics propMetrics = SimulationPrinter.printResults(proposedSimData, result, false, printDetails);
                proposedAccumulator.add(propMetrics);
                
                if (printDetails) {
                    System.setOut(originalOut);
                }

            } // End of Iterations

            baselineAccumulator.divideBy(iterations);
            proposedAccumulator.divideBy(iterations);
            
            SimulationPrinter.printScenarioAverages(scenarioTasks, baselineAccumulator, proposedAccumulator);
        } // End of Scenarios
        
        // Print the detailed output at the very end
        System.out.println(detailedOutput.toString());
    }

    private static SimulationData buildSimData(Task[] tasks, FogNetwork[] fogNetworks, double[] weights, 
                                               int[][] pAll, int[][] pMaj, int[][] pMin, 
                                               Task[] precMaj, Task[] precMin) {
        int numTasks = tasks.length;
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

        return new SimulationData(
            tasks, fogNetworks, normDelayArray, normEnergyArray, normSumArray, 
            preferredFogIndices, pAll, pMaj, pMin, weights, precMaj, precMin
        );
    }
}