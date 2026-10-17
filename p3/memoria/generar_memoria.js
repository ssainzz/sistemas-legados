// Genera memoria.docx (Práctica 3). Uso: NODE_PATH=<carpeta con node_modules de docx> node generar_memoria.js
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, ImageRun, Header, Footer, PageNumber, AlignmentType,
  HeadingLevel, Table, TableRow, TableCell, WidthType, BorderStyle, ShadingType, LevelFormat,
} = require("docx");

const FONT = "Arial";
const TEAL = "0F3D3E";
const CONTENT_W = 9070; // A4 con margenes de 2,5 cm, en DXA
const IMG_W = 604; // ancho util en pixeles (16 cm a 96 ppp)
const FIG = (f) => path.join(__dirname, "figuras", f);

// "texto **negrita** y ~cursiva~" -> TextRuns
function runs(text, base = {}) {
  return text.split(/(\*\*[^*]+\*\*|~[^~]+~)/).filter(Boolean).map((t) => {
    if (t.startsWith("**")) return new TextRun({ text: t.slice(2, -2), bold: true, font: FONT, ...base });
    if (t.startsWith("~")) return new TextRun({ text: t.slice(1, -1), italics: true, font: FONT, ...base });
    return new TextRun({ text: t, font: FONT, ...base });
  });
}
const P = (text, opts = {}) =>
  new Paragraph({ children: runs(text), alignment: AlignmentType.JUSTIFIED, spacing: { after: 120 }, ...opts });
const H1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text, font: FONT })] });
const H2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun({ text, font: FONT })] });
const bullet = (text) =>
  new Paragraph({ children: runs(text), numbering: { reference: "bullets", level: 0 }, alignment: AlignmentType.JUSTIFIED, spacing: { after: 60 } });
const step = (text) =>
  new Paragraph({ children: runs(text), numbering: { reference: "steps", level: 0 }, alignment: AlignmentType.JUSTIFIED, spacing: { after: 60 } });
// Dificultad: titulo en negrita y texto debajo, como en la memoria de la practica 2.
const item = (title, text) => [
  new Paragraph({ children: [new TextRun({ text: title, bold: true, font: FONT })], spacing: { before: 100, after: 20 }, keepNext: true }),
  P(text),
];
const caption = (text) =>
  new Paragraph({ children: runs(text, { italics: true, size: 20 }), alignment: AlignmentType.CENTER, spacing: { before: 40, after: 160 } });

function figure(file, width = IMG_W) {
  const buf = fs.readFileSync(FIG(file));
  const w = buf.readUInt32BE(16), h = buf.readUInt32BE(20);
  return new Paragraph({
    alignment: AlignmentType.CENTER,
    keepNext: true,
    spacing: { before: 80, after: 0 },
    children: [new ImageRun({ type: "png", data: buf, transformation: { width, height: Math.round((width * h) / w) }, altText: { title: file, description: file, name: file } })],
  });
}

const line = { style: BorderStyle.SINGLE, size: 4, color: "A6A6A6" };
const borders = { top: line, bottom: line, left: line, right: line };
function cell(text, width, { header = false, align = AlignmentType.LEFT, minHeight } = {}) {
  return new TableCell({
    width: { size: width, type: WidthType.DXA },
    borders,
    shading: header ? { fill: "DCEBE9", type: ShadingType.CLEAR, color: "auto" } : undefined,
    margins: { top: 70, bottom: 70, left: 110, right: 110 },
    children: [new Paragraph({ alignment: align, children: runs(text, header ? { bold: true, size: 20 } : { size: 20 }) })],
  });
}
function table(widths, head, rows, aligns = []) {
  return new Table({
    width: { size: widths.reduce((a, b) => a + b, 0), type: WidthType.DXA },
    columnWidths: widths,
    rows: [
      new TableRow({ tableHeader: true, children: head.map((t, i) => cell(t, widths[i], { header: true, align: aligns[i] })) }),
      ...rows.map((r) => new TableRow({ cantSplit: true, children: r.map((t, i) => cell(t, widths[i], { align: aligns[i] })) })),
    ],
  });
}
const spacer = () => new Paragraph({ spacing: { after: 80 }, children: [] });

// --------------------------------------------------------------- contenido
const body = [];

