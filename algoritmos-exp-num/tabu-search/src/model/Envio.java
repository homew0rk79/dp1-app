package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Envio {

    /*
     * Formato de línea en el archivo:
     *   000000001-20260102-00-47-SUAA-002-0032535
     *   idEnvio   fecha    hh mm dest cant idCliente
     *
     * El aeropuerto origen NO está en la línea: viene del nombre del archivo.
     * Ej: "_envios_EBCI_.txt" → origen = "EBCI"
     */

    private final String id;                  // "000000001"
    private final String origen;              // "EBCI"  (del nombre del archivo)
    private final String destino;             // "SUAA"
    private final LocalDateTime fechaHoraRegistro; // momento en que se registró el envío
    private final int cantidad;               // número de maletas (1–999)
    private final String idCliente;           // "0032535"

    // Estado durante la ejecución del algoritmo
    private boolean entregado;

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public Envio(String id, String origen, String destino,
                 String fechaStr, String hhStr, String mmStr,
                 String cantidadStr, String idCliente) {

        this.id = id;
        this.origen = origen;
        this.destino = destino;
        this.idCliente = idCliente;
        this.cantidad = Integer.parseInt(cantidadStr);
        this.entregado = false;

        // Construir la fecha-hora de registro a partir de los campos del archivo
        LocalDate fecha = LocalDate.parse(fechaStr, FMT_FECHA);
        int hh = Integer.parseInt(hhStr);
        int mm = Integer.parseInt(mmStr);
        this.fechaHoraRegistro = fecha.atTime(hh, mm);
    }

    // Minutos desde medianoche del día de registro → útil para comparar con horarios de vuelo
    public int getMinutosRegistro() {
        return fechaHoraRegistro.getHour() * 60 + fechaHoraRegistro.getMinute();
    }

    public boolean esLocal() {
        return origen.equals(destino);
    }

    public String getId()                          { return id; }
    public String getOrigen()                      { return origen; }
    public String getDestino()                     { return destino; }
    public LocalDateTime getFechaHoraRegistro()    { return fechaHoraRegistro; }
    public int getCantidad()                       { return cantidad; }
    public String getIdCliente()                   { return idCliente; }
    public boolean isEntregado()                   { return entregado; }
    public void setEntregado(boolean entregado)    { this.entregado = entregado; }

    @Override
    public String toString() {
        return "[" + id + "] " + origen + "→" + destino
             + " | " + fechaHoraRegistro + " | " + cantidad + " maletas"
             + " | Cliente: " + idCliente;
    }
}
