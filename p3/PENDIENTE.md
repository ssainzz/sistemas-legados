# P3 – Lo que queda pendiente

Lista única de todo lo que falta para terminar la práctica, **en el orden en que conviene hacerlo**. Cada tarea se marca
con `[x]` al terminarla y, si origina un problema o una decisión, se apunta también en `PROBLEMAS.md` o `DECISIONES.md`.

- **Obligatorio**: hace falta para cumplir el enunciado (los tres casos de uso, encapsular sin tocar la aplicación,
  memoria, doble clic, entrega).
- **Adicional**: mejoras o añadidos nuestros que el enunciado no pide.

Fecha límite: **lunes 16/11/2026**. Entrega: `SL-P3-07.zip` (grupo 07).

---

## Obligatorio

### 1. Búsqueda por nombre (caso de uso 2)

Al empezar solo funcionaba con nombres de A-Z, 0-9, espacio y `-`, y devolvía un único resultado. Datos reales de `DATOS.DAT`:

- 45 nombres llevan otros caracteres: `'` (30), `.` (21), `!` (2), `&` (2) y uno más raro. Ejemplos: `KNIGHT'S LORE`,
  `EVERYONE'S A WALLY` (sale en la figura del enunciado), `B.C. BILL`, `N.O.M.A.D.`.
- 5 nombres están repetidos: `DEFENDER`, `GALLIPOLI`, `MONTY ON THE RUN`, `STREET HAWK`, `STRIP POKER`.
- 2 nombres llevan minúsculas: `BARRY McGUIGAN'S BOXING`, `FRIDAY 13th`.

- [x] **1.1. Ver a mano en DOSBox cómo busca la opción 7.** Resultado:
  - **Distingue mayúsculas**: hay que escribir el nombre tal cual (`FRIDAY 13th`).
  - **Busca en cualquier parte del nombre**: `MONTY` encuentra los que empiezan por MONTY y también los que lo llevan
    en medio (por ejemplo "AUF WIEDERSEHEN MONTY").
  - **Varios resultados**: muestra el primero y pregunta "ES ESTE?". Con `N` muestra el siguiente que coincide y vuelve
    a preguntar. Cuando no quedan más, sale "NO HAY NINGUN PROGRAMA CON ESTE NOMBRE PULSA ENTER". `DEFENDER` da 2
    resultados, uno de cada cinta.
  - [x] Apuntado en el README (sección "Secuencias verificadas a mano") y en `PROBLEMAS.md` (problema 25).
- [x] **1.2. Escribir `'`, `.`, `!` y `&`.** `KeyboardDriver` pregunta a Windows qué tecla da cada signo en la
  distribución actual (`VkKeyScanW`) y la pulsa con su código de exploración. La web y el servidor ya los aceptan.
  Test `KeyboardDriverTest`: los 763 nombres de `DATOS.DAT` se pueden escribir. Ver `PROBLEMAS.md`, problema 26.
  - [x] Probado en DOSBox (17/10/2026): `KNIGHT'S LORE` (87), `B.C. BILL` (197) y `N.O.M.A.D` (219, encuentra
    `N.O.M.A.D.` porque busca por subcadena) salen bien.
  - [x] Los signos con Shift también: `WHAM! THE MUSIC BOX` y `OLLI & LISSA` se escriben bien.
- [x] **1.3. No pasar el nombre a mayúsculas.** El wrapper y la web envían el nombre tal cual; el campo de la web
  ya no se muestra en mayúsculas (`DECISIONES.md`, D25).
- [x] **1.4. Devolver todos los programas que coinciden.** Se responde `N` a cada "ES ESTE?" hasta "NO HAY NINGUN" y
  se devuelven todos en el orden de la aplicación. La web marca los exactos (fila resaltada y etiqueta `EXACTO`) y
  exige 3 caracteres como mínimo, sin máximo de resultados. Un resultado solo se da por nuevo si su número de registro
  no se ha leído ya (`DECISIONES.md`, D26-D29).
- [x] **1.5. Tests**: `ScreenParserTest.programaPorNombreConSignosYMinusculas` (`KNIGHT'S LORE`, `N.O.M.A.D.`,
  `FRIDAY 13th`) y `KeyboardDriverTest`. El bucle de la búsqueda usa el robot y no tiene test automático: se prueba
  en el 1.6.
- [x] **1.6. Probar en DOSBox.** Antes, cerrar el servidor y recompilar con `mvn clean package`. Por consola
  (`java -jar target\wrapper-legacy.jar programa NOMBRE`) y por la web:
  - `DEFENDER`: 2 resultados, los 2 marcados como exactos.
  - `MONTY`: varios, también "AUF WIEDERSEHEN MONTY". `MONTY ON THE RUN` no se marca como exacto.
  - `FRIDAY 13th` lo encuentra y `friday 13th` no.
  - `THE`: muchos resultados; medir cuánto tarda y anotarlo en D28.
  - `AB`: la web pide 3 caracteres.
  - Un nombre inexistente: lista vacía y DOSBox vuelve al menú.
  - Sacar captura de la web con varios resultados y alguno exacto (`capturas/24-web-varios-resultados.png`).

