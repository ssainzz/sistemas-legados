# Datos añadidos a los ficheros UBD

El programa `POBLACION_PRODUCCION.cbl` limpia completamente los ficheros `.ubd` existentes (al abrirlos en modo `OUTPUT`) y genera los siguientes datos iniciales fijos de prueba:

## espectaculos.ubd
Se añaden 16 registros estáticos correspondientes a eventos o conciertos, con fecha posterior a la actual. Cada registro se compone de ID, Fecha, Hora, Nombre y un campo de Datos (7 dígitos de entradas libres + 6 de precio con 2 decimales).

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
Se generan 5 tarjetas iniciales de forma secuencial.

* **Número de tarjeta (TNUM):** Del `0000000000000001` al `0000000000000005`.
* **PIN (TPIN):** Correspondiente a cada tarjeta (`0001` al `0005`).

## intentos.ubd
Se generan 5 registros para inicializar el contador de intentos de seguridad de las tarjetas.

* **Tarjeta Asociada (INT-TARJETA):** Cada una de las 5 tarjetas iniciales.
* **Número de Intentos (INT-NUM):** 3 (Valor inicial por defecto).

## transferencias.ubd
Se crean ejemplos de transferencias con distintos tipos para visualizar en las opciones del cajero:

* **TRF 1:** Origen: Tarjeta 1, Destino: Tarjeta 2. Importe: 10.00 EUR. Tipo: `I` (Inmediata). Estado: `E` (Ejecutada).
* **TRF 2:** Origen: Tarjeta 3, Destino: Tarjeta 4. Importe: 15.00 EUR. Tipo: `P` (Puntual). Estado: `P` (Pendiente).
* **TRF 3:** Origen: Tarjeta 5, Destino: Tarjeta 3. Importe: 5.00 EUR. Tipo: `M` (Mensual). Estado: `E` (Ejecutada).

## movimientos.ubd
Se añaden movimientos para cada tarjeta que cuadren con un saldo inicial base de 50.00 EUR y los ejemplos de transferencias/retiradas añadidos:

* **MOV 1 a 5:** Ingresos iniciales en cada tarjeta por importe de 50.00 EUR (Concepto: "INGRESO INICIAL").
* **MOV 6:** Retirada de efectivo de 20.00 EUR en la Tarjeta 1.
* **MOV 7:** Ingreso en cajero de 30.00 EUR en la Tarjeta 2.
* **MOV 8:** Retirada de efectivo de 10.00 EUR en la Tarjeta 4.
* **MOV 9:** Transferencia emitida (-10.00 EUR) en la Tarjeta 1 correspondiente a la TRF 1.
* **MOV 10:** Transferencia recibida (+10.00 EUR) en la Tarjeta 2 correspondiente a la TRF 1.
* **MOV 11:** Transferencia emitida (-5.00 EUR) en la Tarjeta 5 correspondiente a la TRF 3.
* **MOV 12:** Transferencia recibida (+5.00 EUR) en la Tarjeta 3 correspondiente a la TRF 3.
*(La TRF 2 no genera movimientos aún por estar Pendiente).*

Saldos finales resultantes:
* Tarjeta 1: 20.00 EUR
* Tarjeta 2: 90.00 EUR
* Tarjeta 3: 55.00 EUR
* Tarjeta 4: 40.00 EUR
* Tarjeta 5: 45.00 EUR