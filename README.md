# Optimized Task Offloading for Healthcare IoT-Fog Systems

Improved a published matching algorithm (M-DAFTO) by introducing Major/Minor task classification with separate quota constraints and type-separated Deferred Acceptance matching for healthcare IoT-Fog systems. Expanded the urgency model from 2 to 4 criteria (delay, energy, preference, severity) using AHP-based weight generation with consistency validation. Implemented severity-weighted normalization and separate preference lists per task type across fog nodes. Evaluated in Java across 250–2000 task scales against 2 baselines, achieving 0% critical-task outage under normal load.

---

## The Problem

In smart hospital IoT networks, devices generate computational tasks (surgeries, diagnostics, monitoring) that must be processed by nearby **Fog Nodes** (edge servers) in real time.

- Tasks have **strict deadlines** — a surgery must be processed within seconds
- Fog nodes have **limited capacity** — not every task gets assigned
- **Not all tasks are equal** — a major surgery matters more than a routine checkup
- An unassigned task = **outage** — unacceptable for critical operations

The existing **M-DAFTO** algorithm treats all tasks equally in a single pool, so critical surgeries compete with routine checkups for the same resources.

---

## What This Project Does

Redesigns the matching algorithm as a **2-Type Multi-Stage Deferred Acceptance (MSDA)** system that separates critical and routine tasks.

| What Changed | Baseline (M-DAFTO) | This Project (2-Type MSDA) |
|:---|:---|:---|
| Task Types | Single pool | **Major** vs **Minor** with separate quotas |
| Urgency Criteria | 2 (delay, preference) | **4** (+ energy, + severity) |
| AHP Matrix | 2×2 | **4×4** with consistency check (CR ≤ 0.1) |
| Normalization | Delay only | Delay + Energy, **severity-weighted** |
| Matching | Single-Type DA | **2-Type DA** with 3-constraint rejection |
| Quota System | One quota per fog node | **Separate** Major and Minor quotas |

---

## Results

Averaged over **10 iterations** per scenario, **4 task scales**, compared against **2 baselines** (Greedy Placement + M-DAFTO):

### Overall Outage Probability

| Scale | Greedy Placement | Baseline (M-DAFTO) | **Proposed (2-Type)** |
|:---:|:---:|:---:|:---:|
| 250 | 0.00% | 0.92% | **0.16%** |
| 500 | 0.00% | 1.34% | **0.02%** |
| 1000 | 0.00% | 3.65% | **0.02%** |
| 2000 | 13.15% | 20.46% | **13.15%** |

### Major Task Outage (Critical Operations)

| Scale | Greedy Placement | Baseline (M-DAFTO) | **Proposed (2-Type)** |
|:---:|:---:|:---:|:---:|
| 250 | 0.00% | 1.06% | **0.00%** |
| 500 | 0.00% | 1.32% | **0.00%** |
| 1000 | 0.00% | 3.72% | **0.00%** |
| 2000 | 13.02% | 20.46% | **9.36%** |

### Key Takeaways

- **0% critical-task outage** under normal load (250–1000 tasks)
- Under extreme overload (2000 tasks): Major outage dropped from 20.46% → 9.36%
- FN Satisfaction improved under overload: ~50% → ~59%
- Comparable task satisfaction and delay metrics across all scenarios

---

## How It Works

The project runs a **7-phase pipeline**:

```
Phase 1 → Generate fog nodes and IoT tasks with randomized attributes
Phase 2 → Split into independent pipelines (Baseline / Proposed / GPM)
Phase 3 → Normalize delay & energy, compute AHP weights, calculate urgency
Phase 4 → Rank tasks per fog node, build precedence lists, determine quotas
Phase 5 → Run matching algorithms (Single-Type MSDA / 2-Type MSDA / Greedy)
Phase 6 → Compute metrics (outage, delay, energy, satisfaction, fairness)
Phase 7 → Loop across 4 scales × 10 iterations, average results
```

### Core Algorithm: 2-Type MSDA

The matching runs in multiple stages:

