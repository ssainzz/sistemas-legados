# P3 – Registro de problemas y capturas para la memoria

Documento de trabajo. La memoria pide explícitamente las **dificultades encontradas** y los **intentos que no
funcionaron**, así que cada problema se apunta aquí en cuanto aparece, con su causa y su solución.

Formato de cada entrada: **síntoma → causa → solución → estado**.

---

## 1. Problemas ya encontrados

### Entorno y compilación

1. **`arrancar.bat` dice "Falta target\wrapper-legacy.jar"**
   - Causa: el proyecto Java nunca se había compilado; el `.jar` solo existe tras `mvn package`.
   - Solución: compilar con Maven.
   - Estado: resuelto.

2. **El `java` del PATH es un Java 8, y el proyecto exige Java 17 o superior**
   - Causa: `java -version` devolvía 1.8.0_461, pero el `pom.xml` usa `maven.compiler.release=17`.
   - Solución: usar el JDK 22 ya instalado en `C:\Program Files\OpenJDK\jdk-22.0.2`, fijando `JAVA_HOME` y `Path` en
     la terminal.
   - Estado: resuelto (en cada terminal nueva hay que repetirlo).

3. **Maven no estaba instalado**
   - Causa: no había `mvn` en el PATH.
   - Estado: resuelto con el problema siguiente.

4. **`winget install Apache.Maven` → "No se encontró ningún paquete coincidente"**
   - Causa: ese identificador no existe en el repositorio de winget.
   - Intento fallido: instalación con winget.
   - Solución: descargar el zip de Maven 3.9.9 desde archive.apache.org y descomprimirlo en
     `C:\Users\dblas\tools\apache-maven-3.9.9`, sin tocar el PATH global.
   - Estado: resuelto.

5. **Error de compilación: no se encuentran `HWND`, `User32`, `RECT`, `POINT`, `WinUser`**
   - Causa: `pom.xml` suponía que Tess4J trae `jna-platform` como dependencia transitiva, y no es así.
   - Solución: añadir `net.java.dev.jna:jna-platform:5.14.0` al `pom.xml`.
   - Estado: resuelto.

6. **Error de compilación: `User32.ClientToScreen` no existe**
   - Causa: la interfaz `User32` de jna-platform no incluye esa función de la API de Windows.
   - Solución: declarar una interfaz propia (`ClientToScreenLib`, extiende `StdCallLibrary`) en
     `DosboxWindow.java` y cargar `user32` con `Native.load`.
   - Estado: resuelto.

### Arranque

7. **`arrancar.bat` usaba el Java 8 del PATH**
   - Causa: el script llamaba a `java` sin más.
   - Solución: el `.bat` busca un JDK (OpenJDK, Adoptium, Java, Microsoft) en `Program Files` y usa el que encuentre;
     si no hay ninguno, recurre a `JAVA_HOME` y por último a `java`.
   - Estado: resuelto en este PC. Pendiente de verificar en un Windows limpio.

8. **`java.net.BindException: Address already in use` al arrancar el servidor**
   - Causa: el puerto 8080 estaba ocupado por Docker (`com.docker.backend`) y WSL (`wslrelay`).
   - Solución: cambiar el puerto a 8090 en `config.properties`, en `Config.java` (valor por defecto) y en la URL de
     `arrancar.bat`.
   - Estado: resuelto, a la espera de que se pruebe de nuevo.
   - Riesgo para la entrega: el PC donde se corrija también puede tener ocupado el 8090.

### Documentación y control

9. **El `README.md` de Berta estaba desactualizado**
   - Lista como pendiente añadir `target/` y `debug/` al `.gitignore`, pero ya estaban.
   - Estado: pendiente de corregir en el README.

10. **Avisos de `maven-shade-plugin` ("overlapping resources")**
    - Causa: varios `.jar` comparten ficheros como `META-INF/LICENSE`.
    - Son avisos, no errores; el `.jar` se genera bien. No requiere acción.

11. **Aviso SLF4J "No SLF4J providers were found"**
    - Causa: Tess4J usa SLF4J y no hay implementación de logging en el proyecto.
    - Solo muestra mensajes en consola; no afecta al funcionamiento. No requiere acción.

### Primeras pruebas contra DOSBox (16/10/2026)

