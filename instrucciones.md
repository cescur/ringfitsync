# Encargo: app Android "Ring Fit Log" con Expo  
  
## Objetivo  
Una app Android solo para uso personal (no se publicará) que registre mis resultados de Ring Fit Adventure (Nintendo Switch). Sustituye a "Ring Fit Sync" (com.kave.rfalink), que ya no está en Google Play y usaba la API de Google Fit, que Google cierra a finales de 2026. Esta app debe usar **Health Connect**.  
  
## Funcionamiento  
1. Hago una foto con la cámara a la pantalla "Registro de ejercicio" del juego, o elijo de la galería una captura de la Switch (la paso al móvil con el código QR del álbum).  
2. La app reconoce el texto en el propio móvil (ML Kit, sin internet) y extrae **todos los días** que salgan en la pantalla: fecha, tiempo de ejercicio, calorías y distancia.  
3. Antes de guardar, muestra una **pantalla de revisión** con los entrenamientos detectados. Ahí puedo corregir valores, desmarcar los que no quiera guardar y ajustar la hora de inicio.  
4. Al confirmar, guarda cada entrenamiento en Health Connect y en un historial local.  
5. Pantalla de historial: lista de entrenamientos, totales y medias por semana y mes, opción de borrar uno (también de Health Connect) y exportar todo a CSV para compartir.  
  
## Referencia: proyecto iOS equivalente (licencia MIT)  
https://github.com/watanabetoshinori/RingFitConnect. Lo importante está en `RingFitConnect/Models/WorkoutLogManager.swift` y `WorkoutLog.swift`. En resumen:  
- Ordena los bloques de texto del OCR de arriba abajo y los recorre en secuencia: al encontrar una **fecha** empieza un entrenamiento nuevo, y luego le asigna **duración**, **calorías** y **distancia**. Cuando tiene las tres, lo da por completo.  
- Expresiones regulares que usa (versión en inglés):  
  - Fecha: `(1[0-2]|[1-9])/(3[0-1]|[1-2][0-9]|0[1-9])`, por ejemplo `11/29 (Fri.)`  
  - Duración: `[0-9]+:[0-5][0-9]:[0-5][0-9]`, por ejemplo `0:20:49`  
  - Calorías: `([0-9]*[.])?[0-9]+ ?(kcal|Cal)`, por ejemplo `74.25 Cal`, `127.10kcal`  
  - Distancia: `... ?km` o `... ?mi.`, por ejemplo `0.77 mi.`, `1.85km`  
- El año no aparece en pantalla: usa el año actual y, si esa fecha caería en el futuro, el año anterior.  
- Aspecto de la pantalla en inglés: cada día ocupa una fila, con "11/29 (Fri.)" encima y debajo tres columnas, "Time Exercising 0:20:49 | Calories Burned 74.25 Cal | Distance Traveled 0.77 mi.". Se ven unos 4–5 días por pantalla.  
  
## Adaptaciones necesarias para mi caso  
- Tengo el juego **en español**. Lo más probable es que la fecha salga como **día/mes** (por ejemplo `29/11 (vie.)`) y que los decimales usen coma (`74,25 kcal`). El lector tiene que aceptar:  
  - fechas día/mes y mes/día (una opción en ajustes, con día/mes por defecto; y si un número es mayor que 12, deducir el orden),  
  - coma o punto como decimal,  
  - kcal/Cal y km/mi (convertir millas a metros).  
- El OCR falla a menudo con brillos y reflejos. Hay que tolerar espacios sueltos, `O` en lugar de `0`, etc. Y si falta algún campo, que la pantalla de revisión lo deje vacío y editable en lugar de descartar el día.  
- **Evitar duplicados**: si vuelvo a fotografiar una pantalla con días ya importados, no deben guardarse dos veces. Usa un `clientRecordId` estable basado en la fecha y la duración, tanto en Health Connect como en el historial local.  
- La pantalla no da la hora de inicio: usa una hora por defecto configurable (por ejemplo 19:00) que se pueda cambiar en la revisión. La hora de fin es la de inicio más la duración.  
- La pantalla "Resultados de hoy" del final de la sesión no hace falta, solo el "Registro de ejercicio". Si es fácil soportar ambas, mejor.  
  
## Tecnología  
- Expo (último SDK estable) con TypeScript y **development build / prebuild**. No funcionará en Expo Go por los módulos nativos.  
- Health Connect: `react-native-health-connect` con su plugin de configuración para Expo (`expo-health-connect`). Registros que escribir:  
  - `ExerciseSession`, con el tipo de ejercicio más adecuado que exista (por ejemplo un entrenamiento genérico / "other workout") y título "Ring Fit Adventure",  
  - `ActiveCaloriesBurned`,  
  - `Distance`.  
  Pide solo permisos de escritura y lectura de esos tipos. Ten en cuenta la pantalla de justificación de permisos (privacy policy rationale) que exige Health Connect y que en Android 14+ viene integrado en el sistema.  
- OCR: `@react-native-ml-kit/text-recognition`, o la alternativa mantenida que funcione mejor con el SDK actual. Necesito las coordenadas de cada bloque para ordenar por posición vertical, y por columnas si ayuda.  
- Imagen: `expo-image-picker` (cámara y galería).  
- Almacenamiento local: `expo-sqlite`. Exportación: `expo-file-system` + `expo-sharing`.  
- Nada de anuncios, analítica ni conexiones a internet.  
- Identificador de paquete personal, por ejemplo `com.solof.ringfitlog`.  
  
## Calidad  
- La lógica de lectura debe ir en un módulo puro (sin dependencias nativas) con **tests en Jest**: listas de textos de OCR simuladas en inglés y en español, con errores típicos (decimales con coma, `O` en vez de `0`, campos que faltan, varios días por pantalla).  
- Cuando te pase fotos reales de mi pantalla, añádelas como casos de prueba con el texto que devuelva el OCR.  
  
## Compilación y entrega  
- Lo que necesito al final es un **APK** que pueda instalar directamente en mi Android.  
- Primero comprueba si este Mac tiene Android SDK y JDK (`ANDROID_HOME`, `adb`, `java -version`).  
  - Si lo tiene: compila localmente un APK de release, firmado con una clave propia que guardes fuera del repo y anotes en el README.  
  - Si no: **pregúntame** antes de instalar nada. Las opciones son instalar Android Studio/SDK aquí o usar EAS Build en la nube con un perfil `preview` que genere APK (`"buildType": "apk"`), lo que necesita mi cuenta de Expo. No crees cuentas ni inicies sesión por mí.  
- Haz un README corto: cómo compilar, cómo instalar el APK (por `adb install` o pasándolo al móvil) y cómo dar los permisos de Health Connect.  
- Inicializa un repositorio git y haz commits a medida que avances.  
  
## Antes de terminar  
Pídeme una foto o captura real de mi pantalla "Registro de ejercicio" para comprobar el lector con datos reales, y ajusta las expresiones regulares según lo que salga.  
