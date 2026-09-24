# Datos añadidos a los ficheros UBD

## espectaculos.ubd
Se añaden 16 registros estáticos correspondientes a eventos o conciertos, con fecha posterior a la actual. Cada registro se compone de ID, Fecha, Hora, Nombre y un campo de Datos (7 dígitos de entradas libres + 6 de precio con 2 decimales). Si un espectáculo ya existe no se duplica.

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
| 0013 | 20410515 | 2100 | Cirque du Soleil: Futuro | 0001600003000 |
| 0014 | 20420310 | 2000 | Opera: Aida en el Espacio | 0002200001500 |
| 0015 | 20421125 | 2200 | Rock in Rio 2042 | 0002800006000 |
| 0016 | 20430814 | 1900 | El Rey Leon: Edicion 50 Aniversario | 0001900002000 |
| 0017 | 20440214 | 2030 | Concierto Especial San Valentin | 0001000001200 |
| 0018 | 20450621 | 2130 | Festival Solsticio de Verano | 0001400002500 |
| 0019 | 20460909 | 2000 | Sinfonica Virtual de Viena | 0001700001800 |
| 0020 | 20480101 | 1800 | Concierto Gala de Ano Nuevo | 0002500003000 |

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