body.push(
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 120, after: 60 },
    children: [new TextRun({ text: "Práctica 3: Encapsular una aplicación BASIC/MS-DOS mediante una página web", font: FONT, size: 40 })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
    children: [new TextRun({ text: "Daniel Blasco Labarta, Adriana Sainz Sampedro y Berta Castillo Crespo", font: FONT, italics: true, color: "666666" })] }),
  P("En esta práctica hemos encapsulado una aplicación antigua, escrita en BASIC para MS-DOS, que guarda los datos de programas del ZX Spectrum. El navegador no sabe nada del programa original: pide los datos a un servidor Java, y este se encarga de teclear en la aplicación, mirar su pantalla y devolver lo que ve."),
);

// 1. Configuración del entorno
body.push(
  H1("1. Configuración del entorno"),
  P("A continuación explicamos cómo hemos dejado preparado el entorno para trabajar con la aplicación antigua y para compilar y ejecutar nuestro wrapper."),
  ...item("Aplicación legada:", "Descomprimimos ~Database-MSDOS.rar~ en la carpeta de la práctica. El fichero ~database.bat~ lanza DOSBox 0.74 y, dentro, GW-BASIC con el programa de la carpeta ~Database~. No hemos cambiado nada de esa carpeta: todo nuestro trabajo consiste en usarla desde fuera."),
  ...item("Java:", "El proyecto necesita Java 17 o superior. En uno de nuestros equipos el comando ~java~ apuntaba a un Java 8, así que usamos un JDK 22 que ya teníamos instalado. El script de arranque busca por sí solo un JDK moderno en ~Program Files~, por lo que no hay que tocar variables de entorno."),
  ...item("Maven:", "Compilamos con Maven. No lo teníamos instalado y ~winget~ no encontró el paquete, así que descargamos el zip desde Apache y lo descomprimimos en una carpeta de usuario. El comando ~mvn clean package~ genera ~target/wrapper-legacy.jar~, un único jar con todas las dependencias (Tess4J y JNA, entre otras)."),
  ...item("Datos del OCR:", "Copiamos ~spa.traineddata~ a la carpeta del proyecto y configuramos ~tessdata.path=.~ para que Tess4J lo encuentre con una ruta relativa."),
  ...item("Puerto y configuración:", "El servidor web escucha en el puerto 8090. Al principio usábamos el 8080, pero en un equipo estaba ocupado por Docker y WSL. Este y el resto de parámetros (esperas, zona de captura, motor de lectura) están en ~config.properties~."),
  ...item("Pantalla:", "El robot escribe en la ventana que tenga el foco y captura los píxeles de la pantalla, así que DOSBox tiene que estar visible, sin minimizar y sin nada encima. Mientras el programa trabaja no se debe tocar el teclado ni el ratón."),
);

// 2. La aplicación legada
body.push(
  H1("2. La aplicación legada"),
  P("El programa guarda 763 registros de juegos del ZX Spectrum (nombre, tipo y cinta o cintas). Antes de programar nada recorrimos a mano los menús para apuntar la secuencia exacta de teclas de cada caso de uso (Tabla 1). Las preguntas de sí/no se contestan en mayúscula y con Enter."),
  figure("fig1-legado.png", 540),
  caption("Figura 1. Dos pantallas de la aplicación legada: la búsqueda por nombre y una página del listado."),
  table([1900, 3500, 3670], ["Operación", "Teclas", "Pantalla que se lee"], [
    ["Número de registros", "**4**, Enter", "“CONTIENE 763 ARCHIVOS”"],
    ["Programa por nombre", "**7**, **N**, nombre, Enter; **S** a “¿ES ESTE?”; **N** a “¿ALTERAR?”; **N** a “¿BUSCAR ALGUNO MÁS?”", "“643 - FEUD   VIDEOAVENTURA   CINTA:21”"],
    ["Programas de una cinta", "**6**, Enter y una vez espacio por página; otra tecla para salir", "Listado de 18 filas por página: Nº, nombre, tipo, cinta y registro"],
  ]),
  caption("Tabla 1. Secuencias de teclas de los tres casos de uso."),
  P("Hay dos detalles importantes. Primero, la aplicación no sabe buscar por cinta, así que para el tercer caso de uso recorremos el listado completo (43 páginas) y nos quedamos con las filas de la cinta pedida. Segundo, un programa puede estar en varias cintas y se muestra como ~A-45~. Para nosotros esa fila pertenece tanto a la cinta A como a la 45, pero no a la 4: filtramos por pertenencia y no por subcadena. Comprobamos también que el orden por defecto es ANTIGÜEDAD, que es el orden de grabación. Nunca usamos las opciones 1, 2, 3 y 5 del menú, porque modifican datos o imprimen."),
);

