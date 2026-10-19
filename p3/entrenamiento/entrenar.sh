#!/bin/bash
# Entrena el modelo "dos" de Tesseract (motor antiguo, como spa.traineddata) con las pantallas de DOSBox.
#
# 1. Datos en entrenamiento/datos: java -jar target\wrapper-legacy.jar cajas [--prueba]
# 2. Desde p3: docker run --rm -v "%cd%\entrenamiento:/e" ubuntu:24.04 bash /e/entrenar.sh
# 3. Copiar entrenamiento/datos/dos.traineddata a tessdata/ (o, con --prueba, a entrenamiento/dos-prueba.traineddata).
set -e
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq && apt-get install -y -qq tesseract-ocr > /dev/null
tesseract --version | head -1
cd /e/datos
rm -f *.tr unicharset font_properties dos.*.unicharset dos.inttemp dos.pffmtable dos.normproto dos.shapetable
# Rasgos de cada carácter a partir de su recuadro
for f in dos.dosbox.exp*.png; do tesseract "$f" "${f%.png}" box.train > /dev/null 2>&1; done
echo "$(ls *.tr | wc -l) pantallas"
unicharset_extractor *.box > /dev/null
echo "dosbox 0 0 1 0 0" > font_properties    # fuente monoespaciada, sin cursiva ni negrita
shapeclustering -F font_properties -U unicharset -O dos.unicharset *.tr > /dev/null 2>&1
mftraining -F font_properties -U unicharset -O dos.unicharset *.tr > /dev/null 2>&1
cntraining *.tr > /dev/null 2>&1
mv inttemp dos.inttemp; mv pffmtable dos.pffmtable; mv normproto dos.normproto; mv shapetable dos.shapetable
combine_tessdata dos. > /dev/null
ls -la dos.traineddata
