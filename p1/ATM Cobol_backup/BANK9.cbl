       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK9.
      *> ------------------------------------------------------------
      *> Opcion 6 - Listado de transferencias.
      *> Muestra, ordenadas por fecha, todas las transferencias de la
      *> tarjeta (enviadas y recibidas) ejecutadas, pendientes o
      *> fallidas entre dos fechas. Las transferencias mensuales
      *> pendientes se proyectan: aparece cada mes del intervalo.
      *> ------------------------------------------------------------

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
           FILE STATUS IS FS-TRF.

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
       77 FS-TRF                   PIC X(2).

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
               10 ANO              PIC 9(4).
               10 MES              PIC 9(2).
               10 DIA              PIC 9(2).
           05 HORA.
               10 HORAS            PIC 9(2).
               10 MINUTOS          PIC 9(2).
               10 SEGUNDOS         PIC 9(2).
               10 MILISEGUNDOS     PIC 9(2).
           05 DIF-GMT              PIC S9(4).

       01 KEYBOARD-STATUS          PIC 9(4).
           88 ENTER-PRESSED        VALUE 0.
           88 PGUP-PRESSED         VALUE 2001.
           88 PGDN-PRESSED         VALUE 2002.
           88 ESC-PRESSED          VALUE 2005.

       77 PRESSED-KEY              PIC X.

      *> Fechas del filtro introducidas por el usuario
       01 FECHAS-FILTRO.
           05 FECHA-INI-D          PIC 9(2).
           05 FECHA-INI-M          PIC 9(2).
           05 FECHA-INI-A          PIC 9(4).
           05 FECHA-FIN-D          PIC 9(2).
           05 FECHA-FIN-M          PIC 9(2).
           05 FECHA-FIN-A          PIC 9(4).

       77 FECHA-COMP-INI           PIC 9(8).
       77 FECHA-COMP-FIN           PIC 9(8).
       77 MENSAJE-ERROR            PIC X(45) VALUE SPACES.

      *> Tabla en memoria con las filas del listado
       78 MAX-FILAS                VALUE 300.
       78 MAX-MESES                VALUE 240.
       77 NUM-FILAS                PIC 9(3) VALUE 0.
       77 TABLA-LLENA              PIC X VALUE "N".
       01 TABLA-LISTADO.
           05 FILA OCCURS 300 TIMES.
               10 F-FECHA          PIC 9(8).
               10 F-TIPO           PIC X(9).
               10 F-ESTADO         PIC X(9).
               10 F-SIGNO          PIC X.
               10 F-IMP-ENT        PIC 9(7).
               10 F-IMP-DEC        PIC 9(2).
               10 F-CUENTA         PIC 9(16).
       01 FILA-AUX.
           10 A-FECHA              PIC 9(8).
           10 A-TIPO               PIC X(9).
           10 A-ESTADO             PIC X(9).
           10 A-SIGNO              PIC X.
           10 A-IMP-ENT            PIC 9(7).
           10 A-IMP-DEC            PIC 9(2).
           10 A-CUENTA             PIC 9(16).

      *> Datos de la fila que se va a anadir
       01 NUEVA-FILA.
           10 N-FECHA              PIC 9(8).
           10 N-TIPO               PIC X(9).
           10 N-ESTADO             PIC X(9).
           10 N-SIGNO              PIC X.
           10 N-IMP-ENT            PIC 9(7).
           10 N-IMP-DEC            PIC 9(2).
           10 N-CUENTA             PIC 9(16).

      *> Proyeccion de las mensuales
       77 FECHA-PROY               PIC 9(8).
       77 AUX-ANO                  PIC 9(4).
       77 AUX-MES                  PIC 9(2).
       77 AUX-DIA                  PIC 9(2).
       77 AUX-RESTO                PIC 9(4).
       77 CONT-MESES               PIC 9(3).

      *> Ordenacion y paginacion
       77 I                        PIC 9(3).
       77 J                        PIC 9(3).
       77 IDX-INICIO-PAG           PIC 9(3).
       77 IDX-FIN-PAG              PIC 9(3).
       77 PAGINA-ACTUAL            PIC 9(3).
       77 TOTAL-PAGINAS            PIC 9(3).
       77 LINEA-PANTALLA           PIC 9(2).

      *> Campos para mostrar
       01 FECHA-MOSTRAR            PIC 9(8).
       01 FECHA-MOSTRAR-R REDEFINES FECHA-MOSTRAR.
           05 FM-ANO               PIC 9(4).
           05 FM-MES               PIC 9(2).
           05 FM-DIA               PIC 9(2).
       77 IMPORTE-ED               PIC Z(6)9.
       77 PAGINA-ED                PIC ZZ9.

       LINKAGE SECTION.
       77 TNUM                     PIC 9(16).

       SCREEN SECTION.
       01 BLANK-SCREEN.
           05 FILLER LINE 1 BLANK SCREEN BACKGROUND-COLOR BLACK.

       01 PANTALLA-FILTRO.
           05 FILLER LINE 10 COL 12
               VALUE "Fecha inicio (DD MM AAAA):".
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 10 COL 40 PIC 9(2) USING FECHA-INI-D.
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 10 COL 43 PIC 9(2) USING FECHA-INI-M.
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 10 COL 46 PIC 9(4) USING FECHA-INI-A.
           05 FILLER LINE 12 COL 12
               VALUE "Fecha fin    (DD MM AAAA):".
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 12 COL 40 PIC 9(2) USING FECHA-FIN-D.
           05 FILLER BLANK WHEN ZERO UNDERLINE AUTO
               LINE 12 COL 43 PIC 9(2) USING FECHA-FIN-M.
           05 FILLER BLANK WHEN ZERO UNDERLINE
               LINE 12 COL 46 PIC 9(4) USING FECHA-FIN-A.

       PROCEDURE DIVISION USING TNUM.
       INICIO.
           SET ENVIRONMENT 'COB_SCREEN_EXCEPTIONS' TO 'Y'.
           SET ENVIRONMENT 'COB_SCREEN_ESC'        TO 'Y'.
           MOVE SPACES TO MENSAJE-ERROR.
           INITIALIZE FECHAS-FILTRO.

      *> ------------------------------------------------------------
      *> 1. Peticion y validacion del intervalo de fechas
      *> ------------------------------------------------------------
       PEDIR-FECHAS.
           PERFORM IMPRIMIR-CABECERA THRU FIN-IMPRIMIR-CABECERA.
           DISPLAY (8, 27) "Listado de transferencias".
           DISPLAY (14, 12)
               "Deje una fecha en blanco para no limitar el".
           DISPLAY (15, 12) "intervalo por ese extremo.".
           DISPLAY (24, 1) "Enter - Aceptar".
           DISPLAY (24, 66) "ESC - Cancelar".

           IF MENSAJE-ERROR NOT = SPACES
               DISPLAY (18, 12) MENSAJE-ERROR
                   WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED
               MOVE SPACES TO MENSAJE-ERROR
           END-IF.

           ACCEPT PANTALLA-FILTRO.
           IF ESC-PRESSED
               EXIT PROGRAM
           END-IF.

           IF FECHA-INI-D = 0 AND FECHA-INI-M = 0 AND FECHA-INI-A = 0
               MOVE 0 TO FECHA-COMP-INI
           ELSE
               COMPUTE FECHA-COMP-INI = (FECHA-INI-A * 10000) +
                                        (FECHA-INI-M * 100) +
                                         FECHA-INI-D
               IF FUNCTION TEST-DATE-YYYYMMDD(FECHA-COMP-INI) NOT = 0
                   MOVE "La fecha de inicio no es valida"
                       TO MENSAJE-ERROR
                   GO TO PEDIR-FECHAS
               END-IF
           END-IF.

           IF FECHA-FIN-D = 0 AND FECHA-FIN-M = 0 AND FECHA-FIN-A = 0
               MOVE 99991231 TO FECHA-COMP-FIN
           ELSE
               COMPUTE FECHA-COMP-FIN = (FECHA-FIN-A * 10000) +
                                        (FECHA-FIN-M * 100) +
                                         FECHA-FIN-D
               IF FUNCTION TEST-DATE-YYYYMMDD(FECHA-COMP-FIN) NOT = 0
                   MOVE "La fecha de fin no es valida"
                       TO MENSAJE-ERROR
                   GO TO PEDIR-FECHAS
               END-IF
           END-IF.

           IF FECHA-COMP-INI > FECHA-COMP-FIN
               MOVE "La fecha inicial es posterior a la final"
                   TO MENSAJE-ERROR
               GO TO PEDIR-FECHAS
           END-IF.

      *> ------------------------------------------------------------
      *> 2. Carga de las transferencias en la tabla
      *> ------------------------------------------------------------
       CARGAR-TRANSFERENCIAS.
           MOVE 0 TO NUM-FILAS.
           MOVE "N" TO TABLA-LLENA.

           OPEN INPUT F-TRANSFERENCIAS.
           IF FS-TRF = "35"
      *>       Todavia no se ha ordenado ninguna transferencia
               GO TO ORDENAR-TABLA
           END-IF.
           IF FS-TRF NOT = "00"
               GO TO PSYS-ERR
           END-IF.

           MOVE 0 TO TRF-ID.
           START F-TRANSFERENCIAS KEY IS NOT LESS THAN TRF-ID
               INVALID KEY GO TO FIN-CARGA
           END-START.

       LEER-TRANSFERENCIA.
           READ F-TRANSFERENCIAS NEXT RECORD
               AT END GO TO FIN-CARGA
           END-READ.

           IF TRF-ORIGEN NOT = TNUM AND TRF-DESTINO NOT = TNUM
               GO TO LEER-TRANSFERENCIA
           END-IF.

      *>   Datos comunes de la fila
           IF TRF-TIPO = "M" OR TRF-TIPO = "m"
               MOVE "PERIODICA" TO N-TIPO
           ELSE
               MOVE "PUNTUAL"   TO N-TIPO
           END-IF.
           IF TRF-ORIGEN = TNUM
               MOVE "-"         TO N-SIGNO
               MOVE TRF-DESTINO TO N-CUENTA
           ELSE
               MOVE "+"         TO N-SIGNO
               MOVE TRF-ORIGEN  TO N-CUENTA
           END-IF.
           MOVE TRF-IMPORTE-ENT TO N-IMP-ENT.
           MOVE TRF-IMPORTE-DEC TO N-IMP-DEC.

           EVALUATE TRUE
               WHEN TRF-ESTADO = "P" AND N-TIPO = "PERIODICA"
                   PERFORM PROYECTAR-MENSUAL THRU FIN-PROYECTAR
               WHEN TRF-ESTADO = "P"
                   MOVE "PENDIENTE" TO N-ESTADO
                   MOVE TRF-FECHA   TO N-FECHA
                   PERFORM ANADIR-SI-EN-RANGO THRU FIN-ANADIR
               WHEN TRF-ESTADO = "E"
                   MOVE "EJECUTADA" TO N-ESTADO
                   MOVE TRF-FECHA   TO N-FECHA
                   PERFORM ANADIR-SI-EN-RANGO THRU FIN-ANADIR
               WHEN TRF-ESTADO = "F"
                   MOVE "FALLIDA"   TO N-ESTADO
                   MOVE TRF-FECHA   TO N-FECHA
                   PERFORM ANADIR-SI-EN-RANGO THRU FIN-ANADIR
           END-EVALUATE.

           GO TO LEER-TRANSFERENCIA.

       FIN-CARGA.
           CLOSE F-TRANSFERENCIAS.

      *> ------------------------------------------------------------
      *> 3. Ordenacion por fecha (insercion, estable)
      *> ------------------------------------------------------------
       ORDENAR-TABLA.
           IF NUM-FILAS < 2
               GO TO MOSTRAR-LISTADO
           END-IF.
           PERFORM VARYING I FROM 2 BY 1 UNTIL I > NUM-FILAS
               MOVE FILA(I) TO FILA-AUX
               MOVE I TO J
               PERFORM UNTIL J < 2
                   IF F-FECHA(J - 1) > A-FECHA
                       MOVE FILA(J - 1) TO FILA(J)
                       SUBTRACT 1 FROM J
                   ELSE
                       EXIT PERFORM
                   END-IF
               END-PERFORM
               MOVE FILA-AUX TO FILA(J)
           END-PERFORM.

      *> ------------------------------------------------------------
      *> 4. Presentacion paginada (10 filas por pantalla)
      *> ------------------------------------------------------------
       MOSTRAR-LISTADO.
           COMPUTE TOTAL-PAGINAS = (NUM-FILAS + 9) / 10.
           IF TOTAL-PAGINAS = 0
               MOVE 1 TO TOTAL-PAGINAS
           END-IF.
           MOVE 1 TO PAGINA-ACTUAL.

       MOSTRAR-PAGINA.
           PERFORM IMPRIMIR-CABECERA THRU FIN-IMPRIMIR-CABECERA.
           DISPLAY (6, 27) "Listado de transferencias".
           DISPLAY (8, 3) "FECHA".
           DISPLAY (8, 15) "TIPO".
           DISPLAY (8, 26) "ESTADO".
           DISPLAY (8, 39) "IMPORTE EUR".
           DISPLAY (8, 55) "CUENTA".
           DISPLAY (9, 3) "----------------------------------------".
           DISPLAY (9, 43) "----------------------------".

           IF NUM-FILAS = 0
               DISPLAY (12, 16)
                   "No hay transferencias en el intervalo indicado"
               DISPLAY (24, 25) "Enter / ESC - Volver al menu"
               GO TO ESPERAR-TECLA
           END-IF.

           COMPUTE IDX-INICIO-PAG = (PAGINA-ACTUAL - 1) * 10 + 1.
           COMPUTE IDX-FIN-PAG = IDX-INICIO-PAG + 9.
           IF IDX-FIN-PAG > NUM-FILAS
               MOVE NUM-FILAS TO IDX-FIN-PAG
           END-IF.

           MOVE 10 TO LINEA-PANTALLA.
           PERFORM VARYING I FROM IDX-INICIO-PAG BY 1
                   UNTIL I > IDX-FIN-PAG
               MOVE F-FECHA(I) TO FECHA-MOSTRAR
               DISPLAY (LINEA-PANTALLA, 3) FM-DIA
               DISPLAY (LINEA-PANTALLA, 5) "/"
               DISPLAY (LINEA-PANTALLA, 6) FM-MES
               DISPLAY (LINEA-PANTALLA, 8) "/"
               DISPLAY (LINEA-PANTALLA, 9) FM-ANO
               DISPLAY (LINEA-PANTALLA, 15) F-TIPO(I)
               DISPLAY (LINEA-PANTALLA, 26) F-ESTADO(I)
               MOVE F-IMP-ENT(I) TO IMPORTE-ED
               DISPLAY (LINEA-PANTALLA, 38) F-SIGNO(I)
               DISPLAY (LINEA-PANTALLA, 39) IMPORTE-ED
               DISPLAY (LINEA-PANTALLA, 46) ","
               DISPLAY (LINEA-PANTALLA, 47) F-IMP-DEC(I)
               DISPLAY (LINEA-PANTALLA, 55) F-CUENTA(I)
               ADD 1 TO LINEA-PANTALLA
           END-PERFORM.

           DISPLAY (21, 3) "Pagina".
           MOVE PAGINA-ACTUAL TO PAGINA-ED.
           DISPLAY (21, 10) PAGINA-ED.
           DISPLAY (21, 14) "de".
           MOVE TOTAL-PAGINAS TO PAGINA-ED.
           DISPLAY (21, 17) PAGINA-ED.
           DISPLAY (21, 38) "(-) enviada   (+) recibida".
           IF TABLA-LLENA = "S"
               DISPLAY (22, 3) "Se muestran solo las primeras 300"
           END-IF.

           IF PAGINA-ACTUAL > 1
               DISPLAY (24, 1) "Re. pag - Anterior"
           END-IF.
           IF PAGINA-ACTUAL < TOTAL-PAGINAS
               DISPLAY (24, 22) "Enter/Av. pag - Siguiente"
               DISPLAY (24, 66) "ESC - Volver"
           ELSE
               DISPLAY (24, 48) "Enter / ESC - Volver al menu"
           END-IF.

       ESPERAR-TECLA.
           ACCEPT (24, 80) PRESSED-KEY.
           IF ESC-PRESSED
               EXIT PROGRAM
           END-IF.
           IF PGUP-PRESSED
               IF PAGINA-ACTUAL > 1
                   SUBTRACT 1 FROM PAGINA-ACTUAL
                   GO TO MOSTRAR-PAGINA
               END-IF
               GO TO ESPERAR-TECLA
           END-IF.
           IF ENTER-PRESSED OR PGDN-PRESSED
               IF PAGINA-ACTUAL < TOTAL-PAGINAS
                   ADD 1 TO PAGINA-ACTUAL
                   GO TO MOSTRAR-PAGINA
               END-IF
               IF ENTER-PRESSED
                   EXIT PROGRAM
               END-IF
           END-IF.
           GO TO ESPERAR-TECLA.

      *> ------------------------------------------------------------
      *> Rutinas auxiliares
      *> ------------------------------------------------------------
      *> Una mensual pendiente se repite cada mes desde su proxima
      *> ejecucion: se anade una fila por cada mes dentro del rango.
       PROYECTAR-MENSUAL.
           MOVE "PENDIENTE" TO N-ESTADO.
           MOVE TRF-FECHA TO FECHA-PROY.
           MOVE 0 TO CONT-MESES.
       BUCLE-PROYECCION.
           IF FECHA-PROY > FECHA-COMP-FIN OR CONT-MESES >= MAX-MESES
              OR TABLA-LLENA = "S"
               GO TO FIN-PROYECTAR
           END-IF.
           MOVE FECHA-PROY TO N-FECHA.
           PERFORM ANADIR-SI-EN-RANGO THRU FIN-ANADIR.
           ADD 1 TO CONT-MESES.
           DIVIDE FECHA-PROY BY 10000 GIVING AUX-ANO
               REMAINDER AUX-RESTO.
           DIVIDE AUX-RESTO BY 100 GIVING AUX-MES
               REMAINDER AUX-DIA.
           ADD 1 TO AUX-MES.
           IF AUX-MES > 12
               MOVE 1 TO AUX-MES
               ADD 1 TO AUX-ANO
           END-IF.
           COMPUTE FECHA-PROY = (AUX-ANO * 10000) + (AUX-MES * 100)
                                + AUX-DIA.
           GO TO BUCLE-PROYECCION.
       FIN-PROYECTAR.
           EXIT.

       ANADIR-SI-EN-RANGO.
           IF N-FECHA < FECHA-COMP-INI OR N-FECHA > FECHA-COMP-FIN
               GO TO FIN-ANADIR
           END-IF.
           IF NUM-FILAS >= MAX-FILAS
               MOVE "S" TO TABLA-LLENA
               GO TO FIN-ANADIR
           END-IF.
           ADD 1 TO NUM-FILAS.
           MOVE NUEVA-FILA TO FILA(NUM-FILAS).
       FIN-ANADIR.
           EXIT.

       IMPRIMIR-CABECERA.
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
       FIN-IMPRIMIR-CABECERA.
           EXIT.

       PSYS-ERR.
           CLOSE F-TRANSFERENCIAS.
           PERFORM IMPRIMIR-CABECERA THRU FIN-IMPRIMIR-CABECERA.
           DISPLAY (9, 25) "Ha ocurrido un error interno"
               WITH FOREGROUND-COLOR IS WHITE
                    BACKGROUND-COLOR IS RED.
           DISPLAY (11, 32) "Vuelva mas tarde"
               WITH FOREGROUND-COLOR IS WHITE
                    BACKGROUND-COLOR IS RED.
           DISPLAY (24, 33) "Enter - Aceptar".
       PSYS-ERR-ENTER.
           ACCEPT (24, 80) PRESSED-KEY.
           IF ENTER-PRESSED
               EXIT PROGRAM
           END-IF.
           GO TO PSYS-ERR-ENTER.
