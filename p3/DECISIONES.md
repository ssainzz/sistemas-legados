# P3 – Decisiones que se apartan de lo que había

Cada entrada dice **qué había** (esqueleto de Berta, `plan.pdf` o enunciado), **qué se hizo en su lugar**, **por qué**
y **qué se pierde o queda pendiente**. Los problemas que originaron muchas de ellas están en `PROBLEMAS.md`.

Formato: *había → decisión → motivo → coste*.

---

## 1. Lectura de pantalla

### D1. Lector por plantillas de carácter en lugar de Tesseract como motor principal
- **Había:** el enunciado propone Tesseract con `spa.traineddata`, y el esqueleto lo usaba con reintentos de
  preprocesado (escala, binarizado, invertir).
- **Decisión:** `GlyphReader` compara cada celda de 8×16 píxeles con el dibujo de cada carácter (`font8x16.txt`).
  Es el motor por defecto (`ocr.engine=glyph`).
- **Motivo:** con la configuración por defecto Tesseract leía "NUNERO" por "NUMERO", `M` por `-`, `O` por `8`. Eso
  rompe las comprobaciones de pantalla y no permite leer 763 registros sin errores. El enunciado permite "cualquier otro
  método" mientras no se modifique ni deje de usarse la aplicación BASIC. Aquí se sigue capturando la pantalla con
  `Robot`, solo cambia cómo se interpreta la imagen.
- **Coste:** la zona de captura debe medir exactamente 640×400 (80×25 celdas). El catálogo de caracteres solo contiene
  los 56 dibujos vistos en el corpus; un carácter nuevo saldría como `#`.

### D2. Tesseract se conserva como motor alternativo
- **Había:** Tesseract era el único motor.
- **Decisión:** se mantiene seleccionable con `ocr.engine=tesseract`.
- **Motivo:** la memoria pide los resultados de las distintas técnicas de OCR y los intentos que no funcionaron.
- **Resultado:** se midieron 10 variantes (`ocr-resultados.md`). La mejor acierta el 95,2 % de los caracteres, pero solo 265 de
  763 registros exactos (35 %) y ninguna de las 43 páginas sale sin errores. Con el defecto del esqueleto (escala 2 sin
  binarizar): 95,0 % de caracteres y 54 de 169 filas de la muestra.

### D3. El catálogo de caracteres se construyó a mano a partir de pantallas reales
- **Decisión:** se capturó el listado completo (`corpus`), se agruparon las celdas distintas y se les puso nombre
  mirando una hoja de muestras. Las pantallas de mensajes se recortaron de capturas de ventana (con el borde y la barra de
  título) en el punto (3,37).
- **Motivo:** no hay otra fuente fiable de la fuente de DOSBox sin tocar la aplicación.
- **Coste:** el etiquetado es manual; si aparece un carácter nuevo hay que añadirlo.

### D4. Se compara con el dibujo más parecido, no con coincidencia exacta
- **Motivo:** el borde redondeado de la ventana de Windows 11 recorta un píxel en las celdas de las esquinas inferiores.
  Umbral: más de 24 píxeles distintos se devuelve `#`.

### D5. El corpus de imágenes forma parte del repositorio
- **Decisión:** `p3/corpus/` (unos 700 KB) se versiona porque los tests de `GlyphReaderTest` lo leen.
- **Coste:** los tests dependen de esas imágenes. Si se vuelve a capturar con otra configuración de DOSBox hay que
  regenerarlas.

---

## 2. Sincronización y control

### D6. Esperar a la pantalla esperada, no solo a que se estabilice
- **Había:** `plan.pdf` pedía esperas adaptativas "capturar hasta que dos capturas seguidas sean iguales", y el esqueleto
  esperaba cambio + estabilidad.
- **Decisión:** `captureUntil` y `waitFor`: tras cada acción se espera a que aparezca la frase de la pantalla
  siguiente, con tiempo máximo (8 s; 30 s en la búsqueda por nombre) y después estabilidad.
- **Motivo:** durante la búsqueda la pantalla no cambia varios segundos y "estable" se confundía con "terminado".

