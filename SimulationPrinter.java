import java.util.ArrayList;
import java.util.List;

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
 *    8. printResults()                   — All results (Section VI metrics)
 *    9. printSummary()                   — Simulation completion summary
 * ============================================================================
 */
public class SimulationPrinter {

    private static final String LINE =
        "================================================================================";
    private static final String DASH =
        "--------------------------------------------------------------------------------";

    // ================================================================
    //  1. HEADER
    // ================================================================

    public static void printHeader(int numTasks, int numFogNodes, double[] weights) {
        System.out.println("\n" + LINE);
        System.out.println("  M-DAFTO FOG COMPUTING OFFLOADING SIMULATION");
        System.out.println(LINE);
        System.out.println("  Tasks          : " + numTasks);
        System.out.println("  Fog Nodes      : " + numFogNodes);
        System.out.println("  Bandwidth      : 20 MHz (2500 KB/s)");
        System.out.printf("  Weights        : w1=%.4f  w2=%.4f  w3=%.4f  w4=%.4f%n",
            weights[0], weights[1], weights[2], weights[3]);
        System.out.println(LINE + "\n");
    }

    // ================================================================
    //  2. AHP DIAGNOSTICS
    // ================================================================

    public static void printAHPDiagnostics(WeightGenerator.WeightResult result) {
        System.out.println(DASH);
        System.out.println("  AHP Diagnostics (Algorithms 3-6)");
        System.out.println(DASH);
        System.out.printf("  Iterations : %d%n", result.iterations);
        System.out.printf("  Lambda Max : %.4f%n", result.lambdaMax);
        System.out.printf("  CI         : %.4f%n", result.consistencyIndex);
        System.out.printf("  CR         : %.4f  (%s)%n",
            result.consistencyRatio,
            result.consistencyRatio <= 0.1 ? "PASSED" : "FAILED");
        System.out.println();

        String[] labels = {"DelayDiff", "PrefCount", "Energy", "Severity"};
        System.out.printf("  %-12s", "");
        for (String lbl : labels) System.out.printf("%12s", lbl);
        System.out.println();

        double[][] pm = result.pairwiseMatrix;
        for (int x = 0; x < 4; x++) {
            System.out.printf("  %-12s", labels[x]);
            for (int y = 0; y < 4; y++) {
                System.out.printf("%12.4f", pm[x][y]);
            }
            System.out.println();
        }
        System.out.println();
    }

    // ================================================================
    //  3. FOG NODES
    // ================================================================

    public static void printFogNodes(FogNetwork[] fogNetworks) {
        System.out.println(DASH);
        System.out.println("  Fog Node Specifications");
        System.out.println(DASH);
        System.out.printf("  %-12s %10s %8s %18s%n", "Name", "CPU(GHz)", "VRUs", "VRU Cap(Mcyc/s)");
        for (FogNetwork fn : fogNetworks) {
            System.out.printf("  %-12s %10.2f %8d %18.2f%n",
                fn.getName(), fn.getTotalCpuCapacity(),
                fn.getNumberOfVRUs(), fn.getVruCapacity());
        }
        System.out.println();
    }

    // ================================================================
    //  4. QUOTAS
    // ================================================================

    public static void printQuotas(FogNetwork[] fogNetworks) {
        System.out.println(DASH);
        System.out.println("  Fog Node Quotas (Algorithm 1)");
        System.out.println(DASH);
        System.out.printf("  %-12s %10s %10s %12s %12s%n",
            "Name", "Min(All)", "Max(All)", "Min(Major)", "Max(Major)");
        for (FogNetwork fn : fogNetworks) {
            String maxMajor = (fn.getMaxQuotaMajorTasks() == null)
                ? "empty" : String.valueOf(fn.getMaxQuotaMajorTasks());
            System.out.printf("  %-12s %10d %10d %12d %12s%n",
                fn.getName(),
                fn.getMinQuotaAllTasks(), fn.getMaxQuotaAllTasks(),
                fn.getMinQuotaMajorTasks(), maxMajor);
        }
        System.out.println();
    }

    // ================================================================
    //  5. SAMPLE TASKS
    // ================================================================