// 3. Arquitectura
body.push(
  H1("3. Arquitectura del wrapper"),
  P("La página web no habla con el programa antiguo. Habla con un servidor Java que, para cada petición, sigue siempre los mismos pasos (Figura 2): recibe la petición, teclea en DOSBox con ~java.awt.Robot~, captura la pantalla y la lee, y devuelve el resultado en JSON. Hay un servicio por caso de uso (~contarRegistros~, ~buscarPorNombre~ y ~listarCinta~) y cada uno consulta de verdad la aplicación legada: no guardamos nada en caché, ni siquiera el contador de registros. Como solo hay un DOSBox y un teclado, un cerrojo hace que las peticiones se atiendan de una en una."),
  figure("fig2-arquitectura.png", 470),
  caption("Figura 2. Recorrido de una petición: (1) el navegador la envía, (2) el robot teclea en DOSBox, (3) se captura y se lee la pantalla y (4) el resultado vuelve en JSON."),
);

// 4. Lectura de la pantalla
body.push(
  H1("4. Lectura de la pantalla"),
  H2("4.1 Primer intento: Tesseract"),
  P("Empezamos como sugiere el enunciado: capturar la pantalla con ~Robot~ y pasarla por Tesseract con ~spa.traineddata~ a través de Tess4J. Para mejorar el resultado probamos varios preprocesados (ampliar la imagen, escala de grises, binarizar e invertir). Para medirlo guardamos 45 pantallas reales de DOSBox, entre ellas las 43 páginas del listado, y comparamos 10 páginas repartidas por todo el listado con el texto correcto (Tabla 2)."),
  table([3700, 1500, 1800, 2070], ["Configuración", "Caracteres", "Filas exactas", "Páginas sin errores"], [
    ["Escala 1, grises", "88,1 %", "12 de 169", "0 de 10"],
    ["Escala 2, grises (valor inicial)", "95,0 %", "54 de 169", "0 de 10"],
    ["Escala 4, grises", "94,8 %", "50 de 169", "0 de 10"],
    ["Escala 2, binarizado a 128", "95,2 %", "62 de 169", "0 de 10"],
    ["Escala 3, binarizado a 128", "90,3 %", "19 de 169", "0 de 10"],
    ["Escala 2, invertido", "95,0 %", "54 de 169", "0 de 10"],
    ["**Lector por plantillas** (apartado 4.2)", "**100 %**", "**169 de 169**", "**10 de 10**"],
  ], [AlignmentType.LEFT, AlignmentType.CENTER, AlignmentType.CENTER, AlignmentType.CENTER]),
  caption("Tabla 2. Precisión de Tesseract según la configuración, y del lector final."),
  P("Los números engañan. Con un 95 % de caracteres acertados parece que funciona, pero basta un carácter mal para que una fila no sirva. La mejor configuración (escala 2 y binarizado a 128) deja 62 filas exactas de 169 y ninguna página completa; sobre los 763 registros acierta 265, un 35 %. Ampliar más la imagen empeora el resultado en lugar de mejorarlo. Los errores son como los de la Figura 3: nombres con letras cambiadas (~TURRO ESPRIT~), cintas irreconocibles (~Q-25~ leído como ~0625~) y filas enteras perdidas. Así no se puede ni comprobar la numeración del listado ni reconocer las preguntas de la aplicación: llegó a leer ~NUNERO~ en vez de ~NUMERO~ y no reconoció la pantalla."),
  figure("fig3-tesseract-errores.png"),
  caption("Figura 3. Pantalla original (izquierda) y texto leído por Tesseract con la configuración inicial (derecha); en rojo, las líneas con errores."),
  H2("4.2 Solución: lector por plantillas"),
  P("El enunciado permite usar cualquier otro método para leer la salida siempre que no se cambie la aplicación BASIC, y en nuestro caso hay una pista clara: DOSBox pinta el texto en una cuadrícula exacta de 80 columnas por 25 filas, con celdas de 8x16 píxeles, siempre con la misma fuente y sin suavizado. No hace falta reconocer formas, basta comparar cada celda con el dibujo de cada carácter. Seguimos capturando la pantalla con ~Robot~; lo único que cambia es cómo se interpreta la imagen."),
  P("Para construir el catálogo de caracteres recorrimos todo el listado y varias pantallas de mensajes, recortamos las celdas, agrupamos las distintas y les pusimos nombre a mano. Salieron solo 56 dibujos distintos (letras, números y unos pocos signos). Cada celda se compara con todos y se elige el que tiene menos píxeles distintos; si ninguno se parece de verdad, se escribe ~#~ en lugar de inventar un carácter. Elegir el más parecido, y no exigir un parecido exacto, fue necesario porque el borde redondeado de la ventana de Windows 11 recorta un píxel en las esquinas inferiores."),
  P("Con este lector los 763 registros se leen con numeración consecutiva, columnas coherentes y ningún carácter desconocido. Lo comprobamos con pruebas automáticas sobre las 45 pantallas guardadas y a mano en varias páginas. Tesseract sigue disponible como motor alternativo (~ocr.engine=tesseract~) para poder compararlos."),
);