12. **El wrapper capturaba otra parte de la pantalla (VS Code en vez de DOSBox)**
    - Síntoma: `calibrar` devolvía texto mezclado con el de otras ventanas; la imagen mostraba el editor.
    - Causa 1, escalado: la pantalla va al 150 % (2240×1400 físicos, 1493×933 lógicos). Windows/JNA dan la
      posición de DOSBox en píxeles físicos pero `Robot` interpreta las coordenadas como lógicas, así que la zona de
      captura salía desplazada.
    - Causa 2, foco: `SetForegroundWindow` no trae la ventana al frente si el proceso que la pide está en segundo
      plano, y DOSBox quedó tapado.
    - Solución 1: `System.setProperty("sun.java2d.uiScale", "1")` al inicio de `Main`, para que Java use píxeles 1:1.
    - Solución 2: en `DosboxWindow.focus()` se simula un toque de Alt (`keybd_event`) antes de pedir el foco y se
      reintenta hasta comprobar que DOSBox es la ventana activa.
    - Estado: resuelto. Ahora captura 640×400 correctos.

13. **`UnsupportedClassVersionError` al ejecutar con `java -jar` desde Git Bash**
    - Causa: desde esa terminal el `PATH` no recogía el JDK 22 y se usó el Java 8.
    - Solución: llamar al ejecutable por su ruta completa, `C:\Program Files\OpenJDK\jdk-22.0.2\bin\java.exe`.
    - Estado: resuelto (las pruebas se lanzan con la ruta completa).

14. **`mvn package` no recompilaba y daba "Unresolved compilation problems"**
    - Causa: la extensión de Java de VS Code compila en `target/classes` mientras se edita (con el compilador de
      Eclipse) y deja clases a medias que Maven reutiliza.
    - Solución: `mvn clean package`.
    - Estado: resuelto.

15. **`mvn clean` falla: "Failed to delete target\wrapper-legacy.jar"**
    - Causa: el servidor de la prueba anterior seguía abierto y tenía el `.jar` bloqueado (Windows no deja borrar
      ficheros en uso).
    - Solución: cerrar ese proceso `java` antes de recompilar.
    - Estado: resuelto. Hay que cerrar el servidor siempre antes de recompilar.

16. **El OCR de Tesseract comete errores y rompe el caso de uso 2 (búsqueda por nombre)**
    - Síntoma: `programa FEUD` falla con "Pantalla inesperada (se esperaba 'SABES EL NUMERO')". El OCR leyó
      "SABES EL NUNERO DE REGISTRO OUE OUIERES BUSCARC7 ISZNI N".
    - Errores vistos en el menú con la configuración por defecto (escala 2, sin binarizar): `-` leído como `M`,
      `M E N U` como `N E N U`, `INFORMACION` como `INFORPHCION`, `IMPRIMIR` como `INPRINIR`, `8` como `O`.
    - Qué funciona: el caso de uso 1 (`contar`) devuelve 763 en unos 4 s, porque solo necesita una palabra y un número
      y ambos salen bien.
    - Intento que no funcionó: Tesseract con escala 2 sin binarizar (la configuración por defecto del esqueleto).
    - Solución: lector por plantillas de carácter (`GlyphReader`), ver el problema 18. Tesseract se mantiene como
      motor alternativo (`ocr.engine=tesseract`) para poder medirlo y compararlo en la memoria.
    - Medido (`ocr-resultados.md`, comando `bench`, 10 variantes): la mejor (escala 2, binarizado 128) acierta el 95,2 % de
      los caracteres, pero solo 265 de 763 registros exactos (35 %) y ninguna de las 43 páginas sale sin errores. Ampliar
      la imagen no ayuda: escala 3 y 4 empeoran (90-94 %). Errores típicos: `TURRO ESPRIT`, `Q-25` leído como `0625`,
      `PINGMPONG`, filas enteras perdidas (captura `13`).
    - Estado: resuelto con el lector por plantillas.

17. **Tras un fallo del wrapper, DOSBox queda a mitad de una pregunta y la siguiente ejecución no se recupera**
    - Síntoma: "La aplicacion legada no esta en el menu y no se pudo recuperar".
    - Causa: `recoverToMenu()` solo pulsa Enter, y en la pregunta "¿SABES EL NUMERO...? (S/N)" Enter no hace nada.
    - Solución provisional: salir a mano con N, un nombre inexistente, Enter y N.
    - Solución definitiva: `recoverToMenu()` lee la pantalla y responde según lo que ve: tecla `X` en un listado, `N` a
      cualquier pregunta S/N, un nombre inexistente si pide nombre, Enter en el resto. Nunca envía `S`.
    - Estado: resuelto. Probado dejando DOSBox a mitad del listado: el siguiente `contar` volvió al menú y devolvió 763.

