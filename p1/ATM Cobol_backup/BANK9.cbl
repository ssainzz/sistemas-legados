       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK9.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CRT STATUS IS KEYBOARD-STATUS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT F-MOVIMIENTOS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS MOV-NUM
           FILE STATUS IS FSM.

       DATA DIVISION.
       FILE SECTION.
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

       WORKING-STORAGE SECTION.
       77 FSM                      PIC   X(2).
       
       01 FECHAS-FILTRO.
           05 FECHA-INI-D          PIC 9(2).
           05 FECHA-INI-M          PIC 9(2).
           05 FECHA-INI-A          PIC 9(4).
           05 FECHA-FIN-D          PIC 9(2).
           05 FECHA-FIN-M          PIC 9(2).
           05 FECHA-FIN-A          PIC 9(4).
           
       01 FECHA-COMP-INI           PIC 9(8).
       01 FECHA-COMP-FIN           PIC 9(8).
       01 FECHA-COMP-MOV           PIC 9(8).

       01 KEYBOARD-STATUS          PIC 9(4).
           88 ENTER-PRESSED        VALUE 0.
           88 ESC-PRESSED          VALUE 2005.

       77 PRESSED-KEY              PIC X.
       77 LINEA-PANTALLA           PIC 9(2) VALUE 10.
       77 CONTADOR-REGISTROS       PIC 9(2) VALUE 0.
       77 EOF-REACHED              PIC X VALUE 'N'.

       LINKAGE SECTION.
       77 TNUM                     PIC 9(16).

       SCREEN SECTION.
       01 BLANK-SCREEN.
           05 FILLER LINE 1 BLANK SCREEN BACKGROUND-COLOR 0.

       01 PANTALLA-FILTRO.
           05 FILLER LINE 10 COL 20 VALUE "Fecha Inicio (DD MM AAAA):".
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 10 COL 48 PIC 9(2) USING FECHA-INI-D.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 10 COL 51 PIC 9(2) USING FECHA-INI-M.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 10 COL 54 PIC 9(4) USING FECHA-INI-A.
           
           05 FILLER LINE 12 COL 20 VALUE "Fecha Fin    (DD MM AAAA):".
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 12 COL 48 PIC 9(2) USING FECHA-FIN-D.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 12 COL 51 PIC 9(2) USING FECHA-FIN-M.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 12 COL 54 PIC 9(4) USING FECHA-FIN-A.

       PROCEDURE DIVISION USING TNUM.
       INICIO.
           SET ENVIRONMENT 'COB_SCREEN_EXCEPTIONS' TO 'Y'.
           SET ENVIRONMENT 'COB_SCREEN_ESC'        TO 'Y'.

       PEDIR-FECHAS.
           DISPLAY BLANK-SCREEN.
           DISPLAY (2, 26) "Cajero Automatico UnizarBank" 
               WITH FOREGROUND-COLOR 1.
           DISPLAY (8, 25) "Listado de Transferencias".
           
           INITIALIZE FECHAS-FILTRO.
           ACCEPT PANTALLA-FILTRO.
           IF ESC-PRESSED
               EXIT PROGRAM.
               
           COMPUTE FECHA-COMP-INI = (FECHA-INI-A * 10000) + 
                                    (FECHA-INI-M * 100) + FECHA-INI-D.
           COMPUTE FECHA-COMP-FIN = (FECHA-FIN-A * 10000) + 
                                    (FECHA-FIN-M * 100) + FECHA-FIN-D.

       PROCESAR-FICHERO.
           OPEN INPUT F-MOVIMIENTOS.
           IF FSM NOT = "00"
               GO TO FIN-PROGRAMA.
               
           MOVE 0 TO MOV-NUM.
           START F-MOVIMIENTOS KEY IS GREATER THAN MOV-NUM
               INVALID KEY GO TO CERRAR-FIN.
               
           MOVE 'N' TO EOF-REACHED.
           
       MOSTRAR-CABECERA-LISTA.
           DISPLAY BLANK-SCREEN.
           DISPLAY (2, 26) "Cajero Automatico UnizarBank" 
               WITH FOREGROUND-COLOR 1.
           DISPLAY (6, 5) 
             "FECHA      TIPO      IMPORTE      CONCEPTO".
           DISPLAY (7, 5) 
             "-------------------------------------------------------".
           MOVE 8 TO LINEA-PANTALLA.
           MOVE 0 TO CONTADOR-REGISTROS.

       LEER-MOVIMIENTO.
           READ F-MOVIMIENTOS NEXT RECORD AT END
               MOVE 'Y' TO EOF-REACHED.
               
           IF EOF-REACHED = 'Y'
               GO TO ESPERAR-TECLA.
               
           IF MOV-TARJETA NOT = TNUM
               GO TO LEER-MOVIMIENTO.

           COMPUTE FECHA-COMP-MOV = (MOV-ANO * 10000) + 
                                    (MOV-MES * 100) + MOV-DIA.

           IF FECHA-COMP-MOV < FECHA-COMP-INI OR 
              FECHA-COMP-MOV > FECHA-COMP-FIN
               GO TO LEER-MOVIMIENTO.
               
           *> Filtro de concepto: Identificar si es transferencia
           IF MOV-CONCEPTO(1:12) = "Transferimos" OR 
              MOV-CONCEPTO(1:15) = "Nos transfieren"
              
               DISPLAY (LINEA-PANTALLA, 5) MOV-DIA
               DISPLAY (LINEA-PANTALLA, 7) "/"
               DISPLAY (LINEA-PANTALLA, 8) MOV-MES
               DISPLAY (LINEA-PANTALLA, 10) "/"
               DISPLAY (LINEA-PANTALLA, 11) MOV-ANO
               
               *> Por ahora, todo es 'Puntual' hasta actualizar BANK6
               DISPLAY (LINEA-PANTALLA, 16) "PUNTUAL"
               
               DISPLAY (LINEA-PANTALLA, 26) MOV-IMPORTE-ENT
               DISPLAY (LINEA-PANTALLA, 33) ","
               DISPLAY (LINEA-PANTALLA, 34) MOV-IMPORTE-DEC
               
               DISPLAY (LINEA-PANTALLA, 39) MOV-CONCEPTO(1:20)
               
               ADD 1 TO LINEA-PANTALLA
               ADD 1 TO CONTADOR-REGISTROS
               
               IF CONTADOR-REGISTROS = 10
                   DISPLAY (24, 20) 
                   "Enter - Pagina Siguiente | ESC - Salir"
                   ACCEPT PRESSED-KEY LINE 24 COLUMN 80
                   IF ESC-PRESSED
                       GO TO CERRAR-FIN
                   ELSE
                       GO TO MOSTRAR-CABECERA-LISTA
                   END-IF
               END-IF
           END-IF.
           
           GO TO LEER-MOVIMIENTO.

       ESPERAR-TECLA.
           DISPLAY (24, 25) "Enter / ESC - Volver al menu".
           ACCEPT PRESSED-KEY LINE 24 COLUMN 80.

       CERRAR-FIN.
           CLOSE F-MOVIMIENTOS.
           
       FIN-PROGRAMA.
           EXIT PROGRAM.
           