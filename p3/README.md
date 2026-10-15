# Práctica 3 – Wrapper web para la aplicación BASIC/MS-DOS (esqueleto)

Java 17 + Maven. `java.awt.Robot` (teclado y captura) + Tess4J (OCR) + `com.sun.net.httpserver`.

## Estructura

| Fichero | Función |
|---|---|
| `Main` | Servidor web y comandos de prueba por consola (`calibrar`, `contar`, `programa X`, `cinta X`) |
| `LegacyService` | Los 3 casos de uso: secuencias de teclas + lectura de pantalla + lock |
| `KeyboardDriver` | Robot: tecla a tecla, Shift explícito, solo A-Z 0-9 espacio `-` |
| `ScreenReader` | Captura, espera de cambio/estabilidad, preprocesado y OCR con reintentos |
| `ScreenParser` | Texto OCR → datos (sin pantalla; tiene tests) |
| `DosboxWindow` | Localiza la ventana de DOSBox (JNA), la enfoca y da su área cliente |
| `WebServer` | `/api/registros`, `/api/programa?nombre=`, `/api/cinta?id=` y estáticos |
| `config.properties` | Retardos, zona de captura, OCR, puerto |
| `arrancar.bat` | Doble clic: DOSBox + servidor + navegador |

`index.html` (la maqueta) **no se ha tocado**: aún tiene datos fijos y no llama a la API.

## Secuencias verificadas a mano en la aplicación

Menú (sin Enter): `4` info · `6` listar · `7` buscar. Las respuestas S/N van **en mayúscula + Enter**.

1. **Registros**: `4` → "CONTIENE 763 ARCHIVOS" → Enter.
2. **Programa por nombre**: `7` → `N`⏎ → `NOMBRE`⏎ → "ES ESTE?" `S`⏎ → "ALTERAR?" `N`⏎ → "BUSCAR MÁS?" `N`⏎.
3. **Cinta**: `6` → ⏎ (desde el principio) → listado por ANTIGÜEDAD, 18 filas/página, `ESPACIO` para seguir, otra tecla para acabar.
   Columnas Nº, NOMBRE, TIPO, CINTA, REGISTRO. Cinta múltiple = `B-E`. Filtrar por pertenencia, no por subcadena.

Nunca usar las opciones 1, 2, 3 ni 5 del menú (modifican/imprimen datos).

## Pendiente (TODO)

- [ ] Compilar (`mvn package`) y ejecutar `calibrar` con DOSBox abierto; ajustar `ocr.*` y la zona de captura.
- [ ] Medir precisión del OCR por variante (escala, binarizado, invertir) → memoria.
- [ ] Verificar el final del listado (última página) y nombres repetidos / búsqueda por subcadena.
- [ ] Escalado de pantalla ≠ 100 %: comprobar coordenadas ventana vs Robot.
- [ ] Conectar `index.html` con la API (`fetch`), mostrar el contador siempre visible.
- [ ] Config propia de DOSBox con `-conf` (hoy usa la de `%LOCALAPPDATA%\DOSBox`) y ventana a posición/tamaño fijos.
- [ ] Probar `arrancar.bat` en un Windows limpio; memoria (4-5 págs.): pasos, fallos, OCR, capturas, reparto y horas.
- [ ] Añadir `target/` y `debug/` al `.gitignore`.