### 2. Que arranque con doble clic en otro PC

- [ ] **2.1. Configuración propia de DOSBox.** Hoy usa la de `%LOCALAPPDATA%\DOSBox`, que en cada PC puede ser distinta,
  y el lector exige un área de exactamente 640×400. Hay que crear un `.conf` fuera de `Database-MSDOS/` (por ejemplo
  `p3/dosbox-wrapper.conf`, con `windowresolution=original`, `output=surface` y `keyboardlayout=auto`, que hace que DOSBox use la misma distribución
  que Windows; el punto 1.2 depende de ello) y lanzar DOSBox
  desde `arrancar.bat` con `-conf`, con los mismos argumentos que `database.bat`. Así no se modifica nada de la carpeta
  de la aplicación.
- [ ] **2.2. Esperar a DOSBox sin pausa fija.** `arrancar.bat` espera 6 s fijos. Mejor que el servidor arranque
  igualmente y que `DosboxWindow` reintente encontrar la ventana unos segundos, o que el `.bat` espere hasta que exista.
- [ ] **2.3. Mensaje claro si el puerto está ocupado.** Hoy sale un `BindException` y la ventana se cierra en la pausa. El
  puerto 8090 está escrito a la vez en `config.properties`, `Config.java` y la URL de `arrancar.bat`: o bien leerlo del
  `.properties` en el `.bat`, o que `Main` abra el navegador con el puerto real (`Desktop.browse`).
- [ ] **2.4. Probar en un Windows limpio** (o en el PC de otro miembro del grupo sin instalar nada), descomprimiendo el
  ZIP de prueba: con y sin Java 17+ en el PATH, escalado 100 % y 150 %, sin Maven. Si no hay Java 17+, que el `.bat` lo
  diga claramente (hoy acaba en un `UnsupportedClassVersionError` con el Java 8 del PATH).
- [ ] **2.5. Captura `22-arrancar-bat.png`**: la ventana de consola de `arrancar.bat` con todo arrancado.

### 3. Memoria (`memoria/memoria.docx`)

Se hace al final, cuando el código ya no cambia.

- [ ] **3.1. Actualizarla con los cambios de los puntos 1 y 2** (búsqueda con varios resultados y caracteres especiales,
  `.conf` propio de DOSBox) y añadir esos problemas a la sección de dificultades.
- [ ] **3.1b. Rehacer las capturas de la web** (`capturas/16` a `21` y `23`, y `memoria/figuras/fig4` y `fig5`): la tabla
  ya no tiene las columnas Nº y REGISTRO por separado (D30), y la búsqueda por nombre puede dar varios resultados.
- [ ] **3.2. Rellenar la tabla de reparto de tareas y horas** (`generar_memoria.js`, línea ~190). El enunciado pide el
  tiempo **real** invertido en cada tarea por cada miembro.
- [ ] **3.3. Ajustar la longitud** a unas 4-5 páginas (hoy son 6). Se pueden pasar figuras o tablas a un anexo.
- [ ] **3.4. Revisar la sección "Cómo ejecutarlo"**: requisitos (Java 17+, Windows), doble clic en `arrancar.bat`, no
  tocar teclado ni ratón mientras trabaja el robot, y qué hacer si falla (puerto, Java).
- [ ] **3.5. Regenerar el `.docx`** (`node memoria/generar_memoria.js`) y revisarlo en Word: figuras, tablas, saltos de
  página.

### 4. Entrega

- [ ] **4.1. Compilar limpio**: `mvn clean package` con el servidor cerrado. Que pasen todos los tests.
- [ ] **4.2. Montar `SL-P3-07.zip`** con:
  `arrancar.bat`, `config.properties`, `index.html`, `spa.traineddata`, `pom.xml`, `src/`,
  `target/wrapper-legacy.jar` (**ya compilado**: `target/` está en `.gitignore` y no viene en el repositorio),
  `Database-MSDOS/` (incluido DOSBox, sin `stdout.txt`/`stderr.txt`), el `.conf` del punto 2.1, `memoria.docx` (o el
  PDF) y el README. El corpus y las capturas son opcionales (los tests usan `corpus/`).
- [ ] **4.3. Probar el ZIP**: descomprimirlo en una carpeta nueva, sin espacios y con espacios en la ruta, y hacer doble
  clic en `arrancar.bat`. Probar los tres casos de uso.
