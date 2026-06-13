import java.util.Random;

/**
 * FOG COMPUTING OFFLOADING SIMULATION — Phase 1 (Urgency and Priority Sorting Extension)
 * Generates tasks, computes delay/energy across all fog nodes, normalizes globally,
 * ranks fog node preferences based on least (normDelay + normEnergy), generates urgency weights,
 * calculates urgency values for all tasks, and sorts tasks by preference for each fog node.
 * 
 * Compile : javac *.java
 * Run     : java Main
 */
public class Main {

    // ============================================================
    //  CONFIGURATION
    // ============================================================
    static final int    NUM_TASKS       = 250;

    static final double INPUT_SIZE_MIN  = 300.0, INPUT_SIZE_MAX  = 600.0;   // KB
    static final double OUTPUT_SIZE_MIN = 10.0,  OUTPUT_SIZE_MAX = 20.0;    // KB
    static final double CPU_DEMAND_MIN  = 210.0, CPU_DEMAND_MAX  = 480.0;   // million cycles
    static final double DEADLINE_MIN    = 15.0,  DEADLINE_MAX    = 25.0;    // sec

    static final double CPU_CAP_MIN_GHZ = 6.0, CPU_CAP_MAX_GHZ = 10.0;      // GHz
    static final int    VRU_MIN         = 200, VRU_MAX         = 500;      // number of VRUs