1. **Compute target counts** — how many Major (R-type) and Minor (S-type) slots are needed
2. **Extract bottom-of-list tasks** — tasks with latest deadlines are deferred to later stages
3. **Run Modified DA** — Deferred Acceptance with 3 simultaneous constraints:
   - Major quota limit (`qr_c`)
   - Minor quota limit (`q_c - pr_c`)
   - Total capacity limit (`q_c`)
4. **Final stage** handles 3 cases:
   - Exact Major count → DA for Majors + Single-Type MSDA for Minors
   - Exact Minor count → DA for Minors + Single-Type MSDA for Majors
   - Otherwise → Recursive 2-Type MSDA

### Urgency Function

**Baseline (2 criteria):**
```
U = w1 × 1/(Deadline - Delay) + w2 × 1/PrefCount
```

**Proposed (4 criteria):**
```
U = w1 × 1/(Deadline - Delay) + w2 × 1/PrefCount + w3 × 1/Energy + w4 × γ
```
Where γ = 1.5 for Major tasks, 1.0 for Minor tasks.

Weights are generated using a **4×4 AHP pairwise comparison matrix** with consistency validation (CR ≤ 0.1).

---

## Project Structure

### Pipeline

| File | Role |
|:---|:---|
| `Main.java` | Orchestrator — runs full pipeline across 4 scenarios × 10 iterations |
| `TaskGenerator.java` | Generates randomized IoT tasks with CPU demand, data sizes, deadlines, severity |
| `FogNetworkGenerator.java` | Generates randomized fog nodes with CPU capacity and VRU count |
| `OffloadingCalculator.java` | Computes delay and energy for every task × fog node pair |
| `Normalizer.java` | Min-Max normalization with severity-weighted scaling |
| `WeightGenerator.java` | AHP weight generation — 4×4 matrix with consistency check |
| `UrgencyCalculator.java` | Multi-criteria urgency scoring |
| `PreferenceRanker.java` | Fog-side preference rankings (All / Major / Minor) and precedence lists |
| `QuotaDeterminator.java` | Proportional min/max quota computation per fog node |
| `MSDAlgorithm.java` | Core matching — Baseline MSDA, 2-Type MSDA, and Greedy Placement |

### Data Models

| File | Role |
|:---|:---|
| `Task.java` | Task model — per-fog arrays for delay, energy, urgency, preferences, severity |
| `FogNetwork.java` | Fog node model — CPU, VRUs, capacity, separate quota fields |
| `SimulationData.java` | DTO aggregating all computed arrays for pipeline stages |
| `SimulationConfig.java` | Central configuration — all constants and bounds |

### Output

| File | Role |
|:---|:---|
| `SimulationPrinter.java` | Results, per-FN stats, utilization bars, satisfaction, fairness indexes |
| `SimulationMetrics.java` | Accumulator for averaging metrics across iterations |

---

## How to Run

**Prerequisites:** JDK 8+

```bash
# Compile
javac *.java

# Run
java Main > output.txt
```

**Windows PowerShell** (if encoding issues):
```powershell
java Main | Out-File -Encoding utf8 output.txt
```

### Output Includes

- Scenario summaries — averaged metrics across 10 iterations per scale
- Detailed first-iteration breakdown — matching tables, per-FN delay/energy, utilization bars, Gini/Jain fairness indexes

---

## References

- **Base Paper:** M-DAFTO — [Multi-Stage Deferred Acceptance Based Fair Task Offloading in IoT-Fog Systems](https://ieeexplore.ieee.org/document/10620404) (IEEE JIOT 2022)
- **Matching Theory:** Gale-Shapley Deferred Acceptance Algorithm
- **AHP:** Saaty's Analytic Hierarchy Process

---

## Tech Stack

**Language:** Java (JDK 8+) · **~2500 LOC across 14 classes**

**Algorithms:** Multi-Stage Deferred Acceptance, AHP, Min-Max Normalization

**Data Structures:** HashMap, ArrayList, 2D arrays, Comparator-based sorting

**Design:** Modular pipeline architecture, DTO pattern, deep copy for independent algorithm pipelines