// 5. Dificultades
body.push(
  H1("5. Dificultades encontradas"),
  ...item("El esqueleto no compilaba:", "Faltaba la dependencia ~jna-platform~ y la función ~ClientToScreen~ de Windows no existe en la clase ~User32~ de JNA. Añadimos la dependencia y declaramos nosotros esa función."),
  ...item("Escalado de pantalla:", "Nuestra pantalla va al 150 %. Windows da la posición de DOSBox en píxeles físicos pero ~Robot~ usa píxeles lógicos, así que la captura salía desplazada y recogía el editor de código en lugar de DOSBox. Lo resolvimos haciendo que Java use escala 1 desde el arranque."),
  ...item("DOSBox no se ponía delante:", "Windows no deja que un programa en segundo plano ponga una ventana al frente, y el robot acababa escribiendo en otra ventana. Ahora simulamos una pulsación de Alt antes de pedir el foco y comprobamos que DOSBox sea la ventana activa, reintentando si hace falta."),
  ...item("La búsqueda tarda varios segundos:", "Al pedir un nombre, GW-BASIC recorre los registros uno a uno y durante ese tiempo la pantalla no cambia. Nuestra primera espera daba la operación por terminada en cuanto la pantalla se quedaba quieta y leíamos demasiado pronto. Ahora, tras cada tecla, esperamos a ver en pantalla el texto de la pregunta siguiente (por ejemplo ~ES ESTE~), con un tiempo máximo."),
  ...item("Páginas leídas a medio dibujar:", "El listado se pinta fila a fila, y el aviso ~PULSA SPACE~ de la página anterior sigue en pantalla mientras tanto. Con la cinta Q llegamos a leer una página con solo 11 de sus 18 filas. Ahora una página solo se acepta si tiene todas sus filas."),
  ...item("Programas sin tipo y nombres con minúsculas:", "Algunos registros muestran ~---~ en lugar del tipo (por ejemplo ~LES FLICS~) y hay nombres con minúsculas, como ~McGUIGAN'S~ o ~FRIDAY 13th~. Nuestro análisis del texto no conocía el primer caso y pasaba todo a mayúsculas. Lo corregimos para que el resultado sea igual al de la aplicación original."),
  ...item("Quedarse a mitad de una pregunta:", "Si una operación fallaba, la aplicación se quedaba en una pregunta de sí/no y la siguiente petición no sabía salir, porque Enter no hace nada en esas preguntas. Ahora el programa mira qué pantalla hay y responde lo que lleva al menú: una tecla distinta de espacio en un listado, N en las preguntas de sí/no, y nunca S."),
  ...item("Una pulsación equivocada:", "En una de las pruebas enviamos por error un 5 (imprimir datos). No pasó nada, porque solo llegó a la primera pregunta, y comprobamos que los ficheros de datos no habían cambiado, pero reiniciamos DOSBox por seguridad."),
);

// 6. Decisiones
body.push(
  H1("6. Decisiones"),
  bullet("**Contador de registros:** se pide al programa antiguo al cargar la página, con un botón para refrescarlo y automáticamente al terminar cada búsqueda. No lo consultamos cada pocos segundos porque ocuparía el teclado y retrasaría las búsquedas."),
  bullet("**Validación en la web:** el robot solo sabe teclear letras sin acentos, números, espacios y guion, así que la web no admite otros caracteres en el nombre, y la cinta es de uno a tres caracteres. Los errores se muestran en pantalla."),
  bullet("**Pruebas con pantallas reales:** guardamos las 45 pantallas en la carpeta ~corpus~ y las usamos en los tests. Comprueban que se leen los 763 registros con numeración consecutiva, que ~McGUIGAN'S~ conserva sus minúsculas y que una fila ~Q-25~ pertenece a las cintas Q y 25."),
);

