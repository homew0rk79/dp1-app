package com.tasfb2b.dto;

public class RegistrarEnvioRequestDTO {
    private String origen;
    private String destino;
    private int cantidad;
    private String fechaHora;   // ISO-8601 local (ej. "2026-01-03T14:30") — null → ahora UTC
    private String idCliente;   // opcional

    public String getOrigen()       { return origen; }
    public String getDestino()      { return destino; }
    public int getCantidad()        { return cantidad; }
    public String getFechaHora()    { return fechaHora; }
    public String getIdCliente()    { return idCliente; }

    public void setOrigen(String origen)         { this.origen = origen; }
    public void setDestino(String destino)       { this.destino = destino; }
    public void setCantidad(int cantidad)        { this.cantidad = cantidad; }
    public void setFechaHora(String fechaHora)  { this.fechaHora = fechaHora; }
    public void setIdCliente(String idCliente)  { this.idCliente = idCliente; }
}
