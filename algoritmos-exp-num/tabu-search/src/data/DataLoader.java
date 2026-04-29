package data;

import model.Aeropuerto;
import model.Envio;
import model.Vuelo;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
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
    public Map<String, Aeropuerto> cargarAeropuertos() throws IOException {
        Map<String, Aeropuerto> aeropuertos = new LinkedHashMap<>();

        // El archivo tiene BOM UTF-16 BE; Java lo lee correctamente con este charset.
        Charset utf16be = Charset.forName("UTF-16BE");

        // Patrón:  num   ICAO   ciudad/pais...   alias   GMT   capacidad   Latitude
        // Grupos:        (1)    (2) lazy          (3)     (4)   (5)
        Pattern patron = Pattern.compile(
            "^\\s*\\d+\\s+([A-Z]{4})\\s+(.+?)\\s{2,}[a-z]{4}\\s+([+-]\\d+)\\s+(\\d{3,4})\\s+Latitude",
            Pattern.UNICODE_CHARACTER_CLASS
        );

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaAeropuertos.toFile()), utf16be))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                // Limpiamos BOM residual y caracteres nulos que deja UTF-16
                linea = linea.replace("\uFEFF", "").replace("\u0000", "").trim();

                // Solo procesamos líneas que arrancan con dígitos (datos reales)
                if (linea.isEmpty() || !Character.isDigit(linea.charAt(0))) continue;

                Matcher m = patron.matcher(linea);
                if (!m.find()) continue;

                String codigo     = m.group(1);          // "SKBO"
                String ciudadPais = m.group(2).trim();   // "Bogota              Colombia"
                int    gmt        = parsearGmt(m.group(3)); // -5, +2, etc.
                int    capacidad  = Integer.parseInt(m.group(4)); // 430

                // Separar ciudad y país: están divididos por 2 o más espacios
                String[] partesCiudadPais = ciudadPais.split("\\s{2,}");
                String ciudad = partesCiudadPais[0].trim();
                String pais   = partesCiudadPais.length > 1 ? partesCiudadPais[1].trim() : "";

                String continente = determinarContinente(codigo);
                aeropuertos.put(codigo, new Aeropuerto(codigo, ciudad, pais, gmt, capacidad, continente));
            }
        }

        System.out.println("[DataLoader] Aeropuertos cargados: " + aeropuertos.size());
        return aeropuertos;
    }

    /**
     * Determina el continente a partir del prefijo ICAO.
     * S/M = América del Sur/Central, K/C = América del Norte,
     * E/L = Europa, O/U/V/W/R/Z/B = Asia.
     */
    private static String determinarContinente(String codigoIcao) {
        if (codigoIcao == null || codigoIcao.isEmpty()) return "DESCONOCIDO";
        switch (codigoIcao.charAt(0)) {
            case 'S': case 'M': case 'T': case 'K': case 'C': case 'N': return "AMERICA";
            case 'E': case 'L':                                          return "EUROPA";
            case 'O': case 'U': case 'V': case 'W': case 'R': case 'Z': case 'B': return "ASIA";
            case 'D': case 'F': case 'G': case 'H':                     return "AFRICA";
            default: return "DESCONOCIDO";
        }
    }

    /** Convierte "+2", "-5", "+3" → 2, -5, 3 */
    private int parsearGmt(String gmtStr) {
        gmtStr = gmtStr.trim();
        if (gmtStr.startsWith("+")) return Integer.parseInt(gmtStr.substring(1));
        return Integer.parseInt(gmtStr);
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
    public List<Vuelo> cargarVuelos() throws IOException {
        List<Vuelo> vuelos = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(rutaVuelos.toFile()), StandardCharsets.US_ASCII))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                // Formato: ORIG-DEST-HH:MM-HH:MM-CCCC
                String[] partes = linea.split("-");
                if (partes.length < 5) continue;

                String origen   = partes[0];                         // "SKBO"
                String destino  = partes[1];                         // "SEQM"
                int salida      = Vuelo.parsearHora(partes[2]);      // "03:34" → 214
                int llegada     = Vuelo.parsearHora(partes[3]);      // "04:21" → 261
                int capacidad   = Integer.parseInt(partes[4]);       // "0300"  → 300

                vuelos.add(new Vuelo(origen, destino, salida, llegada, capacidad));
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
    public List<Envio> cargarEnvios(int limite) throws IOException {
        List<Envio> todos = new ArrayList<>();

        // Listamos solo los archivos que siguen el patrón _envios_XXXX_.txt
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

    /** Sobrecarga sin límite: carga todos los envíos */
    public List<Envio> cargarEnvios() throws IOException {
        return cargarEnvios(-1);
    }

    /**
     * Carga hasta {@code limite} envíos por cada aeropuerto en {@code origenes}.
     * Solo procesa los archivos cuyo código ICAO esté en la lista.
     */
    public List<Envio> cargarEnvios(List<String> origenes, int limite) throws IOException {
        List<Envio> todos = new ArrayList<>();

        File[] archivos = directorioEnvios.toFile().listFiles(
            (dir, nombre) -> nombre.startsWith("_envios_") && nombre.endsWith("_.txt")
        );

        if (archivos == null || archivos.length == 0) {
            System.err.println("[DataLoader] No se encontraron archivos de envíos en: " + directorioEnvios);
            return todos;
        }

        for (File archivo : archivos) {
            String codigoOrigen = extraerCodigoOrigen(archivo.getName());
            if (codigoOrigen == null || !origenes.contains(codigoOrigen)) continue;

            int cargados = cargarEnviosDeArchivo(archivo, codigoOrigen, todos, limite);
            System.out.println("[DataLoader] " + archivo.getName() + " → " + cargados + " envíos");
        }

        System.out.println("[DataLoader] Total envíos cargados: " + todos.size());
        return todos;
    }

    /**
     * Carga solo los envíos cuya fecha de registro cae dentro de la ventana
     * [fechaInicio, fechaInicio + numDias).  Útil para la simulación de período.
     *
     * Ejemplo — simulación de 5 días a partir del 2026-01-01:
     *   loader.cargarEnviosPorPeriodo(LocalDate.of(2026, 1, 1), 5)
     *
     * El filtro escanea todos los archivos línea a línea sin cargar en RAM lo
     * que no pertenece al período, por lo que el consumo de memoria es proporcional
     * al tamaño del resultado, no al total del dataset.
     */
    public List<Envio> cargarEnviosPorPeriodo(LocalDate fechaInicio, int numDias) throws IOException {
        LocalDate fechaFin = fechaInicio.plusDays(numDias);
        List<Envio> todos = new ArrayList<>();

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

            int cargados = cargarEnviosDeArchivoPorPeriodo(archivo, codigoOrigen, todos, fechaInicio, fechaFin);
            System.out.printf("[DataLoader] %s → %d envíos (%s a %s)%n",
                archivo.getName(), cargados, fechaInicio, fechaFin.minusDays(1));
        }

        System.out.println("[DataLoader] Total envíos en período: " + todos.size());
        return todos;
    }

    private int cargarEnviosDeArchivoPorPeriodo(File archivo, String origen,
                                                 List<Envio> destino,
                                                 LocalDate desde, LocalDate hasta) throws IOException {
        int contador = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.US_ASCII))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                Envio envio = parsearLineaEnvio(linea, origen);
                if (envio == null) continue;

                LocalDate fecha = envio.getFechaHoraRegistro().toLocalDate();
                if (!fecha.isBefore(desde) && fecha.isBefore(hasta)) {
                    destino.add(envio);
                    contador++;
                }
            }
        }

        return contador;
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
                                      List<Envio> destino, int limite) throws IOException {
        int contador = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(archivo), StandardCharsets.US_ASCII))) {

            String linea;
            while ((linea = br.readLine()) != null) {
                if (limite >= 0 && contador >= limite) break;

                linea = linea.trim();
                if (linea.isEmpty()) continue;

                Envio envio = parsearLineaEnvio(linea, origen);
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
    private Envio parsearLineaEnvio(String linea, String origen) {
        String[] partes = linea.split("-");
        if (partes.length < 7) return null;

        try {
            String id         = partes[0]; // "000000001"
            String fecha      = partes[1]; // "20260102"
            String hh         = partes[2]; // "00"
            String mm         = partes[3]; // "47"
            String destino    = partes[4]; // "SUAA"
            String cantidad   = partes[5]; // "002"
            String idCliente  = partes[6]; // "0032535"

            return new Envio(id, origen, destino, fecha, hh, mm, cantidad, idCliente);
        } catch (Exception e) {
            System.err.println("[DataLoader] Línea malformada ignorada: " + linea);
            return null;
        }
    }
}
