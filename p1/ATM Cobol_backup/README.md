## Cómo ejecutar el proyecto

Los comandos siguientes deben ejecutarse desde la carpeta que contiene los programas COBOL (`ATM Cobol_backup`):

```powershell
cd "ruta\al\repositorio\p1\ATM Cobol_backup"
```

### 1. Generar o completar los ficheros UBD

El programa `POBLACION.cbl` prepara los ficheros `espectaculos.ubd`, `tarjetas.ubd`, `movimientos.ubd` e `intentos.ubd`:

```powershell
cobc -x -o POBLACION.exe POBLACION.cbl
.\POBLACION.exe
```

- Abre cada fichero en modo `I-O` y, si no existe (file status 35), lo crea vacío con `OPEN OUTPUT`. **No borra** los datos que ya hubiera.
- Añade 16 espectáculos con fechas futuras (IDs 0005 a 0020). Si ya existen, muestra un aviso y no los duplica.
- Crea una tarjeta nueva (última tarjeta + 1, PIN = sus 4 últimos dígitos), su registro de intentos (3) y un ingreso inicial de 5000 EUR.

Cada ejecución añade una tarjeta más, así que basta con ejecutarlo una vez. El fichero `transferencias.ubd` no hace falta crearlo: `BANK1` y `BANK6` lo crean automáticamente la primera vez.

### 2. Compilar los programas

Con GnuCOBOL se generan el ejecutable principal y los módulos que este carga:

```powershell
cobc -m -o BANK2.dll BANK2.cbl
cobc -m -o BANK3.dll BANK3.cbl
cobc -m -o BANK4.dll BANK4.cbl
cobc -m -o BANK5.dll BANK5.cbl
cobc -m -o BANK6.dll BANK6.cbl
cobc -m -o BANK7.dll BANK7.cbl
cobc -m -o BANK8.dll BANK8.cbl
cobc -m -o BANK9.dll BANK9.cbl
cobc -x -o BANK1.exe BANK1.cbl
```

La opción `-m` compila los programas llamados desde `BANK1.cbl` como módulos dinámicos. La opción `-x` genera el ejecutable principal.

**Importante:** los `.dll`, `.exe` y `.ubd` no se suben al repositorio (`.gitignore`), así que después de cada `pull` hay que recompilar los módulos que hayan cambiado.

### 3. Arrancar el cajero

Basta con hacer doble clic en `ejecutar.bat`. El script:

- se sitúa en su propia carpeta (para encontrar las DLL y los `.ubd`);
- abre una consola clásica (`conhost`), porque Windows Terminal ignora `mode con`;
- ajusta la ventana a 80x25 y lanza `BANK1.exe`.

También se puede lanzar desde PowerShell con `.\ejecutar.bat` o directamente con `.\BANK1.exe` (en este caso el tamaño de la ventana no se ajusta).

Menú principal:

| Opción | Programa | Función |
| :--- | :--- | :--- |
| 1 | BANK2 | Consultar saldo |
| 2 | BANK3 | Consultar movimientos |
| 3 | BANK4 | Retirar efectivo |
| 4 | BANK5 | Ingresar efectivo (por billetes de 10, 20 y 50) |
| 5 | BANK6 | Ordenar transferencia (inmediata, puntual o mensual) |
| 6 | BANK9 | Listado de transferencias entre dos fechas |
| 7 | BANK7 | Comprar entradas de espectáculos |
| 8 | BANK8 | Cambiar clave |

Las transferencias puntuales y mensuales pendientes se ejecutan automáticamente al arrancar `BANK1` y cada vez que se vuelve al menú principal.

### 4. Compilación rápida

Después de modificar un programa, basta con recompilar ese módulo. Por ejemplo:

```powershell
cobc -m -o BANK6.dll BANK6.cbl
```

Si se ha modificado `BANK1.cbl`, hay que regenerar el ejecutable principal:

```powershell
cobc -x -o BANK1.exe BANK1.cbl
```

### Requisito

Para compilar hace falta tener GnuCOBOL instalado y el comando `cobc` en el `PATH` (se puede comprobar con `cobc --version`). Para **ejecutar** no hace falta instalar nada: las DLL del runtime (`libcob-4.dll`, `libdb-6.2.dll`, etc.) están en la carpeta.
