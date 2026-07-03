public class SimulationMetrics {
    public double avgDelay;
    public double totalDelay;
    public double totalEnergy;
    public double outageRate; // represented as percentage (0-100) or probability (0-1), based on what's accumulated
    public double avgTaskSat; // 0-100%
    public double avgFnSat;   // 0-100%

    public SimulationMetrics() {
        this.avgDelay = 0.0;
        this.totalDelay = 0.0;
        this.totalEnergy = 0.0;
        this.outageRate = 0.0;
        this.avgTaskSat = 0.0;
        this.avgFnSat = 0.0;
    }

    public SimulationMetrics(double avgDelay, double totalDelay, double totalEnergy, double outageRate, double avgTaskSat, double avgFnSat) {
        this.avgDelay = avgDelay;
        this.totalDelay = totalDelay;
        this.totalEnergy = totalEnergy;
        this.outageRate = outageRate;
        this.avgTaskSat = avgTaskSat;
        this.avgFnSat = avgFnSat;
    }

    public void add(SimulationMetrics other) {
        this.avgDelay += other.avgDelay;
        this.totalDelay += other.totalDelay;
        this.totalEnergy += other.totalEnergy;
        this.outageRate += other.outageRate;
        this.avgTaskSat += other.avgTaskSat;
        this.avgFnSat += other.avgFnSat;
    }

    public void divideBy(int count) {
        if (count > 0) {
            this.avgDelay /= count;
            this.totalDelay /= count;
            this.totalEnergy /= count;
            this.outageRate /= count;
            this.avgTaskSat /= count;
            this.avgFnSat /= count;
        }
    }
}