### Lector por plantillas y sincronización (16/10/2026)

18. **Cambio de motor de lectura: de Tesseract a un lector por plantillas de carácter**
    - Idea: DOSBox pinta el texto en una cuadrícula exacta de 80×25 celdas de 8×16 píxeles, con una sola fuente y sin
      suavizado. En vez de "reconocer" formas, se compara cada celda con el dibujo de cada carácter.
    - Cómo se construyó: se capturaron las 43 páginas del listado y varias pantallas de mensajes (comando `corpus`), se
      recortaron las celdas, se agruparon las distintas (56 dibujos) y se les puso nombre a mano mirando una hoja de
      muestras. Resultado en `src/main/resources/font8x16.txt`.
    - Dificultad: la esquina inferior izquierda y derecha de la captura quedan recortadas por el borde redondeado de la
      ventana de Windows 11, y la celda de esa esquina cambia en un píxel. Por eso se elige el dibujo MÁS PARECIDO
      (menos píxeles distintos) en lugar de exigir coincidencia exacta; si ninguno se parece (>24 píxeles) se devuelve
      `#` para que el fallo se note.
    - Resultado: los 763 registros leídos sin un solo error y con numeración consecutiva (test `GlyphReaderTest`).
    - Estado: resuelto.

19. **El parser no conocía el tipo `---`**
    - Síntoma: el test de las 43 páginas falló en la página 35 con "tipo no reconocido".
    - Causa: el programa antiguo muestra `---` en la columna TIPO de los programas sin tipo (por ejemplo LES FLICS,
      VOLCANIC PLANET, LOADER). El lector acertaba; fallaba el parser.
    - Solución: `ScreenParser.SIN_TIPO` y tratamiento específico en `splitNameAndType`.
    - Estado: resuelto.

20. **El wrapper leía la pantalla antes de que el programa antiguo respondiera (búsqueda por nombre)**
    - Síntoma: `programa FEUD` fallaba con "se esperaba 'ES ESTE'" aunque el texto se leía perfectamente.
    - Causa: tras escribir el nombre y pulsar Enter, GW-BASIC recorre los 763 registros y tarda varios segundos. Durante
      ese tiempo la pantalla no cambia, y la espera "hasta que se estabilice" daba por terminada la operación.
    - Solución: `ScreenReader.captureUntil` y `LegacyService.waitFor`: tras cada acción se espera hasta que aparezca la
      frase de la pantalla siguiente ("ES ESTE", "ALTERAR", ...), con un tiempo máximo (8 s; 30 s para la búsqueda).
    - Estado: resuelto. `programa FEUD`, `FAIRLIGHT II` y un nombre inexistente dan el resultado correcto en 4-6 s.

21. **Páginas del listado leídas a medio dibujar (`cinta Q` fallaba en la página 16)**
    - Síntoma: "Página 16 ilegible: fila 0 número 253 (esperado 264)". La captura tenía solo 11 de 18 filas.
    - Causa: la aplicación pinta las filas una a una, y el aviso "PULSA SPACE" de la página anterior sigue en pantalla
      mientras tanto. Con "la primera fila es la esperada y hay aviso" se daba por buena una página incompleta.
    - Solución: una página solo se acepta si tiene TODAS sus filas (18, o las que queden hasta el último registro).
    - Estado: resuelto. `cinta A`, `21`, `25`, `L` y `Q` funcionan (unos 25 s cada una).

22. **Los nombres perdían las minúsculas ("McGUIGAN'S" salía como "MCGUIGAN'S")**
    - Causa: el parser pasaba todo el texto a mayúsculas antes de analizarlo. Hay nombres reales con minúsculas
      (`McGUIGAN'S`, `FRIDAY 13th`).
    - Solución: `keepCase` (quita acentos pero conserva el caso) para analizar filas; el nombre se devuelve tal cual.
    - Estado: resuelto, con test.

