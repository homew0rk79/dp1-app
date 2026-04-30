package algorithm;

/**
 * Configuración de parámetros del algoritmo Ant Colony System.
 * Incluye constantes con los rangos válidos para experimentación numérica.
 */
public class ACOConfig {

    // --- Rangos de parámetros para experimentación ---

    /** Valor mínimo permitido para α (influencia de feromona) */
    public static final double ALPHA_MIN = 0.5;
    /** Valor máximo permitido para α */
    public static final double ALPHA_MAX = 2.0;

    /** Valor mínimo permitido para β (influencia de heurística) */
    public static final double BETA_MIN = 1.0;
    /** Valor máximo permitido para β */
    public static final double BETA_MAX = 5.0;

    /** Valor mínimo permitido para ρ (tasa de evaporación) */
    public static final double RHO_MIN = 0.1;
    /** Valor máximo permitido para ρ */
    public static final double RHO_MAX = 0.5;

    /** Rango mínimo de hormigas */
    public static final int NUM_ANTS_MIN = 10;
    /** Rango máximo de hormigas */
    public static final int NUM_ANTS_MAX = 50;

    /** Rango mínimo de iteraciones */
    public static final int MAX_ITER_MIN = 100;
    /** Rango máximo de iteraciones */
    public static final int MAX_ITER_MAX = 500;

    /** Valor mínimo de feromona */
    public static final double TAU_MIN = 0.001;
    /** Valor máximo de feromona */
    public static final double TAU_MAX = 10.0;

    // --- Parámetros de instancia ---

    private final double alpha;
    private final double beta;
    private final double rho;
    private final double q;
    private final double tauMin;
    private final double tauMax;
    private final int numAnts;
    private final int maxIterations;
    private final double w1;
    private final double w2;
    private final double w3;
    private final double riskWeight;
    private final double saturationWeight;
    private final double stopsWeight;

    /**
     * Crea una configuración ACO con todos los parámetros.
     *
     * @param alpha            exponente de feromona (α ∈ [0.5, 2.0])
     * @param beta             exponente de heurística (β ∈ [1.0, 5.0])
     * @param rho              tasa de evaporación (ρ ∈ [0.1, 0.5])
     * @param q                constante de depósito de feromona
     * @param tauMin           feromona mínima
     * @param tauMax           feromona máxima
     * @param numAnts          número de hormigas (∈ [10, 50])
     * @param maxIterations    máximo de iteraciones (∈ [100, 500])
     * @param w1               peso de tiempo en heurística
     * @param w2               peso de capacidad de vuelo en heurística
     * @param w3               peso de almacén disponible en heurística
     * @param riskWeight       peso de riesgo en función objetivo
     * @param saturationWeight peso de saturación en función objetivo
     * @param stopsWeight      peso de escalas en función objetivo
     */
    public ACOConfig(double alpha, double beta, double rho, double q,
                     double tauMin, double tauMax, int numAnts, int maxIterations,
                     double w1, double w2, double w3,
                     double riskWeight, double saturationWeight, double stopsWeight) {
        this.alpha = alpha;
        this.beta = beta;
        this.rho = rho;
        this.q = q;
        this.tauMin = tauMin;
        this.tauMax = tauMax;
        this.numAnts = numAnts;
        this.maxIterations = maxIterations;
        this.w1 = w1;
        this.w2 = w2;
        this.w3 = w3;
        this.riskWeight = riskWeight;
        this.saturationWeight = saturationWeight;
        this.stopsWeight = stopsWeight;
    }

    /**
     * Crea una configuración con valores por defecto razonables.
     *
     * @return configuración por defecto
     */
    public static ACOConfig defaults() {
        return new ACOConfig(
                1.0,    // alpha
                2.0,    // beta
                0.3,    // rho
                100.0,  // Q
                TAU_MIN,
                TAU_MAX,
                20,     // numAnts
                200,    // maxIterations
                0.5,    // w1 (tiempo)
                0.3,    // w2 (capacidad vuelo)
                0.2,    // w3 (almacén)
                0.6,    // riskWeight
                0.25,   // saturationWeight
                0.15    // stopsWeight
        );
    }

    /** @return exponente de feromona α */
    public double getAlpha() { return alpha; }

    /** @return exponente de heurística β */
    public double getBeta() { return beta; }

    /** @return tasa de evaporación ρ */
    public double getRho() { return rho; }

    /** @return constante Q de depósito */
    public double getQ() { return q; }

    /** @return feromona mínima */
    public double getTauMin() { return tauMin; }

    /** @return feromona máxima */
    public double getTauMax() { return tauMax; }

    /** @return número de hormigas */
    public int getNumAnts() { return numAnts; }

    /** @return máximo de iteraciones */
    public int getMaxIterations() { return maxIterations; }

    /** @return peso de tiempo en heurística */
    public double getW1() { return w1; }

    /** @return peso de capacidad de vuelo en heurística */
    public double getW2() { return w2; }

    /** @return peso de almacén en heurística */
    public double getW3() { return w3; }

    /** @return peso de riesgo en función objetivo */
    public double getRiskWeight() { return riskWeight; }

    /** @return peso de saturación en función objetivo */
    public double getSaturationWeight() { return saturationWeight; }

    /** @return peso de escalas en función objetivo */
    public double getStopsWeight() { return stopsWeight; }

    @Override
    public String toString() {
        return String.format(
                "ACOConfig{α=%.2f, β=%.2f, ρ=%.2f, Q=%.1f, τ=[%.4f,%.1f], ants=%d, iter=%d, " +
                        "w=[%.2f,%.2f,%.2f], obj=[%.2f,%.2f,%.2f]}",
                alpha, beta, rho, q, tauMin, tauMax, numAnts, maxIterations,
                w1, w2, w3, riskWeight, saturationWeight, stopsWeight);
    }
}
