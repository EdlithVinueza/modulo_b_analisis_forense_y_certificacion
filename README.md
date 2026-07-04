```md
# Microservicio B: Análisis Forense y Certificación

## Propósito General

El propósito principal de este microservicio es orquestar el flujo de validación técnica, registro, firma y certificación de las obras digitales. Este microservicio garantiza la integridad de los archivos a través de un análisis forense (mediante la extracción y análisis de metadata, estructura y similitud visual), recopilando los datos del autor y generar un expediente que será firmado digitalmente con el certificado (.p12) del artista garantizando autoría y no repudio. Finalmente, el sistema inyecta un identificador único en la imagen mediante esteganografía y sella el certificado PDF con la firma del sistema bajo el estándar PAdES, permitiendo así verificar la autenticidad de cualquier obra en circulación directamente desde el archivo, conservando así también su integridad visual.

## Pasos del Análisis y Certificación

### Paso 1: Análisis de estructura y metadata de archivos PSD, PNG y JPEG

El sistema somete al archivo de trabajo original exportado a formato PSD y a la imagen final exportada (PNG o JPEG) a un proceso forense con tres pilares fundamentales para garantizar la autenticidad de la obra:

- **Análisis de Metadatos:** Recorremos los bloques de información oculta (EXIF, XMP, perfiles de color) para encontrar evidencias de manipulación o copiado.
- **Análisis Estructural:** Verificación de integridad a través de bytes, leyendo firmas binarias y decodificando la arquitectura interna (como las capas de Photoshop).
- **Comparación Visual (pHash):** Verificación matemática algorítmica para asegurar que la imagen final provenga indiscutiblemente del lienzo de trabajo aportado.

#### 1.1 Extracción de metadatos

Se usa la librería `metadata-extractor` con una arquitectura de **extractores especializados e inyectables**, esto nos permite recorrer los directorios de metadata de manera modular, además de ayudarnos a recolectar la mayor cantidad de datos posible si en dado caso los archivos no tuvieran algún formato específico de metadatos. Para procesar PSD y las imágenes se utilizan servicios dedicados que aplican los extractores en un orden de prioridad específico (estrategia Last-Win o sobrescritura sucesiva).

```java
public class MetadatosImagenService {
    private final List<MetadataExtractor<MetadatosImagen.MetadatosImagenBuilder>> extractores;

    public MetadatosImagenService() {
        this.extractores = Arrays.asList(
            new FileType(), // Identifica formato real
            new Png(),      // Datos estructurales PNG (pHYs, gAMA, sRGB)
            new Jpeg(),     // Dimensiones JPG
            new Jfif(),     // Densidad de píxeles (DPI) en APP0
            new Exif(),     // Respaldo de DPI y metadatos de hardware
            new Icc()       // Perfil de color incrustado
        );
    }
    // ...
}
```

**Orquestación de extractores para el lienzo de trabajo (PSD):**

```java
public class MetadatosPSDService {
    private final List<MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder>> extractors;

    public MetadatosPSDService() {
        // Ordenados de MENOS fiable a MÁS fiable (Last-Win)
        this.extractors = Arrays.asList(
            new Jfif(),      // Muy genérico (prioridad baja)
            new Exif(),      // Datos de cámara/motor (prioridad media)
            new Iptc(),      // Datos de prensa (prioridad media)
            new Xmp(),       // Datos Adobe (prioridad alta)
            new Photoshop(), // Recursos específicos (prioridad alta)
            new PsdHeader(), // LA VERDAD BINARIA (prioridad máxima)
            new FileType()   // Identificación final
        );
    }

