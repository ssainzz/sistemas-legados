       IDENTIFICATION DIVISION.
       PROGRAM-ID. POBLACION.

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

       WORKING-STORAGE SECTION.

       01  FS                   PIC XX.
       01  FS-TAR               PIC XX.
       01  FS-MOV               PIC XX.
       01  FS-INT               PIC XX.

       01  WS-CONTADOR          PIC 99 VALUE 0.

       01  WS-ULTIMA-TARJETA    PIC 9(16) VALUE 0.
       01  WS-NUEVA-TARJETA     PIC 9(16) VALUE 0.

       01  WS-ENCONTRADA        PIC X VALUE "N".

       01  WS-ULTIMO-MOV        PIC 9(35) VALUE 0.
       01  WS-NUEVO-MOV         PIC 9(35) VALUE 0.

       01  WS-MOV-ENCONTRADO    PIC X VALUE "N".


       PROCEDURE DIVISION.

           PERFORM ABRIR-FICHEROS

           PERFORM CARGAR-ESPECTACULOS

           PERFORM BUSCAR-ULTIMA-TARJETA

           PERFORM BUSCAR-ULTIMO-MOVIMIENTO

           PERFORM CREAR-TARJETA

           PERFORM CERRAR-FICHEROS

           DISPLAY "----------------------------------------"
           DISPLAY "CARGA TERMINADA"
           DISPLAY "ULTIMA TARJETA EXISTENTE: "
                   WS-ULTIMA-TARJETA
           DISPLAY "NUEVA TARJETA: "
                   WS-NUEVA-TARJETA
           DISPLAY "----------------------------------------"

           STOP RUN.


       ABRIR-FICHEROS.

           OPEN I-O F-ESPECTACULOS
           IF FS = "35"
               OPEN OUTPUT F-ESPECTACULOS
               CLOSE F-ESPECTACULOS
               OPEN I-O F-ESPECTACULOS
           END-IF.
           IF FS NOT = "00"
               DISPLAY "ERROR OPEN ESPECTACULOS. STATUS = "
                       FS
               STOP RUN
           END-IF

           OPEN I-O F-TARJETAS
           IF FS-TAR = "35"
               OPEN OUTPUT F-TARJETAS
               CLOSE F-TARJETAS
               OPEN I-O F-TARJETAS
           END-IF.
           IF FS-TAR NOT = "00"
               DISPLAY "ERROR OPEN TARJETAS. STATUS = "
                       FS-TAR
               STOP RUN
           END-IF

           OPEN I-O F-MOVIMIENTOS
           IF FS-MOV = "35"
               OPEN OUTPUT F-MOVIMIENTOS
               CLOSE F-MOVIMIENTOS
               OPEN I-O F-MOVIMIENTOS
           END-IF.
           IF FS-MOV NOT = "00"
               DISPLAY "ERROR OPEN MOVIMIENTOS. STATUS = "
                       FS-MOV
               STOP RUN
           END-IF.
           
           OPEN I-O F-INTENTOS
           IF FS-INT = "35"
               OPEN OUTPUT F-INTENTOS
               CLOSE F-INTENTOS
               OPEN I-O F-INTENTOS
           END-IF.
           IF FS-INT NOT = "00"
               DISPLAY "ERROR OPEN INTENTOS. STATUS = "
                       FS-INT
               STOP RUN
           END-IF.


       CARGAR-ESPECTACULOS.

           PERFORM CARGAR-1
           PERFORM CARGAR-2
           PERFORM CARGAR-3
           PERFORM CARGAR-4
           PERFORM CARGAR-5
           PERFORM CARGAR-6
           PERFORM CARGAR-7
           PERFORM CARGAR-8.


       CARGAR-1.

           MOVE 0005 TO ID-ESPECTACULO
           MOVE 20300115 TO FECHA
           MOVE 2030 TO HORA
           MOVE "Coldplay: World Tour" TO NOMBRE
           MOVE 0001500002500 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-2.

           MOVE 0006 TO ID-ESPECTACULO
           MOVE 20310620 TO FECHA
           MOVE 2130 TO HORA
           MOVE "Taylor Swift: European Tour" TO NOMBRE
           MOVE 0002300003500 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-3.

           MOVE 0007 TO ID-ESPECTACULO
           MOVE 20320910 TO FECHA
           MOVE 2200 TO HORA
           MOVE "Metallica: European Tour" TO NOMBRE
           MOVE 0001800004200 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-4.

           MOVE 0008 TO ID-ESPECTACULO
           MOVE 20330605 TO FECHA
           MOVE 1930 TO HORA
           MOVE "Festival Internacional de Musica" TO NOMBRE
           MOVE 0001200001800 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-5.

           MOVE 0009 TO ID-ESPECTACULO
           MOVE 20350718 TO FECHA
           MOVE 2100 TO HORA
           MOVE "Julio Iglesias: Gran Regreso" TO NOMBRE
           MOVE 0002000003000 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-6.

           MOVE 0010 TO ID-ESPECTACULO
           MOVE 20361231 TO FECHA
           MOVE 2300 TO HORA
           MOVE "Nochevieja Especial" TO NOMBRE
           MOVE 0003000005000 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-7.

           MOVE 0011 TO ID-ESPECTACULO
           MOVE 20380120 TO FECHA
           MOVE 2000 TO HORA
           MOVE "Queen: Homenaje en Directo" TO NOMBRE
           MOVE 0001100002200 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       CARGAR-8.

           MOVE 0012 TO ID-ESPECTACULO
           MOVE 20401201 TO FECHA
           MOVE 2100 TO HORA
           MOVE "Gran Gala del Futuro" TO NOMBRE
           MOVE 0002500004500 TO DATOS

           PERFORM ESCRIBIR-ESPECTACULO.


       ESCRIBIR-ESPECTACULO.

           WRITE REG-ESPECTACULO

           EVALUATE FS

               WHEN "00"

                   ADD 1 TO WS-CONTADOR

                   DISPLAY "OK ESPECTACULO "
                           ID-ESPECTACULO

               WHEN "02"

                   DISPLAY "AVISO: ESPECTACULO YA EXISTE "
                           ID-ESPECTACULO

               WHEN OTHER

                   DISPLAY "ERROR WRITE ESPECTACULO. ID="
                           ID-ESPECTACULO
                           " STATUS="
                           FS

           END-EVALUATE.


       BUSCAR-ULTIMA-TARJETA.

           MOVE 0 TO WS-ULTIMA-TARJETA
           MOVE "N" TO WS-ENCONTRADA

           READ F-TARJETAS NEXT RECORD

           PERFORM UNTIL FS-TAR NOT = "00"

               IF FS-TAR = "00"

                   MOVE "S" TO WS-ENCONTRADA

                   IF TNUM > WS-ULTIMA-TARJETA

                       MOVE TNUM
                           TO WS-ULTIMA-TARJETA

                   END-IF

                   READ F-TARJETAS NEXT RECORD

               END-IF

           END-PERFORM

           IF WS-ENCONTRADA = "N"

               DISPLAY "NO SE ENCONTRARON TARJETAS"

               MOVE 0 TO WS-ULTIMA-TARJETA

           END-IF

           COMPUTE WS-NUEVA-TARJETA =
                   WS-ULTIMA-TARJETA + 1

           DISPLAY "ULTIMA TARJETA ENCONTRADA: "
                   WS-ULTIMA-TARJETA

           DISPLAY "SIGUIENTE TARJETA: "
                   WS-NUEVA-TARJETA.


       BUSCAR-ULTIMO-MOVIMIENTO.

           MOVE 0 TO WS-ULTIMO-MOV
           MOVE "N" TO WS-MOV-ENCONTRADO

           READ F-MOVIMIENTOS NEXT RECORD

           PERFORM UNTIL FS-MOV NOT = "00"

               IF FS-MOV = "00"

                   MOVE "S" TO WS-MOV-ENCONTRADO

                   IF MOV-NUM > WS-ULTIMO-MOV

                       MOVE MOV-NUM
                           TO WS-ULTIMO-MOV

                   END-IF

                   READ F-MOVIMIENTOS NEXT RECORD

               END-IF

           END-PERFORM

           IF WS-MOV-ENCONTRADO = "N"

               DISPLAY "NO SE ENCONTRARON MOVIMIENTOS"

               MOVE 0 TO WS-ULTIMO-MOV

           END-IF

           COMPUTE WS-NUEVO-MOV =
                   WS-ULTIMO-MOV + 1

           DISPLAY "ULTIMO MOVIMIENTO: "
                   WS-ULTIMO-MOV

           DISPLAY "NUEVO MOVIMIENTO: "
                   WS-NUEVO-MOV.


       CREAR-TARJETA.

           MOVE WS-NUEVA-TARJETA
               TO TNUM

           MOVE WS-NUEVA-TARJETA
               TO TPIN

           WRITE TAJETAREG

           IF FS-TAR = "00"

               DISPLAY "TARJETA CREADA: "
                       TNUM
                       " PIN: "
                       TPIN

               PERFORM CREAR-MOVIMIENTO
               PERFORM CREAR-INTENTOS

           ELSE

               IF FS-TAR = "02"

                   DISPLAY "AVISO: TARJETA YA EXISTE: "
                           TNUM

               ELSE

                   DISPLAY "ERROR WRITE TARJETA. STATUS="
                           FS-TAR

               END-IF

           END-IF.


       CREAR-MOVIMIENTO.

           MOVE WS-NUEVO-MOV
               TO MOV-NUM

           MOVE WS-NUEVA-TARJETA
               TO MOV-TARJETA

           MOVE 2030
               TO MOV-ANO

           MOVE 1
               TO MOV-MES

           MOVE 15
               TO MOV-DIA

           MOVE 12
               TO MOV-HOR

           MOVE 0
               TO MOV-MIN

           MOVE 0
               TO MOV-SEG

           MOVE 5000
               TO MOV-IMPORTE-ENT

           MOVE 00
               TO MOV-IMPORTE-DEC

           MOVE "INGRESO INICIAL DE PRUEBA"
               TO MOV-CONCEPTO

           MOVE 5000
               TO MOV-SALDOPOS-ENT

           MOVE 00
               TO MOV-SALDOPOS-DEC

           WRITE MOVIMIENTO-REG

           IF FS-MOV = "00"

               DISPLAY "MOVIMIENTO CREADO: "
                       MOV-NUM

               DISPLAY "SALDO TARJETA "
                       WS-NUEVA-TARJETA
                       " = 5000.00 EUR"

           ELSE

               DISPLAY "ERROR WRITE MOVIMIENTO. STATUS="
                       FS-MOV

           END-IF.

       CREAR-INTENTOS.
       
           MOVE WS-NUEVA-TARJETA TO INT-TARJETA
           MOVE 3 TO INT-NUM
           
           WRITE REG-INTENTOS
           
           IF FS-INT = "00"
               DISPLAY "INTENTOS INICIALIZADOS PARA: "
                       INT-TARJETA
           ELSE
               DISPLAY "ERROR WRITE INTENTOS. STATUS="
                       FS-INT
           END-IF.


       CERRAR-FICHEROS.

           CLOSE F-ESPECTACULOS
                 F-TARJETAS
                 F-MOVIMIENTOS
                 F-INTENTOS.
