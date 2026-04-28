package config;

/**
 * Representa un lote (envío) de maletas que debe trasladarse desde un aeropuerto
 * de origen a uno de destino dentro de un plazo máximo.
 */
public class Shipment {

    /** Estados posibles de un envío */
    public enum Status {
        PENDING, IN_TRANSIT, DELIVERED, FAILED
    }

    private final String id;
    private final String originId;
    private final String destinationId;
    private final int bagCount;
    private final double deadline;
    private Status status;

    /**
     * Crea un nuevo envío de maletas.
     *
     * @param id            identificador único del envío
     * @param originId      id del aeropuerto de origen
     * @param destinationId id del aeropuerto de destino
     * @param bagCount      cantidad de maletas en el lote
     * @param deadline      plazo máximo de entrega en días (1.0 mismo continente, 2.0 distinto)
     */
    public Shipment(String id, String originId, String destinationId, int bagCount, double deadline) {
        if (bagCount <= 0) {
            throw new IllegalArgumentException("Cantidad de maletas debe ser positiva. Recibido: " + bagCount);
        }
        if (deadline <= 0) {
            throw new IllegalArgumentException("Plazo de entrega debe ser positivo. Recibido: " + deadline);
        }
        this.id = id;
        this.originId = originId;
        this.destinationId = destinationId;
        this.bagCount = bagCount;
        this.deadline = deadline;
        this.status = Status.PENDING;
    }

    /** @return identificador del envío */
    public String getId() { return id; }

    /** @return id del aeropuerto de origen */
    public String getOriginId() { return originId; }

    /** @return id del aeropuerto de destino */
    public String getDestinationId() { return destinationId; }

    /** @return cantidad de maletas */
    public int getBagCount() { return bagCount; }

    /** @return plazo máximo en días */
    public double getDeadline() { return deadline; }

    /** @return estado actual del envío */
    public Status getStatus() { return status; }

    /**
     * Actualiza el estado del envío.
     *
     * @param status nuevo estado
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Shipment{" + id + ": " + originId + " -> " + destinationId
                + ", bags=" + bagCount + ", deadline=" + deadline + "d, status=" + status + "}";
    }
}