23. **Error mío en una prueba: se pulsó la opción 5 (IMPRIMIR DATOS)**
    - Síntoma: un script de prueba envió un `5` a DOSBox y apareció "DIME LOS CAMPOS QUE QUIERES IMPRIMIR".
    - Causa: error al escribir el script de prueba; la regla del proyecto es no usar nunca las opciones 1, 2, 3 y 5.
    - Consecuencias: ninguna. Solo llegó a la primera pregunta y no se respondió. `DATOS.DAT` e `INFORM.DAT` conservan
      la misma fecha y tamaño y git no detecta cambios en `Database/`.
    - Solución: se cerró DOSBox y se reinició con `database.bat`, para no responder preguntas de una opción que imprime.
    - Estado: resuelto.

24. **`python` colgaba la terminal**
    - Causa: en este PC `python` es el acceso directo de la tienda de Windows y se queda esperando.
    - Solución: no usar Python; las tareas auxiliares se hacen con Java (`java Fichero.java`) o PowerShell.
    - Estado: resuelto.

### Búsqueda por nombre (17/10/2026)

25. **La búsqueda por nombre solo funcionaba con el nombre exacto, en mayúsculas y con un único resultado**
    - Comprobado a mano en DOSBox (opción 7):
      - La búsqueda **distingue mayúsculas**. El wrapper pasaba el nombre a mayúsculas, así que `FRIDAY 13th` no se
        encontraba.
      - Busca el texto **en cualquier parte del nombre**: `MONTY` encuentra los que empiezan por MONTY y también, por
        ejemplo, "AUF WIEDERSEHEN MONTY".
      - Con varios resultados muestra el primero y pregunta "ES ESTE?". Con `N` muestra el siguiente y vuelve a
        preguntar; tras el último sale "NO HAY NINGUN PROGRAMA CON ESTE NOMBRE PULSA ENTER".
    - Datos reales (`DATOS.DAT`): 5 nombres repetidos (`DEFENDER`, `GALLIPOLI`, `MONTY ON THE RUN`, `STREET HAWK`,
      `STRIP POKER`) y 2 con minúsculas (`BARRY McGUIGAN'S BOXING`, `FRIDAY 13th`). El wrapper respondía `S` al
      primer "ES ESTE?" y devolvía solo ese.
    - Solución: el nombre se escribe tal cual y se responde `N` a cada "ES ESTE?" hasta "NO HAY NINGUN", devolviendo
      todos los resultados en orden. La web marca los exactos y pide 3 caracteres como mínimo (`DECISIONES.md`,
      D25-D29).
    - Estado: resuelto en el código; falta probarlo en DOSBox (`PENDIENTE.md`, 1.6).

26. **45 nombres no se podían buscar porque el robot no sabía escribir sus signos**
    - Síntoma: el robot solo escribía A-Z, 0-9, espacio y `-`. En `DATOS.DAT` hay 45 nombres con `'` (30), `.` (21),
      `!` (2) o `&` (2), por ejemplo `KNIGHT'S LORE`, `EVERYONE'S A WALLY` (sale en la figura del enunciado),
      `B.C. BILL` y `N.O.M.A.D.`.
    - Causa: esos signos no están en la misma tecla en todas las distribuciones (el apóstrofo está junto al 0 en el
      teclado español y junto al Enter en el inglés), y `Robot` traduce sus códigos de tecla de forma distinta según la
      distribución, así que no se puede fijar una tecla.
    - Solución: para los signos se pregunta a Windows qué tecla, y si lleva Shift, produce el carácter en la
      distribución actual (`VkKeyScanW`), y se pulsa con su código de exploración (`MapVirtualKeyW` + `keybd_event`),
      que es lo que lee DOSBox. DOSBox usa la misma distribución que Windows (`keyboardlayout=auto`). Las letras,
      números y espacio siguen yendo con `Robot`. Test `KeyboardDriverTest`: los 763 nombres se pueden escribir.
    - Estado: resuelto. Probado en DOSBox: `KNIGHT'S LORE`, `B.C. BILL`, `N.O.M.A.D`, `WHAM! THE MUSIC BOX` y
      `OLLI & LISSA` se escriben bien y se encuentran.


---

## 2. Riesgos detectados pero que aún no se han comprobado

Se mueven a la sección 1 cuando se confirmen.

