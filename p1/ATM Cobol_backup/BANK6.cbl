       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK6.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CRT STATUS IS KEYBOARD-STATUS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT TARJETAS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS TNUM-E
           FILE STATUS IS FST.

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
           02 TNUM-E      PIC 9(16).
           02 TPIN-E      PIC  9(4).
       
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
       77 FST                      PIC   X(2).
       77 FSM                      PIC   X(2).
       77 FS-TRF                   PIC   X(2).

       78 BLACK                  VALUE      0.
       78 BLUE                   VALUE      1.
       78 GREEN                  VALUE      2.
       78 CYAN                   VALUE      3.
       78 RED                    VALUE      4.
       78 MAGENTA                VALUE      5.
       78 YELLOW                 VALUE      6.
       78 WHITE                  VALUE      7.

       01 CAMPOS-FECHA.
           05 FECHA.
               10 ANO              PIC   9(4).
               10 MES              PIC   9(2).
               10 DIA              PIC   9(2).
           05 HORA.
               10 HORAS            PIC   9(2).
               10 MINUTOS          PIC   9(2).
               10 SEGUNDOS         PIC   9(2).
               10 MILISEGUNDOS     PIC   9(2).
           05 DIF-GMT              PIC  S9(4).

       01 KEYBOARD-STATUS          PIC  9(4).
           88 ENTER-PRESSED      VALUE     0.
           88 PGUP-PRESSED       VALUE  2001.
           88 PGDN-PRESSED       VALUE  2002.
           88 UP-ARROW-PRESSED   VALUE  2003.
           88 DOWN-ARROW-PRESSED VALUE  2004.
           88 ESC-PRESSED        VALUE  2005.

       77 PRESSED-KEY              PIC   X.

       77 LAST-MOV-NUM             PIC  9(35).
       77 LAST-USER-ORD-MOV-NUM    PIC  9(35).
       77 LAST-USER-DST-MOV-NUM    PIC  9(35).
       77 LAST-TRF-ID              PIC  9(8).

       77 SALDO-USER-ENT           PIC  S9(9).
       77 SALDO-USER-DEC           PIC   9(2).

       77 EURENT-USUARIO           PIC   9(7).
       77 EURDEC-USUARIO           PIC   9(2).
       77 CUENTA-DESTINO           PIC  9(16).
       77 NOMBRE-DESTINO           PIC  X(35).

       77 CENT-SALDO-ORD-USER      PIC  S9(9).
       77 CENT-SALDO-DST-USER      PIC  S9(9).
       77 CENT-IMPOR-USER          PIC  S9(9).

       77 MSJ-ORD                  PIC  X(35).
       77 MSJ-DST                  PIC  X(35).

       77 TIPO-TRF                 PIC  X.
       77 TRF-DIA                  PIC  9(2).
       77 TRF-MES                  PIC  9(2).
       77 TRF-ANO                  PIC  9(4).
       77 TRF-FECHA-NUM            PIC  9(8).
       77 AUX-MES                  PIC  9(2).
       77 AUX-ANO                  PIC  9(4).

       *> Variables auxiliares para la ejecucion inmediata de 
       *> transferencias
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

       LINKAGE SECTION.
       77 TNUM                     PIC  9(16).

       SCREEN SECTION.
       01 BLANK-SCREEN.
           05 FILLER LINE 1 BLANK SCREEN BACKGROUND-COLOR BLACK.

       01 FILTRO-CUENTA.
           05 FILLER BLANK WHEN ZERO AUTO UNDERLINE
               LINE 12 COL 54 PIC 9(16) USING CUENTA-DESTINO.
           05 FILLER AUTO UNDERLINE
               LINE 14 COL 54 PIC X(15) USING NOMBRE-DESTINO.
           05 EUR-ENT-TRF BLANK WHEN ZERO AUTO UNDERLINE
               LINE 16 COL 54 PIC 9(7) USING EURENT-USUARIO.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 16 COL 63 PIC 9(2) USING EURDEC-USUARIO.

       01 SALDO-DISPLAY.
           05 FILLER LINE 10 COL 33 PIC 9(7) FROM MOV-SALDOPOS-ENT.
           05 FILLER LINE 10 COL 40 VALUE ",".
           05 FILLER LINE 10 COL 41 PIC 99 FROM MOV-SALDOPOS-DEC.
           05 FILLER LINE 10 COL 44 VALUE "EUR".

       01 PANTALLA-TIPO-TRF.
           05 FILLER LINE 18 COL 19
              VALUE "Tipo (I=Inmediata / P=Puntual / M=Mensual): ".
           05 TIPO-ACC UNDERLINE LINE 18 COL 63
              PIC X USING TIPO-TRF.

       01 PANTALLA-FECHA-PUNTUAL.
           05 FILLER LINE 19 COL 19
              VALUE "Fecha (DD/MM/AAAA) :   /  /    ".
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO LINE 19 COL 40
              PIC 9(2) USING TRF-DIA.
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO LINE 19 COL 43
              PIC 9(2) USING TRF-MES.
           05 FILLER BLANK WHEN ZERO UNDERLINE LINE 19 COL 46
              PIC 9(4) USING TRF-ANO.

       01 PANTALLA-DIA-MENSUAL.
           05 FILLER LINE 19 COL 19
              VALUE "Dia del mes (01-28):   ".
           05 FILLER BLANK WHEN ZERO UNDERLINE LINE 19 COL 40
              PIC 9(2) USING TRF-DIA.

       PROCEDURE DIVISION USING TNUM.
       INICIO.
           SET ENVIRONMENT 'COB_SCREEN_EXCEPTIONS' TO 'Y'.
           SET ENVIRONMENT 'COB_SCREEN_ESC' TO 'Y'.

           INITIALIZE CUENTA-DESTINO.
           INITIALIZE NOMBRE-DESTINO.
           INITIALIZE EURENT-USUARIO.
           INITIALIZE EURDEC-USUARIO.
           INITIALIZE LAST-MOV-NUM.
           INITIALIZE LAST-USER-ORD-MOV-NUM.
           INITIALIZE LAST-USER-DST-MOV-NUM.
           INITIALIZE LAST-TRF-ID.
           INITIALIZE SALDO-USER-ENT.
           INITIALIZE SALDO-USER-DEC.
           INITIALIZE TIPO-TRF.
           INITIALIZE TRF-DIA.
           INITIALIZE TRF-MES.
           INITIALIZE TRF-ANO.

       IMPRIMIR-CABECERA.
           DISPLAY BLANK-SCREEN.
           DISPLAY (2, 26) "Cajero Automatico UnizarBank"
               WITH FOREGROUND-COLOR IS 1.

           MOVE FUNCTION CURRENT-DATE TO CAMPOS-FECHA.

           DISPLAY (4, 32) DIA.
           DISPLAY (4, 34) "-".
           DISPLAY (4, 35) MES.
           DISPLAY (4, 37) "-".
           DISPLAY (4, 38) ANO.
           DISPLAY (4, 44) HORAS.
           DISPLAY (4, 46) ":".
           DISPLAY (4, 47) MINUTOS.

       MOVIMIENTOS-OPEN.
           OPEN I-O F-MOVIMIENTOS.
           IF FSM NOT = "00" THEN
               GO TO PSYS-ERR
           END-IF.

       LECTURA-MOVIMIENTOS.
           READ F-MOVIMIENTOS NEXT RECORD AT END GO TO ORDENACION-TRF.
           IF MOV-TARJETA = TNUM THEN
               IF LAST-USER-ORD-MOV-NUM < MOV-NUM THEN
                   MOVE MOV-NUM TO LAST-USER-ORD-MOV-NUM
                   MOVE MOV-SALDOPOS-ENT TO SALDO-USER-ENT
                   MOVE MOV-SALDOPOS-DEC TO SALDO-USER-DEC
               END-IF
           END-IF.
           IF LAST-MOV-NUM < MOV-NUM THEN
               MOVE MOV-NUM TO LAST-MOV-NUM
           END-IF.
           GO TO LECTURA-MOVIMIENTOS.

       ORDENACION-TRF.
           CLOSE F-MOVIMIENTOS.

           DISPLAY (8, 30) "Ordenar Transferencia".
           DISPLAY (10, 19) "Saldo Actual:".

           DISPLAY (24, 2) "Enter - Confirmar".
           DISPLAY (24, 66) "ESC - Cancelar".

           IF LAST-USER-ORD-MOV-NUM = 0 THEN
               MOVE 0 TO MOV-SALDOPOS-ENT
               MOVE 0 TO MOV-SALDOPOS-DEC
           ELSE
               MOVE SALDO-USER-ENT TO MOV-SALDOPOS-ENT
               MOVE SALDO-USER-DEC TO MOV-SALDOPOS-DEC
           END-IF.

           DISPLAY SALDO-DISPLAY.

       INDICAR-CTA-DST.
           DISPLAY (12, 19) "Indica la cuenta destino".
           DISPLAY (14, 19) "y nombre del titular".
           DISPLAY (16, 19) "Indique la cantidad a transferir".
           DISPLAY (16, 61) ",".
           DISPLAY (16, 66) "EUR".

           ACCEPT FILTRO-CUENTA.
           IF ESC-PRESSED THEN
               EXIT PROGRAM
           END-IF.

           IF CUENTA-DESTINO = 0 THEN
               DISPLAY (20, 19)
                   "Error: Cuenta invalida o vacia!!"
                   WITH BACKGROUND-COLOR RED
               GO TO INDICAR-CTA-DST
           END-IF.

           IF CUENTA-DESTINO = TNUM THEN
               DISPLAY (20, 19)
                   "Error: No puedes transferirte a ti mismo"
                   WITH BACKGROUND-COLOR RED
               GO TO INDICAR-CTA-DST
           END-IF.

           COMPUTE CENT-IMPOR-USER = (EURENT-USUARIO * 100)
                                     + EURDEC-USUARIO.

           IF CENT-IMPOR-USER <= 0 THEN
               DISPLAY (20, 19)
                   "Error: Cantidad invalida"
                   WITH BACKGROUND-COLOR RED
               GO TO INDICAR-CTA-DST
           END-IF.

           DISPLAY (20, 19)
               "                                                "
               WITH BACKGROUND-COLOR BLACK.

       PEDIR-TIPO-TRF.
           DISPLAY (19, 1)
               "                                        ".
           DISPLAY (19, 41)
               "                                        ".
           DISPLAY (20, 1)
               "                                        ".
           DISPLAY (20, 41)
               "                                        ".

           ACCEPT PANTALLA-TIPO-TRF.
           IF ESC-PRESSED THEN
               EXIT PROGRAM
           END-IF.

           IF TIPO-TRF NOT = "I" AND TIPO-TRF NOT = "i" AND
              TIPO-TRF NOT = "P" AND TIPO-TRF NOT = "p" AND
              TIPO-TRF NOT = "M" AND TIPO-TRF NOT = "m" THEN
               DISPLAY (20, 19)
                   "Error: Indique I, P o M"
                   WITH BACKGROUND-COLOR RED
               GO TO PEDIR-TIPO-TRF
           END-IF.

           DISPLAY (20, 19)
               "                                                "
               WITH BACKGROUND-COLOR BLACK.

           *> Comprobacion de saldo suficiente para transferencia 
           *> Inmediata
           IF TIPO-TRF = "I" OR TIPO-TRF = "i" THEN
               COMPUTE CENT-SALDO-ORD-USER = (SALDO-USER-ENT * 100)
                                             + SALDO-USER-DEC
               IF CENT-SALDO-ORD-USER < CENT-IMPOR-USER THEN
                   DISPLAY (20, 19)
                       "Indique una cantidad menor!!"
                       WITH BACKGROUND-COLOR RED
                   GO TO INDICAR-CTA-DST
               END-IF
               MOVE DIA TO TRF-DIA
               MOVE MES TO TRF-MES
               MOVE ANO TO TRF-ANO
               GO TO FIN-PEDIR-FECHA
           END-IF.

           IF TIPO-TRF = "P" OR TIPO-TRF = "p" THEN
               GO TO PEDIR-FECHA-PUNTUAL
           ELSE
               GO TO PEDIR-DIA-MENSUAL
           END-IF.

       PEDIR-FECHA-PUNTUAL.
           ACCEPT PANTALLA-FECHA-PUNTUAL.
           IF ESC-PRESSED THEN
               EXIT PROGRAM
           END-IF.

           IF TRF-DIA NOT NUMERIC OR TRF-MES NOT NUMERIC OR
              TRF-ANO NOT NUMERIC OR
              TRF-DIA < 1 OR TRF-DIA > 31 OR
              TRF-MES < 1 OR TRF-MES > 12 OR
              TRF-ANO < 2024 THEN
               DISPLAY (20, 19)
                   "Error: Fecha invalida (Use 4 cifras para ano)   "
                   WITH BACKGROUND-COLOR RED
               GO TO PEDIR-FECHA-PUNTUAL
           END-IF.

           GO TO FIN-PEDIR-FECHA.

       PEDIR-DIA-MENSUAL.
           ACCEPT PANTALLA-DIA-MENSUAL.
           IF ESC-PRESSED THEN
               EXIT PROGRAM
           END-IF.

           IF TRF-DIA NOT NUMERIC OR
              TRF-DIA < 1 OR TRF-DIA > 28 THEN
               DISPLAY (20, 19)
                   "Error: Dia invalido (01-28)                     "
                   WITH BACKGROUND-COLOR RED
               GO TO PEDIR-DIA-MENSUAL
           END-IF.

       FIN-PEDIR-FECHA.
           DISPLAY (20, 19)
               "                                                "
               WITH BACKGROUND-COLOR BLACK.

           GO TO REALIZAR-TRF-VERIFICACION.

       REALIZAR-TRF-VERIFICACION.
           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (08, 30) "Ordenar Transferencia".
           IF TIPO-TRF = "I" OR TIPO-TRF = "i" THEN
               DISPLAY (11, 33) "Va a transferir:"
           ELSE
               DISPLAY (11, 23) "Va a programar una transferencia de:"
           END-IF.
           DISPLAY (12, 27) EURENT-USUARIO.
           DISPLAY (12, 34) ".".
           DISPLAY (12, 35) EURDEC-USUARIO.
           DISPLAY (12, 38) " EUR de su cuenta".
           DISPLAY (13, 19) "a la cuenta cuyo titular es".
           DISPLAY (13, 48) NOMBRE-DESTINO.

           DISPLAY (24, 2) "Enter - Confirmar".
           DISPLAY (24, 66) "ESC - Cancelar".

       ENTER-VERIFICACION.
           ACCEPT (24, 80) PRESSED-KEY.
           IF ESC-PRESSED THEN
               EXIT PROGRAM
           END-IF.
           IF ENTER-PRESSED THEN
               GO TO VERIFICACION-CTA-CORRECTA
           ELSE
               GO TO ENTER-VERIFICACION
           END-IF.

       VERIFICACION-CTA-CORRECTA.
           OPEN I-O TARJETAS.
           IF FST NOT = "00"
              GO TO PSYS-ERR.

           MOVE CUENTA-DESTINO TO TNUM-E.
           READ TARJETAS INVALID KEY GO TO USER-BAD.
           CLOSE TARJETAS.

           GO TO GUARDAR-TRF.

       GUARDAR-TRF.
           MOVE FUNCTION CURRENT-DATE TO CAMPOS-FECHA.

           IF TIPO-TRF = "I" OR TIPO-TRF = "i" THEN
               COMPUTE TRF-FECHA-NUM = (ANO * 10000) +
                                       (MES * 100) + DIA
           ELSE
               IF TIPO-TRF = "P" OR TIPO-TRF = "p" THEN
                   COMPUTE TRF-FECHA-NUM = (TRF-ANO * 10000) +
                                           (TRF-MES * 100) + TRF-DIA
               ELSE
                   MOVE MES TO AUX-MES
                   MOVE ANO TO AUX-ANO
                   IF TRF-DIA < DIA THEN
                       ADD 1 TO AUX-MES
                       IF AUX-MES > 12 THEN
                           MOVE 1 TO AUX-MES
                           ADD 1 TO AUX-ANO
                       END-IF
                   END-IF
                   COMPUTE TRF-FECHA-NUM = (AUX-ANO * 10000) +
                                           (AUX-MES * 100) + TRF-DIA
               END-IF
           END-IF.

           OPEN I-O F-TRANSFERENCIAS.
           IF FS-TRF = "35" THEN
               OPEN OUTPUT F-TRANSFERENCIAS
               CLOSE F-TRANSFERENCIAS
               OPEN I-O F-TRANSFERENCIAS
           END-IF.
           IF FS-TRF NOT = "00"
               GO TO PSYS-ERR
           END-IF.

           MOVE 0 TO LAST-TRF-ID.
           MOVE 0 TO TRF-ID.
           START F-TRANSFERENCIAS KEY >= TRF-ID
               INVALID KEY GO TO FIN-LECTURA-TRF.
       LECTURA-TRF.
           READ F-TRANSFERENCIAS NEXT RECORD AT END 
               GO TO FIN-LECTURA-TRF.
           IF TRF-ID > LAST-TRF-ID THEN
               MOVE TRF-ID TO LAST-TRF-ID
           END-IF.
           GO TO LECTURA-TRF.

       FIN-LECTURA-TRF.
           ADD 1 TO LAST-TRF-ID.
           MOVE LAST-TRF-ID TO TRF-ID.
           MOVE TNUM TO TRF-ORIGEN.
           MOVE CUENTA-DESTINO TO TRF-DESTINO.
           MOVE EURENT-USUARIO TO TRF-IMPORTE-ENT.
           MOVE EURDEC-USUARIO TO TRF-IMPORTE-DEC.
           
           IF TIPO-TRF = "I" OR TIPO-TRF = "i" THEN
               MOVE "I" TO TRF-TIPO
               MOVE "E" TO TRF-ESTADO
           ELSE
               IF TIPO-TRF = "p" THEN
                   MOVE "P" TO TRF-TIPO
                   MOVE "P" TO TRF-ESTADO
               ELSE
                   IF TIPO-TRF = "m" THEN
                       MOVE "M" TO TRF-TIPO
                       MOVE "P" TO TRF-ESTADO
                   ELSE
                       MOVE TIPO-TRF TO TRF-TIPO
                       MOVE "P" TO TRF-ESTADO
                   END-IF
               END-IF
           END-IF.

           MOVE TRF-FECHA-NUM TO TRF-FECHA.

           WRITE TRF-REG INVALID KEY GO TO PSYS-ERR.
           CLOSE F-TRANSFERENCIAS.

           IF TRF-TIPO = "I" THEN
               PERFORM EJECUTAR-TRF-INMEDIATA
                   THRU FIN-EJECUTAR-TRF-INMEDIATA
               GO TO I-EXITO
           ELSE
               GO TO P-EXITO
           END-IF.

       I-EXITO.
           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (8, 30) "Ordenar Transferencia".
           DISPLAY (11, 23) "Transferencia realizada con exito!".
           DISPLAY (24, 33) "Enter - Aceptar".

           GO TO EXIT-ENTER.

       P-EXITO.
           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (8, 29) "Programar transferencia".
           DISPLAY (11, 23) "Transferencia programada con exito!".
           DISPLAY (13, 26) "Queda pendiente de ejecucion.".
           DISPLAY (24, 33) "Enter - Aceptar".

           GO TO EXIT-ENTER.

       *> Rutina para la ejecucion inmediata en movimientos.ubd
       EJECUTAR-TRF-INMEDIATA.
           OPEN I-O F-MOVIMIENTOS.
           IF FSM = "35" THEN
               OPEN OUTPUT F-MOVIMIENTOS
               CLOSE F-MOVIMIENTOS
               OPEN I-O F-MOVIMIENTOS
           END-IF.
           IF FSM NOT = "00" THEN
               GO TO PSYS-ERR
           END-IF.

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
           IF MOV-NUM > MAX-MOV-NUM THEN
               MOVE MOV-NUM TO MAX-MOV-NUM
           END-IF.
           IF MOV-TARJETA = TNUM THEN
               IF MOV-NUM > LAST-ORIGEN-MOV-NUM THEN
                   MOVE MOV-NUM TO LAST-ORIGEN-MOV-NUM
                   MOVE MOV-SALDOPOS-ENT TO SALDO-ORIGEN-ENT
                   MOVE MOV-SALDOPOS-DEC TO SALDO-ORIGEN-DEC
               END-IF
           END-IF.
           IF MOV-TARJETA = CUENTA-DESTINO THEN
               IF MOV-NUM > LAST-DESTINO-MOV-NUM THEN
                   MOVE MOV-NUM TO LAST-DESTINO-MOV-NUM
                   MOVE MOV-SALDOPOS-ENT TO SALDO-DESTINO-ENT
                   MOVE MOV-SALDOPOS-DEC TO SALDO-DESTINO-DEC
               END-IF
           END-IF.
           GO TO BUSQUEDA-MOV.

       FIN-BUSQUEDA-MOV.
           COMPUTE CENT-SALDO = (SALDO-ORIGEN-ENT * 100) +
                                 SALDO-ORIGEN-DEC.
           COMPUTE CENT-TRF = (EURENT-USUARIO * 100) +
                               EURDEC-USUARIO.

           *> Realizar el Cargo al Origen
           ADD 1 TO MAX-MOV-NUM.
           MOVE MAX-MOV-NUM TO MOV-NUM.
           MOVE TNUM TO MOV-TARJETA.
           MOVE ANO TO MOV-ANO. MOVE MES TO MOV-MES. MOVE DIA TO MOV-DIA.
           MOVE HORAS TO MOV-HOR. MOVE MINUTOS TO MOV-MIN.
           MOVE SEGUNDOS TO MOV-SEG.
           COMPUTE MOV-IMPORTE-ENT = EURENT-USUARIO * -1.
           MOVE EURDEC-USUARIO TO MOV-IMPORTE-DEC.
           MOVE "Transferencia enviada" TO MOV-CONCEPTO.
           COMPUTE CENT-NUEVO-SALDO = CENT-SALDO - CENT-TRF.
           DIVIDE CENT-NUEVO-SALDO BY 100 GIVING MOV-SALDOPOS-ENT
               REMAINDER MOV-SALDOPOS-DEC.
           WRITE MOVIMIENTO-REG
               INVALID KEY GO TO PSYS-ERR
           END-WRITE.

           *> Realizar el Abono al Destino
           COMPUTE CENT-SALDO-DST = (SALDO-DESTINO-ENT * 100) +
                                     SALDO-DESTINO-DEC.
           COMPUTE CENT-NUEVO-SALDO-DST = CENT-SALDO-DST + CENT-TRF.
           ADD 1 TO MAX-MOV-NUM.
           MOVE MAX-MOV-NUM TO MOV-NUM.
           MOVE CUENTA-DESTINO TO MOV-TARJETA.
           MOVE ANO TO MOV-ANO. MOVE MES TO MOV-MES. MOVE DIA TO MOV-DIA.
           MOVE HORAS TO MOV-HOR. MOVE MINUTOS TO MOV-MIN.
           MOVE SEGUNDOS TO MOV-SEG.
           MOVE EURENT-USUARIO TO MOV-IMPORTE-ENT.
           MOVE EURDEC-USUARIO TO MOV-IMPORTE-DEC.
           MOVE "Transferencia recibida" TO MOV-CONCEPTO.
           DIVIDE CENT-NUEVO-SALDO-DST BY 100 GIVING MOV-SALDOPOS-ENT
               REMAINDER MOV-SALDOPOS-DEC.
           WRITE MOVIMIENTO-REG
               INVALID KEY GO TO PSYS-ERR
           END-WRITE.

           CLOSE F-MOVIMIENTOS.
       FIN-EJECUTAR-TRF-INMEDIATA.
           EXIT.

       PSYS-ERR.
           CLOSE TARJETAS.
           CLOSE F-MOVIMIENTOS.
           IF FS-TRF = "00" CLOSE F-TRANSFERENCIAS END-IF.

           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (09, 25) "Ha ocurrido un error interno"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 32) "Vuelva mas tarde"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (24, 33) "Enter - Aceptar".

       EXIT-ENTER.
           ACCEPT (24, 80) PRESSED-KEY.
           IF ENTER-PRESSED THEN
               EXIT PROGRAM
           ELSE
               GO TO EXIT-ENTER
           END-IF.

       USER-BAD.
           CLOSE TARJETAS.
           PERFORM IMPRIMIR-CABECERA THRU IMPRIMIR-CABECERA.
           DISPLAY (9, 22) "La cuenta introducida es incorrecta"
               WITH FOREGROUND-COLOR IS BLACK
                    BACKGROUND-COLOR IS RED.
           DISPLAY (24, 33) "Enter - Salir".
           GO TO EXIT-ENTER.