    public static void printSampleTasks(Task[] tasks, FogNetwork[] fogNetworks, int count) {
        int numToShow = Math.min(count, tasks.length);

        System.out.println(DASH);
        System.out.println("  Sample Task Analysis (First " + numToShow + " Tasks)");
        System.out.println(DASH);

        for (int i = 0; i < numToShow; i++) {
            Task t = tasks[i];
            System.out.printf("%n  Task T%d : CPU=%.1f Mcyc, In=%.1f KB, Out=%.1f KB, "
                + "DL=%.1fs, %s, %s, pref=%d%n",
                t.getTaskId(), t.getCpuDemanded(), t.getInputSize(), t.getOutputSize(),
                t.getDeadline(), t.getPhase(), t.getSeverity(), t.getPrefCount());

            System.out.printf("    %-12s %10s %10s %10s %10s%n",
                "FogNode", "Delay(s)", "Energy(J)", "NormSum", "Urgency");
            for (int f = 0; f < fogNetworks.length; f++) {
                System.out.printf("    %-12s %10.3f %10.3f %10.4f %10.4f%n",
                    fogNetworks[f].getName(),
                    t.getOffloadingDelay(f), t.getEnergy(f),
                    t.getNormSum(f), t.getUrgency(f));
            }

            StringBuilder prefSb = new StringBuilder("    Preferred: ");
            int[] pref = t.getPreferredFogIndices();
            for (int j = 0; j < pref.length; j++) {
                prefSb.append(fogNetworks[pref[j]].getName());
                if (j < pref.length - 1) prefSb.append(" -> ");
            }
            System.out.println(prefSb);
        }
        System.out.println();
    }

    // ================================================================
    //  6. PREFERRED TASK PRIORITIZATION
    // ================================================================

    public static void printPreferredTaskPrioritization(SimulationData simData,
                                                         Task[] tasks,
                                                         FogNetwork[] fogNetworks) {
        int topN = Math.min(SimulationConfig.TOP_RANKED_COUNT, tasks.length);

        System.out.println(DASH);
        System.out.println("  Task Prioritization per Fog Node (Top " + topN + ")");
        System.out.println(DASH);

        for (int f = 0; f < fogNetworks.length; f++) {
            System.out.printf("%n  %s:%n", fogNetworks[f].getName());
            System.out.printf("    %-6s %-6s %10s%n", "Rank", "Task", "Urgency");
            int[] priorityList = simData.getPreferredTasks(f);
            for (int r = 0; r < topN; r++) {
                int taskIdx = priorityList[r];
                Task t = tasks[taskIdx];
                System.out.printf("    %-6d T%-5d %10.4f%n",
                    r + 1, t.getTaskId(), t.getUrgency(f));
            }
        }
        System.out.println();
    }

    // ================================================================
    //  7. PRECEDENCE LISTS
    // ================================================================

    public static void printPrecedenceLists(Task[] precedenceListMajor,
                                             Task[] precedenceListMinor) {
        int topN = SimulationConfig.TOP_RANKED_COUNT;

        System.out.println(DASH);
        System.out.println("  Precedence Lists (Sorted by Ascending Deadline)");
        System.out.println(DASH);

        int showMajor = Math.min(topN, precedenceListMajor.length);
        System.out.printf("%n  Major Tasks (%d total, showing %d):%n", precedenceListMajor.length, showMajor);
        System.out.printf("    %-6s %-6s %12s%n", "Rank", "Task", "Deadline(s)");
        for (int r = 0; r < showMajor; r++) {
            Task t = precedenceListMajor[r];
            System.out.printf("    %-6d T%-5d %12.2f%n", r + 1, t.getTaskId(), t.getDeadline());
        }

        int showMinor = Math.min(topN, precedenceListMinor.length);
        System.out.printf("%n  Minor Tasks (%d total, showing %d):%n", precedenceListMinor.length, showMinor);
        System.out.printf("    %-6s %-6s %12s%n", "Rank", "Task", "Deadline(s)");
        for (int r = 0; r < showMinor; r++) {
            Task t = precedenceListMinor[r];
            System.out.printf("    %-6d T%-5d %12.2f%n", r + 1, t.getTaskId(), t.getDeadline());
        }
        System.out.println();
    }

    // ================================================================
    //  8. RESULTS (Single unified section — Paper Section VI)
    // ================================================================