### D7. Una página del listado solo es válida si está completa
- **Decisión:** se exigen 18 filas (o las que queden hasta el último registro).
- **Motivo:** la aplicación dibuja las filas poco a poco y el aviso de la página anterior sigue visible.

### D8. Recuperación al menú según la pantalla, nunca con `S`
- **Había:** solo se pulsaba Enter.
- **Decisión:** se lee la pantalla y se responde `X` en un listado, `N` a cualquier S/N, un nombre inexistente si pide
  nombre y Enter en el resto.
- **Motivo:** Enter no hace nada en una pregunta S/N, y el wrapper quedaba bloqueado tras un error.

### D9. Escalado de pantalla: `sun.java2d.uiScale=1`
- **Había:** el esqueleto lo dejaba como TODO.
- **Decisión:** se fija al principio de `Main`. Windows (JNA) da píxeles físicos y `Robot` píxeles lógicos; con escala 1
  coinciden.
- **Coste:** depende de una propiedad interna de Java. Si cambiase en futuras versiones habría que usar `capture.*` en
  `config.properties`.

### D10. Foco de DOSBox con un toque de Alt y comprobación
- **Había:** `SetForegroundWindow` sin comprobar el resultado.
- **Decisión:** se simula Alt (`keybd_event`), se pide el foco y se reintenta hasta 5 veces comprobando que DOSBox es la
  ventana activa. Si no se logra, se lanza un error.
- **Motivo:** DOSBox quedaba detrás de otras ventanas y el robot escribía en otro programa.

### D11. El orden ANTIGÜEDAD se verifica, no se establece
- **Había:** `plan.pdf` sugería, si hiciera falta, ordenar con la opción 3. (Decisión heredada del esqueleto.)
- **Decisión:** `listarCinta` comprueba que el orden actual es ANTIGÜEDAD y falla si no lo es.
- **Motivo:** la opción 3 puede modificar la base de datos. Hoy el orden por defecto ya es ANTIGÜEDAD.

---

## 3. Análisis del texto

### D12. Se acepta `---` como tipo de programa
- **Motivo:** la aplicación lo muestra en programas sin tipo (por ejemplo LES FLICS). El esqueleto solo conocía los
  ocho tipos del menú.

### D13. Los nombres conservan mayúsculas y minúsculas
- **Había:** todo se pasaba a mayúsculas.
- **Decisión:** el nombre se devuelve tal cual aparece (`McGUIGAN'S`, `FRIDAY 13th`).
- **Motivo:** el enunciado pide un resultado "lo más parecido posible a la ejecución directa".

---

## 4. Entorno y arranque

### D14. Puerto 8090 en lugar de 8080
- **Motivo:** el 8080 estaba ocupado en este PC por Docker y WSL.
- **Coste:** en otro PC el 8090 también podría estar ocupado; se puede cambiar en `config.properties` (y la URL del `.bat`).

### D15. `arrancar.bat` busca un JDK 17 o superior
- **Había:** llamaba a `java` del PATH, que aquí es un Java 8.
- **Decisión:** busca en `Program Files` (OpenJDK, Adoptium, Java, Microsoft), luego `JAVA_HOME`, y por último `java`.
- **Pendiente:** probar en un Windows sin Java o con otra instalación.

### D16. Maven descargado en `C:\Users\dblas\tools` en lugar de instalarlo con winget
- **Motivo:** `winget install Apache.Maven` no encontró el paquete. No se tocó el PATH global.
- **Coste:** Maven no está en el PATH; hay que llamarlo por su ruta o añadirla en la terminal. Solo afecta a quien compile.

### D17. `jna-platform` explícito y `ClientToScreen` declarado a mano
- **Motivo:** Tess4J no trae `jna-platform` como dependencia transitiva y su `User32` no incluye `ClientToScreen` ni
  `keybd_event`.

### D18. Se cerró el servidor que tenía abierto el usuario
- **Motivo:** bloqueaba `target/wrapper-legacy.jar` y Maven no podía recompilar.
- **Consecuencia:** hay que volver a lanzar `arrancar.bat`.

---

## 5. Web

### D19. El contador de registros se vuelve a pedir tras cada operación
- **Había:** una maqueta con el número 763 escrito a mano.
- **Decisión:** `index.html` consulta `/api/registros` al cargar, con el botón de refrescar y automáticamente después de
  cada búsqueda. No hay sondeo periódico.