- **Configuración de DOSBox ajena:** `Database-MSDOS/stdout.txt` muestra una configuración generada en
  `C:\Users\berta\AppData\Local\DOSBox\`. En cada PC se usará la propia, y su tamaño de ventana puede variar.
- **Escalado de pantalla distinto de 100 %:** las coordenadas de la ventana pueden no coincidir con las del `Robot`.
- **El OCR no llega al 100 %:** el enunciado ya lo avisa. Aún no hay medidas de precisión.
- ~~**Fin del listado por cinta**~~ Resuelto: la última página (registros 757 a 763) sigue mostrando "PULSA SPACE" y
  cualquier tecla vuelve al menú.
- ~~**Nombres repetidos y búsqueda por subcadena** en la opción 7 del programa legado.~~ Comprobado: problema 25.
- ~~**Caracteres no soportados:** `KeyboardDriver` solo escribe A-Z, 0-9, espacio y `-`.~~ Problema 26.
- **Foco y ventana encima:** si otra ventana toma el foco mientras el robot escribe, las teclas van a otro programa.
- ~~`index.html` con datos fijos~~ Resuelto: ahora llama a la API.
- **Windows limpio:** falta probar con Java, DLL nativas de Tess4J y rutas relativas.
- **La web carga Tailwind y las fuentes desde internet** (`cdn.tailwindcss.com`, Google Fonts). Sin conexión se vería sin
  estilos. Si el PC de corrección no tiene red habría que incluir esos ficheros en local.

---

## 3. Registro de nuevos problemas

Copiar esta plantilla al final de cada problema nuevo:

```
### N. Título corto
- Síntoma:
- Causa:
- Intentos que no funcionaron:
- Solución:
- Estado:
- Quién y cuánto tiempo:
```

---

## 4. Capturas que hay que incluir en la memoria

Guardarlas en una carpeta `capturas/` con el nombre indicado.

### Sistema legado
- [x] `01-menu-legado.png` — menú principal de la aplicación BASIC en DOSBox.
- [x] `02-opcion4-registros.png` — pantalla "CONTIENE 763 ARCHIVOS".
- [x] `03-opcion7-busqueda.png` — secuencia de búsqueda por nombre (pregunta "ES ESTE?").
- [x] `04-opcion6-listado.png` — una página del listado con el prompt "PULSA SPACE...".
- [x] `04a-opcion6-orden-antiguedad.png` — pantalla de inicio del listado, con "ORDENACION SEGUN EL CAMPO=ANTIGUEDAD".

### Proceso de desarrollo (intentos y fallos)
- [ ] `05-error-falta-jar.png` — mensaje "Falta target\wrapper-legacy.jar".
- [ ] `06-error-winget-maven.png` — winget sin encontrar Maven.
- [ ] `07-error-compilacion.png` — errores de `HWND`/`ClientToScreen`.
- [ ] `08-error-puerto-8080.png` — `BindException`.
- [ ] `09-build-success.png` — `BUILD SUCCESS` de Maven.

### OCR (lo que más pesa en la memoria)
- [x] `10-captura-cruda.png` — imagen capturada por el `Robot` antes de procesar.
- [x] `11-captura-preprocesada.png` — la misma imagen tras escalar, binarizar o invertir.
- [x] `12-ocr-texto-bueno.png` — texto leído sin errores por el lector por plantillas (motor final): captura y texto lado a lado.
- [x] `13-ocr-texto-con-errores.png` — texto con errores típicos (confusión de caracteres), junto con la imagen original.
- [x] `14-tabla-comparativa-ocr.png` — tabla con la precisión de cada configuración (escala, binarizado, invertir). Se
      puede hacer una tabla en la memoria en vez de captura.
- [x] `15-calibrar.png` — salida del comando `calibrar` con la zona de captura.

### Aplicación terminada
- [x] `16-web-inicio.png` — web con el contador de registros visible.
- [x] `17-web-busqueda-nombre.png` — resultado de buscar un programa por nombre.
- [x] `18-web-nombre-inexistente.png` — mensaje cuando el programa no existe.
- [x] `19-web-cinta-simple.png` — resultado de una cinta, por ejemplo `A`.
- [x] `20-web-cinta-multiple.png` — resultado donde aparece un programa con cinta múltiple, por ejemplo `A-45`.
- [x] `21-web-con-dosbox.png` — web y DOSBox visibles a la vez, para ver al robot trabajando.
- [ ] `22-arrancar-bat.png` — ventana de consola de `arrancar.bat` con todo arrancado.
- [x] `23-comparacion-legado-web.png` — misma consulta en la app original y en la web, lado a lado.

### Vídeo (opcional pero recomendado por el enunciado)
- [ ] Grabación corta (1-2 min) de un doble clic en `arrancar.bat` y de las tres operaciones desde el navegador.
