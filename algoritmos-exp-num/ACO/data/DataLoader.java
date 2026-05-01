package data;

import model.Airport;
import model.Flight;
import model.Shipment;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Carga y parsea los tres tipos de archivos de datos del sistema:
 *
 *  1. Aeropuertos  → UTF-16 Big-Endian, formato en columnas con texto libre
 *  2. Planes de vuelo → ASCII, formato: ORIG-DEST-HH:MM-HH:MM-CCCC
 *  3. Envíos       → ASCII (uno por aeropuerto origen), formato:
 *                    XXXXXXXXX-AAAAMMDD-HH-MM-DEST-CCC-IIIIIII
 */
public class DataLoader {

    private final Path rutaAeropuertos;
    private final Path rutaVuelos;
    private final Path directorioEnvios;

    /**
     * @param rutaAeropuertos  ruta al .txt de aeropuertos (UTF-16BE)
     * @param rutaVuelos       ruta al planes_vuelo.txt
     * @param directorioEnvios directorio que contiene todos los _envios_XXXX_.txt
     */
    public DataLoader(String rutaAeropuertos, String rutaVuelos, String directorioEnvios) {
        this.rutaAeropuertos  = Paths.get(rutaAeropuertos);
        this.rutaVuelos       = Paths.get(rutaVuelos);
        this.directorioEnvios = Paths.get(directorioEnvios);
    }

    // -------------------------------------------------------------------------
    // 1. AEROPUERTOS
    // -------------------------------------------------------------------------