- **Motivo:** el plan pide que se consulte al sistema legado "en cada carga o refresco" sin cachear. Un sondeo
  constante ocuparía el teclado y el único DOSBox, y retrasaría las búsquedas del usuario.
- **Coste:** cada operación tarda unos 1-4 s más en el fondo (el contador se refresca al terminar, sin bloquear la tabla).

### D20. Enlaces directos `?nombre=X` y `?cinta=Y`
- **Había:** no existían.
- **Decisión:** abrir la página con uno de esos parámetros lanza la búsqueda al cargar.
- **Motivo:** permitió hacer capturas reproducibles de la web sin manejar el formulario a mano. Es un añadido menor.

### D21. Validación en el navegador y mensajes de error
- **Decisión:** el nombre admite A-Z, 0-9, espacio y guion (lo que el robot sabe teclear); la cinta, 1 a 3 caracteres
  A-Z/0-9. Los errores del servidor se muestran en la tabla en vez de fallar en silencio. Los formularios se
  desactivan mientras hay una operación en curso (solo hay un DOSBox).
- **Coste:** no se pueden buscar nombres con acentos, ñ o signos. El servidor responde 400 en ese caso.
- **Actualizado (17/10/2026):** ahora admite también minúsculas y los signos `- ' . ! &`, y exige 3 caracteres como
  mínimo (D24, D25 y D28).

### D22. La API devuelve también la columna REGISTRO
- **Había:** el JSON solo traía número, nombre, tipo y cinta.
- **Motivo:** el listado de la aplicación original muestra esa columna y la web debe parecerse a ella.
- **Actualizado (17/10/2026):** la web enseña una sola columna "Nº REGISTRO" (D30); la API mantiene los dos campos.

### D23. Las capturas de la web se recortan al contenido de la página
- **Motivo:** las capturas de ventana completa incluían los marcadores y la barra lateral personales del navegador.

---

## 6. Búsqueda por nombre (17/10/2026)

Comportamiento de la opción 7 comprobado a mano (`PROBLEMAS.md`, problema 25): distingue mayúsculas, busca el texto en
cualquier parte del nombre y enseña los resultados de uno en uno preguntando "ES ESTE?".

### D24. Los signos se teclean según la distribución de teclado de Windows
- **Había:** el robot solo escribía A-Z, 0-9, espacio y `-`, con códigos de tecla de `Robot` fijos.
- **Decisión:** para `- ' . ! &` se pregunta a Windows qué tecla, y si lleva Shift, da el carácter en la distribución
  actual (`VkKeyScanW`), y se pulsa con su código de exploración (`MapVirtualKeyW` + `keybd_event`). Letras, números y
  espacio siguen con `Robot`. Solo se admiten esos cinco signos: son los que aparecen en `DATOS.DAT`.
- **Motivo:** 45 de los 763 nombres llevan esos signos y no se podían buscar. Su tecla cambia con la distribución (el
  apóstrofo está junto al 0 en el teclado español y junto al Enter en el inglés).
- **Coste:** depende de que DOSBox use la misma distribución que Windows (`keyboardlayout=auto`, el valor por
  defecto). Los signos que necesitan AltGr no se envían. Probado en DOSBox con `KNIGHT'S LORE`, `B.C. BILL`,
  `N.O.M.A.D`, `WHAM! THE MUSIC BOX` y `OLLI & LISSA`.

### D25. El nombre se escribe tal cual, sin pasarlo a mayúsculas
- **Había:** el wrapper y la web pasaban el nombre a mayúsculas.
- **Decisión:** se envía exactamente lo que escribe el usuario. La web ya no muestra el campo en mayúsculas y admite
  minúsculas.
- **Motivo:** la aplicación distingue mayúsculas: `FRIDAY 13TH` no encuentra `FRIDAY 13th`.
- **Coste:** quien busque en minúsculas un nombre guardado en mayúsculas no lo encuentra, igual que en la aplicación
  original. La web lo avisa en el mensaje de "sin resultados".

