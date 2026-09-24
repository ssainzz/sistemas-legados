       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK9.

       ENVIRONMENT DIVISION.
       CONFIGURATION SECTION.
       SPECIAL-NAMES.
           CRT STATUS IS KEYBOARD-STATUS.

       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT F-TRANSFERENCIAS ASSIGN TO DISK
           ORGANIZATION IS INDEXED
           ACCESS MODE IS DYNAMIC
           RECORD KEY IS TRF-ID
           FILE STATUS IS FSM.

       DATA DIVISION.
       FILE SECTION.
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
         01 FECHA-COMP-TRF           PIC 9(8).
         01 TIPO-LISTA               PIC X(10).
         01 ESTADO-LISTA             PIC X(10).
         01 CUENTA-CONTRAPARTE       PIC 9(16).

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
               WITH FOREGROUND-COLOR 1 HIGHLIGHT.
           DISPLAY (8, 25) "Listado de Transferencias".
           
           INITIALIZE FECHAS-FILTRO.
           ACCEPT PANTALLA-FILTRO.
           IF ESC-PRESSED
               EXIT PROGRAM.
               
           COMPUTE FECHA-COMP-INI = (FECHA-INI-A * 10000) +
                                    (FECHA-INI-M * 100) + FECHA-INI-D.
           COMPUTE FECHA-COMP-FIN = (FECHA-FIN-A * 10000) +
                                    (FECHA-FIN-M * 100) + FECHA-FIN-D.

           IF FECHA-COMP-INI > FECHA-COMP-FIN
               DISPLAY (20, 20) "Fecha inicial posterior a fecha final"
               GO TO PEDIR-FECHAS
           END-IF.

       PROCESAR-FICHERO.
           OPEN INPUT F-TRANSFERENCIAS.
           IF FSM NOT = "00"
               GO TO FIN-PROGRAMA.
               
           MOVE 0 TO TRF-ID.
           START F-TRANSFERENCIAS KEY IS GREATER THAN TRF-ID
               INVALID KEY GO TO CERRAR-FIN.
               
           MOVE 'N' TO EOF-REACHED.
           
       MOSTRAR-CABECERA-LISTA.
           DISPLAY BLANK-SCREEN.
           DISPLAY (2, 26) "Cajero Automatico UnizarBank" 
               WITH FOREGROUND-COLOR 1 HIGHLIGHT.
           DISPLAY (6, 5) "FECHA".
           DISPLAY (6, 17) "TIPO".
           DISPLAY (6, 28) "ESTADO".
           DISPLAY (6, 39) "IMPORTE".
           DISPLAY (6, 53) "CUENTA".
           DISPLAY (7, 5) "--------------------------------".
           MOVE 8 TO LINEA-PANTALLA.
           MOVE 0 TO CONTADOR-REGISTROS.

       LEER-TRANSFERENCIA.
           READ F-TRANSFERENCIAS NEXT RECORD AT END
               MOVE 'Y' TO EOF-REACHED.
               
           IF EOF-REACHED = 'Y'
               GO TO ESPERAR-TECLA.
               
           IF TRF-ORIGEN NOT = TNUM AND TRF-DESTINO NOT = TNUM
               GO TO LEER-TRANSFERENCIA.

           COMPUTE FECHA-COMP-TRF = TRF-FECHA.

           IF FECHA-COMP-TRF < FECHA-COMP-INI OR
              FECHA-COMP-TRF > FECHA-COMP-FIN
               GO TO LEER-TRANSFERENCIA.
               
           IF TRF-ESTADO NOT = "E" AND TRF-ESTADO NOT = "P"
               GO TO LEER-TRANSFERENCIA.

           IF TRF-TIPO = "M" OR TRF-TIPO = "m"
               MOVE "PERIODICA" TO TIPO-LISTA
           ELSE
               MOVE "PUNTUAL" TO TIPO-LISTA
           END-IF.

           IF TRF-ESTADO = "E"
               MOVE "EJECUTADA" TO ESTADO-LISTA
           ELSE
               MOVE "PENDIENTE" TO ESTADO-LISTA
           END-IF.

           IF TRF-ORIGEN = TNUM
               MOVE TRF-DESTINO TO CUENTA-CONTRAPARTE
           ELSE
               MOVE TRF-ORIGEN TO CUENTA-CONTRAPARTE
           END-IF.

           DISPLAY (LINEA-PANTALLA, 5) TRF-FECHA(7:2)
           DISPLAY (LINEA-PANTALLA, 7) "/"
           DISPLAY (LINEA-PANTALLA, 8) TRF-FECHA(5:2)
           DISPLAY (LINEA-PANTALLA, 10) "/"
           DISPLAY (LINEA-PANTALLA, 11) TRF-FECHA(1:4)
           DISPLAY (LINEA-PANTALLA, 17) TIPO-LISTA
           DISPLAY (LINEA-PANTALLA, 28) ESTADO-LISTA
           DISPLAY (LINEA-PANTALLA, 39) TRF-IMPORTE-ENT
           DISPLAY (LINEA-PANTALLA, 46) ","
           DISPLAY (LINEA-PANTALLA, 47) TRF-IMPORTE-DEC
           DISPLAY (LINEA-PANTALLA, 53) CUENTA-CONTRAPARTE

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
           END-IF.
           
           GO TO LEER-TRANSFERENCIA.

       ESPERAR-TECLA.
           DISPLAY (24, 25) "Enter / ESC - Volver al menu".
           ACCEPT PRESSED-KEY LINE 24 COLUMN 80.

       CERRAR-FIN.
           CLOSE F-TRANSFERENCIAS.
           
       FIN-PROGRAMA.
           EXIT PROGRAM.
           