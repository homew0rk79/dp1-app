package model;

/**
 * Representa un vuelo (arco) entre dos aeropuertos en la red de transporte.
 * Cada vuelo tiene capacidad de carga, tiempo de traslado y frecuencia diaria.
 */
public class Flight {

    private final String id;
    private final String originId;
    private final String destinationId;
    private final int capacity;
    private final double travelTime;
    private final int dailyFrequency;
    private boolean cancelled;
    private int usedCapacity;

    /**
     * Crea un nuevo vuelo.
     *
     * @param id              identificador único del vuelo
     * @param originId        id del aeropuerto de origen
     * @param destinationId   id del aeropuerto de destino
     * @param capacity        capacidad máxima de maletas (150-400)
     * @param travelTime      tiempo de traslado en días (0.5 o 1.0)
     * @param dailyFrequency  número de vuelos por día (>= 1)
     */
    public Flight(String id, String originId, String destinationId,
                  int capacity, double travelTime, int dailyFrequency) {
        if (capacity < 150 || capacity > 400) {
            throw new IllegalArgumentException("Capacidad de vuelo debe estar entre 150 y 400. Recibido: " + capacity);
        }
        if (travelTime <= 0) {
            throw new IllegalArgumentException("Tiempo de traslado debe ser positivo. Recibido: " + travelTime);
        }
        if (dailyFrequency < 1) {
            throw new IllegalArgumentException("Frecuencia diaria debe ser >= 1. Recibido: " + dailyFrequency);
        }
        this.id = id;
        this.originId = originId;
        this.destinationId = destinationId;
        this.capacity = capacity;
        this.travelTime = travelTime;
        this.dailyFrequency = dailyFrequency;
        this.cancelled = false;
        this.usedCapacity = 0;
    }

    /** @return identificador único del vuelo */
    public String getId() { return id; }

    /** @return id del aeropuerto de origen */
    public String getOriginId() { return originId; }

    /** @return id del aeropuerto de destino */
    public String getDestinationId() { return destinationId; }

    /** @return capacidad máxima de maletas del vuelo */
    public int getCapacity() { return capacity; }

    /** @return tiempo de traslado en días */
    public double getTravelTime() { return travelTime; }

    /** @return frecuencia diaria del vuelo */
    public int getDailyFrequency() { return dailyFrequency; }

    /** @return true si el vuelo está cancelado */
    public boolean isCancelled() { return cancelled; }

    /** @return capacidad actualmente utilizada */
    public int getUsedCapacity() { return usedCapacity; }

    /** @return capacidad disponible restante */
    public int getAvailableCapacity() { return capacity - usedCapacity; }

    /**
     * Marca el vuelo como cancelado.
     */
    public void cancel() { this.cancelled = true; }

    /**
     * Reactiva un vuelo previamente cancelado.
     */
    public void reactivate() { this.cancelled = false; }

    /**
     * Reserva capacidad en el vuelo para un número de maletas.
     *
     * @param bags número de maletas a reservar
     * @throws IllegalStateException si no hay capacidad suficiente o el vuelo está cancelado
     */
    public void reserveCapacity(int bags) {
        if (cancelled) {
            throw new IllegalStateException("No se puede reservar en vuelo cancelado: " + id);
        }
        if (usedCapacity + bags > capacity) {
            throw new IllegalStateException(
                    "Capacidad insuficiente en vuelo " + id + ": disponible=" + getAvailableCapacity() + ", solicitado=" + bags);
        }
        this.usedCapacity += bags;
    }

    /**
     * Reinicia la capacidad utilizada a cero.
     */
    public void resetUsedCapacity() {
        this.usedCapacity = 0;
    }

    @Override
    public String toString() {
        return id + " [" + originId + " -> " + destinationId + ", cap=" + capacity
                + ", t=" + travelTime + "d, freq=" + dailyFrequency + "/d"
                + (cancelled ? ", CANCELADO" : "") + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Flight other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
