# Resultados de Tesseract sobre el corpus real

Referencia: texto del lector por plantillas (exacto). Muestra: 10 paginas del listado repartidas por los 763 registros.

| Configuración | Precisión de caracteres | Filas exactas | Páginas válidas | ms/página |
|---|---|---|---|---|
| Escala 1, grises | 88,1 % | 12 de 169 | 0 de 10 | 137 |
| Escala 2, grises (defecto) | 95,0 % | 54 de 169 | 0 de 10 | 272 |
| Escala 3, grises | 94,1 % | 48 de 169 | 0 de 10 | 351 |
| Escala 4, grises | 94,8 % | 50 de 169 | 0 de 10 | 449 |
| Escala 2, binarizado 128 | 95,2 % | 62 de 169 | 0 de 10 | 203 |
| Escala 3, binarizado 128 | 90,3 % | 19 de 169 | 0 de 10 | 273 |
| Escala 3, binarizado 160 | 91,8 % | 27 de 169 | 0 de 10 | 279 |
| Escala 4, binarizado 128 | 91,6 % | 30 de 169 | 0 de 10 | 421 |
| Escala 2, invertido | 95,0 % | 54 de 169 | 0 de 10 | 231 |
| Escala 3, invertido y binarizado 128 | 90,3 % | 19 de 169 | 0 de 10 | 305 |

Mejor configuración por filas exactas: **Escala 2, binarizado 128**.

Con esa configuración sobre el listado completo: **265 de 763** registros exactos y **0 de 43** páginas sin ningún error.

## Ajustes de Tesseract y modelo entrenado

Mismas páginas de muestra. El modelo entrenado que se mide aquí (`dos-prueba`) no ha visto estas páginas.

| Configuración | Precisión de caracteres | Filas exactas | Páginas válidas | ms/página |
|---|---|---|---|---|
| spa, escala 2 sin suavizar, sin diccionarios y con lista blanca | 95,4 % | 65 de 169 | 0 de 10 | 213 |
| Entrenado (dos-prueba), rejilla: escala 2 sin suavizar y hueco de 4 px | 99,6 % | 167 de 169 | 8 de 10 | 107 |

Sobre el listado completo (763 registros), con la rejilla:

- `dos-prueba` (sin las páginas de muestra): **761 de 763** registros exactos y **41 de 43** páginas sin errores
- `dos` (todas las pantallas, el que usa la aplicación): **763 de 763** registros exactos y **43 de 43** páginas sin errores
