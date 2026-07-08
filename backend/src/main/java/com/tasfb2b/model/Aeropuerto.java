package com.tasfb2b.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aeropuerto")
@Getter
@Setter
@NoArgsConstructor
public class Aeropuerto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 
    private String codigo;      // Código ICAO: "SKBO", "EHAM", etc.
    private String nombre;
    private String ciudad;
    private String pais;
    private String continente;  // "AMERICA", "EUROPA", "ASIA"
    private int gmt;            // Huso horario: -5, +2, etc.
    private int capacidadMax;   // Máximo de maletas que puede almacenar simultáneamente
    private double lat;         // Latitud decimal (positivo = N, negativo = S)
    private double lng;         // Longitud decimal (positivo = E, negativo = W)

    // Ocupación dinámica: cambia durante la simulación/algoritmo
    private int ocupacionActual;

    public Aeropuerto(String codigo, String ciudad, String pais, int gmt, int capacidadMax,
                      String continente, double lat, double lng) {
        this.codigo = codigo;
        this.nombre = ciudad;
        this.ciudad = ciudad;
        this.pais = pais;
        this.gmt = gmt;
        this.capacidadMax = capacidadMax;
        this.continente = continente;
        this.lat = lat;
        this.lng = lng;
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
    public String getNombre()       { return nombre != null && !nombre.isBlank() ? nombre : ciudad; }
    public void setNombre(String v) { this.nombre = v; }
    public String getCiudad()       { return ciudad; }
    public String getPais()         { return pais; }
    public String getContinente()   { return continente; }
    public int getGmt()             { return gmt; }
    public int getCapacidadMax()    { return capacidadMax; }
    public double getLat()          { return lat; }
    public double getLng()          { return lng; }
    public int getOcupacionActual() { return ocupacionActual; }
    public void setOcupacionActual(int v) { this.ocupacionActual = v; }

    @Override
    public String toString() {
        return codigo + " (" + ciudad + ", " + continente + ", GMT" + (gmt >= 0 ? "+" : "") + gmt + ")"
             + " [" + ocupacionActual + "/" + capacidadMax + "]";
    }
}
