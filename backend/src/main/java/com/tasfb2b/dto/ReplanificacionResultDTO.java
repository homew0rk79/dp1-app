package com.tasfb2b.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ReplanificacionResultDTO {
    private int enviosAfectados;
    private int enviosReasignados;
    private int enviosSinRuta;
    private String mensaje;
    private String envioSolicitanteId;
    private List<String> enviosAfectadosIds;
    private List<String> enviosCancelados;

    public ReplanificacionResultDTO(int enviosAfectados, int enviosReasignados, int enviosSinRuta, String mensaje) {
        this(enviosAfectados, enviosReasignados, enviosSinRuta, mensaje, Collections.emptyList());
    }

    public ReplanificacionResultDTO(int enviosAfectados, int enviosReasignados, int enviosSinRuta,
                                    String mensaje, List<String> enviosCancelados) {
        this(enviosAfectados, enviosReasignados, enviosSinRuta, mensaje, null, Collections.emptyList(), enviosCancelados);
    }

    public ReplanificacionResultDTO(int enviosAfectados, int enviosReasignados, int enviosSinRuta,
                                    String mensaje, String envioSolicitanteId,
                                    List<String> enviosAfectadosIds, List<String> enviosCancelados) {
        this.enviosAfectados = enviosAfectados;
        this.enviosReasignados = enviosReasignados;
        this.enviosSinRuta = enviosSinRuta;
        this.mensaje = mensaje;
        this.envioSolicitanteId = envioSolicitanteId;
        this.enviosAfectadosIds = enviosAfectadosIds != null ? enviosAfectadosIds : Collections.emptyList();
        this.enviosCancelados = enviosCancelados != null ? enviosCancelados : Collections.emptyList();
    }
}
