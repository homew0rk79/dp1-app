package com.tasfb2b.planner.aco;

/**
 * Representa un aeropuerto (nodo) en la red de transporte.
 * Cada aeropuerto tiene una capacidad de almacén limitada (500-800 maletas)
 * y un stock actual que varía durante la planificación.
 */
public class Airport {

    private final String id;
    private final String name;
    private final String continent;
    private final int warehouseCapacity;
    private int currentStock;

    /**
     * Crea un nuevo aeropuerto.
     *
     * @param id                 identificador único del aeropuerto (e.g. "LIM", "MAD")
     * @param name               nombre descriptivo
     * @param continent          continente al que pertenece (e.g. "SA", "EU")
     * @param warehouseCapacity  capacidad máxima del almacén en maletas (500-800)
     * @param currentStock       stock actual de maletas en almacén
     * @throws IllegalArgumentException si la capacidad está fuera del rango permitido
     */
    public Airport(String id, String name, String continent, int warehouseCapacity, int currentStock) {
        if (warehouseCapacity < 500 || warehouseCapacity > 800) {
            throw new IllegalArgumentException(
                    "Capacidad de almacén debe estar entre 500 y 800. Recibido: " + warehouseCapacity);
        }
        if (currentStock < 0 || currentStock > warehouseCapacity) {
            throw new IllegalArgumentException(
                    "Stock actual (" + currentStock + ") debe estar entre 0 y " + warehouseCapacity);
        }
        this.id = id;
        this.name = name;
        this.continent = continent;
        this.warehouseCapacity = warehouseCapacity;
        this.currentStock = currentStock;
    }

    /** @return identificador único del aeropuerto */
    public String getId() { return id; }

    /** @return nombre descriptivo */
    public String getName() { return name; }

    /** @return continente del aeropuerto */
    public String getContinent() { return continent; }

    /** @return capacidad máxima del almacén */
    public int getWarehouseCapacity() { return warehouseCapacity; }

    /** @return stock actual de maletas */
    public int getCurrentStock() { return currentStock; }

    /**
     * Actualiza el stock actual de maletas en el almacén.
     *
     * @param stock nuevo valor de stock
     * @throws IllegalArgumentException si el stock es negativo o excede la capacidad
     */
    public void setCurrentStock(int stock) {
        if (stock < 0 || stock > warehouseCapacity) {
            throw new IllegalArgumentException(
                    "Stock (" + stock + ") fuera de rango [0, " + warehouseCapacity + "]");
        }
        this.currentStock = stock;
    }

    /**
     * Calcula el espacio disponible en el almacén.
     *
     * @return número de maletas que aún caben
     */
    public int getAvailableSpace() {
        return warehouseCapacity - currentStock;
    }

    @Override
    public String toString() {
        return id + " (" + name + ", " + continent + ") [" + currentStock + "/" + warehouseCapacity + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Airport other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
