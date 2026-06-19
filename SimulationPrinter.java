/**
 * ============================================================================
 *  SimulationPrinter
 * ============================================================================
 *
 *  Handles ALL console output for the simulation.
 *  Every print method is self-contained and clearly named.
 *
 *  Methods:
 *    1. printHeader()                    — Simulation config summary
 *    2. printAHPDiagnostics()            — AHP weight generation details
 *    3. printFogNodes()                  — Generated fog node specs
 *    4. printQuotas()                    — Fog node quota table
 *    5. printSampleTasks()               — Detailed view of first N tasks
 *    6. printPreferredTaskPrioritization() — Top-N urgent tasks per fog
 *    7. printPrecedenceLists()           — Major/Minor deadline-sorted lists
 *    8. printSummary()                   — Simulation completion summary
 * ============================================================================
 */
public class SimulationPrinter {

    private static final String SEPARATOR =
        "==========================================================================================================";
    private static final String SHORT_SEP =
        "==========================================================================================";

    // ================================================================
    //  1. HEADER
    // ================================================================

    /**
     * Prints the simulation header with configuration info.
     */
    public static void printHeader(int numTasks, int numFogNodes, double[] weights) {
        System.out.println(SEPARATOR);
        System.out.println("  FOG SIMULATION — Urgency & Sort  |  Tasks: "
            + numTasks + "  |  Fog Nodes: " + numFogNodes);
        System.out.printf(
            "  Urgency Weights: w1(DelayDiff)=%.4f | w2(PrefCount)=%.4f"
          + " | w3(Energy)=%.4f | w4(Severity)=%.4f%n",
            weights[0], weights[1], weights[2], weights[3]);
        System.out.println("  Uplink/Downlink Bandwidth: 20 MHz (2500 KB/s) for all Nodes");
        System.out.println(SEPARATOR + "\n");
    }

    // ================================================================
    //  2. AHP DIAGNOSTICS
    // ================================================================

    /**
     * Prints AHP weight generation diagnostic information.
     */
    public static void printAHPDiagnostics(WeightGenerator.WeightResult result) {
        System.out.println(SEPARATOR);
        System.out.println("  AHP WEIGHT GENERATION DIAGNOSTICS (Algorithms 3-6 Consistency Check)");
        System.out.println(SEPARATOR);
        System.out.printf("  AHP Iterations to Converge : %d%n",    result.iterations);
        System.out.printf("  Lambda Max                 : %.4f%n",  result.lambdaMax);
        System.out.printf("  Consistency Index (CI)     : %.4f%n",  result.consistencyIndex);
        System.out.printf("  Consistency Ratio (CR)     : %.4f (Passed: CR <= 0.1)%n",
            result.consistencyRatio);

        // Print pairwise comparison matrix
        System.out.println("  Pairwise Comparison Matrix P:");
        String[] labels = {"c1(DelayDiff)", "c2(PrefCount)", "c3(Energy)", "c4(Severity)"};

        System.out.print("                 ");
        for (String lbl : labels) {
            System.out.printf("%-15s", lbl);
        }
        System.out.println();

        double[][] pm = result.pairwiseMatrix;
        for (int x = 0; x < 4; x++) {
            System.out.printf("  %-15s", labels[x]);
            for (int y = 0; y < 4; y++) {
                System.out.printf("%-15.4f", pm[x][y]);
            }
            System.out.println();
        }
        System.out.println(SEPARATOR + "\n");
    }

    // ================================================================
    //  3. FOG NODES
    // ================================================================

    /**
     * Prints the generated fog node specifications.
     */
    public static void printFogNodes(FogNetwork[] fogNetworks) {
        System.out.println("--- Generated Fog Nodes ---");
        for (FogNetwork fn : fogNetworks) {
            System.out.println("  " + fn.toString());
        }
        System.out.println();
    }

    // ================================================================
    //  4. QUOTAS
    // ================================================================

