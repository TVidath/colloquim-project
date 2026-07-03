import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 *  MSDAlgorithm
 * ============================================================================
 *
 *  Implements the 2-Type Multi-Stage Deferred Acceptance (MSDA) algorithm
 *  for task offloading in IoT-Fog systems (Oomori & Manabe).
 *
 * ============================================================================
 */
public class MSDAlgorithm {

    public static class MatchingResult {
        private final int[] taskAssignment;         
        private final List<List<Integer>> fogAssignments; 
        private final int unmatchedCount;
        private final int matchedCount;

        public MatchingResult(int[] taskAssignment, List<List<Integer>> fogAssignments,
                              int unmatchedCount, int matchedCount) {
            this.taskAssignment = taskAssignment;
            this.fogAssignments = fogAssignments;
            this.unmatchedCount = unmatchedCount;
            this.matchedCount   = matchedCount;
        }

        public int[] getTaskAssignment() { return taskAssignment; }
        public List<List<Integer>> getFogAssignments() { return fogAssignments; }
        public int getUnmatchedCount() { return unmatchedCount; }
        public int getMatchedCount() { return matchedCount; }
    }

    private SimulationData simData;
    private int numFogs;

    private MSDAlgorithm(SimulationData simData) {
        this.simData = simData;
        this.numFogs = simData.getFogNetworks().length;
    }

    public static MatchingResult match(SimulationData simData) {
        MSDAlgorithm algo = new MSDAlgorithm(simData);
        return algo.execute();
    }

    public static MatchingResult matchBaseline(SimulationData simData) {
        MSDAlgorithm algo = new MSDAlgorithm(simData);
        return algo.executeBaseline();
    }

    private MatchingResult execute() {
        Task[] tasks = simData.getTasks();
        
        // Build a unified Precedence List sorted by deadline 
        Task[] unifiedPL = Arrays.copyOf(tasks, tasks.length);
        Arrays.sort(unifiedPL, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });
        
        List<Task> initialTasks = new ArrayList<>(Arrays.asList(unifiedPL));
        
        int[] minAll = new int[numFogs];
        int[] maxAll = new int[numFogs];
        int[] minMajor = new int[numFogs];
        int[] maxMajor = new int[numFogs];
        
        for (int c = 0; c < numFogs; c++) {
            FogNetwork fn = simData.getFogNetworks()[c];
            minAll[c] = fn.getMinQuotaAllTasks();
            maxAll[c] = fn.getMaxQuotaAllTasks();
            minMajor[c] = fn.getMinQuotaMajorTasks();
            Integer mxMj = fn.getMaxQuotaMajorTasks();
            maxMajor[c] = (mxMj != null) ? mxMj : fn.getNumberOfVRUs();
        }
        
        Map<Task, Integer> finalMatches = doTwoTypeMSDA(initialTasks, minAll, maxAll, minMajor, maxMajor);
        
        // Build MatchingResult
        int[] taskAssignment = new int[tasks.length];
        Arrays.fill(taskAssignment, -1);
        List<List<Integer>> fogAssignments = new ArrayList<>();
        for (int i = 0; i < numFogs; i++) fogAssignments.add(new ArrayList<>());
        
        int matchedCount = 0;
        for(Map.Entry<Task, Integer> entry : finalMatches.entrySet()) {
            int tIdx = entry.getKey().getTaskId() - 1;
            int fIdx = entry.getValue();
            taskAssignment[tIdx] = fIdx;
            fogAssignments.get(fIdx).add(tIdx);
            matchedCount++;
        }
        