### D26. Se devuelven todos los resultados, en el orden en que los da la aplicación
- **Había:** se respondía `S` al primer "ES ESTE?", luego `N` a "ALTERAR?" y a "BUSCAR MÁS?", y se devolvía un único
  programa.
- **Decisión:** se responde `N` a cada "ES ESTE?" para pasar al siguiente, hasta "NO HAY NINGUN PROGRAMA"; después Enter
  y `N` a "BUSCAR ALGUNO MAS". `/api/programa` devuelve la lista completa sin reordenar ni filtrar.
- **Motivo:** hay 5 nombres repetidos (`DEFENDER`, `GALLIPOLI`, ...) y la búsqueda es por subcadena. Devolver todo lo
  que enseña la aplicación es lo más parecido a usarla directamente.
- **Efecto añadido:** el wrapper ya no envía nunca una `S`, así que la aplicación no llega a preguntar "QUIERES ALTERAR
  SUS DATOS?". Es más seguro que antes.
- **Coste:** cada resultado cuesta una `N` y una espera mientras la aplicación sigue recorriendo registros. Una búsqueda
  con muchos resultados tarda bastante, y durante ese tiempo no se atienden otras peticiones (solo hay un DOSBox).

### D27. Se marcan en la web los que coinciden exactamente
- **Decisión:** cada resultado de `/api/programa` lleva `"exacto": true/false` (nombre idéntico al buscado, distinguiendo
  mayúsculas, como la aplicación). La web resalta esas filas con el color del formulario de nombre y la etiqueta
  `EXACTO`, y el pie de la tabla dice cuántos hay.
- **Motivo:** como la búsqueda es por subcadena, `MONTY` devuelve también "AUF WIEDERSEHEN MONTY"; así se ve de un
  vistazo cuál es el programa pedido sin perder el resto.
- **Coste:** la comparación se hace en el servidor (`WebServer.json`); `/api/cinta` no lleva ese campo.

### D28. Mínimo de 3 caracteres y sin máximo de resultados
- **Decisión:** el nombre buscado debe tener al menos 3 caracteres (`LegacyService.MIN_NOMBRE`, y también en la web).
  No hay límite de resultados: se muestran todos los que da la aplicación.
- **Motivo:** con 1 o 2 letras (`A`, `E`) coinciden cientos de programas y la búsqueda tardaría minutos con el DOSBox
  bloqueado. Con 3 caracteres el número baja mucho y se puede enseñar todo sin recortar.
- **Alternativa descartada:** un máximo de unos 50 resultados y salir con `recoverToMenu`. Se descartó porque ocultaba
  resultados que la aplicación sí enseña.
- **Coste:** búsquedas de 3 letras muy comunes (`THE`) siguen dando muchos resultados y tardan. El servidor responde
  400 si el nombre es más corto.

### D29. Una pantalla de resultado solo es nueva si muestra un registro aún no leído
- **Decisión:** tras cada `N` se espera (hasta 30 s) a "NO HAY NINGUN" o a un "ES ESTE?" con un número de registro que
  no se haya leído ya. Si al acabar la espera sigue el mismo registro, se lanza un error ("la búsqueda no avanza").
- **Motivo:** justo después de la `N` la pantalla anterior sigue visible un momento; esperar solo a "ES ESTE" la daría
  por buena y contaría dos veces el mismo programa.
- **Coste:** si la aplicación mostrase dos veces el mismo registro en una búsqueda (no se ha visto) se tomaría como
  error.

### D30. Una sola columna "Nº REGISTRO" en la web
- **Había:** la tabla de la web tenía las columnas Nº y REGISTRO, como el listado de la aplicación (D22).
- **Decisión:** se muestra una sola columna, "Nº REGISTRO", a la izquierda del nombre, con el valor de REGISTRO.
- **Motivo:** las dos columnas siempre valen lo mismo: el listado está ordenado por ANTIGÜEDAD (D11, y `checkPage`
  comprueba que Nº = REGISTRO en cada fila) y en la búsqueda por nombre solo hay un número. Repetirlo confundía.
- **Coste:** ninguno en los datos: la API sigue devolviendo `numero` y `registro`. Si algún día el orden no fuera
  ANTIGÜEDAD, la web dejaría de enseñar la posición en el listado; hoy eso ya se trata como error (D11).