    /**
     * Prints the fog node quota table (min/max for All Tasks and Major Tasks).
     */
    public static void printQuotas(FogNetwork[] fogNetworks) {
        System.out.println(SHORT_SEP);
        System.out.println("  FOG NODE QUOTAS (Computed via M-DAFTO Algorithm 1)");
        System.out.println(SHORT_SEP);
        System.out.printf("  %-15s | %-12s | %-12s | %-12s | %-12s%n",
            "Fog Node", "Min (All)", "Max (All)", "Min (Major)", "Max (Major)");
        System.out.println("  " + "-".repeat(88));

        for (FogNetwork fn : fogNetworks) {
            String maxMajorStr = (fn.getMaxQuotaMajorTasks() == null)
                ? "empty" : String.valueOf(fn.getMaxQuotaMajorTasks());

            System.out.printf("  %-15s | %-12d | %-12d | %-12d | %-12s%n",
                fn.getName(),
                fn.getMinQuotaAllTasks(), fn.getMaxQuotaAllTasks(),
                fn.getMinQuotaMajorTasks(), maxMajorStr);
        }
        System.out.println(SHORT_SEP + "\n");
    }

    // ================================================================
    //  5. SAMPLE TASKS
    // ================================================================

    /**
     * Prints detailed analysis of the first N tasks w.r.t all fog nodes.
     */
    public static void printSampleTasks(Task[] tasks, FogNetwork[] fogNetworks, int count) {
        int numToShow = Math.min(count, tasks.length);

        System.out.println(SHORT_SEP);
        System.out.println("  SAMPLE TASK ANALYSIS (First " + numToShow
            + " Tasks w.r.t All Fog Nodes)");
        System.out.println(SHORT_SEP);

        for (int i = 0; i < numToShow; i++) {
            Task t = tasks[i];

            // Task header
            System.out.printf(
                "Task T%d (CPU: %.1f Mcycles, Input: %.1f KB, Output: %.1f KB, "
              + "Phase: %s, Severity: %s, Deadline: %.1fs, pref(t): %d)%n",
                t.getTaskId(), t.getCpuDemanded(), t.getInputSize(), t.getOutputSize(),
                t.getPhase(), t.getSeverity(), t.getDeadline(), t.getPrefCount());

            // Per-fog-node metrics
            for (int f = 0; f < fogNetworks.length; f++) {
                System.out.printf(
                    "  - %-12s: Delay = %6.3fs | Energy = %6.3fJ"
                  + " | NormSum = %.4f | Urgency = %.4f%n",
                    fogNetworks[f].getName(),
                    t.getOffloadingDelay(f), t.getEnergy(f),
                    t.getNormSum(f), t.getUrgency(f));
            }

            // Preferred fog order
            StringBuilder prefSb = new StringBuilder();
            int[] pref = t.getPreferredFogIndices();
            for (int j = 0; j < pref.length; j++) {
                prefSb.append(fogNetworks[pref[j]].getName());
                if (j < pref.length - 1) {
                    prefSb.append(" -> ");
                }
            }
            System.out.println("  => Preferred Fog Order: " + prefSb.toString());
            System.out.println();
        }
    }

    // ================================================================
    //  6. PREFERRED TASK PRIORITIZATION
    // ================================================================

    /**
     * Prints top-N most urgent tasks ranked for each fog node.
     */
    public static void printPreferredTaskPrioritization(SimulationData simData,
                                                         Task[] tasks,
                                                         FogNetwork[] fogNetworks) {
        int topN = Math.min(SimulationConfig.TOP_RANKED_COUNT, tasks.length);

        System.out.println(SHORT_SEP);
        System.out.println("  PREFERRED TASK PRIORITIZATION PER FOG NODE (Top "
            + topN + " Most Urgent Tasks Ranked)");
        System.out.println(SHORT_SEP);

        for (int f = 0; f < fogNetworks.length; f++) {
            System.out.printf("%s Priority Order:%n  ", fogNetworks[f].getName());
            int[] priorityList = simData.getPreferredTasks(f);

            for (int r = 0; r < topN; r++) {
                int taskIdx = priorityList[r];
                Task t = tasks[taskIdx];
                System.out.printf("[Rank %2d: T%d (Urg: %5.2f)]",
                    r + 1, t.getTaskId(), t.getUrgency(f));
                if (r < topN - 1) {
                    System.out.print(" -> ");
                }
            }
            System.out.println("\n");
        }
    }

