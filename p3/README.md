# Práctica 3 – Wrapper web para la aplicación BASIC/MS-DOS

Java 17+ y Maven. `java.awt.Robot` (teclado y captura) + lector de pantalla por plantillas (y Tess4J como motor
alternativo) + `com.sun.net.httpserver`.

Documentos de trabajo: [`PROBLEMAS.md`](PROBLEMAS.md) (problemas y capturas para la memoria) y
[`DECISIONES.md`](DECISIONES.md) (qué se hizo distinto al esqueleto, al plan o al enunciado).

## Cómo ejecutarlo

1. Tener instalado **Java 17 o superior** (el `.bat` busca un JDK en `Program Files`).
2. Doble clic en **`arrancar.bat`**: abre DOSBox con la aplicación, arranca el servidor y abre el navegador en
   `http://localhost:8090/`.
3. No tocar teclado ni ratón mientras la web muestra "RPA & OCR Ejecutándose": el robot escribe en DOSBox.

Si falta `target\wrapper-legacy.jar`, compilar con `mvn clean package` (hace falta cerrar antes el servidor).

## Estructura

| Fichero | Función |
|---|---|
| `Main` | Servidor web y comandos de consola (`calibrar`, `corpus`, `contar`, `programa X`, `cinta X`) |
| `LegacyService` | Los 3 casos de uso: teclas + espera de la pantalla esperada + lock + recuperación al menú |
| `KeyboardDriver` | Robot: tecla a tecla, Shift explícito, solo A-Z 0-9 espacio `-` |
| `ScreenReader` | Captura, espera (`captureUntil`) y lectura con el motor elegido (`ocr.engine`) |
| `GlyphReader` | **Motor por defecto**: compara cada celda de 8×16 px con su carácter (`src/main/resources/font8x16.txt`) |
| `ScreenParser` | Texto de pantalla → datos (con tests) |
| `DosboxWindow` | Localiza la ventana de DOSBox (JNA), la pone al frente y da su área cliente |
| `WebServer` | `/api/registros`, `/api/programa?nombre=`, `/api/cinta?id=` y estáticos |
| `index.html` | Interfaz web conectada a la API |
| `config.properties` | Retardos, zona de captura, motor de lectura, puerto |
| `arrancar.bat` | Doble clic: DOSBox + servidor + navegador |
| `corpus/` | Pantallas reales de DOSBox (menú, mensajes y las 43 páginas del listado) usadas por los tests |
| `capturas/` | Capturas para la memoria (lista en `PROBLEMAS.md`, sección 4) |

## Secuencias verificadas a mano en la aplicación

Menú (sin Enter): `4` info · `6` listar · `7` buscar. Las respuestas S/N van **en mayúscula + Enter**.

1. **Registros**: `4` → "CONTIENE 763 ARCHIVOS" → Enter.
2. **Programa por nombre**: `7` → `N`⏎ → `NOMBRE`⏎ → "ES ESTE?" `S`⏎ → "ALTERAR?" `N`⏎ → "BUSCAR MÁS?" `N`⏎.
3. **Cinta**: `6` → ⏎ (desde el principio) → listado por ANTIGÜEDAD, 18 filas/página, `ESPACIO` para seguir, otra tecla
   para acabar. La última página (registros 757 a 763) sigue mostrando el aviso y cualquier tecla vuelve al menú.
   Columnas Nº, NOMBRE, TIPO, CINTA, REGISTRO. Cinta múltiple = `B-E`. Se filtra por pertenencia, no por subcadena.

Nunca usar las opciones 1, 2, 3 ni 5 del menú (modifican/imprimen datos).

## Estado

### Hecho
- [x] Compilar (`mvn clean package`) y ejecutar contra DOSBox.
- [x] Calibrar la zona de captura (640×400, área cliente de DOSBox).
- [x] Escalado de pantalla ≠ 100 %: resuelto (`sun.java2d.uiScale=1`); probado al 150 %.
- [x] Foco de DOSBox: se pone al frente solo.
- [x] Lectura de pantalla exacta (lector por plantillas): los 763 registros sin errores.
- [x] Final del listado verificado.
- [x] Los tres casos de uso por consola y por la API (registros, programa, cinta con cintas múltiples).
- [x] Peticiones simultáneas: se atienden de una en una sin mezclarse.
- [x] Recuperación al menú tras un fallo.
- [x] Conectar `index.html` con la API; contador de registros consultado al legado en cada carga y operación.
- [x] Probar `arrancar.bat` con doble clic (en el PC de Daniel, 16/10/2026).
- [x] `target/` y `debug/` en el `.gitignore`.
- [x] Capturas 01-04 (legado) y 16-21 (web).

### Pendiente
- [ ] Medir la precisión de Tesseract por variante (escala, binarizado, invertir) → tabla para la memoria.
- [ ] Capturas del OCR (10-15), de `arrancar.bat` (22) y de la comparación legado/web (23).
- [ ] Nombres repetidos y búsqueda por subcadena en la opción 7 (la aplicación pregunta "ES ESTE?" por cada uno).
- [ ] Config propia de DOSBox con `-conf` (hoy usa la de `%LOCALAPPDATA%\DOSBox`) y ventana a posición/tamaño fijos.
- [ ] Probar `arrancar.bat` en un Windows limpio.
- [ ] La web carga Tailwind y las fuentes desde internet: valorar incluirlas en local.
- [ ] Memoria (4-5 págs.): pasos, fallos, OCR, capturas, reparto de tareas y horas.
- [ ] Empaquetar `SL-P3-nn.zip` incluyendo `target\wrapper-legacy.jar` ya compilado.
