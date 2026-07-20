package com.tasfb2b.dto;

import java.util.List;

public class AnimacionManifestDTO {

    private int duracionTotalMinutos;
    private int fechaInicioMinutos;
    private List<OcurrenciaVueloDTO> ocurrencias;
    private List<AeropuertoManifestDTO> aeropuertos;

    /**
     * DIA_A_DIA: minuto simulado "actual" derivado del tiempo real transcurrido
     * (60x → 1 s real = 1 min simulado). Permite que un navegador que entra a
     * mitad de simulación ancle su animación al ahora. -1 en otros escenarios.
     */
    private int tiempoSimuladoActualMin = -1;

    /** DIA_A_DIA: intervalo del tick de replanificación en segundos. -1 en otros escenarios. */
    private int tickIntervaloSegundos = -1;

    public AnimacionManifestDTO(int duracionTotalMinutos,
                                 int fechaInicioMinutos,
                                 List<OcurrenciaVueloDTO> ocurrencias,
                                 List<AeropuertoManifestDTO> aeropuertos) {
        this.duracionTotalMinutos = duracionTotalMinutos;
        this.fechaInicioMinutos   = fechaInicioMinutos;
        this.ocurrencias          = ocurrencias;
        this.aeropuertos          = aeropuertos;
    }

    public int getDuracionTotalMinutos()             { return duracionTotalMinutos; }
    public int getFechaInicioMinutos()               { return fechaInicioMinutos; }
    public List<OcurrenciaVueloDTO> getOcurrencias() { return ocurrencias; }
    public List<AeropuertoManifestDTO> getAeropuertos() { return aeropuertos; }

    public int getTiempoSimuladoActualMin() { return tiempoSimuladoActualMin; }
    public int getTickIntervaloSegundos()   { return tickIntervaloSegundos; }

    public void setTiempoSimuladoActualMin(int v) { this.tiempoSimuladoActualMin = v; }
    public void setTickIntervaloSegundos(int v)   { this.tickIntervaloSegundos = v; }
}