    // ============================================================
    //  MAIN
    // ============================================================
    public static void main(String[] args) {

        Random rand = new Random(42);

        // --- Generate Fog Networks ---
        FogNetwork[] fogNetworks = new FogNetwork[5];
        for (int i = 0; i < fogNetworks.length; i++) {
            double cpuGHz = randomInRange(rand, CPU_CAP_MIN_GHZ, CPU_CAP_MAX_GHZ);
            int vrus = randomIntInRange(rand, VRU_MIN, VRU_MAX);
            fogNetworks[i] = new FogNetwork(i + 1, "FogNode_" + (i + 1), cpuGHz, vrus);
        }

        // --- Generate Urgency Weights via AHP (Normalized, w1 + w2 + w3 + w4 = 1.0) ---
        double[] weights = WeightGenerator.generateWeights(rand);
        double w1 = weights[0], w2 = weights[1], w3 = weights[2], w4 = weights[3];

        // --- Header & AHP Diagnostics ---
        System.out.println("==========================================================================================================");
        System.out.println("  FOG SIMULATION — Phase 1 (Urgency & Sort)  |  Tasks: " + NUM_TASKS + "  |  Fog Nodes: " + fogNetworks.length);
        System.out.printf ("  Urgency Weights: w1(DelayDiff)=%.4f | w2(PrefCount)=%.4f | w3(Energy)=%.4f | w4(Severity)=%.4f%n",
            w1, w2, w3, w4);
        System.out.println("  Uplink/Downlink Bandwidth: 20 MHz (2500 KB/s) for all Nodes");
        System.out.println("==========================================================================================================\n");

        System.out.println("==========================================================================================================");
        System.out.println("  AHP WEIGHT GENERATION DIAGNOSTICS (Algorithms 3-6 Consistency Check)");
        System.out.println("==========================================================================================================");
        System.out.printf ("  AHP Iterations to Converge : %d%n", WeightGenerator.iterations);
        System.out.printf ("  Lambda Max                 : %.4f%n", WeightGenerator.lambdaMax);
        System.out.printf ("  Consistency Index (CI)     : %.4f%n", WeightGenerator.consistencyIndex);
        System.out.printf ("  Consistency Ratio (CR)     : %.4f (Passed: CR <= 0.1)%n", WeightGenerator.consistencyRatio);
        System.out.println("  Pairwise Comparison Matrix P:");
        double[][] pm = WeightGenerator.pairwiseMatrix;
        String[] labels = {"c1(DelayDiff)", "c2(PrefCount)", "c3(Energy)", "c4(Severity)"};
        System.out.print("                 ");
        for (String lbl : labels) System.out.printf("%-15s", lbl);
        System.out.println();
        for (int x = 0; x < 4; x++) {
            System.out.printf("  %-15s", labels[x]);
            for (int y = 0; y < 4; y++) {
                System.out.printf("%-15.4f", pm[x][y]);
            }
            System.out.println();
        }
        System.out.println("==========================================================================================================\n");

        System.out.println("--- Generated Fog Nodes ---");
        for (FogNetwork fn : fogNetworks) {
            System.out.println("  " + fn.toString());
        }
        System.out.println();

        // --- Generate Tasks ---
        Task[] tasks = new Task[NUM_TASKS];
        for (int i = 0; i < NUM_TASKS; i++) {
            tasks[i] = new Task(
                i + 1,
                randomInRange(rand, CPU_DEMAND_MIN,  CPU_DEMAND_MAX),
                randomInRange(rand, INPUT_SIZE_MIN,  INPUT_SIZE_MAX),
                randomInRange(rand, OUTPUT_SIZE_MIN, OUTPUT_SIZE_MAX),
                randomInRange(rand, DEADLINE_MIN,    DEADLINE_MAX)
            );

            // Randomly assign phase (Pre-Surgery / Post-Surgery) and severity (Minor / Major)
            String phase = rand.nextBoolean() ? "Pre-Surgery" : "Post-Surgery";
            String severity = rand.nextBoolean() ? "Minor" : "Major";
            tasks[i].setPhase(phase);
            tasks[i].setSeverity(severity);
        }

        // --- Compute delay & energy w.r.t all fog nodes ---
        for (Task task : tasks) {
            OffloadingCalculator.computeAndStore(task, fogNetworks);
        }

        // --- Normalize globally and sort preferences ---
        Normalizer.normalize(tasks, fogNetworks.length);

        // --- Compute task urgency w.r.t all fog nodes ---
        for (Task task : tasks) {
            task.computeUrgencies(w1, w2, w3, w4);
        }

        // --- Compute preferred task ranking list for each fog node (descending urgency) ---
        int[][] preferredTasksPerFog = new int[fogNetworks.length][NUM_TASKS];
        for (int f = 0; f < fogNetworks.length; f++) {
            final int fogIndex = f;
            Integer[] indices = new Integer[NUM_TASKS];
            for (int i = 0; i < NUM_TASKS; i++) {
                indices[i] = i;
            }
            
            // Sort tasks in descending order of urgency (highest urgency first)
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex), tasks[a].getUrgency(fogIndex));
                }
            });
            
            for (int i = 0; i < NUM_TASKS; i++) {
                preferredTasksPerFog[f][i] = indices[i];
            }
        }

        // --- Create Precedence Lists: Major and Minor cases separately, sorted by ascending order of deadline ---
        int majorCount = 0;
        int minorCount = 0;
        for (Task t : tasks) {
            if (t.getSeverity() != null && t.getSeverity().equalsIgnoreCase("Major")) {
                majorCount++;
            } else {
                minorCount++;
            }
        }

        Task[] precedenceListMajor = new Task[majorCount];
        Task[] precedenceListMinor = new Task[minorCount];
        int majorIdx = 0;
        int minorIdx = 0;
        for (Task t : tasks) {
            if (t.getSeverity() != null && t.getSeverity().equalsIgnoreCase("Major")) {
                precedenceListMajor[majorIdx++] = t;
            } else {
                precedenceListMinor[minorIdx++] = t;
            }
        }

        // Sort both arrays in ascending order of deadline
        java.util.Arrays.sort(precedenceListMajor, new java.util.Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });

        java.util.Arrays.sort(precedenceListMinor, new java.util.Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });

        // --- Compute Quotas for All Tasks and Major Tasks sets ---
        QuotaDeterminator.computeMinimumQuotas(fogNetworks, NUM_TASKS, true);
        QuotaDeterminator.computeMinimumQuotas(fogNetworks, majorCount, false);

        // --- Print Quota Information ---
        System.out.println("==========================================================================================");
        System.out.println("  FOG NODE QUOTAS (Computed via M-DAFTO Algorithm 1)");
        System.out.println("==========================================================================================");
        System.out.printf("  %-15s | %-12s | %-12s | %-12s | %-12s%n",
            "Fog Node", "Min (All)", "Max (All)", "Min (Major)", "Max (Major)");
        System.out.println("  ----------------------------------------------------------------------------------------");
        for (FogNetwork fn : fogNetworks) {
            String maxMajorStr = (fn.getMaxQuotaMajorTasks() == null) ? "empty" : String.valueOf(fn.getMaxQuotaMajorTasks());
            System.out.printf("  %-15s | %-12d | %-12d | %-12d | %-12s%n",
                fn.getName(), fn.getMinQuotaAllTasks(), fn.getMaxQuotaAllTasks(),
                fn.getMinQuotaMajorTasks(), maxMajorStr);
        }
        System.out.println("==========================================================================================\n");

        // --- Compute preferred task ranking list for each fog node restricted to Major tasks ---
        int[][] preferredMajorTasksPerFog = new int[fogNetworks.length][majorCount];
        for (int f = 0; f < fogNetworks.length; f++) {
            final int fogIndex = f;
            Integer[] indices = new Integer[majorCount];
            int idx = 0;
            for (int i = 0; i < NUM_TASKS; i++) {
                if (tasks[i].getSeverity() != null && tasks[i].getSeverity().equalsIgnoreCase("Major")) {
                    indices[idx++] = i;
                }
            }
            
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex), tasks[a].getUrgency(fogIndex));
                }
            });
            
            for (int i = 0; i < majorCount; i++) {
                preferredMajorTasksPerFog[f][i] = indices[i];
            }
        }

        // --- Compute preferred task ranking list for each fog node restricted to Minor tasks ---
        int[][] preferredMinorTasksPerFog = new int[fogNetworks.length][minorCount];
        for (int f = 0; f < fogNetworks.length; f++) {
            final int fogIndex = f;
            Integer[] indices = new Integer[minorCount];
            int idx = 0;
            for (int i = 0; i < NUM_TASKS; i++) {
                if (tasks[i].getSeverity() == null || !tasks[i].getSeverity().equalsIgnoreCase("Major")) {
                    indices[idx++] = i;
                }
            }
            
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex), tasks[a].getUrgency(fogIndex));
                }
            });
            
            for (int i = 0; i < minorCount; i++) {
                preferredMinorTasksPerFog[f][i] = indices[i];
            }
        }

        // --- Extract normalized and ranked arrays for SimulationData container ---
        double[][] normDelayArray = new double[NUM_TASKS][fogNetworks.length];
        double[][] normEnergyArray = new double[NUM_TASKS][fogNetworks.length];
        double[][] normSumArray = new double[NUM_TASKS][fogNetworks.length];
        int[][] preferredFogIndices = new int[NUM_TASKS][fogNetworks.length];

        for (int i = 0; i < NUM_TASKS; i++) {
            normDelayArray[i]      = tasks[i].getNormalizedDelays();
            normEnergyArray[i]     = tasks[i].getNormalizedEnergies();
            normSumArray[i]        = tasks[i].getNormSums();
            preferredFogIndices[i] = tasks[i].getPreferredFogIndices();
        }

        SimulationData simData = new SimulationData(
            tasks, fogNetworks, normDelayArray, normEnergyArray, normSumArray, preferredFogIndices,
            preferredTasksPerFog, preferredMajorTasksPerFog, preferredMinorTasksPerFog, weights,
            precedenceListMajor, precedenceListMinor);

        // --- Sample task detailed view (first 3 tasks) ---
        System.out.println("==========================================================================================");
        System.out.println("  SAMPLE TASK ANALYSIS (First 3 Tasks w.r.t All Fog Nodes)");
        System.out.println("==========================================================================================");
        for (int i = 0; i < Math.min(3, NUM_TASKS); i++) {
            Task t = tasks[i];
            System.out.printf("Task T%d (CPU: %.1f Mcycles, Input: %.1f KB, Output: %.1f KB, Phase: %s, Severity: %s, Deadline: %.1fs, pref(t): %d)%n",
                t.getTaskId(), t.getCpuDemanded(), t.getInputSize(), t.getOutputSize(), t.getPhase(), t.getSeverity(), t.getDeadline(), t.getPrefCount());
            for (int f = 0; f < fogNetworks.length; f++) {
                System.out.printf("  - %-12s: Delay = %6.3fs | Energy = %6.3fJ | NormSum = %.4f | Urgency = %.4f%n",
                    fogNetworks[f].getName(),
                    t.getOffloadingDelay(f),
                    t.getEnergy(f),
                    t.getNormSum(f),
                    t.getUrgency(f)
                );
            }
            
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

        // --- Preferred task priority lists per fog node (Top 15 tasks) ---
        System.out.println("==========================================================================================");
        System.out.println("  PREFERRED TASK PRIORITIZATION PER FOG NODE (Top 15 Most Urgent Tasks Ranked)");
        System.out.println("==========================================================================================");
        for (int f = 0; f < fogNetworks.length; f++) {
            System.out.printf("%s Priority Order:%n  ", fogNetworks[f].getName());
            int[] priorityList = simData.getPreferredTasks(f);
            for (int r = 0; r < Math.min(15, NUM_TASKS); r++) {
                int taskIdx = priorityList[r];
                Task t = tasks[taskIdx];
                System.out.printf("[Rank %2d: T%d (Urg: %5.2f)]", r + 1, t.getTaskId(), t.getUrgency(f));
                if (r < Math.min(15, NUM_TASKS) - 1) {
                    System.out.print(" -> ");
                }
            }
            System.out.println("\n");
        }

        // --- Print Precedence Lists Diagnostics ---
        System.out.println("==========================================================================================");
        System.out.println("  PRECEDENCE LISTS (Major and Minor Tasks sorted by Ascending Deadline)");
        System.out.println("==========================================================================================");
        System.out.printf("  Precedence List Major (Total: %d tasks):%n  ", precedenceListMajor.length);
        for (int r = 0; r < Math.min(15, precedenceListMajor.length); r++) {
            Task t = precedenceListMajor[r];
            System.out.printf("[Rank %2d: T%d (DL: %5.2fs)]", r + 1, t.getTaskId(), t.getDeadline());
            if (r < Math.min(15, precedenceListMajor.length) - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println("\n");
        
        System.out.printf("  Precedence List Minor (Total: %d tasks):%n  ", precedenceListMinor.length);
        for (int r = 0; r < Math.min(15, precedenceListMinor.length); r++) {
            Task t = precedenceListMinor[r];
            System.out.printf("[Rank %2d: T%d (DL: %5.2fs)]", r + 1, t.getTaskId(), t.getDeadline());
            if (r < Math.min(15, precedenceListMinor.length) - 1) {
                System.out.print(" -> ");
            }
        }
        System.out.println("\n");

        System.out.println("==========================================================================================");
        System.out.println("  [Phase 1 Complete] SimulationData ready for M-MOORA");
        System.out.println("  Tasks count             → " + simData.getTasks().length);
        System.out.println("  normDelayArray shape    → " + simData.getNormDelayArray().length + " x " + simData.getNormDelayArray()[0].length);
        System.out.println("  normEnergyArray shape   → " + simData.getNormEnergyArray().length + " x " + simData.getNormEnergyArray()[0].length);
        System.out.println("  normSumArray shape      → " + simData.getNormSumArray().length + " x " + simData.getNormSumArray()[0].length);
        System.out.println("  preferredFogIndices     → " + simData.getPreferredFogIndices().length + " x " + simData.getPreferredFogIndices()[0].length);
        System.out.println("  preferredTasksPerFog    → " + simData.getPreferredTasksPerFog().length + " x " + simData.getPreferredTasksPerFog()[0].length);
        System.out.println("  preferredMajorTasksPerFog → " + simData.getPreferredMajorTasksPerFog().length + " x " + simData.getPreferredMajorTasksPerFog()[0].length);
        System.out.println("  preferredMinorTasksPerFog → " + simData.getPreferredMinorTasksPerFog().length + " x " + simData.getPreferredMinorTasksPerFog()[0].length);
        System.out.println("  precedenceListMajor     → " + simData.getPrecedenceListMajor().length + " tasks");
        System.out.println("  precedenceListMinor     → " + simData.getPrecedenceListMinor().length + " tasks");
        System.out.println("==========================================================================================");
    }

    private static double randomInRange(Random rand, double min, double max) {
        return min + (max - min) * rand.nextDouble();
    }

    private static int randomIntInRange(Random rand, int min, int max) {
        return rand.nextInt(max - min + 1) + min;
    }
}