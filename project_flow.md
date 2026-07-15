================================================================================
         M-DAFTO / 2-TYPE MSDA SIMULATION — COMPLETE PROJECT FLOW
================================================================================
  Orchestrated by: Main.java (Multi-Scenario Framework)
  Compile: javac *.java
  Run:     java Main > output.txt
================================================================================


================================================================================
  PHASE 1: CORE DATA GENERATION (Common to both Baseline & Proposed)
================================================================================

STEP 1: GENERATE FOG NETWORKS
  File: FogNetworkGenerator.java
  Config: SimulationConfig.java

  Creates N = 5 fog nodes. For each fog node j:
    - totalCpuGHz   = random in [6.0, 10.0] GHz
    - numberOfVRUs  = random integer in [200, 500]
    - vruCapacity   = (totalCpuGHz * 1000) / numberOfVRUs   (Mcycles/s per VRU)
      Example: 8.0 GHz → 8000 Mcycles/s / 400 VRUs = 20.0 Mcycles/s/VRU
    - name          = "FogNode_1", "FogNode_2", etc.
    - fogId         = 1, 2, ..., N

  Output: FogNetwork[] fogNetworks  (array of 5 objects)

................................................................................

STEP 2: GENERATE IOT TASKS
  File: TaskGenerator.java
  Config: SimulationConfig.java (NUM_TASKS)

  Creates M tasks (where M is determined by the current scenario: 250, 500, 1000, or 2000). For each task i:
    - cpuDemanded = random in [210.0, 480.0] Mcycles
    - inputSize   = random in [300.0, 600.0] KB
    - outputSize  = random in [10.0, 20.0] KB
    - deadline    = random in [15.0, 22.0] seconds
    - phase       = "Pre-Surgery" or "Post-Surgery"  (50/50 random)
    - severity    = "Minor" or "Major"                (50/50 random)
    - taskId      = 1, 2, ..., M

  Output: Task[] baseTasks  (array of 250 objects)

................................................................................

STEP 3: COMPUTE ABSOLUTE DELAY & ENERGY (for every task × every fog node)
  File: OffloadingCalculator.java

  Constants:
    - UPLINK_DATA_RATE   = 2500.0 KB/s   (20 MHz bandwidth)
    - DOWNLINK_DATA_RATE = 2500.0 KB/s
    - TRANSMISSION_POWER = 0.5 W         (device → fog)
    - EXECUTION_POWER    = 1.0 W         (fog processing)
    - RECEIVING_POWER    = 0.5 W         (fog → device)

  For each task i and fog node j:
    1. txTime   = inputSize_i / 2500.0                           (seconds)
    2. execTime = cpuDemanded_i / vruCapacity_j                  (seconds)
    3. rxTime   = outputSize_i / 2500.0                          (seconds)
    4. delay    = txTime + execTime + rxTime                     (seconds)
    5. energy   = (0.5 * txTime) + (1.0 * execTime) + (0.5 * rxTime)  (Joules)

  These values are stored into each Task object's per-fog arrays:
    task.transmissionTimes[j], task.executionTimes[j],
    task.receivingTimes[j], task.offloadingDelays[j], task.energies[j]

  Also calls task.initFogMetrics(5) to allocate all per-fog arrays first.

................................................................................

STEP 4: DEEP COPY (PIPELINE SPLIT)
  File: Main.java, Task.java (deepCopy method)

  Creates two independent copies of the baseTasks array:
    - baselineTasks[i] = baseTasks[i].deepCopy()   (for Baseline M-DAFTO)
    - proposedTasks[i] = baseTasks[i].deepCopy()   (for Proposed 2-Type MSDA)

  deepCopy() creates a new Task with identical:
    taskId, cpuDemanded, inputSize, outputSize, deadline, phase, severity,
    and copies all per-fog arrays (transmissionTimes, executionTimes,
    receivingTimes, offloadingDelays, energies, normalizedDelays,
    normalizedEnergies, normSums, urgencies, preferredFogIndices, prefCount).

  PURPOSE: From this point, the two pipelines modify their own copies
  independently. The raw delays and energies are identical, but
  normalization, urgency, and rankings will diverge.


