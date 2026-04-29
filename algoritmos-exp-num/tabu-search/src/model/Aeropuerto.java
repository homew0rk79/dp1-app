package model;

public class Aeropuerto {

    private final String codigo;      // Código ICAO: "SKBO", "EHAM", etc.
    private final String ciudad;
    private final String pais;
    private final String continente;  // "AMERICA", "EUROPA", "ASIA"
    private final int gmt;            // Huso horario: -5, +2, etc.
    private final int capacidadMax;   // Máximo de maletas que puede almacenar simultáneamente

    // Ocupación dinámica: cambia durante la simulación/algoritmo
    private int ocupacionActual;

    public Aeropuerto(String codigo, String ciudad, String pais, int gmt, int capacidadMax, String continente) {
        this.codigo = codigo;
        this.ciudad = ciudad;
        this.pais = pais;
        this.gmt = gmt;
        this.capacidadMax = capacidadMax;
        this.continente = continente;
        this.ocupacionActual = 0;
    }

    // Intenta agregar maletas al aeropuerto. Retorna false si excede capacidad.
    public boolean agregarMaletas(int cantidad) {
        if (ocupacionActual + cantidad > capacidadMax) return false;
        ocupacionActual += cantidad;
        return true;
    }

    public void liberarMaletas(int cantidad) {
        ocupacionActual = Math.max(0, ocupacionActual - cantidad);
    }

    public boolean tieneCapacidad(int cantidad) {
        return ocupacionActual + cantidad <= capacidadMax;
    }

    public String getCodigo()       { return codigo; }
    public String getCiudad()       { return ciudad; }
    public String getPais()         { return pais; }
    public String getContinente()   { return continente; }
    public int getGmt()             { return gmt; }
    public int getCapacidadMax()    { return capacidadMax; }
    public int getOcupacionActual() { return ocupacionActual; }
    public void setOcupacionActual(int v) { this.ocupacionActual = v; }

    @Override
    public String toString() {
        return codigo + " (" + ciudad + ", " + continente + ", GMT" + (gmt >= 0 ? "+" : "") + gmt + ")"
             + " [" + ocupacionActual + "/" + capacidadMax + "]";
    }
}
