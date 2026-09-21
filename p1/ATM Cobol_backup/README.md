## Cómo ejecutar el proyecto

Los comandos siguientes deben ejecutarse desde la carpeta que contiene los programas COBOL:

```powershell
cd "C:\Users\sainz\Documents\GitHub\sistemas-legados\p1\ATM Cobol_backup"
```

### 1. Generar o regenerar los ficheros UBD

El programa `POBLACION.cbl` crea los ficheros `espectaculos.ubd`, `tarjetas.ubd`, `movimientos.ubd` e `intentos.ubd`.

```powershell
cobc -x -o POBLACION.exe POBLACION.cbl
.\POBLACION.exe
```

Este paso debe ejecutarse antes de probar el cajero si los ficheros UBD todavía no existen. Como el programa utiliza `OPEN OUTPUT`, vuelve a crear la población inicial; por tanto, no se debe ejecutar de nuevo si se quieren conservar movimientos, ingresos o transferencias ya realizados.

### 2. Compilar los programas

Si los módulos todavía no están compilados, se pueden generar los ejecutables y módulos con GnuCOBOL:

```powershell
cobc -m -o BANK2.dll BANK2.cbl
cobc -m -o BANK3.dll BANK3.cbl
cobc -m -o BANK4.dll BANK4.cbl
cobc -m -o BANK5.dll BANK5.cbl
cobc -m -o BANK6.dll BANK6.cbl
cobc -m -o BANK9.dll BANK9.cbl
cobc -x -o BANK1.exe BANK1.cbl
```
o

```powershell
cobc -x *.cbl
```


La opción `-m` compila los programas llamados desde `BANK1.cbl` como módulos dinámicos. La opción `-x` genera el ejecutable principal (si no ponemos nada BANK1.exe).

### 3. Arrancar el cajero

Con los ficheros UBD y los programas compilados en la misma carpeta, se puede arrancar el cajero con:

```powershell
.\BANK1.exe
```

También se puede utilizar el archivo incluido `ejecutar.bat` haciendo doble clic sobre él o ejecutándolo desde PowerShell:

```powershell
.\ejecutar.bat
```

El menú principal muestra:

- opción **4**: ingresar efectivo, ejecutada por `BANK5`;
- opción **5**: ordenar transferencia, ejecutada por `BANK6`.

### 4. Compilación rápida

Después de modificar un programa, basta con recompilar ese módulo. Por ejemplo:

```powershell
cobc -m -o BANK5.dll BANK5.cbl
cobc -m -o BANK6.dll BANK6.cbl
```

Después se vuelve a ejecutar `BANK1.exe`. Si también se ha modificado `BANK1.cbl`, hay que regenerar el ejecutable principal:

```powershell
cobc -x -o BANK1.exe BANK1.cbl
```

### Requisito

Es necesario tener GnuCOBOL instalado y que el comando `cobc` esté disponible en el `PATH`. Se puede comprobar con:

```powershell
cobc --version
```

Si PowerShell muestra que `cobc` no se reconoce, hay que instalar GnuCOBOL o añadir su carpeta `bin` al `PATH` antes de compilar.