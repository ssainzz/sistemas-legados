# Guía para generar un ejecutable portable 

Generar un ejecuble para los usuarios puedan lanzar la aplicación sin tener Java instalado y haciendo doble click en un fichero.

## 1. Compilar los archivos Java

Abre la terminal integrada de Visual Studio Code en la raíz de tu proyecto y crea una carpeta para los binarios. Luego, compila tus cuatro archivos fuente.

```bash
mkdir bin
javac -d bin *.java
```

## 2. Empaquetar en un archivo JAR

Usa el comando `jar` para empaquetar los archivos compilados e indicar que `Main` es la clase principal que debe ejecutarse al inicio.

```bash
jar cfe GestorTareas.jar Main -C bin .
```

## 3. Preparar el directorio para jpackage

Crea una carpeta llamada `input` y mueve el archivo `.jar` que acabas de generar dentro de ella. `jpackage` usará esta carpeta como fuente.

```bash
mkdir input
move GestorTareas.jar input\
```

> **Nota:** Si usas PowerShell, el comando es:
>
> ```powershell
> mv Aplicacion.jar input\
> ```

## 4. Crear la aplicación autónoma con jpackage

Ejecuta `jpackage` para crear la carpeta independiente con el `.exe` y la máquina virtual de Java (JRE) reducida e incrustada.

```bash
jpackage --type app-image --name GestorTareas --input input --main-jar GestorTareas.jar --main-class Main
```

## 5. Incluir `ws3270.exe` en la carpeta final

Una vez finalizado el paso anterior, aparecerá una nueva carpeta llamada `MiAppMainframe` en tu proyecto.

1. Copia tu archivo `ws3270.exe` y pégalo directamente en la raíz de esa nueva carpeta `MiAppMainframe` (justo al lado del archivo `MiAppMainframe.exe` recién generado).
2. Comprime la carpeta `MiAppMainframe` entera en un archivo `.zip`.

## Resultado

El usuario final solo tendrá que descomprimir el `.zip` y hacer doble clic en `MiAppMainframe.exe`. Todo lo necesario (Java y el cliente ws3270) ya está autocontenido en la carpeta.