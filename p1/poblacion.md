# Datos añadidos a los ficheros UBD

## espectaculos.ubd
Se añaden 8 registros estáticos correspondientes a eventos o conciertos, con fecha posterior a la actual. Cada registro se compone de ID, Fecha, Hora, Nombre y un campo de Datos (precios/aforos).

| ID | Fecha | Hora | Nombre | Datos |
| :--- | :--- | :--- | :--- | :--- |
| 0005 | 20300115 | 2030 | Coldplay: World Tour | 0001500002500 |
| 0006 | 20310620 | 2130 | Taylor Swift: European Tour | 0002300003500 |
| 0007 | 20320910 | 2200 | Metallica: European Tour | 0001800004200 |
| 0008 | 20330605 | 1930 | Festival Internacional de Musica | 0001200001800 |
| 0009 | 20350718 | 2100 | Julio Iglesias: Gran Regreso | 0002000003000 |
| 0010 | 20361231 | 2300 | Nochevieja Especial | 0003000005000 |
| 0011 | 20380120 | 2000 | Queen: Homenaje en Directo | 0001100002200 |
| 0012 | 20401201 | 2100 | Gran Gala del Futuro | 0002500004500 |

## tarjetas.ubd
Se añade 1 único registro generado dinámicamente al calcular cuál es el último número de tarjeta guardado y sumarle uno.

* **Número de tarjeta (TNUM):** `WS-NUEVA-TARJETA` (Última tarjeta existente + 1, longitud 16).
* **PIN (TPIN):** El mismo valor de `WS-NUEVA-TARJETA` truncado a 4 dígitos.

## movimientos.ubd
Se añade 1 único registro correspondiente al ingreso inicial de la nueva tarjeta generada.

* **Número de Movimiento:** `WS-NUEVO-MOV` (Último movimiento existente + 1).
* **Tarjeta Asociada:** `WS-NUEVA-TARJETA`.
* **Fecha y Hora:** 15/01/2030 a las 12:00:00.
* **Importe:** 5000.00.
* **Concepto:** "INGRESO INICIAL DE PRUEBA".
* **Saldo Posterior:** 5000.00.

## intentos.ubd
Se añade 1 único registro para inicializar el contador de intentos de seguridad de la nueva tarjeta creada.

* **Tarjeta Asociada (INT-TARJETA):** `WS-NUEVA-TARJETA`.
* **Número de Intentos (INT-NUM):** 3 (Valor inicial por defecto).