================================================================================
  PHASE 2A: BASELINE M-DAFTO PIPELINE
================================================================================
  Uses: baselineTasks[]

STEP 5: GENERATE BASELINE AHP WEIGHTS (2 criteria only)
  File: WeightGenerator.java — generateBaselineWeights()

  Criteria: C1 = Delay Difference, C2 = Preference Count
  (Energy and Severity are NOT considered)

  Process:
    1. Generate two random perceived importances:
       v1 = random in [1.0, 9.0], v2 = random in [1.0, 9.0]
    2. Build 2×2 Pairwise Comparison Matrix P:
       P[0][0] = 1.0,    P[0][1] = getSaatyScale(v1/v2)
       P[1][0] = 1/P[0][1],  P[1][1] = 1.0
    3. getSaatyScale(ratio): Rounds ratio to nearest integer in Saaty scale
       {1,2,3,4,5,6,7,8,9}. If ratio < 1, returns 1/scale.
    4. Compute weights via CWD (Algorithm 5):
       - Column-normalize P → P_bar
       - Row-average P_bar → W[0], W[1]   (sum to 1.0)
    5. For 2×2 matrix: lambdaMax = 2.0, CI = 0.0, CR = 0.0 (always consistent)

  Output: baselineWeights.weights = [w1, w2]

................................................................................

STEP 6: BASELINE NORMALIZATION (Delay only, no energy, no severity weight)
  File: Normalizer.java — normalizeBaseline()

  Process:
    1. Find global min/max of offloadingDelay across ALL baselineTasks × ALL fogs:
       minDelay = min over all (i,j) of baselineTasks[i].offloadingDelays[j]
       maxDelay = max over all (i,j) of baselineTasks[i].offloadingDelays[j]
       delayRange = (maxDelay - minDelay), or 1.0 if all identical

    2. For each task i and fog node j:
       normDelay = (delay_ij - minDelay) / delayRange

       task.setNormalizedDelay(j, normDelay)
       task.setNormalizedEnergy(j, 0.0)          ← Energy explicitly set to 0
       task.setNormSum(j, normDelay)              ← normSum = normDelay ONLY

    NOTE: NO severity weight reduction is applied.
          Major and Minor tasks are treated identically.

    3. For each task: compute preferred fog order
       PreferenceRanker.computePreferredFogOrder(task, 5)
       Sorts fog indices by ascending normSum (= normDelay).
       Fog with lowest normalized delay = rank 1 (best).

................................................................................

STEP 7: BASELINE URGENCY CALCULATION (2 criteria only)
  File: UrgencyCalculator.java — computeBaselineUrgencies()

  For each task i:
    1. Calculate prefCount = number of fog nodes where delay ≤ deadline
       (Counts how many fogs can serve the task within its deadline)

    2. invPref = (prefCount == 0) ? 1.0 : (1.0 / prefCount)

    3. For each fog node j:
       delayDiff   = deadline_i - delay_ij
       invDelayDiff = 1.0 / max(delayDiff, 0.5)

       urgency = w1 * invDelayDiff + w2 * invPref

       task.setUrgency(j, urgency)

  NOTE: Energy term (w3 * 1/Energy) is ABSENT.
        Severity/Critical score term (w4 * gamma) is ABSENT.

................................................................................

STEP 8: BASELINE PREFERENCE RANKINGS
  File: PreferenceRanker.java

  8a. Task Rankings per Fog Node (All tasks, single pool):
      rankAllTasksPerFog(baselineTasks, 5)
      For each fog j: sort ALL 250 task indices by DESCENDING urgency for fog j.
      Result: prefBase[j][rank] → task index

  8b. Precedence Lists:
      buildMajorPrecedenceList(baselineTasks) → precBaseMaj[]
      buildMinorPrecedenceList(baselineTasks) → precBaseMin[]
      Each list: filters tasks by severity, then sorts by ASCENDING deadline.

  NOTE: Baseline does NOT create separate Major/Minor preference rankings
        per fog node. prefBase is used for all three slots (pAll, pMaj, pMin)
        in the SimulationData constructor.

................................................................................

