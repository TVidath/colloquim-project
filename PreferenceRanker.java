/**
 * ============================================================================
 *  PreferenceRanker
 * ============================================================================
 *
 *  Handles ALL preference ranking and precedence list operations:
 *
 *    1. computePreferredFogOrder()    — Rank fog nodes for each task
 *                                       (least normSum = best)
 *
 *    2. rankAllTasksPerFog()          — Rank ALL tasks per fog node
 *                                       (descending urgency)
 *
 *    3. rankMajorTasksPerFog()        — Rank only MAJOR tasks per fog node
 *                                       (descending urgency)
 *
 *    4. rankMinorTasksPerFog()        — Rank only MINOR tasks per fog node
 *                                       (descending urgency)
 *
 *    5. buildMajorPrecedenceList()    — Major tasks sorted by ascending deadline
 *
 *    6. buildMinorPrecedenceList()    — Minor tasks sorted by ascending deadline
 *
 *    7. countMajorTasks()             — Count Major tasks in array
 *    8. countMinorTasks()             — Count Minor tasks in array
 * ============================================================================
 */
public class PreferenceRanker {

    // ================================================================
    //  1. FOG NODE PREFERENCE ORDER (per task)
    // ================================================================

    /**
     * Sorts fog node indices for a task based on normSum (ascending).
     * The fog node with the lowest normSum (least delay + energy) is ranked first.
     * Result is stored in the task's preferredFogIndices field.
     *
     * @param task         The task to compute fog preferences for
     * @param numFogNodes  Number of fog nodes
     */
    public static void computePreferredFogOrder(Task task, int numFogNodes) {
        Integer[] indices = new Integer[numFogNodes];
        for (int i = 0; i < numFogNodes; i++) {
            indices[i] = i;
        }

        // Sort by ascending normSum (best = lowest sum)
        final Task t = task;
        java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
            @Override
            public int compare(Integer a, Integer b) {
                return Double.compare(t.getNormSum(a), t.getNormSum(b));
            }
        });

        int[] result = new int[numFogNodes];
        for (int i = 0; i < numFogNodes; i++) {
            result[i] = indices[i];
        }
        task.setPreferredFogIndices(result);
    }

    // ================================================================
    //  2. TASK RANKING PER FOG NODE — All Tasks
    // ================================================================

    /**
     * Ranks ALL tasks per fog node in descending order of urgency.
     * The most urgent task is ranked first.
     *
     * @param tasks        Array of all tasks
     * @param numFogNodes  Number of fog nodes
     * @return             2D array [fogIndex][rank] → task index
     */
    public static int[][] rankAllTasksPerFog(final Task[] tasks, int numFogNodes) {
        int numTasks = tasks.length;
        int[][] result = new int[numFogNodes][numTasks];

        for (int f = 0; f < numFogNodes; f++) {
            final int fogIndex = f;
            Integer[] indices = new Integer[numTasks];
            for (int i = 0; i < numTasks; i++) {
                indices[i] = i;
            }

            // Sort by descending urgency (highest urgency first)
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex),
                                          tasks[a].getUrgency(fogIndex));
                }
            });

            for (int i = 0; i < numTasks; i++) {
                result[f][i] = indices[i];
            }
        }

        return result;
    }

    // ================================================================
    //  3. TASK RANKING PER FOG NODE — Major Tasks Only
    // ================================================================

    /**
     * Ranks only MAJOR tasks per fog node in descending order of urgency.
     *
     * @param tasks        Array of all tasks
     * @param numFogNodes  Number of fog nodes
     * @return             2D array [fogIndex][rank] → task index (into original array)
     */
    public static int[][] rankMajorTasksPerFog(final Task[] tasks, int numFogNodes) {
        int majorCount = countMajorTasks(tasks);
        int[][] result = new int[numFogNodes][majorCount];

        for (int f = 0; f < numFogNodes; f++) {
            final int fogIndex = f;

            // Collect indices of Major tasks
            Integer[] indices = new Integer[majorCount];
            int idx = 0;
            for (int i = 0; i < tasks.length; i++) {
                if (isMajor(tasks[i])) {
                    indices[idx++] = i;
                }
            }

            // Sort by descending urgency
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex),
                                          tasks[a].getUrgency(fogIndex));
                }
            });

            for (int i = 0; i < majorCount; i++) {
                result[f][i] = indices[i];
            }
        }

        return result;
    }

    // ================================================================
    //  4. TASK RANKING PER FOG NODE — Minor Tasks Only
    // ================================================================

    /**
     * Ranks only MINOR tasks per fog node in descending order of urgency.
     *
     * @param tasks        Array of all tasks
     * @param numFogNodes  Number of fog nodes
     * @return             2D array [fogIndex][rank] → task index (into original array)
     */
    public static int[][] rankMinorTasksPerFog(final Task[] tasks, int numFogNodes) {
        int minorCount = countMinorTasks(tasks);
        int[][] result = new int[numFogNodes][minorCount];

        for (int f = 0; f < numFogNodes; f++) {
            final int fogIndex = f;

            // Collect indices of Minor tasks
            Integer[] indices = new Integer[minorCount];
            int idx = 0;
            for (int i = 0; i < tasks.length; i++) {
                if (!isMajor(tasks[i])) {
                    indices[idx++] = i;
                }
            }

            // Sort by descending urgency
            java.util.Arrays.sort(indices, new java.util.Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return Double.compare(tasks[b].getUrgency(fogIndex),
                                          tasks[a].getUrgency(fogIndex));
                }
            });

            for (int i = 0; i < minorCount; i++) {
                result[f][i] = indices[i];
            }
        }

        return result;
    }

    // ================================================================
    //  5. PRECEDENCE LIST — Major Tasks (sorted by ascending deadline)
    // ================================================================

    /**
     * Builds a precedence list of Major tasks sorted by ascending deadline.
     * Tasks with the earliest deadline are ranked first.
     *
     * @param tasks  Array of all tasks
     * @return       Array of Major tasks sorted by deadline
     */
    public static Task[] buildMajorPrecedenceList(Task[] tasks) {
        int count = countMajorTasks(tasks);
        Task[] list = new Task[count];

        int idx = 0;
        for (Task t : tasks) {
            if (isMajor(t)) {
                list[idx++] = t;
            }
        }

        // Sort by ascending deadline
        java.util.Arrays.sort(list, new java.util.Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });

        return list;
    }

    // ================================================================
    //  6. PRECEDENCE LIST — Minor Tasks (sorted by ascending deadline)
    // ================================================================

    /**
     * Builds a precedence list of Minor tasks sorted by ascending deadline.
     * Tasks with the earliest deadline are ranked first.
     *
     * @param tasks  Array of all tasks
     * @return       Array of Minor tasks sorted by deadline
     */
    public static Task[] buildMinorPrecedenceList(Task[] tasks) {
        int count = countMinorTasks(tasks);
        Task[] list = new Task[count];

        int idx = 0;
        for (Task t : tasks) {
            if (!isMajor(t)) {
                list[idx++] = t;
            }
        }

        // Sort by ascending deadline
        java.util.Arrays.sort(list, new java.util.Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });

        return list;
    }

    // ================================================================
    //  7 & 8. UTILITY — Task Counting
    // ================================================================

    /** Counts the number of Major tasks in the array. */
    public static int countMajorTasks(Task[] tasks) {
        int count = 0;
        for (Task t : tasks) {
            if (isMajor(t)) count++;
        }
        return count;
    }

    /** Counts the number of Minor tasks in the array. */
    public static int countMinorTasks(Task[] tasks) {
        int count = 0;
        for (Task t : tasks) {
            if (!isMajor(t)) count++;
        }
        return count;
    }

    // ================================================================
    //  HELPER
    // ================================================================

    /**
     * Checks if a task has "Major" severity.
     */
    private static boolean isMajor(Task t) {
        String sev = t.getSeverity();
        return sev != null && sev.equalsIgnoreCase("Major");
    }
}
