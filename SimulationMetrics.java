public class SimulationMetrics {
    public double avgDelay;
    public double totalDelay;
    public double totalEnergy;
    public double outageRate; // represented as percentage (0-100) or probability (0-1), based on what's accumulated
    public double avgTaskSat; // 0-100%
    public double avgFnSat;   // 0-100%
    
    public double majorOutageRate;
    public double minorOutageRate;
    public double majorAvgDelay;
    public double minorAvgDelay;

    public SimulationMetrics() {
        this.avgDelay = 0.0;
        this.totalDelay = 0.0;
        this.totalEnergy = 0.0;
        this.outageRate = 0.0;
        this.avgTaskSat = 0.0;
        this.avgFnSat = 0.0;
        
        this.majorOutageRate = 0.0;
        this.minorOutageRate = 0.0;
        this.majorAvgDelay = 0.0;
        this.minorAvgDelay = 0.0;
    }

    public SimulationMetrics(double avgDelay, double totalDelay, double totalEnergy, double outageRate, double avgTaskSat, double avgFnSat,
                             double majorOutageRate, double minorOutageRate, double majorAvgDelay, double minorAvgDelay) {
        this.avgDelay = avgDelay;
        this.totalDelay = totalDelay;
        this.totalEnergy = totalEnergy;
        this.outageRate = outageRate;
        this.avgTaskSat = avgTaskSat;
        this.avgFnSat = avgFnSat;
        
        this.majorOutageRate = majorOutageRate;
        this.minorOutageRate = minorOutageRate;
        this.majorAvgDelay = majorAvgDelay;
        this.minorAvgDelay = minorAvgDelay;
    }

    public void add(SimulationMetrics other) {
        this.avgDelay += other.avgDelay;
        this.totalDelay += other.totalDelay;
        this.totalEnergy += other.totalEnergy;
        this.outageRate += other.outageRate;
        this.avgTaskSat += other.avgTaskSat;
        this.avgFnSat += other.avgFnSat;
        
        this.majorOutageRate += other.majorOutageRate;
        this.minorOutageRate += other.minorOutageRate;
        this.majorAvgDelay += other.majorAvgDelay;
        this.minorAvgDelay += other.minorAvgDelay;
    }

    public void divideBy(int count) {
        if (count > 0) {
            this.avgDelay /= count;
            this.totalDelay /= count;
            this.totalEnergy /= count;
            this.outageRate /= count;
            this.avgTaskSat /= count;
            this.avgFnSat /= count;
            
            this.majorOutageRate /= count;
            this.minorOutageRate /= count;
            this.majorAvgDelay /= count;
            this.minorAvgDelay /= count;
        }
    }
}