STEP 8c: ASSEMBLE BASELINE SimulationData
  File: Main.java — buildSimData()

  Extracts from each baselineTask:
    normDelayArray[i]      = task.getNormalizedDelays()
    normEnergyArray[i]     = task.getNormalizedEnergies()    (all zeros)
    normSumArray[i]        = task.getNormSums()              (= normDelay only)
    preferredFogIndices[i] = task.getPreferredFogIndices()

  Assembles into: baselineSimData = new SimulationData(
    baselineTasks, fogNetworks,
    normDelayArray, normEnergyArray, normSumArray, preferredFogIndices,
    prefBase, prefBase, prefBase,     ← same ranking for all/major/minor
    baselineWeights.weights,          ← [w1, w2] (2 elements)
    precBaseMaj, precBaseMin
  )


================================================================================
  PHASE 2B: PROPOSED 2-TYPE MSDA PIPELINE
================================================================================
  Uses: proposedTasks[]

STEP 9: GENERATE PROPOSED AHP WEIGHTS (4 criteria)
  File: WeightGenerator.java — generateWeights()

  Criteria: C1 = Delay Difference, C2 = Preference Count,
            C3 = Energy, C4 = Severity

  Process (Algorithm 3: ACW loop):
    1. Generate 4 random perceived importances v[0..3] in [1.0, 9.0]
    2. Build 4×4 Pairwise Comparison Matrix P (Algorithm 4: PMC):
       Diagonal = 1.0
       Upper triangle: P[x][y] = getSaatyScale(v[x] / v[y])
       Lower triangle: P[x][y] = 1.0 / P[y][x]
    3. Compute weights via CWD (Algorithm 5):
       Column-normalize → Row-average → W[0..3]
    4. Consistency Check (Algorithm 6: CC):
       P' = P × W (element-wise P[x][y] * W[y])
       w'[x] = rowSum(P'[x]) / W[x]
       lambdaMax = mean(w')
       CI = (lambdaMax - 4) / (4 - 1)
       RI = 0.90 (for n=4)
       CR = CI / RI
       If CR ≤ 0.1 → PASS, else → regenerate (back to step 1)
    5. Max 100 iterations safeguard.

  Output: proposedWeights.weights = [w1, w2, w3, w4]

................................................................................

STEP 10: PROPOSED NORMALIZATION (Delay + Energy, with severity weight)
  File: Normalizer.java — normalize()

  Process:
    1. Find global min/max of BOTH delay and energy across ALL proposedTasks:
       minDelay, maxDelay, minEnergy, maxEnergy
       delayRange  = (maxDelay - minDelay), or 1.0 if identical
       energyRange = (maxEnergy - minEnergy), or 1.0 if identical

    2. For each task i:
       Determine severity weight:
         if severity == "Minor" → weight = 0.5
         else (Major)           → weight = 1.0

       For each fog node j:
         nd = ((delay_ij - minDelay) / delayRange) * weight
         ne = ((energy_ij - minEnergy) / energyRange) * weight

         task.setNormalizedDelay(j, nd)
         task.setNormalizedEnergy(j, ne)
         task.setNormSum(j, nd + ne)     ← normSum = normDelay + normEnergy

    3. For each task: compute preferred fog order
       PreferenceRanker.computePreferredFogOrder(task, 5)
       Sorts fog indices by ascending normSum (delay + energy combined).

................................................................................

STEP 11: PROPOSED URGENCY CALCULATION (4 criteria)
  File: UrgencyCalculator.java — computeAllUrgencies() → computeUrgency()

  For each task i:
    1. Calculate prefCount (same as baseline — fogs meeting deadline)

    2. invPref = (prefCount == 0) ? 1.0 : (1.0 / prefCount)

    3. Determine Critical Score:
       if severity == "Major" → criticalScore = 1.5  (formula: 1 + 1*0.5)
       else (Minor)           → criticalScore = 1.0  (formula: 1 + 0*0.5)

    4. For each fog node j:
       delayDiff    = deadline_i - delay_ij
       invDelayDiff = 1.0 / max(delayDiff, 0.5)
       invEnergy    = 1.0 / max(energy_ij, 0.001)

       urgency = w1 * invDelayDiff
               + w2 * invPref
               + w3 * invEnergy
               + w4 * criticalScore

       task.setUrgency(j, urgency)

................................................................................

STEP 12: PROPOSED PREFERENCE RANKINGS
  File: PreferenceRanker.java

  12a. Task Rankings per Fog — ALL tasks:
       rankAllTasksPerFog(proposedTasks, 5)
       For each fog j: sort ALL 250 indices by descending urgency.
       Result: prefProp[j][rank]

  12b. Task Rankings per Fog — MAJOR tasks only:
       rankMajorTasksPerFog(proposedTasks, 5)
       For each fog j: collect Major task indices, sort by descending urgency.
       Result: prefPropMaj[j][rank]

  12c. Task Rankings per Fog — MINOR tasks only:
       rankMinorTasksPerFog(proposedTasks, 5)
       For each fog j: collect Minor task indices, sort by descending urgency.
       Result: prefPropMin[j][rank]

  12d. Precedence Lists:
       buildMajorPrecedenceList(proposedTasks) → precPropMaj[]
       buildMinorPrecedenceList(proposedTasks) → precPropMin[]
       Filter by severity, sort by ascending deadline.

................................................................................

STEP 12e: ASSEMBLE PROPOSED SimulationData
  File: Main.java — buildSimData()

  Same extraction as baseline, but with proposed values:
    normSumArray includes energy component.

  Assembles into: proposedSimData = new SimulationData(
    proposedTasks, fogNetworks,
    normDelayArray, normEnergyArray, normSumArray, preferredFogIndices,
    prefProp, prefPropMaj, prefPropMin,   ← separate Major/Minor rankings
    proposedWeights.weights,               ← [w1, w2, w3, w4] (4 elements)
    precPropMaj, precPropMin
  )


================================================================================
  PHASE 2C: GPM PIPELINE
================================================================================
  Uses: gpmTasks[]

STEP 12f: GPM DATA PREPARATION
  File: Main.java, Normalizer.java, UrgencyCalculator.java

  Reuses Proposed normalization and AHP weights on gpmTasks to generate
  identical task/FN preference lists for fair evaluation:
    - Normalizer.normalize(gpmTasks, 5)
    - UrgencyCalculator.computeAllUrgencies(gpmTasks, w1, w2, w3, w4)
    - PreferenceRanker preference rankings and precedence lists are compiled.
  
  Assembles into: gpmSimData = new SimulationData(...)



================================================================================
  PHASE 3: QUOTA DETERMINATION (Common — computed once on fogNetworks)
================================================================================

STEP 13: MINIMUM QUOTA DETERMINATION (Algorithm 1: MQD)
  File: QuotaDeterminator.java

  Called TWICE:
    1. computeMinimumQuotas(fogNetworks, 250, true)   — for ALL tasks
    2. computeMinimumQuotas(fogNetworks, majorCount, false) — for MAJOR tasks only
       where majorCount = PreferenceRanker.countMajorTasks(proposedTasks)

  Algorithm for each call:
    1. Find the most efficient fog node (highest vruCapacity):
       lambdaMax = max(vruCapacity_j for all j)
       maxIndex  = index of that fog node

    2. Compute lMax for the most efficient node:
       hMax = numberOfVRUs of the most efficient fog
       lMax = min(hMax, floor(numTasks / numFogs))

    3. For all other fog nodes:
       lPrime = floor((vruCapacity_j / lambdaMax) * lMax)
       minQuota_j = min(lPrime, numberOfVRUs_j)

    4. Max quota for all fogs:
       maxQuota_j = numberOfVRUs_j

  Stored into FogNetwork objects:
    - isAllTasks=true:  setMinQuotaAllTasks(), setMaxQuotaAllTasks()
    - isAllTasks=false: setMinQuotaMajorTasks(), setMaxQuotaMajorTasks()


================================================================================
  PHASE 4: OUTPUT — PRINT CONFIGURATION & DIAGNOSTICS
================================================================================

STEP 14: PRINT HEADER & AHP DIAGNOSTICS
  File: SimulationPrinter.java

  14a. printHeader() — Shows task count, fog count, bandwidth, proposed weights
  14b. Print "[BASELINE M-DAFTO AHP DIAGNOSTICS]"
       printAHPDiagnostics(baselineWeights) — Shows 2×2 matrix
  14c. Print "[PROPOSED 2-TYPE MSDA AHP DIAGNOSTICS]"
       printAHPDiagnostics(proposedWeights) — Shows 4×4 matrix
  14d. printFogNodes() — CPU, VRUs, VRU Capacity for each fog
  14e. printQuotas() — Min(All), Max(All), Min(Major), Max(Major) per fog

STEP 15: PRINT INPUT DATA (using Proposed pipeline data)
  15a. printSampleTasks(proposedTasks, fogNetworks, 3)
       Shows first 3 tasks: attributes, delay/energy/normSum/urgency per fog,
       and preferred fog order.
  15b. printPreferredTaskPrioritization(proposedSimData, proposedTasks, fogNetworks)
       Shows top 15 most-urgent tasks per fog node.
  15c. printPrecedenceLists(precPropMaj, precPropMin)
       Shows top 15 tasks from Major and Minor precedence lists (by deadline).


================================================================================
  PHASE 5: MATCHING ALGORITHM EXECUTION
================================================================================

STEP 16A: RUN BASELINE M-DAFTO ALGORITHM
  File: MSDAlgorithm.java — matchBaseline() → executeBaseline()

  16A.1: Build unified Precedence List (ALL baselineTasks sorted by ascending deadline)
  16A.2: Extract minAll[] and maxAll[] quotas from fogNetworks
  16A.3: Run runSingleTypeMSDABaseline(initialTasks, minAll, maxAll)

  runSingleTypeMSDABaseline — Algorithm 9 (Single-Type MSDA):
    Input: PL (precedence list), curr_l[] (min quotas), curr_h[] (max quotas)
    Loop while PL is not empty:
      1. r_k = sum of all curr_l[c]  (total remaining min quotas)
      2. R_k = last r_k tasks from PL (tasks with latest deadlines)
      3. diff = R_prev \ R_k  (tasks to process this round)
      4. If diff is not empty:
         - Run runStandardDABaseline(diff, curr_h, status=true)
           (status=true means deadlines ARE enforced — tasks skip fogs
            where delay > deadline)
         - Remove matched tasks from PL
         - Update: curr_h[c] -= matched, curr_l[c] = max(0, curr_l[c] - matched)
      5. Else (final round — only R_k remains):
         - Run runStandardDABaseline(R_k, curr_l, status=false)
           (status=false means deadlines are RELAXED to fill min quotas)
         - Remove matched tasks from PL
         - Update quotas

  runStandardDABaseline(candidates, q[], status) — Deferred Acceptance:
    1. Each unmatched task proposes to its next preferred fog node
       - If status=true: SKIP fogs where delay > deadline
       - If status=false: propose regardless of deadline
    2. Each fog node c accepts up to q[c] tasks (sorted by descending urgency)
       - If more than q[c] applicants: reject lowest-urgency tasks
    3. Rejected tasks re-propose in next iteration
    4. Loop until no task can propose further
    Result: Map<Task, Integer> (task → assigned fog index)

................................................................................

STEP 16B: RUN PROPOSED 2-TYPE MSDA ALGORITHM
  File: MSDAlgorithm.java — match() → execute()

  16B.1: Build unified Precedence List (ALL proposedTasks sorted by ascending deadline)
  16B.2: Extract quotas: minAll[], maxAll[], minMajor[], maxMajor[]
  16B.3: Run doTwoTypeMSDA(initialTasks, minAll, maxAll, minMajor, maxMajor)

  doTwoTypeMSDA — Main loop (Algorithm 10: 2-Type MSDA):
    Notation: p=minAll, q=maxAll, pr=minMajor, qr=maxMajor, V=task set
    Loop:
      1. Compute target counts:
         vs = sum of max(0, p[c] - qr[c])   (minor slots needed)
         vr = sum of pr[c]                   (major slots needed)
         vt = sum of p[c]                    (total min slots)

      2. V_new = last vt tasks from V_prev (satisfying vr major + vs minor)
         Scans from bottom of precedence list to ensure enough R and S types.

      3. da_tasks = V_prev \ V_new  (tasks to match this stage)

      4. If da_tasks not empty:
         Run runModifiedDA(da_tasks, p, q, pr, qr) — 2-Type DA
         Update all quota arrays based on how many Major/Minor were matched
         V_prev = V_new, continue loop

      5. Else (final stage): Check three cases:
         a. If |V_R| == vr:
            - Match R (Major) with runStandardDA using pr quotas
            - Match S (Minor) with runSingleTypeMSDA using remaining quotas
         b. If |V_S| == vs:
            - Match S (Minor) with runStandardDA
            - Match R (Major) with runSingleTypeMSDA
         c. Otherwise:
            - Recurse: doTwoTypeMSDA(V_new, adjusted quotas)

  runModifiedDA(candidates, p, q, pr, qr) — 2-Type Deferred Acceptance:
    1. Each unmatched task proposes to its next preferred fog node
       (NO deadline filtering — quotas must be satisfied)
    2. Each fog node c evaluates applicants in two pools:
       Rc = Major applicants, Sc = Minor applicants
       - If |Rc| > qr[c]: reject lowest-urgency Major tasks
       - Elif |Sc| > q[c] - pr[c]: reject lowest-urgency Minor tasks
       - Elif total > q[c]: reject lowest-urgency from combined pool
    3. Rejected tasks re-propose
    Result: Map<Task, Integer>

  runStandardDA(candidates, q[]) — Standard DA (no deadline filtering):
    Same as baseline DA but without the status flag.
    Always accepts proposals regardless of deadline.

  runSingleTypeMSDA(T, p[], q[]) — Single-Type MSDA for one type:
    Multi-stage loop similar to baseline, using runStandardDA internally.


................................................................................

STEP 16C: RUN GPM ALGORITHM
  File: MSDAlgorithm.java — matchGPM() → executeGPM()

  16C.1: Copy tasks and sort them by ascending deadline (matching baseline PL order).
  16C.2: Retrieve all Fog Networks and sort their indices by `numberOfVRUs` descending (with stable node ID sorting).
  16C.3: Loop through sorted tasks and assign each to the current fog node if its assigned count is less than the node's `numberOfVRUs` limit.
  16C.4: If the current fog node is full, move to the next fog node in the sorted list.
  16C.5: If all fog nodes are fully utilized, leave the remaining tasks unassigned.


================================================================================
  PHASE 6: EVALUATION METRICS & RESULTS PRINTING
================================================================================

STEP 17: COMPUTE & PRINT RESULTS
  File: SimulationPrinter.java — printResults()

  Called THREE TIMES: for baselineResult, proposedResult, and gpmResult.
  For gpmResult, printDetails is false to suppress console print logs.
  For each result set, computes (and optionally prints) metrics:


  17a. MATCHING OVERVIEW:
       matched = result.matchedCount
       outages = result.unmatchedCount
       Outage Rate = (outages / numTasks) * 100 %

  17b. TOTAL DELAY & ENERGY (physical measurement):
       totalDelay  = sum of offloadingDelay[assigned_fog] for all matched tasks
       totalEnergy = sum of energy[assigned_fog] for all matched tasks
       avgDelay    = totalDelay / matched
       avgEnergy   = totalEnergy / matched

  17c. DEADLINE COMPLIANCE:
       metDeadline = count of matched tasks where delay ≤ deadline
       missedDL    = matched - metDeadline
       successRate = (metDeadline / matched) * 100 %

  17d. TASK SATISFACTION (Eq. 20):
       For each matched task i assigned to fog f:
         1. Build list of "acceptable" fogs (where delay ≤ deadline),
            ordered by the task's preferredFogIndices.
         2. K = size of acceptable list
         3. pos = position of f in the acceptable list (0-indexed)
         4. satisfaction_i = ((K - pos) / K) * 100
       avgTaskSat = sum(satisfaction_i for all tasks) / numTasks

  17e. FN SATISFACTION (Eq. 21-22):
       For each fog node j:
         For each task assigned to fog j:
           1. Find rank of that task in fog j's preference list
           2. satisfaction = ((prefListSize - rank + 1) / prefListSize) * 100
         sumSat = sum of satisfactions for all tasks assigned to j
         divisor = min(numberOfVRUs_j, prefListSize)
         fnSat_j = sumSat / divisor
       avgFnSat = sum(fnSat_j) / numFogs

  17f. FAIRNESS INDICES:
       matchesPerFog[j] = number of tasks assigned to fog j

       Gini Index:
         sumDiff = sum over all pairs (a,b) of |matchesPerFog[a] - matchesPerFog[b]|
         mean    = sum(matchesPerFog) / numFogs
         gini    = sumDiff / (2 * numFogs^2 * mean)

       Jain Fairness Index:
         sum   = sum(matchesPerFog[j])
         sumSq = sum(matchesPerFog[j]^2)
         jain  = sum^2 / (numFogs * sumSq)

  17g. MAJOR vs MINOR BREAKDOWN:
       Counts, match rates, avg delay, avg energy for each severity type.

  17h. FN ASSIGNMENT TABLE:
       Per fog: MinQ, MaxQ, Major assigned, Minor assigned, Total, Utilization%.

  17i. PER-FN DELAY & ENERGY:
       Per fog: task count, average delay, total energy.

  17j. RESOURCE UTILIZATION BAR:
       Per fog: assigned/maxQuota with visual bar.

  17k. TRAFFIC LOAD DISTRIBUTION:
       Per fog: task count with proportional bar.


================================================================================
  PHASE 7: MULTI-SCENARIO ITERATION & METRIC AVERAGING
================================================================================

STEP 18: SCENARIO LOOPING & OUTPUT CAPTURE
  File: Main.java, SimulationMetrics.java

  The entire pipeline (Steps 1 to 17) is wrapped in two loops:
    - Outer Loop: Iterates through load scenarios (250, 500, 1000, 2000 tasks).
    - Inner Loop: Iterates 10 times per scenario to smooth random variations.
  
  For each iteration, SimulationMetrics accumulates:
    - Total Offloading Delay, Avg Offloading Delay
    - Total Energy
    - Outage Probability
    - Task Satisfaction, FN Satisfaction

  Output Strategy:
    1. For iteration 0 of each scenario, full detailed metrics are captured 
       using a ByteArrayOutputStream.
    2. After 10 iterations, the accumulators are averaged and SimulationPrinter.printScenarioAverages() 
       prints a comparative summary table (with GPM, Baseline, and Proposed) for the scenario.
    3. The 4 scenario tables print sequentially at the top of the output file.
    4. Finally, the captured detailed 1st-iteration logs are appended at the bottom.


================================================================================
  KEY DIFFERENCES: GPM vs BASELINE vs PROPOSED
================================================================================

  FEATURE                  GPM                        BASELINE (M-DAFTO)         PROPOSED (2-Type MSDA)
  ─────────────────────────────────────────────────────────────────────────────────────────────────────────────
  Initial Data             IDENTICAL raw delays/energy IDENTICAL raw delays/energy IDENTICAL raw delays/energy
                           from Steps 1-3             from Steps 1-3             from Steps 1-3
  AHP Criteria             None                       2 (DelayDiff, PrefCount)   4 (+Energy, +Severity)
  AHP Matrix               None                       2×2 (always CR=0)          4×4 (loop until CR≤0.1)
  Normalization normSum    Proposed norm (for stats)  normDelay only             normDelay + normEnergy
  Severity Weight on Norm  None                       None                       Minor=0.5, Major=1.0
  Urgency Formula          None                       w1/slack + w2/pref         + w3/energy + w4*gamma
  Critical Score (gamma)   Not used                   Not used                   Major=1.5, Minor=1.0
  Fog Preference Ranking   Proposed rankings          By delay only              By delay+energy
  Task Ranking per Fog     Proposed rankings          Single pool (all tasks)    Separate Major/Minor pools
  Matching Algorithm       Greedy VRU assignment      Single-Type MSDA (Alg 9)   2-Type MSDA (Alg 10)
  Deadline Handling        Ignored during match       status flag (true/false)   No deadline filtering in DA
  Quota Enforcement        Max capacity = VRUs only   Min-quota with relaxation  Split min-quota R/S types
  ─────────────────────────────────────────────────────────────────────────────────────────────────────────────