    /**
     * Parsea el archivo de aeropuertos (codificado en UTF-16 Big-Endian).
     *
     * Formato de cada línea de dato:
     *   01   SKBO   Bogota              Colombia        bogo    -5     430     Latitude: ...
     *
     * Las líneas de encabezado/sección (ej. "America del Sur.", "****", etc.)
     * se descartan: solo procesamos líneas que empiecen con un número de 2 dígitos.
     *
     * Estrategia de parseo:
     *   - Usamos regex para extraer los campos que SÍ tienen formato fijo:
     *     código ICAO (4 letras mayúsculas), GMT (±dígito(s)) y capacidad
     *     (el número de 3 dígitos inmediatamente antes de "Latitude").
     *   - Ciudad y país quedan entre el ICAO y el alias (4 letras minúsculas);
     *     como pueden tener espacios ("Arabia Saudita", "Santiago de Chile")
     *     los capturamos con un grupo lazy y los separamos por 2+ espacios.
     */
        public Map<String, Airport> cargarAeropuertos() throws IOException {
        Map<String, Airport> aeropuertos = new LinkedHashMap<>();

        Charset utf16be = Charset.forName("UTF-16BE");

        Pattern patron = Pattern.compile(
            "^\\s*\\d+\\s+([A-Z]{4})\\s+(.+?)\\s{2,}[a-z]{4}\\s+([+-]\\d+)\\s+(\\d{3,4})\\s+Latitude",
            Pattern.UNICODE_CHARACTER_CLASS
        );

        String continenteActual = "UNK";

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaAeropuertos.toFile()), utf16be))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.replace("\uFEFF", "").replace("\u0000", "").trim();

                if (linea.isEmpty()) continue;

                String lower = linea.toLowerCase();

                if (lower.contains("america")) continenteActual = "AM";
                else if (lower.contains("europa")) continenteActual = "EU";
                else if (lower.contains("asia")) continenteActual = "AS";

                if (!Character.isDigit(linea.charAt(0))) continue;

                Matcher m = patron.matcher(linea);
                if (!m.find()) continue;

                String codigo = m.group(1);
                String ciudadPais = m.group(2).trim();
                int capacidadOriginal = Integer.parseInt(m.group(4));

                String[] partesCiudadPais = ciudadPais.split("\\s{2,}");
                String ciudad = partesCiudadPais[0].trim();

                int capacidadACO = Math.max(500, Math.min(800, capacidadOriginal));

                aeropuertos.put(
                        codigo,
                        new Airport(codigo, ciudad, continenteActual, capacidadACO, 0)
                );
            }
        }

        System.out.println("[DataLoader] Aeropuertos cargados: " + aeropuertos.size());
        return aeropuertos;
    }


    // -------------------------------------------------------------------------
    // 2. PLANES DE VUELO
    // -------------------------------------------------------------------------

    /**
     * Parsea el archivo de planes de vuelo.
     *
     * Formato de cada línea:
     *   SKBO-SEQM-03:34-04:21-0300
     *   orig dest  salida llegada capacidad
     *
     * La capacidad viene con un cero a la izquierda (0300 = 300 maletas).
     * Los horarios se convierten a minutos desde medianoche para facilitar
     * la aritmética de tiempo en el algoritmo.
     */
        public List<Flight> cargarVuelos() throws IOException {
        List<Flight> vuelos = new ArrayList<>();
        int id = 1;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaVuelos.toFile()), StandardCharsets.US_ASCII))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                String[] partes = linea.split("-");
                if (partes.length < 5) continue;

                String origen = partes[0];
                String destino = partes[1];
                int salida = parsearHora(partes[2]);
                int llegada = parsearHora(partes[3]);
                int capacidad = Integer.parseInt(partes[4]);

                int duracionMin = llegada - salida;
                if (duracionMin <= 0) duracionMin += 24 * 60;

                double duracionDias = duracionMin / 1440.0;

                if (duracionDias <= 0) duracionDias = 0.5;

                String flightId = origen + "-" + destino + "-" + id++;

                vuelos.add(new Flight(
                        flightId,
                        origen,
                        destino,
                        capacidad,
                        duracionDias,
                        1
                ));
            }
        }

        System.out.println("[DataLoader] Vuelos cargados: " + vuelos.size());
        return vuelos;
    }

    // -------------------------------------------------------------------------
    // 3. ENVÍOS
    // -------------------------------------------------------------------------

    /**
     * Carga todos los archivos de envíos del directorio dado.
     *
     * Cada archivo se llama "_envios_XXXX_.txt" donde XXXX es el código ICAO
     * del aeropuerto origen. Ese código se extrae del nombre del archivo.
     *
     * Formato de cada línea:
     *   000000001-20260102-00-47-SUAA-002-0032535
     *   idEnvio   fecha    hh mm dest cant idCliente
     *
     * Con 330.000+ líneas por archivo se usa BufferedReader para no cargar
     * todo en memoria de golpe.
     *
     * @param limite máximo de envíos a cargar por archivo (-1 = todos).
     *               Útil para pruebas rápidas.
     */
        public List<Shipment> cargarEnvios(int limite) throws IOException {
        List<Shipment> todos = new ArrayList<>();

        File[] archivos = directorioEnvios.toFile().listFiles(
                (dir, nombre) -> nombre.startsWith("_envios_") && nombre.endsWith("_.txt")
        );

        if (archivos == null || archivos.length == 0) {
            System.err.println("[DataLoader] No se encontraron archivos de envíos en: " + directorioEnvios);
            return todos;
        }

        for (File archivo : archivos) {
            String codigoOrigen = extraerCodigoOrigen(archivo.getName());
            if (codigoOrigen == null) continue;

            int cargados = cargarEnviosDeArchivo(archivo, codigoOrigen, todos, limite);
            System.out.println("[DataLoader] " + archivo.getName() + " → " + cargados + " envíos");
        }

        System.out.println("[DataLoader] Total envíos cargados: " + todos.size());
        return todos;
    }

    public List<Shipment> cargarEnvios() throws IOException {
        return cargarEnvios(-1);
    }

    /**
     * Extrae el código ICAO del nombre del archivo.
     * "_envios_EBCI_.txt" → "EBCI"
     */
    private String extraerCodigoOrigen(String nombreArchivo) {
        // Patrón: _envios_XXXX_.txt  donde XXXX = 4 letras mayúsculas
        Pattern p = Pattern.compile("_envios_([A-Z]{4})_\\.txt");
        Matcher m = p.matcher(nombreArchivo);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Parsea línea a línea un archivo de envíos y agrega los objetos a la lista.
     * Retorna la cantidad de envíos añadidos desde este archivo.
     */
    private int cargarEnviosDeArchivo(File archivo, String origen,
                                  List<Shipment> destino, int limite) throws IOException {
        int contador = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.US_ASCII))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                if (limite >= 0 && contador >= limite) break;

                linea = linea.trim();
                if (linea.isEmpty()) continue;

                Shipment envio = parsearLineaEnvio(linea, origen);
                if (envio != null) {
                    destino.add(envio);
                    contador++;
                }
            }
        }

        return contador;
    }

    /**
     * Convierte una línea de texto en un objeto Envio.
     *
     * Formato esperado:
     *   000000001-20260102-00-47-SUAA-002-0032535
     *   [0]       [1]      [2][3][4]  [5] [6]
     *
     * Retorna null si la línea está malformada (para no cortar la carga completa).
     */
    private Shipment parsearLineaEnvio(String linea, String origen) {
        String[] partes = linea.split("-");
        if (partes.length < 7) return null;

        try {
            String id = origen + "-" + partes[0];
            String destino = partes[4];
            int cantidad = Integer.parseInt(partes[5]);

            return new Shipment(id, origen, destino, cantidad, 1.0);
        } catch (Exception e) {
            System.err.println("[DataLoader] Línea malformada ignorada: " + linea);
            return null;
        }
    }

    private int parsearHora(String hora) {
        String[] partes = hora.split(":");
        int hh = Integer.parseInt(partes[0]);
        int mm = Integer.parseInt(partes[1]);
        return hh * 60 + mm;
    }
}
