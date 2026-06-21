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
 *    8. printMatchingResults()           — 2-Type MSDA matching report
 *    9. printSummary()                   — Simulation completion summary
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
    //  8. MATCHING RESULTS REPORT
    // ================================================================

    /**
     * Computes and prints detailed metrics and assignment tables for MSDA matching.
     */
    public static void printMatchingResults(SimulationData simData, MSDAlgorithm.MatchingResult result) {
        Task[] tasks = simData.getTasks();
        FogNetwork[] fogNetworks = simData.getFogNetworks();
        int[] assignments = result.getTaskAssignment();
        int numTasks = tasks.length;
        int numFogs = fogNetworks.length;

        // 1. Total Offloading Delay
        double totalDelay = 0.0;
        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                totalDelay += tasks[i].getOffloadingDelay(fogIdx);
            }
        }

        // 2. Outages
        int outages = result.getUnmatchedCount();

        // 3. Average Task Satisfaction Factor
        double totalTaskSatisfaction = 0.0;
        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                Task t = tasks[i];
                int[] preferredFogs = t.getPreferredFogIndices();
                List<Integer> acceptableFogs = new ArrayList<>();
                for (int fIdx : preferredFogs) {
                    if (t.getOffloadingDelay(fIdx) <= t.getDeadline()) {
                        acceptableFogs.add(fIdx);
                    }
                }
                int K = acceptableFogs.size();
                if (K > 0) {
                    int pos = acceptableFogs.indexOf(fogIdx);
                    if (pos != -1) {
                        double sat = ((double) (K - pos) / K) * 100.0;
                        totalTaskSatisfaction += sat;
                    }
                }
            }
        }
        double avgTaskSatisfaction = numTasks > 0 ? (totalTaskSatisfaction / numTasks) : 0.0;

        // 4. Average FN Satisfaction Factor
        double totalFNSatisfactionSum = 0.0;
        double[] fogSatArray = new double[numFogs];
        for (int j = 0; j < numFogs; j++) {
            List<Integer> matchedTasks = result.getFogAssignments().get(j);
            int capacity = fogNetworks[j].getNumberOfVRUs();
            int prefListSize = tasks.length;
            double sumTaskSatsForFog = 0.0;
            int[] prefTasks = simData.getPreferredTasks(j);

            for (int taskIdx : matchedTasks) {
                int rank = -1;
                for (int r = 0; r < prefTasks.length; r++) {
                    if (prefTasks[r] == taskIdx) {
                        rank = r + 1; // 1-based rank
                        break;
                    }
                }
                if (rank != -1) {
                    double taskSat = ((double) (prefListSize - rank + 1) / prefListSize) * 100.0;
                    sumTaskSatsForFog += taskSat;
                }
            }
            int divisor = Math.min(capacity, prefListSize);
            double fogSat = divisor > 0 ? (sumTaskSatsForFog / divisor) : 0.0;
            fogSatArray[j] = fogSat;
            totalFNSatisfactionSum += fogSat;
        }
        double avgFNSatisfaction = numFogs > 0 ? (totalFNSatisfactionSum / numFogs) : 0.0;

        // 5. Gini Index
        int[] matchesPerFog = new int[numFogs];
        for (int j = 0; j < numFogs; j++) {
            matchesPerFog[j] = result.getFogAssignments().get(j).size();
        }
        double giniIndex = computeGiniIndex(matchesPerFog);

        // ── Print Results ──
        System.out.println(SEPARATOR);
        System.out.println("  2-TYPE MULTI-STAGE DEFERRED ACCEPTANCE (MSDA) MATCHING RESULTS");
        System.out.println(SEPARATOR);
        System.out.printf("  Total Tasks Matched       : %d / %d%n", result.getMatchedCount(), numTasks);
        System.out.printf("  Outages (Unassigned)      : %d (Outage Rate: %.2f%%)%n", outages, ((double) outages / numTasks) * 100.0);
        System.out.printf("  Total Offloading Delay    : %.4f s%n", totalDelay);
        System.out.printf("  Average Task Satisfaction : %.2f%%%n", avgTaskSatisfaction);
        System.out.printf("  Average FN Satisfaction   : %.2f%%%n", avgFNSatisfaction);
        System.out.printf("  Load Balancer Gini Index  : %.4f (Lower is more balanced)%n", giniIndex);
        System.out.println();

        System.out.println("  FN Assignment & Utilization Table:");
        System.out.printf("  %-15s | %-12s | %-12s | %-12s | %-12s | %-12s | %-15s%n",
            "Fog Node", "Min Quota", "Max Quota", "Major Assigned", "Minor Assigned", "Total Assigned", "Utilization (%)");
        System.out.println("  " + "-".repeat(98));

        for (int j = 0; j < numFogs; j++) {
            FogNetwork fn = fogNetworks[j];
            List<Integer> matchedTasks = result.getFogAssignments().get(j);
            int majorCount = 0;
            int minorCount = 0;
            for (int tIdx : matchedTasks) {
                if (tasks[tIdx].getSeverity().equalsIgnoreCase("Major")) {
                    majorCount++;
                } else {
                    minorCount++;
                }
            }
            int totalAssigned = matchedTasks.size();
            double util = fn.getMaxQuotaAllTasks() > 0 ? (((double) totalAssigned / fn.getMaxQuotaAllTasks()) * 100.0) : 0.0;

            System.out.printf("  %-15s | %-12d | %-12d | %-12d | %-12d | %-12d | %-15.2f%%%n",
                fn.getName(), fn.getMinQuotaAllTasks(), fn.getMaxQuotaAllTasks(),
                majorCount, minorCount, totalAssigned, util);
        }
        System.out.println(SEPARATOR + "\n");
    }

    private static double computeGiniIndex(int[] matchesPerFog) {
        int n = matchesPerFog.length;
        double sumDiff = 0.0;
        double sumMatches = 0.0;
        for (int a = 0; a < n; a++) {
            sumMatches += matchesPerFog[a];
            for (int b = 0; b < n; b++) {
                sumDiff += Math.abs(matchesPerFog[a] - matchesPerFog[b]);
            }
        }
        if (sumMatches == 0.0) {
            return 0.0;
        }
        double mean = sumMatches / n;
        return sumDiff / (2.0 * n * n * mean);
    }

    // ================================================================
    //  8a. DETAILED PERFORMANCE ANALYSIS (Paper Metrics)
    // ================================================================

    /**
     * Prints comprehensive performance analysis matching M-DAFTO paper
     * Section VI metrics: performance, deadline compliance, major/minor
     * breakdown, per-FN stats, resource utilization, traffic load,
     * satisfaction factors, and fairness indices.
     */
    public static void printDetailedAnalysis(SimulationData simData,
                                              MSDAlgorithm.MatchingResult result) {
        Task[] tasks = simData.getTasks();
        FogNetwork[] fogNetworks = simData.getFogNetworks();
        int[] assignments = result.getTaskAssignment();
        int numTasks = tasks.length;
        int numFogs  = fogNetworks.length;
        int matched  = result.getMatchedCount();

        // ═══════════════════════ Compute All Metrics ═══════════════════════

        // ── 1. Performance Metrics ──
        double totalDelay = 0.0, totalEnergy = 0.0;
        double worstDelay = 0.0;
        int worstTaskId = -1, worstFogIdx = -1;

        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                double delay  = tasks[i].getOffloadingDelay(fogIdx);
                double energy = tasks[i].getEnergy(fogIdx);
                totalDelay  += delay;
                totalEnergy += energy;
                if (delay > worstDelay) {
                    worstDelay  = delay;
                    worstTaskId = tasks[i].getTaskId();
                    worstFogIdx = fogIdx;
                }
            }
        }
        double avgDelay  = matched > 0 ? totalDelay  / matched : 0.0;
        double avgEnergy = matched > 0 ? totalEnergy / matched : 0.0;

        // ── 2. Deadline Compliance ──
        int meetingDeadline = 0, violatingDeadline = 0;
        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                if (tasks[i].getOffloadingDelay(fogIdx) <= tasks[i].getDeadline()) {
                    meetingDeadline++;
                } else {
                    violatingDeadline++;
                }
            }
        }

        // ── 3. Major vs Minor Breakdown ──
        int majorTotal = 0, majorMatched = 0, minorTotal = 0, minorMatched = 0;
        double majorDelaySum = 0, minorDelaySum = 0;
        double majorEnergySum = 0, minorEnergySum = 0;
        for (int i = 0; i < numTasks; i++) {
            String sev = tasks[i].getSeverity();
            boolean isMajor = sev != null && sev.equalsIgnoreCase("Major");
            if (isMajor) {
                majorTotal++;
                if (assignments[i] != -1) {
                    majorMatched++;
                    majorDelaySum  += tasks[i].getOffloadingDelay(assignments[i]);
                    majorEnergySum += tasks[i].getEnergy(assignments[i]);
                }
            } else {
                minorTotal++;
                if (assignments[i] != -1) {
                    minorMatched++;
                    minorDelaySum  += tasks[i].getOffloadingDelay(assignments[i]);
                    minorEnergySum += tasks[i].getEnergy(assignments[i]);
                }
            }
        }

        // ── 4. Per-FN Stats ──
        double[] fnDelaySum  = new double[numFogs];
        double[] fnDelayMin  = new double[numFogs];
        double[] fnDelayMax  = new double[numFogs];
        double[] fnEnergySum = new double[numFogs];
        int[]    fnTaskCount = new int[numFogs];
        for (int j = 0; j < numFogs; j++) {
            fnDelayMin[j] =  Double.MAX_VALUE;
            fnDelayMax[j] = -Double.MAX_VALUE;
        }
        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                double d = tasks[i].getOffloadingDelay(fogIdx);
                fnDelaySum[fogIdx]  += d;
                fnEnergySum[fogIdx] += tasks[i].getEnergy(fogIdx);
                fnTaskCount[fogIdx]++;
                if (d < fnDelayMin[fogIdx]) fnDelayMin[fogIdx] = d;
                if (d > fnDelayMax[fogIdx]) fnDelayMax[fogIdx] = d;
            }
        }

        // ── 5. Per-FN load counts ──
        int[] matchesPerFog = new int[numFogs];
        for (int j = 0; j < numFogs; j++) {
            matchesPerFog[j] = result.getFogAssignments().get(j).size();
        }

        // ── 6. Fairness indices ──
        double gini = computeGiniIndex(matchesPerFog);
        double jain = computeJainIndex(matchesPerFog);

        // ── 7. Per-FN Satisfaction (Eq. 21-22) ──
        double[] fnSatisfaction = new double[numFogs];
        for (int j = 0; j < numFogs; j++) {
            List<Integer> matchedList = result.getFogAssignments().get(j);
            int prefListSize = numTasks;
            int[] prefTasks  = simData.getPreferredTasks(j);
            double sumSat = 0.0;

            for (int taskIdx : matchedList) {
                int rank = -1;
                for (int r = 0; r < prefTasks.length; r++) {
                    if (prefTasks[r] == taskIdx) {
                        rank = r + 1;
                        break;
                    }
                }
                if (rank != -1) {
                    sumSat += ((double)(prefListSize - rank + 1) / prefListSize) * 100.0;
                }
            }
            int divisor = Math.min(fogNetworks[j].getNumberOfVRUs(), prefListSize);
            fnSatisfaction[j] = divisor > 0 ? sumSat / divisor : 0.0;
        }

        // ── 8. Task Satisfaction (Eq. 20) ──
        double totalTaskSat = 0.0;
        for (int i = 0; i < numTasks; i++) {
            int fogIdx = assignments[i];
            if (fogIdx != -1) {
                Task t = tasks[i];
                int[] preferredFogs = t.getPreferredFogIndices();
                List<Integer> acceptable = new ArrayList<>();
                for (int fIdx : preferredFogs) {
                    if (t.getOffloadingDelay(fIdx) <= t.getDeadline()) {
                        acceptable.add(fIdx);
                    }
                }
                int K = acceptable.size();
                if (K > 0) {
                    int pos = acceptable.indexOf(fogIdx);
                    if (pos != -1) {
                        totalTaskSat += ((double)(K - pos) / K) * 100.0;
                    }
                }
            }
        }
        double avgTaskSat = numTasks > 0 ? totalTaskSat / numTasks : 0.0;
        double avgFnSat = 0.0;
        for (int j = 0; j < numFogs; j++) avgFnSat += fnSatisfaction[j];
        avgFnSat = numFogs > 0 ? avgFnSat / numFogs : 0.0;

        // ═══════════════════════ Print All Sections ═══════════════════════

        System.out.println(SEPARATOR);
        System.out.println("  DETAILED PERFORMANCE ANALYSIS (M-DAFTO Paper Metrics — Section VI)");
        System.out.println(SEPARATOR);

        // ── Section 1: Performance Metrics (Paper Fig. 3 & 4) ──
        System.out.println("\n  -- 1. PERFORMANCE METRICS (Fig. 3 & 4) --");
        System.out.printf("  Total Offloading Delay        : %.4f s%n", totalDelay);
        System.out.printf("  Average Offloading Delay      : %.4f s  (per matched task)%n", avgDelay);
        System.out.printf("  Worst Offloading Delay        : %.4f s  (Task T%d -> %s)%n",
            worstDelay, worstTaskId,
            worstFogIdx >= 0 ? fogNetworks[worstFogIdx].getName() : "N/A");
        System.out.printf("  Total Energy Consumed         : %.4f J%n", totalEnergy);
        System.out.printf("  Average Energy per Task       : %.4f J  (per matched task)%n", avgEnergy);
        System.out.printf("  Outages (Unassigned Tasks)    : %d / %d  (Outage Rate: %.2f%%)%n",
            result.getUnmatchedCount(), numTasks,
            (result.getUnmatchedCount() * 100.0 / numTasks));

        // ── Section 2: Deadline Compliance ──
        System.out.println("\n  -- 2. DEADLINE COMPLIANCE --");
        System.out.printf("  Tasks Meeting Deadline        : %d / %d matched  (%.2f%%)%n",
            meetingDeadline, matched,
            matched > 0 ? (meetingDeadline * 100.0 / matched) : 0);
        System.out.printf("  Tasks Violating Deadline      : %d  (%.2f%%)%n",
            violatingDeadline,
            matched > 0 ? (violatingDeadline * 100.0 / matched) : 0);

        // ── Section 3: Major vs Minor Breakdown ──
        System.out.println("\n  -- 3. MAJOR vs MINOR TASK BREAKDOWN --");
        System.out.printf("  %-10s | %-20s | %-10s | %-14s | %-14s%n",
            "Type", "Matched", "Outages", "Avg Delay", "Avg Energy");
        System.out.println("  " + "-".repeat(78));
        System.out.printf("  %-10s | %4d / %-4d (%5.1f%%) | %-10d | %12.4f s | %12.4f J%n",
            "Major", majorMatched, majorTotal,
            majorTotal > 0 ? (majorMatched * 100.0 / majorTotal) : 0,
            majorTotal - majorMatched,
            majorMatched > 0 ? majorDelaySum / majorMatched : 0,
            majorMatched > 0 ? majorEnergySum / majorMatched : 0);
        System.out.printf("  %-10s | %4d / %-4d (%5.1f%%) | %-10d | %12.4f s | %12.4f J%n",
            "Minor", minorMatched, minorTotal,
            minorTotal > 0 ? (minorMatched * 100.0 / minorTotal) : 0,
            minorTotal - minorMatched,
            minorMatched > 0 ? minorDelaySum / minorMatched : 0,
            minorMatched > 0 ? minorEnergySum / minorMatched : 0);

        // ── Section 4: Per-FN Offloading Delay & Energy ──
        System.out.println("\n  -- 4. PER-FN OFFLOADING DELAY & ENERGY --");
        System.out.printf("  %-15s | %-6s | %-12s | %-12s | %-12s | %-14s%n",
            "Fog Node", "Tasks", "Avg Delay", "Min Delay", "Max Delay", "Total Energy");
        System.out.println("  " + "-".repeat(85));
        for (int j = 0; j < numFogs; j++) {
            if (fnTaskCount[j] > 0) {
                System.out.printf("  %-15s | %5d | %10.4f s | %10.4f s | %10.4f s | %12.4f J%n",
                    fogNetworks[j].getName(), fnTaskCount[j],
                    fnDelaySum[j] / fnTaskCount[j], fnDelayMin[j],
                    fnDelayMax[j], fnEnergySum[j]);
            } else {
                System.out.printf("  %-15s | %5d | %12s | %12s | %12s | %14s%n",
                    fogNetworks[j].getName(), 0, "  --", "  --", "  --", "  --");
            }
        }

        // ── Section 5: Resource Utilization (Paper Fig. 7) ──
        System.out.println("\n  -- 5. RESOURCE UTILIZATION — Tasks Assigned / Max Quota (Fig. 7) --");
        for (int j = 0; j < numFogs; j++) {
            int assigned = matchesPerFog[j];
            int maxQ     = fogNetworks[j].getMaxQuotaAllTasks();
            double util  = maxQ > 0 ? (assigned * 100.0 / maxQ) : 0;
            System.out.printf("  %-12s %s  %4d / %-4d  (%5.2f%%)%n",
                fogNetworks[j].getName(), buildProgressBar(util, 30),
                assigned, maxQ, util);
        }

        // ── Section 6: Traffic Load Distribution ──
        System.out.println("\n  -- 6. TRAFFIC LOAD DISTRIBUTION --");
        int maxLoad = 1;
        for (int j = 0; j < numFogs; j++) {
            if (matchesPerFog[j] > maxLoad) maxLoad = matchesPerFog[j];
        }
        for (int j = 0; j < numFogs; j++) {
            double pct    = matched > 0 ? (matchesPerFog[j] * 100.0 / matched) : 0;
            int    barLen = (int) Math.round(matchesPerFog[j] * 30.0 / maxLoad);
            System.out.printf("  %-12s %s  %4d tasks  (%5.2f%%)%n",
                fogNetworks[j].getName(), buildAbsoluteBar(barLen, 30),
                matchesPerFog[j], pct);
        }

        // ── Section 7: Satisfaction Factors (Paper Fig. 5 & 6) ──
        System.out.println("\n  -- 7. SATISFACTION FACTORS (Fig. 5 & 6) --");
        System.out.printf("  Average Task Satisfaction     : %6.2f%%   (Eq. 20)%n", avgTaskSat);
        System.out.printf("  Average FN Satisfaction       : %6.2f%%   (Eq. 21-22)%n", avgFnSat);
        System.out.println("  Per-FN Satisfaction:");
        for (int j = 0; j < numFogs; j++) {
            System.out.printf("    %-15s : %6.2f%%  %s%n",
                fogNetworks[j].getName(), fnSatisfaction[j],
                buildProgressBar(fnSatisfaction[j], 20));
        }

        // ── Section 8: Fairness Indices (Paper Fig. 8 & Appendix E) ──
        System.out.println("\n  -- 8. FAIRNESS INDICES (Fig. 8 & Appendix E) --");
        System.out.printf("  Gini Index                    : %.4f   (0 = perfect equality, 1 = inequality)%n", gini);
        System.out.printf("  Jain Fairness Index           : %.4f   (%.4f = worst, 1.0 = best)%n",
            jain, 1.0 / numFogs);

        System.out.println("\n" + SEPARATOR);
    }

    // ================================================================
    //  HELPERS — Fairness & Visualization
    // ================================================================

    /** Computes Jain Fairness Index: (sum(x))^2 / (n * sum(x^2)). Range: [1/n, 1.0]. */
    private static double computeJainIndex(int[] values) {
        int n = values.length;
        if (n == 0) return 0.0;
        double sum = 0.0, sumSq = 0.0;
        for (int x : values) {
            sum   += x;
            sumSq += (double) x * x;
        }
        if (sumSq == 0.0) return 1.0;
        return (sum * sum) / (n * sumSq);
    }

    /** Builds a progress bar: [####..........] based on percentage (0-100). */
    private static String buildProgressBar(double percent, int width) {
        int filled = (int) Math.round(percent / 100.0 * width);
        filled = Math.max(0, Math.min(filled, width));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? '#' : '.');
        }
        sb.append(']');
        return sb.toString();
    }

    /** Builds an absolute-length bar: [######........] with given fill length. */
    private static String buildAbsoluteBar(int filled, int width) {
        filled = Math.max(0, Math.min(filled, width));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? '#' : '.');
        }
        sb.append(']');
        return sb.toString();
    }

    // ================================================================
    //  9. SUMMARY
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
