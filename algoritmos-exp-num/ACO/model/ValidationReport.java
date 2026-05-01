package model;

public class ValidationReport {
    public int rutasFueraDePlazo = 0;
    public int vuelosSobrecapacidad = 0;
    public int almacenesSobrecapacidad = 0;

    public boolean isValid() {
        return rutasFueraDePlazo == 0 &&
               vuelosSobrecapacidad == 0 &&
               almacenesSobrecapacidad == 0;
    }
}