- [ ] **4.4. Comprobar que `Database-MSDOS/Database` está intacta**: `git diff --stat HEAD -- Database-MSDOS/Database`
  vacío, y comparar `DATOS.DAT` e `INFORM.DAT` con los del `.rar` original.
- [ ] **4.5. Enviar** antes del 16/11.

---

## Adicional

Ordenado de más útil a menos. Lo marcado con ⚠️ no lo pide el enunciado pero puede afectar a la corrección.

### 5. Robustez

- [ ] ⚠️ **5.1. Web sin internet.** `index.html` carga Tailwind (`cdn.tailwindcss.com`) y las fuentes de Google Fonts.
  Sin conexión funciona, pero se ve sin estilos. Generar el CSS de Tailwind una vez y servirlo en local (y usar
  fuentes del sistema o incluir los `.woff2`). `WebServer.contentType` ya sirve `.css`; habría que añadir `.woff2`.
- [ ] **5.2. Avisar si la pantalla no llega a estabilizarse** (`ScreenReader.waitStable`, TODO de la línea ~110): un
  `System.err.println` con el tiempo esperado. Ayuda a diagnosticar en otro PC.
- [ ] **5.3. Mensaje claro si DOSBox está minimizado o tapado**: hoy la captura se hace igualmente y el error dice
  "Pantalla inesperada". Comprobar `IsIconic` en `DosboxWindow.focus()` y restaurar la ventana.
- [ ] **5.4. Página de cinta inexistente**: hoy `cinta ZZ` recorre los 763 registros (~25 s) para devolver una lista
  vacía. Es correcto, pero se podría avisar en la web de que tarda.

### 6. Limpieza de código y TODOs obsoletos

Ya están resueltos pero el comentario sigue diciendo lo contrario:

- [ ] **6.1.** `arrancar.bat:2` – quitar "(ESQUELETO)".
- [ ] **6.2.** `DosboxWindow.java:26-27` – el TODO del escalado está resuelto (`sun.java2d.uiScale=1`, probado al
  150 %, D9). Sustituirlo por una nota que lo explique.
- [ ] **6.3.** `LegacyService.java:116` – el final del listado ya está verificado (la última página muestra "PULSA SPACE"
  y cualquier tecla vuelve al menú). Quitar el TODO.
- [ ] **6.4.** `LegacyService.java:117` – decidido no reordenar nunca (D11: se comprueba ANTIGÜEDAD y si no, error).
  Sustituir el TODO por esa decisión.
- [ ] **6.5.** `LegacyService.java` cabecera de la clase y de `listarCinta`: dice "cambio + estabilización + OCR" y
  "se reintenta el OCR con otros preprocesados". Actualizar: se espera a la frase de la pantalla siguiente (D6) y los
  reintentos solo se aplican con Tesseract.
- [ ] **6.6.** `ScreenParser.java:127` – el TODO de corregir confusiones B/8, E/F, O/0 solo tendría sentido con
  Tesseract; con el lector por plantillas no hay confusiones. Quitarlo o dejar una nota de que no aplica.
- [ ] **6.7.** `Database-MSDOS/stdout.txt` sale como modificado en cada ejecución: está en `.gitignore` pero se subió
  antes. `git rm --cached Database-MSDOS/stdout.txt Database-MSDOS/stderr.txt`.

### 7. Documentos de trabajo

- [ ] **7.1.** `PROBLEMAS.md`:
  - el problema 8 dice "a la espera de que se pruebe de nuevo": ya está probado;
  - el problema 9 (README desactualizado): ya corregido;
  - sección 2 (riesgos): el escalado y la precisión del OCR ya están resueltos y medidos; los nombres repetidos, los
    caracteres no soportados y la configuración de DOSBox pasan a los puntos 1 y 2 de este fichero.
- [ ] **7.2.** `README.md`: sustituir la lista "Hecho / Pendiente" por un enlace a este fichero, para no tener dos listas.
- [ ] **7.3.** Al terminar cada punto de "Obligatorio", apuntar quién lo hizo y cuánto tardó (sirve para la tabla 3.2).

### 8. Material extra para la memoria

- [ ] **8.1. Vídeo corto** (1-2 min): doble clic en `arrancar.bat` y las tres operaciones desde el navegador con DOSBox
  visible. El enunciado pide "pantallazos/vídeo"; con las capturas ya se cumple, pero el vídeo enseña el robot
  trabajando.
- [ ] **8.2. Capturas de los errores del desarrollo** (`05` a `09` en `PROBLEMAS.md`, sección 4): falta el `.jar`,
  winget sin Maven, errores de compilación, puerto ocupado, `BUILD SUCCESS`. Solo si caben en la memoria; si no,
  basta con describirlos.
