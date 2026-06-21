import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 *  MSDAlgorithm
 * ============================================================================
 *
 *  Implements the 2-Type Multi-Stage Deferred Acceptance (MSDA) algorithm
 *  for task offloading in IoT-Fog systems.
 *
 *  Algorithm Steps:
 *    1. Match Major tasks using MSDA subject to Major quotas.
 *    2. Update remaining quotas of Fog Nodes.
 *    3. Match Minor tasks using MSDA subject to remaining capacities.
 *    4. Combine and return matching results.
 * ============================================================================
 */
public class MSDAlgorithm {

    /**
     * DTO containing the results of the matching algorithm.
     */
    public static class MatchingResult {
        private final int[] taskAssignment;         // taskAssignment[taskIdx] = fogIdx (or -1 if unmatched)
        private final List<List<Integer>> fogAssignments; // fogAssignments.get(fogIdx) = list of assigned task indices
        private final int unmatchedCount;
        private final int matchedCount;

        public MatchingResult(int[] taskAssignment, List<List<Integer>> fogAssignments,
                              int unmatchedCount, int matchedCount) {
            this.taskAssignment = taskAssignment;
            this.fogAssignments = fogAssignments;
            this.unmatchedCount = unmatchedCount;
            this.matchedCount   = matchedCount;
        }

        public int[] getTaskAssignment() {
            return taskAssignment;
        }

        public List<List<Integer>> getFogAssignments() {
            return fogAssignments;
        }

        public int getUnmatchedCount() {
            return unmatchedCount;
        }

        public int getMatchedCount() {
            return matchedCount;
        }
    }

    /**
     * Runs the 2-Type MSDA matching orchestrator.
     *
     * @param simData The collected simulation data
     * @return MatchingResult containing assignments and stats
     */
    public static MatchingResult match(SimulationData simData) {
        Task[] tasks = simData.getTasks();
        FogNetwork[] fogNetworks = simData.getFogNetworks();
        int numTasks = tasks.length;
        int numFogs  = fogNetworks.length;

        // ── Stage 1: Match Major Tasks ──
        Task[] precedenceListMajor = simData.getPrecedenceListMajor();
        int[] majorMaxQuotas = new int[numFogs];
        int[] majorMinQuotas = new int[numFogs];

        for (int j = 0; j < numFogs; j++) {
            Integer maxQuotaMajor = fogNetworks[j].getMaxQuotaMajorTasks();
            majorMaxQuotas[j] = (maxQuotaMajor != null) ? maxQuotaMajor : fogNetworks[j].getNumberOfVRUs();
            majorMinQuotas[j] = fogNetworks[j].getMinQuotaMajorTasks();
        }

        int[] majorAssignments = runMSDA(tasks, precedenceListMajor,
                                         simData.getPreferredFogIndices(),
                                         majorMaxQuotas, majorMinQuotas, numFogs);

        // ── Stage 2: Match Minor Tasks ──
        // Compute remaining capacities for each fog node after Major tasks are assigned
        int[] majorMatchesPerFog = new int[numFogs];
        for (int tIdx = 0; tIdx < numTasks; tIdx++) {
            int fogIdx = majorAssignments[tIdx];
            if (fogIdx != -1) {
                majorMatchesPerFog[fogIdx]++;
            }
        }

        int[] minorMaxQuotas = new int[numFogs];
        int[] minorMinQuotas = new int[numFogs];

        for (int j = 0; j < numFogs; j++) {
            minorMaxQuotas[j] = fogNetworks[j].getMaxQuotaAllTasks() - majorMatchesPerFog[j];
            minorMinQuotas[j] = Math.max(0, fogNetworks[j].getMinQuotaAllTasks() - majorMatchesPerFog[j]);
        }

        Task[] precedenceListMinor = simData.getPrecedenceListMinor();
        int[] minorAssignments = runMSDA(tasks, precedenceListMinor,
                                         simData.getPreferredFogIndices(),
                                         minorMaxQuotas, minorMinQuotas, numFogs);

        // ── Step 3: Combine and Compile Results ──
        int[] finalAssignments = new int[numTasks];
        Arrays.fill(finalAssignments, -1);

        List<List<Integer>> fogAssignments = new ArrayList<>();
        for (int j = 0; j < numFogs; j++) {
            fogAssignments.add(new ArrayList<>());
        }

        int matchedCount = 0;
        for (int tIdx = 0; tIdx < numTasks; tIdx++) {
            int assignedFog = -1;
            if (majorAssignments[tIdx] != -1) {
                assignedFog = majorAssignments[tIdx];
            } else if (minorAssignments[tIdx] != -1) {
                assignedFog = minorAssignments[tIdx];
            }

            finalAssignments[tIdx] = assignedFog;
            if (assignedFog != -1) {
                fogAssignments.get(assignedFog).add(tIdx);
                matchedCount++;
            }
        }

        int unmatchedCount = numTasks - matchedCount;

        return new MatchingResult(finalAssignments, fogAssignments, unmatchedCount, matchedCount);
    }

