package tutorial.withinday.withinDayReplanningAgents;

public class AcoLink {
    //Valeur initial
    static Double F0 = 0.2;
    private Double f;

    AcoLink() {
        setPheromone(F0);
    }

    AcoLink(Double F) {
        setPheromone(F);
    }

    void setPheromoneAMD(Double d) {
        this.f = (F0 * (1 + d));
    }

    double getPheromone() {
        return this.f;
    }

    void setPheromone(double nf) {
        //We have to check Limits of pheromone values [min - Max] -
        if (nf <= 0) {
            if (nf == 0) {
                this.f = F0;
            } else {
                if (nf < -1) {
                    this.f = F0 / (-nf / F0);
                } else {
                    this.f = F0 * (1 + nf);
                }
            }
        } else {
            this.f = nf;
        }
    }

    void AddDeltaF(double val, Double evap) {
        this.setPheromone((this.getPheromone() * (1 - evap)) + val);
    }
}