// 7. La página web
body.push(
  H1("7. La página web"),
  P("La web mantiene el diseño que preparamos al principio, pero ahora está conectada de verdad. Arriba muestra siempre el número de registros, debajo hay un formulario para buscar por nombre y otro por cinta, y los resultados salen en una tabla con las mismas columnas que el listado original (Figura 4). Mientras el robot trabaja se muestra un aviso para que nadie toque el teclado. Una búsqueda por nombre tarda unos 4 o 5 segundos y listar una cinta unos 25, porque hay que recorrer las 43 páginas."),
  figure("fig4-comparacion.png", 540),
  caption("Figura 4. La misma consulta (FEUD) en la aplicación original y en la web."),
  P("Los programas que están en varias cintas aparecen con su cinta completa. Por ejemplo, la cinta 25 incluye ~L-25~, ~M-25~, ~Q-25~ y ~R-25~ además de los de la cinta 25 sola (Figura 5). Si varias personas hacen consultas a la vez, el servidor las pone en cola y las atiende de una en una; lo probamos con cuatro peticiones simultáneas y todas devolvieron el resultado correcto."),
  figure("fig5-web.png", 500),
  caption("Figura 5. Resultado de la cinta 25: los programas grabados en varias cintas (L-25, M-25, Q-25...) aparecen con su cinta completa."),
);

// 8. Lanzamiento
body.push(
  H1("8. Lanzamiento de la aplicación"),
  P("Para ejecutar la práctica en Windows hay que seguir estos pasos:"),
  step("Tener instalado Java 17 o superior."),
  step("Descomprimir el archivo .zip entregado."),
  step("Hacer doble clic en ~arrancar.bat~. Se abre DOSBox con la aplicación, se arranca el servidor y se abre el navegador en ~http://localhost:8090/~."),
  step("Esperar a que el contador muestre 763 y no tocar el teclado ni el ratón mientras aparezca el aviso “RPA & OCR ejecutándose”."),
  P("El zip incluye ~target/wrapper-legacy.jar~ ya compilado, así que no hace falta compilar; el código está en ~src~ y se puede volver a compilar con ~mvn clean package~. La página carga su diseño (Tailwind y las fuentes) desde internet: sin conexión funciona igual, pero se ve sin estilos.", { spacing: { before: 80, after: 120 }, alignment: AlignmentType.JUSTIFIED }),
);

// 9. Organización del trabajo
const rowH = 1100;
body.push(
  H1("9. Organización del trabajo"),
  P("Cada uno de los integrantes del grupo ha realizado las siguientes tareas y ha dedicado el siguiente tiempo:"),
  new Table({
    width: { size: CONTENT_W, type: WidthType.DXA },
    columnWidths: [1900, 2390, 2390, 2390],
    rows: [
      new TableRow({ tableHeader: true, children: ["", "Adriana", "Daniel", "Berta"].map((t, i) => cell(t, [1900, 2390, 2390, 2390][i], { header: true })) }),
      new TableRow({ height: { value: rowH, rule: "atLeast" }, children: ["**Tareas**", "", "", ""].map((t, i) => cell(t, [1900, 2390, 2390, 2390][i])) }),
      new TableRow({ height: { value: 700, rule: "atLeast" }, children: ["**Horas**", "", "", ""].map((t, i) => cell(t, [1900, 2390, 2390, 2390][i])) }),
    ],
  }),
);

// --------------------------------------------------------------- documento
const doc = new Document({
  creator: "Grupo 07",
  title: "Práctica 3: Encapsular una aplicación BASIC/MS-DOS mediante una página web",
  styles: {
    default: { document: { run: { font: FONT, size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 32, bold: true, color: TEAL },
        paragraph: { spacing: { before: 320, after: 120 }, outlineLevel: 0, keepNext: true } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 24, bold: true, color: TEAL },
        paragraph: { spacing: { before: 200, after: 80 }, outlineLevel: 1, keepNext: true } },
    ],
  },
  numbering: {
    config: [
      { reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "steps", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ],
  },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1418, right: 1418, bottom: 1418, left: 1418 } } },
    headers: { default: new Header({ children: [new Paragraph({ alignment: AlignmentType.RIGHT,
      children: [new TextRun({ text: "Sistemas Legados - Grupo 07", font: FONT, italics: true, color: "666666", size: 20 })] })] }) },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
      children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 20 })] })] }) },
    children: body,
  }],
});

Packer.toBuffer(doc).then((buf) => {
  const out = path.join(__dirname, "memoria.docx");
  fs.writeFileSync(out, buf);
  console.log("Escrito " + out + " (" + buf.length + " bytes)");
});
