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

---

## 2. Riesgos detectados pero que aún no se han comprobado

Se mueven a la sección 1 cuando se confirmen.

- **Configuración de DOSBox ajena:** `Database-MSDOS/stdout.txt` muestra una configuración generada en
  `C:\Users\berta\AppData\Local\DOSBox\`. En cada PC se usará la propia, y su tamaño de ventana puede variar.
- **Escalado de pantalla distinto de 100 %:** las coordenadas de la ventana pueden no coincidir con las del `Robot`.
- **El OCR no llega al 100 %:** el enunciado ya lo avisa. Aún no hay medidas de precisión.
- **Fin del listado por cinta:** no se ha verificado cómo termina la última página.
- **Nombres repetidos y búsqueda por subcadena** en la opción 7 del programa legado.
- **Caracteres no soportados:** `KeyboardDriver` solo escribe A-Z, 0-9, espacio y `-`.
- **Foco y ventana encima:** si otra ventana toma el foco mientras el robot escribe, las teclas van a otro programa.
- **`index.html` sigue con datos fijos:** no llama a la API.
- **Windows limpio:** falta probar con Java, DLL nativas de Tess4J y rutas relativas.

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
- [ ] `10-captura-cruda.png` — imagen capturada por el `Robot` antes de procesar.
- [ ] `11-captura-preprocesada.png` — la misma imagen tras escalar, binarizar o invertir.
- [ ] `12-ocr-texto-bueno.png` — texto del OCR sin errores.
- [ ] `13-ocr-texto-con-errores.png` — texto con errores típicos (confusión de caracteres), junto con la imagen original.
- [ ] `14-tabla-comparativa-ocr.png` — tabla con la precisión de cada configuración (escala, binarizado, invertir). Se
      puede hacer una tabla en la memoria en vez de captura.
- [ ] `15-calibrar.png` — salida del comando `calibrar` con la zona de captura.

### Aplicación terminada
- [ ] `16-web-inicio.png` — web con el contador de registros visible.
- [ ] `17-web-busqueda-nombre.png` — resultado de buscar un programa por nombre.
- [ ] `18-web-nombre-inexistente.png` — mensaje cuando el programa no existe.
- [ ] `19-web-cinta-simple.png` — resultado de una cinta, por ejemplo `A`.
- [ ] `20-web-cinta-multiple.png` — resultado donde aparece un programa con cinta múltiple, por ejemplo `A-45`.
- [ ] `21-web-con-dosbox.png` — web y DOSBox visibles a la vez, para ver al robot trabajando.
- [ ] `22-arrancar-bat.png` — ventana de consola de `arrancar.bat` con todo arrancado.
- [ ] `23-comparacion-legado-web.png` — misma consulta en la app original y en la web, lado a lado.

### Vídeo (opcional pero recomendado por el enunciado)
- [ ] Grabación corta (1-2 min) de un doble clic en `arrancar.bat` y de las tres operaciones desde el navegador.
