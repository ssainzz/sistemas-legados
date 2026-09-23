       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK1.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CRT STATUS IS KEYBOARD-STATUS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT TARJETAS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS TNUM
           FILE STATUS IS FST.

           SELECT INTENTOS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS INUM
           FILE STATUS IS FSI.

           SELECT F-MOVIMIENTOS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS MOV-NUM
           FILE STATUS IS FSM.

           SELECT F-TRANSFERENCIAS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS TRF-ID
           FILE STATUS IS FS-TRF.


       DATA DIVISION.
       FILE SECTION.
       FD TARJETAS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "tarjetas.ubd".
       01 TAJETAREG.
           02 TNUM      PIC 9(16).
           02 TPIN      PIC  9(4).

       FD INTENTOS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "intentos.ubd".
       01 INTENTOSREG.
           02 INUM      PIC 9(16).
           02 IINTENTOS PIC 9(1).

       FD F-MOVIMIENTOS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "movimientos.ubd".
       01 MOVIMIENTO-REG.
           02 MOV-NUM              PIC  9(35).
           02 MOV-TARJETA          PIC  9(16).
           02 MOV-ANO              PIC   9(4).
           02 MOV-MES              PIC   9(2).
           02 MOV-DIA              PIC   9(2).
           02 MOV-HOR              PIC   9(2).
           02 MOV-MIN              PIC   9(2).
           02 MOV-SEG              PIC   9(2).
           02 MOV-IMPORTE-ENT      PIC  S9(7).
           02 MOV-IMPORTE-DEC      PIC   9(2).
           02 MOV-CONCEPTO         PIC  X(35).
           02 MOV-SALDOPOS-ENT     PIC  S9(9).
           02 MOV-SALDOPOS-DEC     PIC   9(2).

       FD F-TRANSFERENCIAS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "transferencias.ubd".
       01 TRF-REG.
           02 TRF-ID              PIC 9(8).
           02 TRF-ORIGEN          PIC 9(16).
           02 TRF-DESTINO         PIC 9(16).
           02 TRF-IMPORTE-ENT     PIC 9(7).
           02 TRF-IMPORTE-DEC     PIC 9(2).
           02 TRF-TIPO            PIC X.
           02 TRF-FECHA           PIC 9(8).
           02 TRF-ESTADO          PIC X.

       WORKING-STORAGE SECTION.
       77 FST                      PIC  X(2).
       77 FSI                      PIC  X(2).
       77 FSM                      PIC  X(2).
       77 FS-TRF                   PIC  X(2).

       78 BLACK   VALUE 0.
       78 BLUE    VALUE 1.
       78 GREEN   VALUE 2.
       78 CYAN    VALUE 3.
       78 RED     VALUE 4.
       78 MAGENTA VALUE 5.
       78 YELLOW  VALUE 6.
       78 WHITE   VALUE 7.

       01 CAMPOS-FECHA.
           05 FECHA.
               10 ANO              PIC  9(4).
               10 MES              PIC  9(2).
               10 DIA              PIC  9(2).
           05 HORA.
               10 HORAS            PIC  9(2).
               10 MINUTOS          PIC  9(2).
               10 SEGUNDOS         PIC  9(2).
               10 MILISEGUNDOS     PIC  9(2).
           05 DIF-GMT              PIC S9(4).

       01 KEYBOARD-STATUS           PIC 9(4).
           88 ENTER-PRESSED          VALUE 0.
           88 PGUP-PRESSED        VALUE 2001.
           88 PGDN-PRESSED        VALUE 2002.
           88 UP-ARROW-PRESSED    VALUE 2003.
           88 DOWN-ARROW-PRESSED  VALUE 2004.
           88 ESC-PRESSED         VALUE 2005.

       77 PRESSED-KEY              PIC  9(4).
       77 PIN-INTRODUCIDO          PIC  9(4).
       77 CHOICE                   PIC X.

       *> Variables auxiliares para la ejecución de transferencias
       77 HOY-NUM                  PIC  9(8).
       77 CURRENT-TRF-ID           PIC  9(8).
       77 MAX-TRF-ID               PIC  9(8).
       77 MAX-MOV-NUM              PIC  9(35).
       77 LAST-ORIGEN-MOV-NUM      PIC  9(35).
       77 LAST-DESTINO-MOV-NUM     PIC  9(35).
       77 SALDO-ORIGEN-ENT         PIC S9(9).
       77 SALDO-ORIGEN-DEC         PIC  9(2).
       77 SALDO-DESTINO-ENT        PIC S9(9).
       77 SALDO-DESTINO-DEC        PIC  9(2).
       77 CENT-SALDO               PIC S9(11).
       77 CENT-SALDO-DST           PIC S9(11).
       77 CENT-TRF                 PIC S9(11).
       77 CENT-NUEVO-SALDO         PIC S9(11).
       77 CENT-NUEVO-SALDO-DST     PIC S9(11).
       77 AUX-ANO                  PIC  9(4).
       77 AUX-MES                  PIC  9(2).
       77 AUX-DIA                  PIC  9(2).
       77 AUX-RESTO                PIC  9(4).
       77 NUEVA-FECHA              PIC  9(8).


       SCREEN SECTION.
       01 BLANK-SCREEN.
           05 FILLER LINE 1 BLANK SCREEN BACKGROUND-COLOR BLACK.

       01 DATA-ACCEPT.
           05 TARJETA-ACCEPT BLANK ZERO AUTO LINE 08 COL 50
               PIC 9(16) USING TNUM.
           05 PIN-ACCEPT BLANK ZERO SECURE LINE 09 COL 50
               PIC 9(4) USING PIN-INTRODUCIDO.



       PROCEDURE DIVISION.
       INICIO.
           *> Comprobación de transferencias pendientes al abrir la app
           PERFORM VERIFICAR-TRANSFERENCIAS THRU FIN-VERIFICAR-TRF.

       IMPRIMIR-CABECERA.

           SET ENVIRONMENT 'COB_SCREEN_EXCEPTIONS' TO 'Y'
           SET ENVIRONMENT 'COB_SCREEN_ESC'        TO 'Y'

           DISPLAY BLANK-SCREEN.

           DISPLAY (2, 26) "Cajero Automatico UnizarBank"
               WITH FOREGROUND-COLOR IS BLUE HIGHLIGHT.

           MOVE FUNCTION CURRENT-DATE TO CAMPOS-FECHA.

           DISPLAY (4, 32) DIA.
           DISPLAY (4, 34) "-".
           DISPLAY (4, 35) MES.
           DISPLAY (4, 37) "-".
           DISPLAY (4, 38) ANO.
           DISPLAY (4, 44) HORAS.
           DISPLAY (4, 46) ":".
           DISPLAY (4, 47) MINUTOS.


       P1.
           DISPLAY (8, 28) "Bienvenido a UnizarBank".
           DISPLAY (10, 18) 
               "Por favor, introduzca la tarjeta para operar".

           DISPLAY (24, 33) "Enter - Aceptar".

       P1-ENTER.
           ACCEPT (24, 80) CHOICE
           IF ENTER-PRESSED
               GO TO P2
           ELSE
               GO TO P1-ENTER.


       P2.
           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (24, 33) "ESC - Salir".
           INITIALIZE TNUM.
           INITIALIZE PIN-INTRODUCIDO.
           INITIALIZE TPIN.
           DISPLAY (8, 15) "Numero de tarjeta:".
           DISPLAY (9, 15) "Inserte el pin de tarjeta:".
           ACCEPT DATA-ACCEPT
               IF ESC-PRESSED
                   GO TO IMPRIMIR-CABECERA.

           OPEN I-O TARJETAS.
           IF FST NOT = 00
               GO TO PSYS-ERR.
           READ TARJETAS INVALID KEY GO TO PSYS-ERR.

           OPEN I-O INTENTOS.
           IF FSI NOT = 00
               GO TO PSYS-ERR.
           MOVE TNUM TO INUM.

           READ INTENTOS INVALID KEY GO TO PSYS-ERR.

           IF IINTENTOS = 0
               GO TO PINT-ERR.

           IF PIN-INTRODUCIDO NOT = TPIN
               GO TO PPIN-ERR.

           PERFORM REINICIAR-INTENTOS THRU REINICIAR-INTENTOS.

       PMENU.
           CLOSE TARJETAS.
           CLOSE INTENTOS.

           *> Comprobación periódica al refrescar menú
           PERFORM VERIFICAR-TRANSFERENCIAS THRU FIN-VERIFICAR-TRF.

           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (8, 15) "1 - Consultar saldo".
           DISPLAY (9, 15) "2 - Consultar movimientos".
           DISPLAY (10, 15) "3 - Retirar efectivo".
           DISPLAY (11, 15) "4 - Ingresar efectivo".
           DISPLAY (12, 15) "5 - Ordenar transferencia".
           DISPLAY (13, 15) "6 - Listado de transferencias".
           DISPLAY (14, 15) "7 - Comprar entradas de espectaculos".
           DISPLAY (15, 15) "8 - Cambiar clave".
           DISPLAY (24, 34) "ESC - Salir".

       PMENUA1.
           ACCEPT (24, 80) CHOICE.
               IF ESC-PRESSED
                   GO TO IMPRIMIR-CABECERA.

           IF CHOICE = "1"
               CALL "BANK2" USING TNUM
               GO TO PMENU.

           IF CHOICE = "2"
               CALL "BANK3" USING TNUM
               GO TO PMENU.

           IF CHOICE = "3"
               CALL "BANK4" USING TNUM
               GO TO PMENU.

           IF CHOICE = "4"
               CALL "BANK5" USING TNUM
               GO TO PMENU.

           IF CHOICE = "5"
               CALL "BANK6" USING TNUM
               GO TO PMENU.

           IF CHOICE = "6"
               CALL "BANK9" USING TNUM
               GO TO PMENU.

           IF CHOICE = "7"
               CALL "BANK7" USING TNUM
               GO TO PMENU.

           IF CHOICE = "8"
               CALL "BANK8" USING TNUM

               *> Comprobar si BANK8 ha bloqueado la tarjeta
               OPEN I-O INTENTOS
               MOVE TNUM TO INUM
               READ INTENTOS
               IF IINTENTOS = 0
                   CLOSE INTENTOS
                   GO TO IMPRIMIR-CABECERA
               END-IF
               CLOSE INTENTOS

               GO TO PMENU.

           GO TO PMENU.


       PSYS-ERR.

           CLOSE TARJETAS.
           CLOSE INTENTOS.

           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (9, 25) "Ha ocurrido un error interno"
               WITH FOREGROUND-COLOR IS WHITE
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 32) "Vuelva mas tarde"
               WITH FOREGROUND-COLOR IS WHITE
                    BACKGROUND-COLOR IS RED.
           DISPLAY (24, 33) "Enter - Aceptar".
           GO TO PINT-ERR-ENTER.


       PINT-ERR.

           CLOSE TARJETAS.
           CLOSE INTENTOS.

           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (9, 20) "Se ha sobrepasado el numero de intentos"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 18) 
               "Por su seguridad se ha bloqueado la tarjeta"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (12, 30) "Acuda a una sucursal"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (24, 33) "Enter - Aceptar".

       PINT-ERR-ENTER.
           ACCEPT (24, 80) CHOICE
           IF ENTER-PRESSED
               GO TO IMPRIMIR-CABECERA
           ELSE
               GO TO PINT-ERR-ENTER.


       PPIN-ERR.
           SUBTRACT 1 FROM IINTENTOS.
           REWRITE INTENTOSREG INVALID KEY GO TO PSYS-ERR.

           CLOSE TARJETAS.
           CLOSE INTENTOS.

           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (9, 26) "El codigo PIN es incorrecto"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 30) "Le quedan "
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 40) IINTENTOS
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 42) " intentos"

               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.

           DISPLAY (24, 1) "Enter - Aceptar".
           DISPLAY (24, 65) "ESC - Cancelar".

       PPIN-ERR-ENTER.
           ACCEPT (24, 80) CHOICE
           IF ENTER-PRESSED
               GO TO P2
           ELSE
               IF ESC-PRESSED
                   GO TO IMPRIMIR-CABECERA
               ELSE
                   GO TO PPIN-ERR-ENTER.


       REINICIAR-INTENTOS.
           MOVE 3 TO IINTENTOS.
           REWRITE INTENTOSREG INVALID KEY GO TO PSYS-ERR.


       *> -----------------------------------------------------------
       *> Rutinas de ejecución automática de transferencias
       *> -----------------------------------------------------------
       VERIFICAR-TRANSFERENCIAS.
           MOVE FUNCTION CURRENT-DATE TO CAMPOS-FECHA.
           COMPUTE HOY-NUM = (ANO * 10000) + (MES * 100) + DIA.

           OPEN I-O F-TRANSFERENCIAS.
           IF FS-TRF = "35"
               OPEN OUTPUT F-TRANSFERENCIAS
               CLOSE F-TRANSFERENCIAS
               OPEN I-O F-TRANSFERENCIAS
           END-IF.
           IF FS-TRF NOT = "00"
               GO TO FIN-VERIFICAR-TRF.

           OPEN I-O F-MOVIMIENTOS.
           IF FSM = "35"
               OPEN OUTPUT F-MOVIMIENTOS
               CLOSE F-MOVIMIENTOS
               OPEN I-O F-MOVIMIENTOS
           END-IF.
           IF FSM NOT = "00"
               CLOSE F-TRANSFERENCIAS
               GO TO FIN-VERIFICAR-TRF.

           *> 1. Buscar Maximo ID de transferencias actual
           MOVE 0 TO TRF-ID.
           MOVE 0 TO MAX-TRF-ID.
           START F-TRANSFERENCIAS KEY >= TRF-ID
               INVALID KEY GO TO FIN-BUSCAR-MAX-TRF.
       BUSCAR-MAX-TRF.
           READ F-TRANSFERENCIAS NEXT RECORD
               AT END GO TO FIN-BUSCAR-MAX-TRF
           END-READ.
           IF TRF-ID > MAX-TRF-ID
               MOVE TRF-ID TO MAX-TRF-ID
           END-IF.
           GO TO BUSCAR-MAX-TRF.
       FIN-BUSCAR-MAX-TRF.

           *> 2. Bucle para buscar pendientes ('P') listas para ejecutar
           MOVE 0 TO TRF-ID.
           START F-TRANSFERENCIAS KEY >= TRF-ID
               INVALID KEY GO TO FIN-BUCLE-TRF
           END-START.
       BUCLE-TRF.
           READ F-TRANSFERENCIAS NEXT RECORD
               AT END GO TO FIN-BUCLE-TRF
           END-READ.
           MOVE TRF-ID TO CURRENT-TRF-ID.

           IF TRF-ESTADO = "P" AND TRF-FECHA <= HOY-NUM
               PERFORM EJECUTAR-TRF THRU FIN-EJECUTAR-TRF
               *> Restaurar puntero de lectura que pudo ser alterado
               MOVE CURRENT-TRF-ID TO TRF-ID
               START F-TRANSFERENCIAS KEY = TRF-ID
                   INVALID KEY CONTINUE
               END-START
           END-IF.
           GO TO BUCLE-TRF.

       FIN-BUCLE-TRF.
           CLOSE F-MOVIMIENTOS.
           CLOSE F-TRANSFERENCIAS.
       FIN-VERIFICAR-TRF.
           EXIT.

       EJECUTAR-TRF.
           *> Extraer ultimo movimiento de origen y destino,
           *> capturando tambien sus saldos en el mismo barrido
           MOVE 0 TO MAX-MOV-NUM.
           MOVE 0 TO LAST-ORIGEN-MOV-NUM.
           MOVE 0 TO LAST-DESTINO-MOV-NUM.
           MOVE 0 TO SALDO-ORIGEN-ENT.
           MOVE 0 TO SALDO-ORIGEN-DEC.
           MOVE 0 TO SALDO-DESTINO-ENT.
           MOVE 0 TO SALDO-DESTINO-DEC.
           MOVE 0 TO MOV-NUM.
           START F-MOVIMIENTOS KEY >= MOV-NUM
               INVALID KEY GO TO FIN-BUSQUEDA-MOV
           END-START.
       BUSQUEDA-MOV.
           READ F-MOVIMIENTOS NEXT RECORD
               AT END GO TO FIN-BUSQUEDA-MOV
           END-READ.
           IF MOV-NUM > MAX-MOV-NUM
               MOVE MOV-NUM TO MAX-MOV-NUM
           END-IF.
           IF MOV-TARJETA = TRF-ORIGEN
               IF MOV-NUM > LAST-ORIGEN-MOV-NUM
                   MOVE MOV-NUM TO LAST-ORIGEN-MOV-NUM
                   MOVE MOV-SALDOPOS-ENT TO SALDO-ORIGEN-ENT
                   MOVE MOV-SALDOPOS-DEC TO SALDO-ORIGEN-DEC
               END-IF
           END-IF.
           IF MOV-TARJETA = TRF-DESTINO
               IF MOV-NUM > LAST-DESTINO-MOV-NUM
                   MOVE MOV-NUM TO LAST-DESTINO-MOV-NUM
                   MOVE MOV-SALDOPOS-ENT TO SALDO-DESTINO-ENT
                   MOVE MOV-SALDOPOS-DEC TO SALDO-DESTINO-DEC
               END-IF
           END-IF.
           GO TO BUSQUEDA-MOV.
       FIN-BUSQUEDA-MOV.

           COMPUTE CENT-SALDO = (SALDO-ORIGEN-ENT * 100) +
                                 SALDO-ORIGEN-DEC.
           COMPUTE CENT-TRF = (TRF-IMPORTE-ENT * 100) +
                               TRF-IMPORTE-DEC.

           *> Origen en números negativos (no debería pasar): 
           *> marcar como Fallida
           IF SALDO-ORIGEN-ENT < 0
               MOVE CURRENT-TRF-ID TO TRF-ID
               READ F-TRANSFERENCIAS
                   INVALID KEY CONTINUE
               END-READ
               MOVE "F" TO TRF-ESTADO
               REWRITE TRF-REG
               GO TO FIN-EJECUTAR-TRF
           END-IF.

           *> Saldo insuficiente: marcar como Fallida
           IF CENT-SALDO < CENT-TRF
               MOVE CURRENT-TRF-ID TO TRF-ID
               READ F-TRANSFERENCIAS
                   INVALID KEY CONTINUE
               END-READ
               MOVE "F" TO TRF-ESTADO
               REWRITE TRF-REG
               GO TO FIN-EJECUTAR-TRF
           END-IF.

           *> Realizar el Cargo al Origen
           ADD 1 TO MAX-MOV-NUM.
           MOVE MAX-MOV-NUM TO MOV-NUM.
           MOVE TRF-ORIGEN TO MOV-TARJETA.
           MOVE ANO TO MOV-ANO. MOVE MES TO MOV-MES. MOVE DIA TO MOV-DIA.
           MOVE HORAS TO MOV-HOR. MOVE MINUTOS TO MOV-MIN.
           MOVE SEGUNDOS TO MOV-SEG.
           COMPUTE MOV-IMPORTE-ENT = TRF-IMPORTE-ENT * -1.
           MOVE TRF-IMPORTE-DEC TO MOV-IMPORTE-DEC.
           MOVE "Transferencia enviada" TO MOV-CONCEPTO.
           COMPUTE CENT-NUEVO-SALDO = CENT-SALDO - CENT-TRF.
           DIVIDE CENT-NUEVO-SALDO BY 100 GIVING MOV-SALDOPOS-ENT
               REMAINDER MOV-SALDOPOS-DEC.
           WRITE MOVIMIENTO-REG
               INVALID KEY GO TO FIN-EJECUTAR-TRF
           END-WRITE.

           *> Realizar el Abono al Destino
           COMPUTE CENT-SALDO-DST = (SALDO-DESTINO-ENT * 100) +
                                     SALDO-DESTINO-DEC.
           COMPUTE CENT-NUEVO-SALDO-DST = CENT-SALDO-DST + CENT-TRF.
           ADD 1 TO MAX-MOV-NUM.
           MOVE MAX-MOV-NUM TO MOV-NUM.
           MOVE TRF-DESTINO TO MOV-TARJETA.
           MOVE ANO TO MOV-ANO. MOVE MES TO MOV-MES. MOVE DIA TO MOV-DIA.
           MOVE HORAS TO MOV-HOR. MOVE MINUTOS TO MOV-MIN.
           MOVE SEGUNDOS TO MOV-SEG.
           MOVE TRF-IMPORTE-ENT TO MOV-IMPORTE-ENT.
           MOVE TRF-IMPORTE-DEC TO MOV-IMPORTE-DEC.
           MOVE "Transferencia recibida" TO MOV-CONCEPTO.
           DIVIDE CENT-NUEVO-SALDO-DST BY 100 GIVING MOV-SALDOPOS-ENT
               REMAINDER MOV-SALDOPOS-DEC.
           WRITE MOVIMIENTO-REG.

           *> Actualizar el estado de la transferencia
           MOVE CURRENT-TRF-ID TO TRF-ID.
           READ F-TRANSFERENCIAS
               INVALID KEY CONTINUE
           END-READ.
           MOVE "E" TO TRF-ESTADO.
           REWRITE TRF-REG.

           *> Reprogramar si es periodica mensual
           IF TRF-TIPO = "M" OR TRF-TIPO = "m"
               PERFORM PROGRAMAR-SIGUIENTE-TRF
                   THRU FIN-PROGRAMAR-SIGUIENTE
           END-IF.
       FIN-EJECUTAR-TRF.
           EXIT.

       PROGRAMAR-SIGUIENTE-TRF.
           DIVIDE TRF-FECHA BY 10000 GIVING AUX-ANO REMAINDER AUX-RESTO.
           DIVIDE AUX-RESTO BY 100 GIVING AUX-MES REMAINDER AUX-DIA.
           ADD 1 TO AUX-MES.
           IF AUX-MES > 12
               MOVE 1 TO AUX-MES
               ADD 1 TO AUX-ANO
           END-IF.
           COMPUTE NUEVA-FECHA = (AUX-ANO * 10000) + 
                                 (AUX-MES * 100) + AUX-DIA.

           ADD 1 TO MAX-TRF-ID.
           MOVE MAX-TRF-ID TO TRF-ID.
           MOVE NUEVA-FECHA TO TRF-FECHA.
           MOVE "P" TO TRF-ESTADO.
           WRITE TRF-REG.
       FIN-PROGRAMAR-SIGUIENTE.
           EXIT.