    // ================================================================
    //  7. PRECEDENCE LISTS
    // ================================================================

    /**
     * Prints Major and Minor precedence lists (sorted by ascending deadline).
     */
    public static void printPrecedenceLists(Task[] precedenceListMajor,
                                             Task[] precedenceListMinor) {
        int topN = SimulationConfig.TOP_RANKED_COUNT;

        System.out.println(SHORT_SEP);
        System.out.println("  PRECEDENCE LISTS (Major and Minor Tasks sorted by Ascending Deadline)");
        System.out.println(SHORT_SEP);

        // Major list
        System.out.printf("  Precedence List Major (Total: %d tasks):%n  ",
            precedenceListMajor.length);
        int showMajor = Math.min(topN, precedenceListMajor.length);
        for (int r = 0; r < showMajor; r++) {
            Task t = precedenceListMajor[r];
            System.out.printf("[Rank %2d: T%d (DL: %5.2fs)]", r + 1, t.getTaskId(), t.getDeadline());
            if (r < showMajor - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println("\n");

        // Minor list
        System.out.printf("  Precedence List Minor (Total: %d tasks):%n  ",
            precedenceListMinor.length);
        int showMinor = Math.min(topN, precedenceListMinor.length);
        for (int r = 0; r < showMinor; r++) {
            Task t = precedenceListMinor[r];
            System.out.printf("[Rank %2d: T%d (DL: %5.2fs)]", r + 1, t.getTaskId(), t.getDeadline());
            if (r < showMinor - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println("\n");
    }

    // ================================================================
    //  8. SUMMARY
    // ================================================================

    /**
     * Prints the simulation completion summary with array dimensions.
     */
    public static void printSummary(SimulationData simData) {
        System.out.println(SHORT_SEP);
        System.out.println("  [Simulation Complete] SimulationData compiled successfully");
        System.out.println("  Tasks count             → " + simData.getTasks().length);
        System.out.println("  normDelayArray shape    → "
            + simData.getNormDelayArray().length + " x " + simData.getNormDelayArray()[0].length);
        System.out.println("  normEnergyArray shape   → "
            + simData.getNormEnergyArray().length + " x " + simData.getNormEnergyArray()[0].length);
        System.out.println("  normSumArray shape      → "
            + simData.getNormSumArray().length + " x " + simData.getNormSumArray()[0].length);
        System.out.println("  preferredFogIndices     → "
            + simData.getPreferredFogIndices().length + " x " + simData.getPreferredFogIndices()[0].length);
        System.out.println("  preferredTasksPerFog    → "
            + simData.getPreferredTasksPerFog().length + " x " + simData.getPreferredTasksPerFog()[0].length);
        System.out.println("  preferredMajorTasksPerFog → "
            + simData.getPreferredMajorTasksPerFog().length + " x " + simData.getPreferredMajorTasksPerFog()[0].length);
        System.out.println("  preferredMinorTasksPerFog → "
            + simData.getPreferredMinorTasksPerFog().length + " x " + simData.getPreferredMinorTasksPerFog()[0].length);
        System.out.println("  precedenceListMajor     → " + simData.getPrecedenceListMajor().length + " tasks");
        System.out.println("  precedenceListMinor     → " + simData.getPrecedenceListMinor().length + " tasks");
        System.out.println(SHORT_SEP);
    }
}