    /**
     * Runs the MSDA algorithm for a subset of tasks (Major or Minor).
     */
    private static int[] runMSDA(Task[] allTasks, Task[] precedenceList,
                                  int[][] preferredFogIndices,
                                  int[] initMaxQuotas, int[] initMinQuotas, int numFogs) {
        int[] assignments = new int[allTasks.length];
        Arrays.fill(assignments, -1);

        int[] maxQuotas = Arrays.copyOf(initMaxQuotas, numFogs);
        int[] minQuotas = Arrays.copyOf(initMinQuotas, numFogs);

        List<Task> pl = new ArrayList<>(Arrays.asList(precedenceList));

        // Iterate through MSDA stages
        while (!pl.isEmpty()) {
            // Compute quota reservation parameter rk = sum of current minimum quotas
            int rk = 0;
            for (int q : minQuotas) {
                rk += q;
            }

            List<Task> daTasks;
            int[] capacityLimits;
            boolean allowReplacements;

            if (pl.size() <= rk) {
                // Minimum quota stage (Compulsory matching, no replacements allowed)
                daTasks = new ArrayList<>(pl);
                capacityLimits = minQuotas;
                allowReplacements = false;
            } else {
                // Maximum quota stage (Standard matching with replacements allowed)
                int daSize = pl.size() - rk;
                daTasks = new ArrayList<>(pl.subList(0, daSize));
                capacityLimits = maxQuotas;
                allowReplacements = true;
            }

            // Run Deferred Acceptance for tasks in this stage
            int[] daMatches = runDA(allTasks, daTasks, capacityLimits,
                                    preferredFogIndices, allowReplacements, numFogs);

            // Update matching decisions and quotas
            int[] matchesPerFog = new int[numFogs];
            for (Task t : daTasks) {
                int taskIdx = t.getTaskId() - 1;
                int fogIdx = daMatches[taskIdx];
                assignments[taskIdx] = fogIdx;
                if (fogIdx != -1) {
                    matchesPerFog[fogIdx]++;
                }
            }

            for (int j = 0; j < numFogs; j++) {
                maxQuotas[j] -= matchesPerFog[j];
                minQuotas[j] = Math.max(0, minQuotas[j] - matchesPerFog[j]);
            }

            // All tasks in daTasks are processed in this stage and removed from PL
            List<Task> nextPl = new ArrayList<>();
            for (Task t : pl) {
                if (!daTasks.contains(t)) {
                    nextPl.add(t);
                }
            }
            pl = nextPl;
        }

        return assignments;
    }

    /**
     * Standard Deferred Acceptance matching.
     */
    private static int[] runDA(Task[] allTasks, List<Task> candidateTasks,
                               int[] capacityLimits, int[][] preferredFogIndices,
                               boolean allowReplacements, int numFogs) {
        int[] daMatches = new int[allTasks.length];
        Arrays.fill(daMatches, -1);

        List<List<Task>> fogMatched = new ArrayList<>();
        for (int j = 0; j < numFogs; j++) {
            fogMatched.add(new ArrayList<>());
        }

        int[] nextProposedFogIndex = new int[allTasks.length];
        boolean[] isMatched = new boolean[allTasks.length];

        boolean active = true;
        while (active) {
            active = false;
            for (Task t : candidateTasks) {
                int taskIdx = t.getTaskId() - 1;
                if (isMatched[taskIdx]) {
                    continue;
                }

                int[] preferredFogs = preferredFogIndices[taskIdx];
                int nextFogIdx = nextProposedFogIndex[taskIdx];
                if (nextFogIdx >= preferredFogs.length) {
                    continue; // proposed to all available fog nodes
                }

                // This task is unmatched and still has nodes to propose to
                active = true;
                int fogIdx = preferredFogs[nextFogIdx];
                nextProposedFogIndex[taskIdx]++;

                // Deadline constraint (delay check)
                double delay = t.getOffloadingDelay(fogIdx);
                if (delay > t.getDeadline()) {
                    continue; // unacceptable match, skip fog node
                }

                // Propose to fog node
                List<Task> matchedList = fogMatched.get(fogIdx);
                int cap = capacityLimits[fogIdx];

                if (matchedList.size() < cap) {
                    matchedList.add(t);
                    daMatches[taskIdx] = fogIdx;
                    isMatched[taskIdx] = true;
                } else if (allowReplacements && cap > 0) {
                    // Find the matched task with the lowest urgency
                    Task tWorst = null;
                    double minUrgency = Double.MAX_VALUE;
                    for (Task matched : matchedList) {
                        double urg = matched.getUrgency(fogIdx);
                        if (urg < minUrgency) {
                            minUrgency = urg;
                            tWorst = matched;
                        }
                    }

                    if (tWorst != null && t.getUrgency(fogIdx) > tWorst.getUrgency(fogIdx)) {
                        // Replace tWorst with t
                        int tWorstIdx = tWorst.getTaskId() - 1;
                        matchedList.remove(tWorst);
                        daMatches[tWorstIdx] = -1;
                        isMatched[tWorstIdx] = false;

                        matchedList.add(t);
                        daMatches[taskIdx] = fogIdx;
                        isMatched[taskIdx] = true;
                    }
                }
            }
        }

        return daMatches;
    }
}
