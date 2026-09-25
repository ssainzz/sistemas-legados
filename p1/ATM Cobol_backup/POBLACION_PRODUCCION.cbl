       IDENTIFICATION DIVISION.
       PROGRAM-ID. POBLACION-PROD.

       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.

           SELECT F-ESPECTACULOS
               ASSIGN TO "espectaculos.ubd"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS ID-ESPECTACULO
               FILE STATUS IS FS.

           SELECT F-TARJETAS
               ASSIGN TO "tarjetas.ubd"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS TNUM
               FILE STATUS IS FS-TAR.

           SELECT F-MOVIMIENTOS
               ASSIGN TO "movimientos.ubd"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS MOV-NUM
               FILE STATUS IS FS-MOV.
               
           SELECT F-INTENTOS
               ASSIGN TO "intentos.ubd"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS INT-TARJETA
               FILE STATUS IS FS-INT.

           SELECT F-TRANSFERENCIAS
               ASSIGN TO "transferencias.ubd"
               ORGANIZATION IS INDEXED
               ACCESS MODE IS DYNAMIC
               RECORD KEY IS TRF-ID
               FILE STATUS IS FS-TRF.

       DATA DIVISION.
       FILE SECTION.

       FD  F-ESPECTACULOS.
       01  REG-ESPECTACULO.
           05 ID-ESPECTACULO    PIC 9(4).
           05 FECHA             PIC 9(8).
           05 HORA              PIC 9(4).
           05 NOMBRE            PIC X(40).
           05 DATOS             PIC 9(13).

       FD  F-TARJETAS.
       01  TAJETAREG.
           02 TNUM              PIC 9(16).
           02 TPIN              PIC 9(4).

       FD  F-MOVIMIENTOS.
       01  MOVIMIENTO-REG.
           02 MOV-NUM           PIC 9(35).
           02 MOV-TARJETA       PIC 9(16).
           02 MOV-ANO           PIC 9(4).
           02 MOV-MES           PIC 9(2).
           02 MOV-DIA           PIC 9(2).
           02 MOV-HOR           PIC 9(2).
           02 MOV-MIN           PIC 9(2).
           02 MOV-SEG           PIC 9(2).
           02 MOV-IMPORTE-ENT   PIC S9(7).
           02 MOV-IMPORTE-DEC   PIC 9(2).
           02 MOV-CONCEPTO      PIC X(35).
           02 MOV-SALDOPOS-ENT  PIC S9(9).
           02 MOV-SALDOPOS-DEC  PIC 9(2).

       FD  F-INTENTOS.
       01  REG-INTENTOS.
           02 INT-TARJETA       PIC 9(16).
           02 INT-NUM           PIC 9.

       FD  F-TRANSFERENCIAS.
       01  TRF-REG.
           02 TRF-ID              PIC 9(8).
           02 TRF-ORIGEN          PIC 9(16).
           02 TRF-DESTINO         PIC 9(16).
           02 TRF-IMPORTE-ENT     PIC 9(7).
           02 TRF-IMPORTE-DEC     PIC 9(2).
           02 TRF-TIPO            PIC X.
           02 TRF-FECHA           PIC 9(8).
           02 TRF-ESTADO          PIC X.

       WORKING-STORAGE SECTION.
       01  FS                   PIC XX.
       01  FS-TAR               PIC XX.
       01  FS-MOV               PIC XX.
       01  FS-INT               PIC XX.
       01  FS-TRF               PIC XX.

       01  WS-I                 PIC 99 VALUE 0.

       PROCEDURE DIVISION.
       MAIN-PROCEDURE.
           PERFORM ABRIR-FICHEROS.
           PERFORM CARGAR-ESPECTACULOS.
           PERFORM CARGAR-TARJETAS.
           PERFORM CARGAR-TRANSACCIONES-EXTRA.
           PERFORM CERRAR-FICHEROS.
           DISPLAY "Poblacion completada".
           STOP RUN.

       ABRIR-FICHEROS.
           *> Abrir en OUTPUT recrea los ficheros desde cero, 
           *> eliminando todo el contenido previo (limpieza completa).
           OPEN OUTPUT F-ESPECTACULOS.
           IF FS NOT = "00"
               DISPLAY "ERROR OPEN ESPECTACULOS: " FS
               STOP RUN
           END-IF.
           
           OPEN OUTPUT F-TARJETAS.
           IF FS-TAR NOT = "00"
               DISPLAY "ERROR OPEN TARJETAS: " FS-TAR
               STOP RUN
           END-IF.
           
           OPEN OUTPUT F-MOVIMIENTOS.
           IF FS-MOV NOT = "00"
               DISPLAY "ERROR OPEN MOVIMIENTOS: " FS-MOV
               STOP RUN
           END-IF.
           
           OPEN OUTPUT F-INTENTOS.
           IF FS-INT NOT = "00"
               DISPLAY "ERROR OPEN INTENTOS: " FS-INT
               STOP RUN
           END-IF.
           
           OPEN OUTPUT F-TRANSFERENCIAS.
           IF FS-TRF NOT = "00"
               DISPLAY "ERROR OPEN TRANSFERENCIAS: " FS-TRF
               STOP RUN
           END-IF.

       CARGAR-ESPECTACULOS.
           PERFORM CARGAR-E1
           PERFORM CARGAR-E2
           PERFORM CARGAR-E3
           PERFORM CARGAR-E4
           PERFORM CARGAR-E5
           PERFORM CARGAR-E6
           PERFORM CARGAR-E7
           PERFORM CARGAR-E8
           PERFORM CARGAR-E9
           PERFORM CARGAR-E10
           PERFORM CARGAR-E11
           PERFORM CARGAR-E12
           PERFORM CARGAR-E13
           PERFORM CARGAR-E14
           PERFORM CARGAR-E15
           PERFORM CARGAR-E16.

       CARGAR-E1.
           MOVE 0005 TO ID-ESPECTACULO
           MOVE 20300115 TO FECHA
           MOVE 2030 TO HORA
           MOVE "Coldplay: World Tour" TO NOMBRE
           MOVE 0001500002500 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E2.
           MOVE 0006 TO ID-ESPECTACULO
           MOVE 20310620 TO FECHA
           MOVE 2130 TO HORA
           MOVE "Taylor Swift: European Tour" TO NOMBRE
           MOVE 0002300003500 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E3.
           MOVE 0007 TO ID-ESPECTACULO
           MOVE 20320910 TO FECHA
           MOVE 2200 TO HORA
           MOVE "Metallica: European Tour" TO NOMBRE
           MOVE 0001800004200 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E4.
           MOVE 0008 TO ID-ESPECTACULO
           MOVE 20330605 TO FECHA
           MOVE 1930 TO HORA
           MOVE "Festival Internacional de Musica" TO NOMBRE
           MOVE 0001200001800 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E5.
           MOVE 0009 TO ID-ESPECTACULO
           MOVE 20350718 TO FECHA
           MOVE 2100 TO HORA
           MOVE "Julio Iglesias: Gran Regreso" TO NOMBRE
           MOVE 0002000003000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E6.
           MOVE 0010 TO ID-ESPECTACULO
           MOVE 20361231 TO FECHA
           MOVE 2300 TO HORA
           MOVE "Nochevieja Especial" TO NOMBRE
           MOVE 0003000005000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E7.
           MOVE 0011 TO ID-ESPECTACULO
           MOVE 20380120 TO FECHA
           MOVE 2000 TO HORA
           MOVE "Queen: Homenaje en Directo" TO NOMBRE
           MOVE 0001100002200 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E8.
           MOVE 0012 TO ID-ESPECTACULO
           MOVE 20401201 TO FECHA
           MOVE 2100 TO HORA
           MOVE "Gran Gala del Futuro" TO NOMBRE
           MOVE 0002500004500 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E9.
           MOVE 0013 TO ID-ESPECTACULO
           MOVE 20410515 TO FECHA
           MOVE 2100 TO HORA
           MOVE "Cirque du Soleil: Futuro" TO NOMBRE
           MOVE 0001600003000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E10.
           MOVE 0014 TO ID-ESPECTACULO
           MOVE 20420310 TO FECHA
           MOVE 2000 TO HORA
           MOVE "Opera: Aida en el Espacio" TO NOMBRE
           MOVE 0002200001500 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E11.
           MOVE 0015 TO ID-ESPECTACULO
           MOVE 20421125 TO FECHA
           MOVE 2200 TO HORA
           MOVE "Rock in Rio 2042" TO NOMBRE
           MOVE 0002800006000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E12.
           MOVE 0016 TO ID-ESPECTACULO
           MOVE 20430814 TO FECHA
           MOVE 1900 TO HORA
           MOVE "El Rey Leon: Edicion 50 Aniversario" TO NOMBRE
           MOVE 0001900002000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E13.
           MOVE 0017 TO ID-ESPECTACULO
           MOVE 20440214 TO FECHA
           MOVE 2030 TO HORA
           MOVE "Concierto Especial San Valentin" TO NOMBRE
           MOVE 0001000001200 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E14.
           MOVE 0018 TO ID-ESPECTACULO
           MOVE 20450621 TO FECHA
           MOVE 2130 TO HORA
           MOVE "Festival Solsticio de Verano" TO NOMBRE
           MOVE 0001400002500 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E15.
           MOVE 0019 TO ID-ESPECTACULO
           MOVE 20460909 TO FECHA
           MOVE 2000 TO HORA
           MOVE "Sinfonica Virtual de Viena" TO NOMBRE
           MOVE 0001700001800 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-E16.
           MOVE 0020 TO ID-ESPECTACULO
           MOVE 20480101 TO FECHA
           MOVE 1800 TO HORA
           MOVE "Concierto Gala de Ano Nuevo" TO NOMBRE
           MOVE 0002500003000 TO DATOS
           WRITE REG-ESPECTACULO.

       CARGAR-TARJETAS.
           PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 5
               *> TARJETA
               MOVE WS-I TO TNUM
               MOVE WS-I TO TPIN
               WRITE TAJETAREG

               *> INTENTOS
               MOVE WS-I TO INT-TARJETA
               MOVE 3 TO INT-NUM
               WRITE REG-INTENTOS

               *> MOVIMIENTO INICIAL DE INGRESO (50 EUR)
               MOVE WS-I TO MOV-NUM
               MOVE WS-I TO MOV-TARJETA
               MOVE 2026 TO MOV-ANO
               MOVE 9 TO MOV-MES
               MOVE 15 TO MOV-DIA
               MOVE 12 TO MOV-HOR
               MOVE 0 TO MOV-MIN
               MOVE 0 TO MOV-SEG
               MOVE 50 TO MOV-IMPORTE-ENT
               MOVE 0 TO MOV-IMPORTE-DEC
               MOVE "INGRESO INICIAL" TO MOV-CONCEPTO
               MOVE 50 TO MOV-SALDOPOS-ENT
               MOVE 0 TO MOV-SALDOPOS-DEC
               WRITE MOVIMIENTO-REG
           END-PERFORM.

       CARGAR-TRANSACCIONES-EXTRA.
           *> ======= MOVIMIENTOS EXTRA ======= 
           
           *> MOV 6: Retirada tarjeta 1 (-20)
           MOVE 6 TO MOV-NUM
           MOVE 1 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 16 TO MOV-DIA
           MOVE 9 TO MOV-HOR
           MOVE 0 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE -20 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "RETIRADA DE EFECTIVO" TO MOV-CONCEPTO
           MOVE 30 TO MOV-SALDOPOS-ENT  *> Saldo antes 50, ahora 30
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.
           
           *> MOV 7: Ingreso tarjeta 2 (+30)
           MOVE 7 TO MOV-NUM
           MOVE 2 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 16 TO MOV-DIA
           MOVE 9 TO MOV-HOR
           MOVE 30 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE 30 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "INGRESO EN CAJERO" TO MOV-CONCEPTO
           MOVE 80 TO MOV-SALDOPOS-ENT  *> Saldo antes 50, ahora 80
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

           *> MOV 8: Retirada tarjeta 4 (-10)
           MOVE 8 TO MOV-NUM
           MOVE 4 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 17 TO MOV-DIA
           MOVE 10 TO MOV-HOR
           MOVE 15 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE -10 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "RETIRADA DE EFECTIVO" TO MOV-CONCEPTO
           MOVE 40 TO MOV-SALDOPOS-ENT  *> Saldo antes 50, ahora 40
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

           *> ======= TRANSFERENCIAS ======= 
           
           *> TRF 1: Tarjeta 1 a 2 (10 EUR, Inmediata)
           MOVE 1 TO TRF-ID
           MOVE 1 TO TRF-ORIGEN
           MOVE 2 TO TRF-DESTINO
           MOVE 10 TO TRF-IMPORTE-ENT
           MOVE 0 TO TRF-IMPORTE-DEC
           MOVE "I" TO TRF-TIPO
           MOVE 20260917 TO TRF-FECHA
           MOVE "E" TO TRF-ESTADO
           WRITE TRF-REG.

           *> MOV 9: TRF Emitida Tarjeta 1
           MOVE 9 TO MOV-NUM
           MOVE 1 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 17 TO MOV-DIA
           MOVE 11 TO MOV-HOR
           MOVE 0 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE -10 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "TRANSFERENCIA EMITIDA" TO MOV-CONCEPTO
           MOVE 20 TO MOV-SALDOPOS-ENT  *> Saldo antes 30, ahora 20
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

           *> MOV 10: TRF Recibida Tarjeta 2
           MOVE 10 TO MOV-NUM
           MOVE 2 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 17 TO MOV-DIA
           MOVE 11 TO MOV-HOR
           MOVE 0 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE 10 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "TRANSFERENCIA RECIBIDA" TO MOV-CONCEPTO
           MOVE 90 TO MOV-SALDOPOS-ENT  *> Saldo antes 80, ahora 90
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

           *> TRF 2: Tarjeta 3 a 4 (15 EUR, Puntual)
           MOVE 2 TO TRF-ID
           MOVE 3 TO TRF-ORIGEN
           MOVE 4 TO TRF-DESTINO
           MOVE 15 TO TRF-IMPORTE-ENT
           MOVE 0 TO TRF-IMPORTE-DEC
           MOVE "P" TO TRF-TIPO
           MOVE 20260918 TO TRF-FECHA
           MOVE "P" TO TRF-ESTADO   *> Pendiente
           WRITE TRF-REG.
           
           *> No añado movimiento para TRF 2 porque esta pendiente

           *> TRF 3: Tarjeta 5 a 3 (5 EUR, Mensual)
           MOVE 3 TO TRF-ID
           MOVE 5 TO TRF-ORIGEN
           MOVE 3 TO TRF-DESTINO
           MOVE 5 TO TRF-IMPORTE-ENT
           MOVE 0 TO TRF-IMPORTE-DEC
           MOVE "M" TO TRF-TIPO
           MOVE 20260920 TO TRF-FECHA
           MOVE "E" TO TRF-ESTADO
           WRITE TRF-REG.

           *> MOV 11: TRF Emitida Tarjeta 5
           MOVE 11 TO MOV-NUM
           MOVE 5 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 20 TO MOV-DIA
           MOVE 10 TO MOV-HOR
           MOVE 0 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE -5 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "TRANSFERENCIA EMITIDA" TO MOV-CONCEPTO
           MOVE 45 TO MOV-SALDOPOS-ENT  *> Saldo antes 50, ahora 45
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

           *> MOV 12: TRF Recibida Tarjeta 3
           MOVE 12 TO MOV-NUM
           MOVE 3 TO MOV-TARJETA
           MOVE 2026 TO MOV-ANO
           MOVE 9 TO MOV-MES
           MOVE 20 TO MOV-DIA
           MOVE 10 TO MOV-HOR
           MOVE 0 TO MOV-MIN
           MOVE 0 TO MOV-SEG
           MOVE 5 TO MOV-IMPORTE-ENT
           MOVE 0 TO MOV-IMPORTE-DEC
           MOVE "TRANSFERENCIA RECIBIDA" TO MOV-CONCEPTO
           MOVE 55 TO MOV-SALDOPOS-ENT  *> Saldo antes 50, ahora 55
           MOVE 0 TO MOV-SALDOPOS-DEC
           WRITE MOVIMIENTO-REG.

       CERRAR-FICHEROS.
           CLOSE F-ESPECTACULOS
                 F-TARJETAS
                 F-MOVIMIENTOS
                 F-INTENTOS
                 F-TRANSFERENCIAS.