    /**
     * Prints all results in a single section: matching summary, performance,
     * deadline compliance, major/minor breakdown, per-FN stats, utilization,
     * traffic load, satisfaction, and fairness.
     */
    public static void printResults(SimulationData simData, MSDAlgorithm.MatchingResult result) {
        Task[] tasks = simData.getTasks();
        FogNetwork[] fogNetworks = simData.getFogNetworks();
        int[] assignments = result.getTaskAssignment();
        int numTasks = tasks.length;
        int numFogs  = fogNetworks.length;
        int matched  = result.getMatchedCount();
        int outages  = result.getUnmatchedCount();

        // --- Compute all metrics ---

        // Total delay & energy
        double totalDelay = 0.0, totalEnergy = 0.0;
        for (int i = 0; i < numTasks; i++) {
            int f = assignments[i];
            if (f != -1) {
                totalDelay  += tasks[i].getOffloadingDelay(f);
                totalEnergy += tasks[i].getEnergy(f);
            }
        }
        double avgDelay  = matched > 0 ? totalDelay  / matched : 0.0;
        double avgEnergy = matched > 0 ? totalEnergy / matched : 0.0;

        // Deadline compliance
        int metDeadline = 0;
        for (int i = 0; i < numTasks; i++) {
            int f = assignments[i];
            if (f != -1 && tasks[i].getOffloadingDelay(f) <= tasks[i].getDeadline()) {
                metDeadline++;
            }
        }

        // Major vs Minor
        int majTotal = 0, majMatched = 0, minTotal = 0, minMatched = 0;
        double majDelay = 0, minDelay = 0, majEnergy = 0, minEnergy = 0;
        for (int i = 0; i < numTasks; i++) {
            boolean isMaj = "Major".equalsIgnoreCase(tasks[i].getSeverity());
            if (isMaj) {
                majTotal++;
                if (assignments[i] != -1) {
                    majMatched++;
                    majDelay  += tasks[i].getOffloadingDelay(assignments[i]);
                    majEnergy += tasks[i].getEnergy(assignments[i]);
                }
            } else {
                minTotal++;
                if (assignments[i] != -1) {
                    minMatched++;
                    minDelay  += tasks[i].getOffloadingDelay(assignments[i]);
                    minEnergy += tasks[i].getEnergy(assignments[i]);
                }
            }
        }

        // Per-FN stats
        double[] fnDelay  = new double[numFogs];
        double[] fnEnergy = new double[numFogs];
        int[]    fnCount  = new int[numFogs];
        for (int i = 0; i < numTasks; i++) {
            int f = assignments[i];
            if (f != -1) {
                fnDelay[f]  += tasks[i].getOffloadingDelay(f);
                fnEnergy[f] += tasks[i].getEnergy(f);
                fnCount[f]++;
            }
        }

        // Traffic load
        int[] matchesPerFog = new int[numFogs];
        for (int j = 0; j < numFogs; j++) {
            matchesPerFog[j] = result.getFogAssignments().get(j).size();
        }

        // Task satisfaction (Eq. 20)
        double totalTaskSat = 0.0;
        for (int i = 0; i < numTasks; i++) {
            int f = assignments[i];
            if (f != -1) {
                Task t = tasks[i];
                List<Integer> acceptable = new ArrayList<>();
                for (int fIdx : t.getPreferredFogIndices()) {
                    if (t.getOffloadingDelay(fIdx) <= t.getDeadline()) {
                        acceptable.add(fIdx);
                    }
                }
                int K = acceptable.size();
                if (K > 0) {
                    int pos = acceptable.indexOf(f);
                    if (pos != -1) {
                        totalTaskSat += ((double)(K - pos) / K) * 100.0;
                    }
                }
            }
        }
        double avgTaskSat = numTasks > 0 ? totalTaskSat / numTasks : 0.0;

        // FN satisfaction (Eq. 21-22)
        double totalFnSat = 0.0;
        for (int j = 0; j < numFogs; j++) {
            List<Integer> mTasks = result.getFogAssignments().get(j);
            int prefListSize = numTasks;
            int[] prefTasks = simData.getPreferredTasks(j);
            double sumSat = 0.0;
            for (int taskIdx : mTasks) {
                int rank = -1;
                for (int r = 0; r < prefTasks.length; r++) {
                    if (prefTasks[r] == taskIdx) { rank = r + 1; break; }
                }
                if (rank != -1) {
                    sumSat += ((double)(prefListSize - rank + 1) / prefListSize) * 100.0;
                }
            }
            int divisor = Math.min(fogNetworks[j].getNumberOfVRUs(), prefListSize);
            totalFnSat += divisor > 0 ? sumSat / divisor : 0.0;
        }
        double avgFnSat = numFogs > 0 ? totalFnSat / numFogs : 0.0;

        // Fairness
        double gini = computeGiniIndex(matchesPerFog);
        double jain = computeJainIndex(matchesPerFog);

        // === PRINT EVERYTHING ===

        System.out.println("\n" + LINE);
        System.out.println("  RESULTS (M-DAFTO Section VI)");
        System.out.println(LINE);

        // Matching overview
        System.out.printf("%n  Matched / Total        : %d / %d%n", matched, numTasks);
        System.out.printf("  Outage Rate            : %.2f%%  (%d tasks)%n",
            (outages * 100.0 / numTasks), outages);

        // Performance
        System.out.printf("%n  Total Offloading Delay : %.4f s%n", totalDelay);
        System.out.printf("  Avg Delay per Task     : %.4f s%n", avgDelay);
        System.out.printf("  Total Energy Consumed  : %.4f J%n", totalEnergy);
        System.out.printf("  Avg Energy per Task    : %.4f J%n", avgEnergy);

        // Deadline compliance
        System.out.printf("%n  Deadline Compliance    : (out of %d total tasks)%n", numTasks);
        System.out.printf("    Assigned & Met DL    : %d%n", metDeadline);
        System.out.printf("    Assigned & Missed DL : %d%n", matched - metDeadline);
        System.out.printf("    Unassigned (outages) : %d%n", outages);
        System.out.printf("    Success Rate         : %.1f%% of assigned tasks met their deadline%n",
            matched > 0 ? (metDeadline * 100.0 / matched) : 0);

        // Satisfaction & Fairness
        System.out.printf("%n  Task Satisfaction      : %.2f%%  (Eq. 20)%n", avgTaskSat);
        System.out.printf("  FN Satisfaction        : %.2f%%  (Eq. 21-22)%n", avgFnSat);
        System.out.printf("  Gini Index             : %.4f%n", gini);
        System.out.printf("  Jain Fairness Index    : %.4f%n", jain);

        // Major vs Minor
        System.out.printf("%n  Major vs Minor Breakdown:%n");
        System.out.printf("  %-8s %14s %10s %12s %12s%n",
            "Type", "Matched", "Outages", "Avg Delay", "Avg Energy");
        System.out.println("  " + "-".repeat(60));
        System.out.printf("  %-8s %4d/%-4d(%4.1f%%) %10d %10.4f s %10.4f J%n",
            "Major", majMatched, majTotal,
            majTotal > 0 ? (majMatched * 100.0 / majTotal) : 0,
            majTotal - majMatched,
            majMatched > 0 ? majDelay / majMatched : 0,
            majMatched > 0 ? majEnergy / majMatched : 0);
        System.out.printf("  %-8s %4d/%-4d(%4.1f%%) %10d %10.4f s %10.4f J%n",
            "Minor", minMatched, minTotal,
            minTotal > 0 ? (minMatched * 100.0 / minTotal) : 0,
            minTotal - minMatched,
            minMatched > 0 ? minDelay / minMatched : 0,
            minMatched > 0 ? minEnergy / minMatched : 0);

        // FN Assignment Table
        System.out.printf("%n  FN Assignment Table:%n");
        System.out.printf("  %-12s %8s %8s %8s %8s %8s %10s%n",
            "Fog Node", "MinQ", "MaxQ", "Major", "Minor", "Total", "Util(%)");
        System.out.println("  " + "-".repeat(68));
        for (int j = 0; j < numFogs; j++) {
            FogNetwork fn = fogNetworks[j];
            List<Integer> mTasks = result.getFogAssignments().get(j);
            int majCnt = 0, minCnt = 0;
            for (int tIdx : mTasks) {
                if (tasks[tIdx].getSeverity().equalsIgnoreCase("Major")) majCnt++;
                else minCnt++;
            }
            int total = mTasks.size();
            double util = fn.getMaxQuotaAllTasks() > 0
                ? (total * 100.0 / fn.getMaxQuotaAllTasks()) : 0.0;
            System.out.printf("  %-12s %8d %8d %8d %8d %8d %9.2f%%%n",
                fn.getName(), fn.getMinQuotaAllTasks(), fn.getMaxQuotaAllTasks(),
                majCnt, minCnt, total, util);
        }

        // Per-FN Delay & Energy
        System.out.printf("%n  Per-FN Delay & Energy:%n");
        System.out.printf("  %-12s %6s %12s %14s%n", "Fog Node", "Tasks", "Avg Delay(s)", "Total Energy(J)");
        System.out.println("  " + "-".repeat(50));
        for (int j = 0; j < numFogs; j++) {
            if (fnCount[j] > 0) {
                System.out.printf("  %-12s %6d %12.4f %14.4f%n",
                    fogNetworks[j].getName(), fnCount[j],
                    fnDelay[j] / fnCount[j], fnEnergy[j]);
            } else {
                System.out.printf("  %-12s %6d %12s %14s%n",
                    fogNetworks[j].getName(), 0, "--", "--");
            }
        }

        // Resource Utilization
        System.out.printf("%n  Resource Utilization:%n");
        for (int j = 0; j < numFogs; j++) {
            int assigned = matchesPerFog[j];
            int maxQ = fogNetworks[j].getMaxQuotaAllTasks();
            double util = maxQ > 0 ? (assigned * 100.0 / maxQ) : 0;
            System.out.printf("  %-12s %s %4d/%-4d (%5.1f%%)%n",
                fogNetworks[j].getName(), bar(util, 25), assigned, maxQ, util);
        }

        // Traffic Load
        System.out.printf("%n  Traffic Load Distribution:%n");
        int maxLoad = 1;
        for (int j = 0; j < numFogs; j++) {
            if (matchesPerFog[j] > maxLoad) maxLoad = matchesPerFog[j];
        }
        for (int j = 0; j < numFogs; j++) {
            double pct = matched > 0 ? (matchesPerFog[j] * 100.0 / matched) : 0;
            int barLen = (int) Math.round(matchesPerFog[j] * 25.0 / maxLoad);
            System.out.printf("  %-12s %s %4d tasks (%5.1f%%)%n",
                fogNetworks[j].getName(), buildBar(barLen, 25), matchesPerFog[j], pct);
        }

        System.out.println("\n" + LINE + "\n");
    }

