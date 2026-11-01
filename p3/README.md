# Práctica 3 – Wrapper web para la aplicación BASIC/MS-DOS

Java 17+ y Maven (solo para compilar: el ZIP lleva su propio Java). `java.awt.Robot` (teclado y captura) + lector de pantalla por plantillas (y Tess4J como motor
alternativo) + `com.sun.net.httpserver`.


## Cómo ejecutarlo

1. Descomprimir el ZIP de la entrega. **No hay que instalar nada**: trae su propio Java en `jre\` y el jar compilado.
2. Doble clic en **`arrancar.bat`**: abre DOSBox con la aplicación, arranca el servidor y abre el navegador en
   `http://localhost:8090/`. Si el puerto 8090 está ocupado, lo dice (se cambia en `config.properties`).
3. No tocar teclado ni ratón mientras la web muestra "RPA & OCR Ejecutándose": el robot escribe en DOSBox.

Desde el repositorio (sin `jre\` ni el jar) hace falta un JDK 17 o superior y Maven: `mvn clean package` (con el
servidor cerrado). El `.bat` busca entonces Java en `JAVA_HOME`, `Program Files` y el PATH, y si solo hay uno más
antiguo lo dice y no arranca nada.

## Montar el ZIP de la entrega

```
powershell -ExecutionPolicy Bypass -File .\empaquetar.ps1
```

Compila el jar (con los tests), crea `jre\` con `jlink` (un Java reducido de unos 46 MB, solo con los módulos que usa
el jar y el runtime de Visual C++ que necesita Tesseract) y deja `dist\SL-P3-07.zip`. Antes hay que poner la memoria en
`memoria y vídeo/`. `jre/` y `dist/` no se suben al repositorio.

## Estructura

| Carpeta o fichero | Contenido |
|---|---|
| `arrancar.bat` | Doble clic: comprueba Java y el puerto, abre DOSBox (con `dosbox-wrapper.conf`) y arranca el servidor |
| `config.properties` | Puerto, esperas, zona de captura y motor de lectura |
| `dosbox-wrapper.conf` | Configuración fija de DOSBox (ventana de 640×400, teclado como Windows) |
| `maven.ps1` | Instala Maven y lo añade al PATH |
| `empaquetar.ps1` | Compila, crea `jre\` y monta `dist\SL-P3-07.zip` |
| `jre/` | Solo en el ZIP: Java 23 reducido con `jlink`, para no tener que instalar nada |
| `Database-MSDOS/` | La aplicación legada, sin modificar |
| `src/main/java/sl/p3/` | El wrapper (clases abajo) |
| `src/test/` | Tests y `resources/corpus/`: pantallas reales de DOSBox (menú, mensajes y las 43 páginas del listado) |
| `web/` | `index.html` y `tailwind.css` (ya generado, para que funcione sin internet) |
| `tessdata/` | `spa.traineddata` (el de la asignatura) y `dos.traineddata` (entrenado: 763 registros sin errores) |
| `entrenamiento/` | `entrenar.sh` (Docker), `sin-diccionarios.cfg` y `dos-prueba.traineddata` (para el `bench`) |
| `ocr-resultados.md` | Precisión de Tesseract con cada configuración y con el modelo entrenado (comando `bench`) |
| `capturas/` | Capturas de la aplicación, del OCR y de la web |
| `memoria y vídeo/` | Memoria y vídeo de la demostración |

| Clase | Función |
|---|---|
| `Main` | Servidor web y comandos de consola (`contar`, `programa X`, `cinta X`, `calibrar`, `corpus`, `bench`, `cajas`) |
| `LegacyService` | Los 3 casos de uso: teclas, espera de cada pantalla, cerrojo y vuelta al menú |
| `KeyboardDriver` | Teclea con Robot, tecla a tecla. Los signos `- ' . ! &` según la distribución de teclado de Windows |
| `DosboxWindow` | Localiza DOSBox (JNA), lo pone al frente y comprueba que se ve entero |
| `ScreenReader` | Captura, espera de pantallas y lectura con el motor elegido (`ocr.engine`) |
| `GlyphReader` | **Motor por defecto**: compara cada celda de 8×16 px con su carácter (`font8x16.txt`) |
| `ScreenParser` | Texto de pantalla a datos |
| `WebServer` | `/api/registros`, `/api/programa?nombre=`, `/api/cinta?id=` y la web |
| `OcrBench`, `TrainingData` | Herramientas: medida de Tesseract (`bench`) y datos de entrenamiento (`cajas`) |