    public MetadatosPSD procesarArchivo(File archivo) {
        MetadatosPSD.MetadatosPSDBuilder builder = MetadatosPSD.builder();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(archivo);
            for (MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> extractor : extractors) {
                try {
                    extractor.extraer(metadata, builder);
                } catch (Exception ex) {
                    System.err.println("Advertencia en PSD: El extractor " + extractor.getClass().getSimpleName() + " falló.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error crítico: " + e.getMessage());
        }
        return builder.build();
    }
}
```

Cada extractor implementa la interfaz `MetadataExtractor`, por ejemplo, el análisis más crítico para un archivo PSD es la extracción de su encabezado binario (Verdad Binaria) para conocer su estructura real de capas, dimensiones, bits por canal y modo de color (RGB, CMYK, etc.):

**Ejemplo de Extractor Estructural (PsdHeader):**

```java
public class PsdHeader implements MetadataExtractor<MetadatosPSD.MetadatosPSDBuilder> {
    @Override
    public void extraer(Metadata metadata, MetadatosPSD.MetadatosPSDBuilder builder) {
        PsdHeaderDirectory directory = metadata.getFirstDirectoryOfType(PsdHeaderDirectory.class);
        if (directory != null) {
            builder.cantidadCanales(directory.getInteger(PsdHeaderDirectory.TAG_CHANNEL_COUNT))
                   .altoImagen(directory.getInteger(PsdHeaderDirectory.TAG_IMAGE_HEIGHT))
                   .anchoImagen(directory.getInteger(PsdHeaderDirectory.TAG_IMAGE_WIDTH))
                   .bitsPorCanal(directory.getInteger(PsdHeaderDirectory.TAG_BITS_PER_CHANNEL))
                   .modoColor(directory.getDescription(PsdHeaderDirectory.TAG_COLOR_MODE));
        }
    }
}
```

Entre los principales formatos de metadatos que se analizaron tenemos los siguientes:

| **Extractor**   | **Aplica a** | **Información obtenida** |
|-----------------|--------------|---------------------------|
| **FileType**    | PNG / JPEG   | Formato real y extensión detectada |
| **Png**         | PNG          | Chunks pHYs (resolución), gAMA, sRGB |
| **Jfif**        | JPEG         | DPI declarados en el segmento APP0 |
| **Exif**        | PNG / JPEG   | DPI de respaldo (segmento APP1) |
| **Icc**         | PNG / JPEG   | Perfil de color embebido |
| **Xmp**         | PSD          | Metadatos Adobe (autor, software, fecha) |
| **Photoshop**   | PSD          | Recursos específicos de Photoshop |
| **PsdHeader**   | PSD          | **Verdad binaria**: dimensiones, canales, modo de color |

*Tab. 9. Formatos de Metadatos Analizados*

#### 1.2 Firma binaria (Números Mágicos)

Antes de cualquier procesamiento, se leen los primeros bytes del archivo y se comparan contra las firmas hexadecimales conocidas de cada formato, detectando así si los archivos fueron renombrados o adulterados.

```java
public class NumerosMagicos {
    // Firmas hexadecimales
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PSD_SIGNATURE = {0x38, 0x42, 0x50, 0x53}; // "8BPS"

    public static String detectarFormatoReal(File archivo) {
        byte[] encabezado = new byte[8];
        try (FileInputStream fis = new FileInputStream(archivo)) {
            if (fis.read(encabezado) < 4) return "DESCONOCIDO";
            if (compararBytes(encabezado, PSD_SIGNATURE, 4)) return "PSD";
            if (compararBytes(encabezado, PNG_SIGNATURE, 8)) return "PNG";
            if (compararBytes(encabezado, JPEG_SIGNATURE, 3)) return "JPEG";
        } catch (IOException e) {
            return "ERROR_LECTURA";
        }
        return "OTRO";
    }