        int unmatchedCount = tasks.length - matchedCount;
        return new MatchingResult(taskAssignment, fogAssignments, unmatchedCount, matchedCount);
    }

    private MatchingResult executeBaseline() {
        Task[] tasks = simData.getTasks();
        
        // Build a unified Precedence List sorted by deadline 
        Task[] unifiedPL = Arrays.copyOf(tasks, tasks.length);
        Arrays.sort(unifiedPL, new Comparator<Task>() {
            @Override
            public int compare(Task a, Task b) {
                return Double.compare(a.getDeadline(), b.getDeadline());
            }
        });
        
        List<Task> initialTasks = new ArrayList<>(Arrays.asList(unifiedPL));
        
        int[] minAll = new int[numFogs];
        int[] maxAll = new int[numFogs];
        
        for (int c = 0; c < numFogs; c++) {
            FogNetwork fn = simData.getFogNetworks()[c];
            minAll[c] = fn.getMinQuotaAllTasks();
            maxAll[c] = fn.getMaxQuotaAllTasks();
        }
        
        Map<Task, Integer> finalMatches = runSingleTypeMSDABaseline(initialTasks, minAll, maxAll);
        
        // Build MatchingResult
        int[] taskAssignment = new int[tasks.length];
        Arrays.fill(taskAssignment, -1);
        List<List<Integer>> fogAssignments = new ArrayList<>();
        for (int i = 0; i < numFogs; i++) fogAssignments.add(new ArrayList<>());
        
        int matchedCount = 0;
        for(Map.Entry<Task, Integer> entry : finalMatches.entrySet()) {
            int tIdx = entry.getKey().getTaskId() - 1;
            int fIdx = entry.getValue();
            taskAssignment[tIdx] = fIdx;
            fogAssignments.get(fIdx).add(tIdx);
            matchedCount++;
        }
        
        int unmatchedCount = tasks.length - matchedCount;
        return new MatchingResult(taskAssignment, fogAssignments, unmatchedCount, matchedCount);
    }

    private Map<Task, Integer> doTwoTypeMSDA(List<Task> T, int[] p_init, int[] q_init, int[] pr_init, int[] qr_init) {
        int[] p = Arrays.copyOf(p_init, p_init.length);
        int[] q = Arrays.copyOf(q_init, q_init.length);
        int[] pr = Arrays.copyOf(pr_init, pr_init.length);
        int[] qr = Arrays.copyOf(qr_init, qr_init.length);
        
        List<Task> V_prev = new ArrayList<>(T);
        Map<Task, Integer> finalMu = new HashMap<>();
        
        while(true) {
            int vr = 0, vs = 0, vt = 0;
            for(int c = 0; c < numFogs; c++) {
                vs += Math.max(0, p[c] - qr[c]);
                vr += pr[c];
                vt += p[c];
            }
            
            List<Task> V_new = new ArrayList<>();
            int countR = 0, countS = 0, countT = 0;
            // V^k is the minimum set from the bottom of the PL
            for(int i = V_prev.size() - 1; i >= 0; i--) {
                if(countT >= vt && countS >= vs && countR >= vr) {
                    break;
                }
                Task t = V_prev.get(i);
                V_new.add(0, t); // add to front to maintain order
                countT++;
                if(t.getSeverity().equalsIgnoreCase("Major")) countR++;
                else countS++;
            }
            
            List<Task> da_tasks = new ArrayList<>(V_prev);
            da_tasks.removeAll(V_new); // V^{k-1} \ V^k
            
            if(!da_tasks.isEmpty()) {
                Map<Task, Integer> mu = runModifiedDA(da_tasks, p, q, pr, qr);
                finalMu.putAll(mu);
                
                for(int c = 0; c < numFogs; c++) {
                    int matchedR = 0, matchedS = 0;
                    for(Map.Entry<Task, Integer> entry : mu.entrySet()) {
                        if(entry.getValue() == c) {
                            if(entry.getKey().getSeverity().equalsIgnoreCase("Major")) matchedR++;
                            else matchedS++;
                        }
                    }
                    int matchedT = matchedR + matchedS;
                    
                    int q_next = q[c] - matchedT;
                    int qr_next = Math.min(qr[c] - matchedR, q_next);
                    int pr_next = Math.max(0, pr[c] - matchedR);
                    int p_next = Math.max(0, p[c] - matchedS - Math.max(pr[c], matchedR)) + pr_next;
                    
                    q[c] = q_next;
                    qr[c] = qr_next;
                    pr[c] = pr_next;
                    p[c] = p_next;
                }
                V_prev = V_new;
            } else {
                // Final stage logic (Lines 13 - 25)
                boolean[] C_prime = new boolean[numFogs];
                for(int c = 0; c < numFogs; c++) if(p[c] > 0) C_prime[c] = true;
                
                List<Task> Vk_R = new ArrayList<>();
                List<Task> Vk_S = new ArrayList<>();
                for(Task t : V_new) {
                    if(t.getSeverity().equalsIgnoreCase("Major")) Vk_R.add(t);
                    else Vk_S.add(t);
                }
                
                if(Vk_R.size() == vr) {
                    int[] q_temp = new int[numFogs];
                    for(int c = 0; c < numFogs; c++) {
                        if(C_prime[c]) q_temp[c] = pr[c]; // FIX: The paper typo said qr_c^k, but to force min quotas it MUST be pr_c^k, just like line 19 does for S.
                        else q_temp[c] = 0;
                    }
                    finalMu.putAll(runStandardDA(Vk_R, q_temp));
                    
                    int[] p_temp = new int[numFogs];
                    int[] q_temp2 = new int[numFogs];
                    for(int c = 0; c < numFogs; c++) {
                        if(C_prime[c]) {
                            p_temp[c] = p[c] - pr[c];
                            q_temp2[c] = q[c] - pr[c];
                        } else {
                            p_temp[c] = 0;
                            q_temp2[c] = 0;
                        }
                    }
                    finalMu.putAll(runSingleTypeMSDA(Vk_S, p_temp, q_temp2));
                    break;
                    
                } else if(Vk_S.size() == vs) {
                    int[] q_temp = new int[numFogs];
                    for(int c = 0; c < numFogs; c++) {
                        if(C_prime[c]) q_temp[c] = Math.max(p[c] - qr[c], 0);
                        else q_temp[c] = 0;
                    }
                    finalMu.putAll(runStandardDA(Vk_S, q_temp));
                    
                    int[] pr_temp = new int[numFogs];
                    int[] qr_temp = new int[numFogs];
                    for(int c = 0; c < numFogs; c++) {
                        if(C_prime[c]) {
                            pr_temp[c] = pr[c];
                            qr_temp[c] = qr[c];
                        } else {
                            pr_temp[c] = 0;
                            qr_temp[c] = 0;
                        }
                    }
                    finalMu.putAll(runSingleTypeMSDA(Vk_R, pr_temp, qr_temp));
                    break;
                    
                } else {
                    int[] p_temp = new int[numFogs];
                    int[] q_temp = new int[numFogs];
                    int[] pr_temp = new int[numFogs];
                    int[] qr_temp = new int[numFogs];
                    for(int c = 0; c < numFogs; c++) {
                        if(C_prime[c]) {
                            pr_temp[c] = pr[c];
                            p_temp[c] = pr[c];
                            qr_temp[c] = Math.min(qr[c], p[c]);
                            q_temp[c] = p[c];
                        } else {
                            p_temp[c] = 0;
                            q_temp[c] = 0;
                            pr_temp[c] = 0;
                            qr_temp[c] = 0;
                        }
                    }
                    finalMu.putAll(doTwoTypeMSDA(V_new, p_temp, q_temp, pr_temp, qr_temp));
                    break;
                }
            }
        }
        return finalMu;
    }

    private Map<Task, Integer> runSingleTypeMSDABaseline(List<Task> T_PL, int[] p_init, int[] q_init) {
        int[] curr_l = Arrays.copyOf(p_init, p_init.length);
        int[] curr_h = Arrays.copyOf(q_init, q_init.length);
        
        List<Task> PL = new ArrayList<>(T_PL);
        List<Task> R_prev = new ArrayList<>(T_PL);
        
        Map<Task, Integer> finalM = new HashMap<>();
        
        while(!PL.isEmpty()) {
            int r_k = 0;
            for(int c = 0; c < numFogs; c++) r_k += curr_l[c];
            
            List<Task> R_k = new ArrayList<>();
            int startIndex = Math.max(0, PL.size() - r_k);
            for(int i = startIndex; i < PL.size(); i++) {
                R_k.add(PL.get(i));
            }
            
            List<Task> diff = new ArrayList<>(R_prev);
            diff.removeAll(R_k);
            
            Map<Task, Integer> Mk;
            
            if(!diff.isEmpty()) {
                Mk = runStandardDABaseline(diff, curr_h, true);
                finalM.putAll(Mk);
                PL.removeAll(diff);
                R_prev = R_k;
            } else {
                Mk = runStandardDABaseline(R_k, curr_l, false);
                finalM.putAll(Mk);
                PL.removeAll(R_k);
            }
            
            for(int c = 0; c < numFogs; c++) {
                int matched = 0;
                for(Map.Entry<Task, Integer> entry : Mk.entrySet()) {
                    if(entry.getValue() == c) matched++;
                }
                curr_h[c] = curr_h[c] - matched;
                curr_l[c] = Math.max(0, curr_l[c] - matched);
            }
        }
        return finalM;
    }

    private Map<Task, Integer> runSingleTypeMSDA(List<Task> T, int[] p_init, int[] q_init) {
        int[] curr_p = Arrays.copyOf(p_init, p_init.length);
        int[] curr_q = Arrays.copyOf(q_init, q_init.length);
        List<Task> V_prev = new ArrayList<>(T);
        Map<Task, Integer> finalMu = new HashMap<>();
        
        while(true) {
            int vt = 0;
            for(int c = 0; c < numFogs; c++) vt += curr_p[c];
            
            List<Task> V_new = new ArrayList<>();
            int countT = 0;
            for(int i = V_prev.size() - 1; i >= 0; i--) {
                if(countT >= vt) break;
                V_new.add(0, V_prev.get(i));
                countT++;
            }
            
            List<Task> da_tasks = new ArrayList<>(V_prev);
            da_tasks.removeAll(V_new);
            
            if(!da_tasks.isEmpty()) {
                Map<Task, Integer> mu = runStandardDA(da_tasks, curr_q);
                
                for(int c = 0; c < numFogs; c++) {
                    int matchedT = 0;
                    for(Map.Entry<Task, Integer> entry : mu.entrySet()) {
                        if(entry.getValue() == c) matchedT++;
                    }
                    curr_q[c] -= matchedT;
                    curr_p[c] = Math.max(0, curr_p[c] - matchedT);
                }
                finalMu.putAll(mu);
                V_prev = V_new;
            } else {
                boolean[] C_prime = new boolean[numFogs];
                for(int c = 0; c < numFogs; c++) if(curr_p[c] > 0) C_prime[c] = true;
                
                int[] q_temp = new int[numFogs];
                for(int c = 0; c < numFogs; c++) {
                    if(C_prime[c]) q_temp[c] = curr_p[c]; 
                    else q_temp[c] = 0;
                }
                Map<Task, Integer> mu = runStandardDA(V_new, q_temp);
                finalMu.putAll(mu);
                break;
            }
        }
        return finalMu;
    }

    private Map<Task, Integer> runModifiedDA(List<Task> candidateTasks, int[] p, int[] q, int[] pr, int[] qr) {
        int totalTasks = simData.getTasks().length;
        int[] nextFog = new int[totalTasks];
        boolean[] isMatched = new boolean[totalTasks];
        
        List<List<Task>> applicants = new ArrayList<>();
        for(int c = 0; c < numFogs; c++) applicants.add(new ArrayList<>());
        
        boolean active = true;
        while(active) {
            active = false;
            for(Task t : candidateTasks) {
                int tIdx = t.getTaskId() - 1;
                if(isMatched[tIdx]) continue;
                
                int[] prefs = t.getPreferredFogIndices();
                while(nextFog[tIdx] < prefs.length) {
                    int fIdx = prefs[nextFog[tIdx]];
                    nextFog[tIdx]++;
                    
                    // To strictly satisfy minimum quotas, we do not restrict proposals by deadline
                    applicants.get(fIdx).add(t);
                    isMatched[tIdx] = true;
                    active = true;
                    break;
                }
            }
            
            for(int c = 0; c < numFogs; c++) {
                List<Task> Rc = new ArrayList<>();
                List<Task> Sc = new ArrayList<>();
                for(Task t : applicants.get(c)) {
                    if(t.getSeverity().equalsIgnoreCase("Major")) Rc.add(t);
                    else Sc.add(t);
                }
                
                if(Rc.size() > qr[c]) {
                    sortByUrgency(Rc, c);
                    while(Rc.size() > qr[c]) {
                        Task rejected = Rc.remove(Rc.size() - 1);
                        applicants.get(c).remove(rejected);
                        isMatched[rejected.getTaskId() - 1] = false;
                        active = true;
                    }
                }
                else if(Sc.size() > q[c] - pr[c]) {
                    sortByUrgency(Sc, c);
                    while(Sc.size() > q[c] - pr[c]) {
                        Task rejected = Sc.remove(Sc.size() - 1);
                        applicants.get(c).remove(rejected);
                        isMatched[rejected.getTaskId() - 1] = false;
                        active = true;
                    }
                }
                else if(applicants.get(c).size() > q[c]) {
                    List<Task> Tc = applicants.get(c);
                    sortByUrgency(Tc, c);
                    while(Tc.size() > q[c]) {
                        Task rejected = Tc.remove(Tc.size() - 1);
                        isMatched[rejected.getTaskId() - 1] = false;
                        active = true;
                    }
                }
            }
        }
        
        Map<Task, Integer> mu = new HashMap<>();
        for(int c = 0; c < numFogs; c++) {
            for(Task t : applicants.get(c)) mu.put(t, c);
        }
        return mu;
    }

    private Map<Task, Integer> runStandardDABaseline(List<Task> candidateTasks, int[] q, boolean status) {
        int totalTasks = simData.getTasks().length;
        int[] nextFog = new int[totalTasks];
        boolean[] isMatched = new boolean[totalTasks];
        
        List<List<Task>> applicants = new ArrayList<>();
        for(int c = 0; c < numFogs; c++) applicants.add(new ArrayList<>());
        
        boolean active = true;
        while(active) {
            active = false;
            for(Task t : candidateTasks) {
                int tIdx = t.getTaskId() - 1;
                if(isMatched[tIdx]) continue;
                
                int[] prefs = t.getPreferredFogIndices();
                while(nextFog[tIdx] < prefs.length) {
                    int fIdx = prefs[nextFog[tIdx]];
                    nextFog[tIdx]++;
                    
                    if (status) {
                        if (t.getOffloadingDelay(fIdx) > t.getDeadline()) {
                            continue;
                        }
                    }
                    
                    applicants.get(fIdx).add(t);
                    isMatched[tIdx] = true;
                    active = true;
                    break;
                }
            }
            
            for(int c = 0; c < numFogs; c++) {
                if(applicants.get(c).size() > q[c]) {
                    List<Task> Tc = applicants.get(c);
                    sortByUrgency(Tc, c);
                    while(Tc.size() > q[c]) {
                        Task rejected = Tc.remove(Tc.size() - 1);
                        isMatched[rejected.getTaskId() - 1] = false;
                        active = true;
                    }
                }
            }
        }
        
        Map<Task, Integer> mu = new HashMap<>();
        for(int c = 0; c < numFogs; c++) {
            for(Task t : applicants.get(c)) mu.put(t, c);
        }
        return mu;
    }

    private Map<Task, Integer> runStandardDA(List<Task> candidateTasks, int[] q) {
        int totalTasks = simData.getTasks().length;
        int[] nextFog = new int[totalTasks];
        boolean[] isMatched = new boolean[totalTasks];
        
        List<List<Task>> applicants = new ArrayList<>();
        for(int c = 0; c < numFogs; c++) applicants.add(new ArrayList<>());
        
        boolean active = true;
        while(active) {
            active = false;
            for(Task t : candidateTasks) {
                int tIdx = t.getTaskId() - 1;
                if(isMatched[tIdx]) continue;
                
                int[] prefs = t.getPreferredFogIndices();
                while(nextFog[tIdx] < prefs.length) {
                    int fIdx = prefs[nextFog[tIdx]];
                    nextFog[tIdx]++;
                    
                    // To strictly satisfy minimum quotas, we do not restrict proposals by deadline
                    applicants.get(fIdx).add(t);
                    isMatched[tIdx] = true;
                    active = true;
                    break;
                }
            }
            
            for(int c = 0; c < numFogs; c++) {
                if(applicants.get(c).size() > q[c]) {
                    List<Task> Tc = applicants.get(c);
                    sortByUrgency(Tc, c);
                    while(Tc.size() > q[c]) {
                        Task rejected = Tc.remove(Tc.size() - 1);
                        isMatched[rejected.getTaskId() - 1] = false;
                        active = true;
                    }
                }
            }
        }
        
        Map<Task, Integer> mu = new HashMap<>();
        for(int c = 0; c < numFogs; c++) {
            for(Task t : applicants.get(c)) mu.put(t, c);
        }
        return mu;
    }

    private void sortByUrgency(List<Task> list, int fogIdx) {
        list.sort(new Comparator<Task>() {
            @Override
            public int compare(Task t1, Task t2) {
                return Double.compare(t2.getUrgency(fogIdx), t1.getUrgency(fogIdx));
            }
        });
    }
}
