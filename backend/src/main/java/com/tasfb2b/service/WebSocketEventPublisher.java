package com.tasfb2b.service;

import com.tasfb2b.dto.MetricasDTO;
import com.tasfb2b.dto.ProgresoEventDTO;
import com.tasfb2b.dto.SnapshotEventDTO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketEventPublisher {

    private static final String TOPIC_PROGRESO  = "/topic/planificacion/progreso";
    private static final String TOPIC_SNAPSHOT  = "/topic/planificacion/snapshot";
    private static final String TOPIC_COMPLETADO = "/topic/planificacion/completado";

    private final SimpMessagingTemplate messaging;

    public WebSocketEventPublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    /** Progreso ligero: porcentaje + mensaje + costo actual */
    public void publicarProgreso(int porcentaje, String mensaje, String estado, double costoActual) {
        messaging.convertAndSend(TOPIC_PROGRESO,
            new ProgresoEventDTO(porcentaje, mensaje, estado, costoActual));
    }

    /** Snapshot completo: estado de aeropuertos + carga de rutas */
    public void publicarSnapshot(SnapshotEventDTO snapshot) {
        messaging.convertAndSend(TOPIC_SNAPSHOT, snapshot);
    }

    /** Planificación completada: métricas finales */
    public void publicarCompletado(MetricasDTO metricas) {
        messaging.convertAndSend(TOPIC_COMPLETADO, metricas);
    }
}
