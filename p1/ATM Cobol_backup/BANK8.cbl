       IDENTIFICATION DIVISION.
       PROGRAM-ID. BANK8.

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

       DATA DIVISION.
       FILE SECTION.
       FD TARJETAS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "tarjetas.ubd".
       01 TARJETAREG.
           02 TNUM      PIC 9(16).
           02 TPIN      PIC 9(4).

       FD INTENTOS
           LABEL RECORD STANDARD
           VALUE OF FILE-ID IS "intentos.ubd".
       01 INTENTOSREG.
           02 INUM      PIC 9(16).
           02 IINTENTOS PIC 9(1).

       WORKING-STORAGE SECTION.
       77 FST                      PIC X(2).
       77 FSI                      PIC X(2).

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
           88 ESC-PRESSED          VALUE 2005.

       77 CHOICE                   PIC X.
       77 CLAVE-ACTUAL             PIC 9(4).
       77 CLAVE-NUEVA-1            PIC 9(4).
       77 CLAVE-NUEVA-2            PIC 9(4).
       77 SW-FIN                   PIC X VALUE 'N'.
       77 SW-ENTER                 PIC X VALUE 'N'.

       LINKAGE SECTION.
       01 L-TNUM                   PIC 9(16).

       SCREEN SECTION.
       01 BLANK-SCREEN.
           05 FILLER LINE 1 BLANK SCREEN BACKGROUND-COLOR BLACK.

       01 PANTALLA-CLAVE-ACTUAL.
           05 BLANK ZERO SECURE LINE 10 COL 42 PIC 9(4) USING 
           CLAVE-ACTUAL.

       01 PANTALLA-CLAVE-N1.
           05 BLANK ZERO SECURE LINE 11 COL 42 PIC 9(4) USING 
           CLAVE-NUEVA-1.

       01 PANTALLA-CLAVE-N2.
           05 BLANK ZERO SECURE LINE 12 COL 42 PIC 9(4) USING 
           CLAVE-NUEVA-2.

       PROCEDURE DIVISION USING L-TNUM.
       INICIO.
           OPEN I-O TARJETAS.
           OPEN I-O INTENTOS.

           MOVE L-TNUM TO TNUM.
           READ TARJETAS INVALID KEY
               CLOSE TARJETAS
               CLOSE INTENTOS
               EXIT PROGRAM
           END-READ.

           MOVE L-TNUM TO INUM.
           READ INTENTOS INVALID KEY
               CLOSE TARJETAS
               CLOSE INTENTOS
               EXIT PROGRAM
           END-READ.

           MOVE 'N' TO SW-FIN.
           PERFORM PROCESO-CAMBIO-CLAVE UNTIL SW-FIN = 'Y'.

           CLOSE TARJETAS.
           CLOSE INTENTOS.
           EXIT PROGRAM.

       PROCESO-CAMBIO-CLAVE.
           PERFORM IMPRIMIR-CABECERA.
           DISPLAY (8, 15) "Cambio de clave personal".

           DISPLAY (10, 15) "Introduzca clave actual: ".
           DISPLAY (11, 15) "Introduzca nueva clave: ".
           DISPLAY (12, 15) "Repita la nueva clave: ".

           DISPLAY (24, 1) "Enter - Confirmar".
           DISPLAY (24, 65) "ESC - Cancelar".

           INITIALIZE CLAVE-ACTUAL.
           INITIALIZE CLAVE-NUEVA-1.
           INITIALIZE CLAVE-NUEVA-2.

           ACCEPT PANTALLA-CLAVE-ACTUAL.
           IF ESC-PRESSED
               MOVE 'Y' TO SW-FIN
           END-IF.

           IF SW-FIN = 'N'
               ACCEPT PANTALLA-CLAVE-N1
               IF ESC-PRESSED
                   MOVE 'Y' TO SW-FIN
               END-IF
           END-IF.

           IF SW-FIN = 'N'
               ACCEPT PANTALLA-CLAVE-N2
               IF ESC-PRESSED
                   MOVE 'Y' TO SW-FIN
               END-IF
           END-IF.

           *> VALIDACION Y ACTUALIZACION
           IF SW-FIN = 'N'
               IF CLAVE-ACTUAL NOT = TPIN
                   SUBTRACT 1 FROM IINTENTOS
                   REWRITE INTENTOSREG
                   IF IINTENTOS = 0
                       PERFORM MOSTRAR-BLOQUEO

                       CLOSE TARJETAS
                       CLOSE INTENTOS
                       EXIT PROGRAM
                   ELSE
                       PERFORM MOSTRAR-ERROR-CLAVE
                   END-IF
               ELSE
                   IF CLAVE-NUEVA-1 = CLAVE-NUEVA-2
                       MOVE CLAVE-NUEVA-1 TO TPIN
                       REWRITE TARJETAREG
                       PERFORM MOSTRAR-EXITO
                       MOVE 'Y' TO SW-FIN
                   ELSE
                       PERFORM MOSTRAR-ERROR-COINCIDENCIA
                   END-IF
               END-IF
           END-IF.

       MOSTRAR-EXITO.
           PERFORM IMPRIMIR-CABECERA.
           DISPLAY (8, 15) "Cambio de clave personal".
           DISPLAY (12, 15) "La clave se ha cambiado correctamente".
           DISPLAY (24, 33) "Enter-Aceptar".
           PERFORM REINICIAR-INTENTOS.
           PERFORM ESPERAR-ENTER.

       MOSTRAR-ERROR-CLAVE.
           PERFORM IMPRIMIR-CABECERA.
           DISPLAY (9, 26) "El codigo PIN actual es incorrecto"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (11, 30) "Le quedan "
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (11, 40) IINTENTOS
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (11, 42) " intentos"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (24, 33) "Enter - Aceptar".
           PERFORM ESPERAR-ENTER.

       MOSTRAR-ERROR-COINCIDENCIA.
           PERFORM IMPRIMIR-CABECERA.
           DISPLAY (10, 20) "Las nuevas claves no coinciden"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (24, 33) "Enter - Aceptar".
           PERFORM ESPERAR-ENTER.

       MOSTRAR-BLOQUEO.
           PERFORM IMPRIMIR-CABECERA.
           DISPLAY (9, 20) "Se ha sobrepasado el numero de intentos"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (11, 18) 
               "Por su seguridad se ha bloqueado la tarjeta"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (12, 30) "Acuda a una sucursal"
               WITH FOREGROUND-COLOR WHITE BACKGROUND-COLOR RED.
           DISPLAY (24, 33) "Enter - Aceptar".
           PERFORM ESPERAR-ENTER.

       ESPERAR-ENTER.
           MOVE 'N' TO SW-ENTER.
           PERFORM UNTIL SW-ENTER = 'Y'
               ACCEPT (24, 80) CHOICE
               IF ENTER-PRESSED
                   MOVE 'Y' TO SW-ENTER
               END-IF
           END-PERFORM.

       IMPRIMIR-CABECERA.
           SET ENVIRONMENT 'COB_SCREEN_EXCEPTIONS' TO 'Y'.
           SET ENVIRONMENT 'COB_SCREEN_ESC'        TO 'Y'.

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

       REINICIAR-INTENTOS.
           MOVE 3 TO IINTENTOS.
           REWRITE INTENTOSREG.
           