    // ================================================================
    //  HELPERS
    // ================================================================

    private static double computeGiniIndex(int[] vals) {
        int n = vals.length;
        double sumDiff = 0, sumVals = 0;
        for (int a = 0; a < n; a++) {
            sumVals += vals[a];
            for (int b = 0; b < n; b++) {
                sumDiff += Math.abs(vals[a] - vals[b]);
            }
        }
        if (sumVals == 0) return 0;
        return sumDiff / (2.0 * n * n * (sumVals / n));
    }

    private static double computeJainIndex(int[] vals) {
        int n = vals.length;
        if (n == 0) return 0;
        double sum = 0, sumSq = 0;
        for (int x : vals) { sum += x; sumSq += (double) x * x; }
        if (sumSq == 0) return 1;
        return (sum * sum) / (n * sumSq);
    }

    private static String bar(double percent, int width) {
        int filled = Math.max(0, Math.min(width, (int) Math.round(percent / 100.0 * width)));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) sb.append(i < filled ? '#' : '.');
        sb.append(']');
        return sb.toString();
    }

    private static String buildBar(int filled, int width) {
        filled = Math.max(0, Math.min(filled, width));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) sb.append(i < filled ? '#' : '.');
        sb.append(']');
        return sb.toString();
    }

    // ================================================================
    //  9. SUMMARY
    // ================================================================

    public static void printSummary(SimulationData simData) {
        System.out.println(LINE);
        System.out.println("  Simulation Complete");
        System.out.println(LINE);
        System.out.println("  Tasks                     : " + simData.getTasks().length);
        System.out.println("  normDelayArray            : "
            + simData.getNormDelayArray().length + " x " + simData.getNormDelayArray()[0].length);
        System.out.println("  normEnergyArray           : "
            + simData.getNormEnergyArray().length + " x " + simData.getNormEnergyArray()[0].length);
        System.out.println("  normSumArray              : "
            + simData.getNormSumArray().length + " x " + simData.getNormSumArray()[0].length);
        System.out.println("  preferredFogIndices       : "
            + simData.getPreferredFogIndices().length + " x " + simData.getPreferredFogIndices()[0].length);
        System.out.println("  preferredTasksPerFog      : "
            + simData.getPreferredTasksPerFog().length + " x " + simData.getPreferredTasksPerFog()[0].length);
        System.out.println("  preferredMajorTasksPerFog : "
            + simData.getPreferredMajorTasksPerFog().length + " x " + simData.getPreferredMajorTasksPerFog()[0].length);
        System.out.println("  preferredMinorTasksPerFog : "
            + simData.getPreferredMinorTasksPerFog().length + " x " + simData.getPreferredMinorTasksPerFog()[0].length);
        System.out.println("  precedenceListMajor       : " + simData.getPrecedenceListMajor().length + " tasks");
        System.out.println("  precedenceListMinor       : " + simData.getPrecedenceListMinor().length + " tasks");
        System.out.println(LINE + "\n");
    }
}