    private static boolean compararBytes(byte[] a, byte[] b, int n) {
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
```

#### 1.3 Análisis Estructural de la Resolución Física (Imágenes)

Además de la firma binaria, hacemos un análisis estructural de las imágenes que incluye la extracción de la Densidad de Píxeles (DPI) leyendo directamente el flujo de bytes. Esto nos permite auditar la resolución real sin depender exclusivamente de librerías de metadatos, ya que estos pueden ser alterados o simplemente ser inexistentes.

La clase `ResolucionFisica` escanea los chunks estructurales de los archivos PNG (buscando el bloque `pHYs`) y los marcadores de los archivos JPEG (buscando el segmento APP0 / JFIF), extrayendo matemáticamente la resolución base de la ilustración. Esta información nos permite determinar si la imagen proviene de internet, de una captura de pantalla, o si tiene una buena resolución (característica relevante de imágenes exportadas directamente desde un programa de dibujo o diseño).

```java
public class ResolucionFisica {
    // Ejemplo de escaneo estructural para formato PNG
    private static int[] buscarDpiPng(DataInputStream dis) throws IOException {
        dis.skipBytes(8); // Saltar Firma Binaria del PNG
        while (dis.available() > 0) {
            int length = dis.readInt();
            byte[] type = new byte[4];
            dis.readFully(type);
            if ("pHYs".equals(new String(type))) {
                int x = dis.readInt();
                int y = dis.readInt();
                if (dis.readByte() == 1) { // 1 = Unidad en Metros
                    // Conversión matemática de píxeles/metro a píxeles/pulgada (DPI)
                    return new int[]{ (int)Math.round(x * 0.0254), (int)Math.round(y * 0.0254) };
                }
                break;
            }
            dis.skipBytes(length + 4); // Saltar a siguiente chunk (Datos + CRC32)
        }
        return new int[]{72, 72}; // Valor web por defecto si no existe bloque pHYs
    }
    // ... lógica similar para JPEG (buscando el segmento APP0) ...
}
```

#### 1.4 Extracción de la Estructura Interna del PSD (Capas)

Mientras que los metadatos describen el archivo, la **estructura de capas** es lo que realmente demuestra el proceso creativo de una persona. Para analizarlas, el sistema lee el archivo PSD byte a byte utilizando saltos estratégicos (skipExacto) para llegar al bloque relevante como: Layer and Mask Information, donde extrae la posición, opacidad, modos de fusión (blendMode) y efectos adicionales (máscaras, filtros) aplicados a cada capa.

**Ejemplo de análisis de bytes en ExtractorCapasPSD:**

```java
// ... saltando hasta el bloque de información de capas
long layerInfoLen = isPsb ? dis.readLong() : readUint32(dis);
short layerCountRaw = dis.readShort();
int layerCount = Math.abs(layerCountRaw);
for (int i = 0; i < layerCount; i++) {
    // 1. Extraer dimensiones espaciales de la capa (Bounding Box)
    int top = dis.readInt();
    int left = dis.readInt();
    int bottom = dis.readInt();
    int right = dis.readInt();
    // 2. Extraer modo de fusión ("norm", "mul", "scrn") y opacidad
    skipExacto(dis, ...); // Saltar datos de canal
    skipExacto(dis, 4);   // Firma "8BIM"
    byte[] blendBytes = new byte[4];
    dis.readFully(blendBytes);
    int opacity = dis.readUnsignedByte();
    // 3. Extraer metadatos adicionales (efectos y tipo de capa)
    ExtraParseado ep = parsearExtra(extraBytes);
    capas.add(EstructuraCapaPSD.builder()
            .nombre(ep.nombre)
            .blendModeKey(new String(blendBytes).trim())
            .tieneEfectos(ep.tieneEfectos)
            .ancho(right - left)
            .alto(bottom - top)
            // ...
            .build());
}
```

#### 1.5 Reglas de validación forense

Con la información estructural ya extraída, la arquitectura inyecta una serie de clases que implementan la interfaz `IReglaValidacion`. Cada una de estas reglas analiza un posible fraude basándose en los datos crudos extraídos de la estructura de nuestros archivos analizados.

| **Regla** | **Archivo** | **Detección** |
|-----------|-------------|---------------|
| **Firma Estructural** | PNG/JPEG | Formato binario ≠ extensión declarada → fraude |
| **Coherencia de Resolución** | PNG/JPEG | DPI en metadatos ≠ DPI en bytes → manipulación |
| **Análisis de Origen** | PNG/JPEG | 72 DPI sin ICC → internet · 90-125 DPI + sRGB → pantalla · ≥300 DPI → válida |
| **Formato PSD** | PSD | Confirma firma 8BPS en los bytes |
| **Resolución Profesional** | PSD | DPI < 150 → no apto para impresión |
| **Imagen Pegada** | PSD | Capa única que cubre el lienzo sin edición técnica → fraude |

*Tab. 10. Reglas Principales de Validación*

Por ejemplo, la regla establecida en: **ReglaImagenPegada** usa la estructura de capas obtenida anteriormente para determinar si una persona simplemente pegó una imagen de internet sin editarla en un archivo PSD, para así pasar desapercibido los requisitos solicitados en la interfaz web. Esta regla demuestra cómo la validación forense se acopla directamente a los resultados del análisis estructural:

```java
public class ReglaImagenPegada implements IReglaValidacion<ArchivoPSD> {
    @Override
    public ResultadoValidacion validar(ArchivoPSD psd) {
        if (psd.getCapas().size() > 5) {
            return ResultadoValidacion.builder().esValido(true).mensaje("Estructura compleja detectada.").build();
        }
        int lienzoW = psd.getMetadatos().getAnchoImagen();
        int lienzoH = psd.getMetadatos().getAltoImagen();
        for (EstructuraCapaPSD capa : psd.getCapas()) {
            // Si la capa cubre todo el lienzo y no tiene efectos ni modos de fusión
            if (capa.getAncho() == lienzoW && capa.getAlto() == lienzoH) {
                if (!capa.isTieneMascaraCapa() && !capa.isTieneEfectos() &&
                    !capa.isEsClippingMask() && "norm".equals(capa.getBlendModeKey())) {
                    return ResultadoValidacion.builder()
                            .esValido(false)
                            .mensaje("Fraude: Se detectó una capa única que cubre todo el lienzo sin edición técnica.")
                            .build();
                }
            }
        }
        return ResultadoValidacion.builder().esValido(true).mensaje("OK").build();
    }
}
```

#### 1.6 Comparación Visual (pHash) y Hashes Criptográficos

Como último paso de nuestro análisis forense, una vez que la estructura y los metadatos han sido validados por las reglas que establecimos, el sistema debe garantizar que la imagen exportada (PNG/JPEG) corresponda visualmente al archivo de trabajo (PSD) entregado, por lo que generamos un *Perceptual Hash (pHash)* de ambos archivos, el cual extrae las características visuales y calcula su grado de similitud algorítmica. El sistema exige un **umbral mínimo del 95%** de similitud, tolerando alteraciones insignificantes, propias de la compresión (como pasar de PSD a JPEG) o por configuraciones de color, pero evitando el fraude de la obra subiendo el archivo fuente de otra.

```java
// Verificación algorítmica de similitud visual (pHash ≥ 95%)
double similitud = calcPHash.compararSimilitud(
    calcPHash.generarHash(imgPSD), calcPHash.generarHash(imgImagen));
if (similitud < 95.0) {
    throw new RuntimeException("Rechazado: La imagen exportada no coincide visualmente con el lienzo del PSD (Similitud: " + similitud + "%).");
}
// Generación de huellas criptográficas inmutables (SHA-512) para el expediente
contexto.setSha512PSD(hashPort.calcularSHA512(Files.readAllBytes(psdFile.toPath())));
contexto.setSha512Imagen(hashPort.calcularSHA512(Files.readAllBytes(imgFile.toPath())));
```

### Paso 2: Toma de datos adicionales de la obra

Recopilamos los datos más relevantes de la obra (título, descripción, software, hardware y categoría) y las declaraciones del autor (titularidad de derechos y aceptación de términos), construyendo así los objetos de dominio `Autor`, `Obra` y `Declaraciones` que conformarán en la segunda fase del expediente.

```java
Autor autor = Autor.builder()
    .nombres(usuarioDb.nombres).apellidos(usuarioDb.apellidos)
    .cedula(usuarioDb.cedula).correo(usuarioDb.correo).build();

Obra obra = Obra.builder()
    .titulo((String) body.get("titulo_obra"))
    .descripcion((String) body.get("descripcion"))
    .software((String) body.get("software"))
    .hardware((String) body.get("hardware"))
    .categoria(cat).fechaCreacion(LocalDate.now()).build();
```

### Paso 3: Generación del expediente

Con toda la información recopilada en los pasos anteriores, se construye un objeto **Expediente** y se serializa a JSON (pretty-print). Este documento es nuestra fuente de verdad y será firmado por el artista.

```java
ExpedienteService expedienteServ = new ExpedienteService();
Expediente expediente = expedienteServ.construir(contexto);
String expedienteJson = new GsonBuilder()
    .setPrettyPrinting().create().toJson(expediente);
```

**Ejemplo de JSON del expediente generado:**

```json
{
  "idExpediente": "EXP-2026-000047",
  "fechaRegistro": "2026-06-30T23:00:00Z",
  "autor": {
    "nombres": "María Fernanda",
    "apellidos": "Lozano Vega",
    "cedula": "1723456789",
    "correo": "mflozano@gmail.com",
    "seudonimo": "MFArt"
  },
  "obra": {
    "titulo": "Colibrí Digital",
    "descripcion": "Ilustración digital de fauna andina del Ecuador",
    "software": "Adobe Photoshop 2024",
    "hardware": "Wacom Intuos Pro",
    "categoria": "ILUSTRACION",
    "fechaCreacion": "2026-06-30"
  },
  "analisis": {
    "resultado": "APROBADO",
    "capasPSD": 24,
    "metadatosDetectados": true,
    "dimensiones": "3508x4961 px",
    "detallesTecnicos": "DPI: 300 | Modo color: RGB | Similitud pHash: 98.7%"
  },
  "hashes": {
    "sha512PSD": "a3f1c8e2b74d...9f2e01c",
    "sha512Imagen": "d92b47fa1c3e...8a10d7b",
    "pHash": "f8c0e3a1b5d27490"
  }
}
```

### Paso 4: Firma del expediente con el certificado del artista

El JSON del expediente es firmado digitalmente usando el archivo .p12 del artista, recuperado a través de la **[API de gestión de claves de usuarios]**. Se aplica el algoritmo SHA512withRSA: el hash SHA-512 del JSON se cifra con la clave privada de la artista extraída del keystore PKCS#12. El resultado vincula legalmente al autor con el contenido exacto del expediente (no repudio).

```java
// Comunicación con el adaptador de firma → API .p12 del usuario
FirmadorExpedientePort firmadorExp = new FirmadorP12Adapter();
FirmaAutorService firmaServ = new FirmaAutorService(firmadorExp);
firmaServ.validar(p12File, password); // Valida que el .p12 sea legítimo
FirmaAutor firma = firmaServ.firmar(expedienteJson, p12File, password);
// Resultado: firma SHA512withRSA del expediente, codificada en Base64
```

### Paso 5: Generación del certificado PDF y adjunto de datos

Se genera un PDF del certificado (utilizando plantillas Thymeleaf y html2pdf), el cual incluye visualmente los datos de la obra, su autor y un código QR con información relevante. El JSON del expediente firmado por el autor se **incrusta físicamente dentro de la estructura interna del archivo PDF como un archivo adjunto** (`expediente-firmado.json`). Además, se inyecta XMP Metadata en el documento para su correcta indexación.

```java
// Contenido a certificar: expediente JSON + firma del autor en Base64
String expedienteFirmadoJson = expedienteJson + "\n---FIRMA---\n" + firma.getFirmaBase64();

// 1. Generación del documento visual PDF
PdfDocument pdf = new PdfDocument(writer);

// 2. Inserción del JSON firmado como un archivo incrustado en el PDF
PdfFileSpec adjunto = PdfFileSpec.createEmbeddedFileSpec(
    pdf,
    expedienteFirmadoJson.getBytes(StandardCharsets.UTF_8),
    "Expediente Firmado Verisart",
    "expediente-firmado.json",
    null,
    new PdfName("application/json")
);
pdf.addFileAttachment("expediente-firmado.json", adjunto);

// 3. Inserción de metadatos XMP y propiedades del documento
PdfDocumentInfo info = pdf.getDocumentInfo();
info.setTitle("Certificado Verisart --- " + certificado.getIdCertificado());
info.setSubject("Certificado de Autenticidad Digital");
info.setKeywords("idCertificado=" + certificado.getIdCertificado()
    + "; idExpediente=" + certificado.getIdExpediente()
    + "; hash=" + certificado.getHashExpedienteFirmado());
info.setCreator("Sistema Verisart --- UCE");
```

Además del archivo adjunto (que es lo más crítico), **en la propia metadata del archivo PDF (XMP / Propiedades del documento)** se insertan los siguientes datos estándar:

- **Título (Title):** El nombre oficial, ej. Certificado Verisart --- CERT-2026-000047.
- **Asunto (Subject):** Certificado de Autenticidad Digital.
- **Palabras Clave (Keywords):** Contiene la tríada de verificación inmutable: `idCertificado`, `idExpediente` y el hash criptográfico del expediente.
- **Creador (Creator):** Sistema Verisart --- UCE.

**Ejemplo del texto exacto que se inserta como adjunto (`expediente-firmado.json`):**

El contenido incrustado es la concatenación del JSON del expediente y la firma criptográfica en Base64, que se encuentran separados por la cadena `---FIRMA---`, para facilitar su separación y análisis comparatorio. Este es el contenido exacto que un auditor vería si extrae el archivo adjunto del PDF:

```json
{
  "idExpediente": "EXP-2026-000047",
  "fechaRegistro": "2026-06-30T23:00:00Z",
  "autor": {
    "nombres": "María Fernanda",
    "apellidos": "Lozano Vega",
    "cedula": "1723456789",
    "correo": "mflozano@gmail.com",
    "seudonimo": "MFArt"
  },
  "obra": {
    "titulo": "Colibrí Digital",
    "descripcion": "Ilustración digital de fauna andina del Ecuador",
    "software": "Adobe Photoshop 2024",
    "hardware": "Wacom Intuos Pro",
    "categoria": "ILUSTRACION",
    "fechaCreacion": "2026-06-30"
  },
  "analisis": {
    "resultado": "APROBADO",
    "capasPSD": 24,
    "metadatosDetectados": true,
    "dimensiones": "3508x4961 px",
    "detallesTecnicos": "DPI: 300 | Modo color: RGB | Similitud pHash: 98.7%"
  },
  "hashes": {
    "sha512PSD": "a3f1c8e2b74d49b531...9f2e01c",
    "sha512Imagen": "d92b47fa1c3e03...8a10d7b",
    "pHash": "f8c0e3a1b5d27490"
  }
}
---FIRMA---
MIICdgYJKoZIhvcNAQcCoIICZzCCAmMCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3DQEHATGCAY0w
ggGJAgEBMIGhMIGbMQswCQYDVQQGEwJFQzEQMA4GA1UECAwHUGljaGluY2hhMREwDwYDVQQHDAhR
dWl0bzENMAsGA1UECgwEVUNFMQ4wDAYDVQQLDAVGRVZDMSEwHwYDVQQDDBhDZXJ0aWZpY2Fkb3Ig
Rm9yZW5zZSBVUEUxIjAgBgkqhkiG9w0BCQEWE2FkbWluQGNlcnRpZmljYS5lYwIJAKK1...
```

**Verificación de la inserción**

Para comprobar que el sistema backend en Java está insertando correctamente este JSON en el PDF generado, se desarrolló un proyecto de prueba en Python (explicado en la sección pruebas más adelante). Este código extrae el adjunto para validar que la información viaja correctamente con el archivo.

### Paso 6: Inserción de JSON en la metadata estructural de la imagen

El sistema **no modifica los píxeles de la imagen, porque se debe respetar la integridad visual de la obra**. En su lugar, aprovecha los mecanismos de extensión propios de cada formato de imagen para insertar datos directamente en la estructura binaria del archivo.

**El Payload (JSON) insertado en las imágenes**

Es importante destacar que **el JSON inyectado en las imágenes es totalmente distinto al del PDF.** Mientras que el archivo adjunto del PDF almacena el "expediente completo" junto con la firma criptográfica en Base64, la imagen almacena únicamente un pequeño *payload esteganográfico*. No inyectamos el expediente JSON completo porque incrementaríamos el peso de la imagen, además de exceder la capacidad de ciertos bloques estructurales. Por lo tanto, en la imagen solo insertamos el identificador y la huella inmutable del expediente, que es nada más que el **Hash SHA-512 del expediente completo firmado, que ya generamos antes:**

```json
{
  "id": "CERT-2026-000047",
  "hash": "d92b47fa1c3e...8a10d7b"
}
```

A continuación, detallaremos cómo se almacena este payload en los diferentes formatos de imagen que puede manejar nuestro sistema:

#### En archivos PNG — Chunk tEXt personalizado

El formato PNG está compuesto por una secuencia de **chunks** (bloques de datos). El sistema inserta un chunk de tipo `tEXt` con la clave `verisart-cert`, ubicándolo justo antes del chunk de cierre IEND. Cada chunk incluye su propio **CRC32** para verificar integridad.

```java
// Construir el chunk tEXt con CRC32
String contenido = "verisart-cert\0" + jsonCertificacion;
byte[] datos = contenido.getBytes(StandardCharsets.ISO_8859_1);
CRC32 crc = new CRC32();
crc.update("tEXt".getBytes());
crc.update(datos);
// Ensamblar: imagen original hasta IEND + nuevo chunk + IEND
baos.write(imagenOriginal, 0, posicionIEND); // todo antes del cierre
baos.write(chunkData);                       // chunk con el JSON
baos.write(imagenOriginal, posicionIEND, imagenOriginal.length - posicionIEND);
```

#### En archivos JPEG — Segmento APP11 (0xFF 0xEB)

El formato JPEG usa segmentos marcados. El sistema inserta un segmento en APP11 (marcador FF EB), el cual está reservado para uso privado y separado del segmento APP1 donde reside el EXIF, inmediatamente después del marcador de inicio SOI (FF D8).

```java
// SOI original (primeros 2 bytes intactos)
baos.write(imagenOriginal, 0, 2);
// Insertar APP11 (FF EB) con el JSON prefijado
baos.write(0xFF);
baos.write(0xEB);                  // marcador APP11
baos.write((longitudSegmento >> 8) & 0xFF);  // longitud alta
baos.write(longitudSegmento & 0xFF);         // longitud baja
baos.write(("verisart-cert:" + json).getBytes(StandardCharsets.UTF_8));
// Resto del JPEG sin modificar
baos.write(imagenOriginal, 2, imagenOriginal.length - 2);
```

**Verificación de la inserción en imágenes**

Al igual que el PDF, para comprobar de forma simple que nuestro sistema en Java está inyectando correctamente el JSON sin corromper la imagen, en el mismo proyecto de prueba en Python (explicado en la sección pruebas más adelante), lee los bytes de la imagen para asegurar que el *payload* se insertó en el lugar correcto.

### Paso 7: Sellado del PDF con firma institucional (PAdES) y empaquetado final

El PDF del certificado (que ya contiene el expediente completo en formato JSON incrustado y firmado por el autor) es sellado criptográficamente con el certificado raíz institucional (Root CA) **[mediante la API del sistema]**. Se utiliza el estándar **PAdES** (*PDF Advanced Electronic Signatures*), que incrusta la firma dentro del propio PDF. Cualquier alteración posterior al sellado invalida automáticamente la firma.

```java
// Comunicación con el adaptador de firma institucional
FirmadorPDFPort firmadorPDF = new FirmadorPDFAdapter(RUTA_ROOT_CA);
byte[] pdfFirmado = firmadorPDF.firmarPDF(pdfSinFirmar, PASS_CA); // Sello institucional
```

Para finalizar todo el material se **empaqueta en un archivo .zip** que se entrega al usuario final. Este paquete contiene tanto la obra certificada (con el JSON inyectado en su estructura) como el PDF del certificado firmado por el sistema (con el expediente del autor incrustado).

```java
// Generación del paquete ZIP descargable
ByteArrayOutputStream baosZip = new ByteArrayOutputStream();
ZipOutputStream zos = new ZipOutputStream(baosZip);

// 1. Añadir el certificado PDF firmado
ZipEntry pdfEntry = new ZipEntry(certificado.getIdCertificado() + ".pdf");
zos.putNextEntry(pdfEntry);
zos.write(pdfFirmado);
zos.closeEntry();

// 2. Añadir la imagen final certificada (con el JSON estructural)
String imgExt = rutaImagen.endsWith(".jpg") ? ".jpg" : ".png";
ZipEntry imgEntry = new ZipEntry(certificado.getIdCertificado() + "-obra-certificada" + imgExt);
zos.putNextEntry(imgEntry);
zos.write(imagenCertificada);
zos.closeEntry();

zos.close();
return baosZip.toByteArray(); // Retorna los bytes del archivo .zip
```