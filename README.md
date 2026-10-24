# Ring Fit Log

App Android de uso personal que lee la pantalla «Registro de ejercicio» de Ring Fit Adventure
(foto o captura), extrae cada día (fecha, tiempo, kcal, distancia) y lo guarda en
**Health Connect** y en un historial local. Sustituye a Ring Fit Sync, que usaba Google Fit.

- Expo SDK 58 + TypeScript, con development build (no funciona en Expo Go).
- OCR en el móvil con ML Kit (`@infinitered/react-native-mlkit-text-recognition`), sin internet.
- Health Connect con `react-native-health-connect` (su plugin de Expo viene incluido; `expo-health-connect` está obsoleto).
- Historial en `expo-sqlite`; exportación CSV con `expo-file-system` + `expo-sharing`.
- La lógica de lectura está en `src/lib/parser.ts`, sin dependencias nativas, con tests en `src/lib/__tests__`.

## Desarrollo

```bash
npm install
npm test            # tests del lector y de las estadísticas
npm run typecheck
```

El Mac no tiene Android SDK, así que los builds se hacen en la nube con EAS (cuenta de Expo `cescur`).

1. Build de desarrollo (una sola vez, o cuando cambien librerías nativas o `app.json`):
   ```bash
   npm run build:dev
   ```
   Instala en el móvil el APK que da EAS (enlace o QR al terminar el build).
2. Arranca el servidor y abre la app instalada; los cambios de código se ven al instante:
   ```bash
   npx expo start --tunnel
   ```
   (`--tunnel` si el móvil no está en la misma red que el Mac.)

## APK final

```bash
npm run build:apk
```

Genera un APK de release (perfil `preview`) firmado con la clave que EAS guarda en la cuenta
de Expo. Para instalarlo: descárgalo en el móvil desde el enlace de EAS y ábrelo (Android pedirá
permitir instalar apps de esa fuente), o con el móvil conectado por USB: `adb install archivo.apk`.

## Permisos de Health Connect

1. Abre la app y pulsa **Dar permisos** en la tarjeta de Health Connect.
2. Activa ejercicio, calorías activas y distancia (lectura y escritura).
3. En Android 13 o anterior hace falta tener instalada la app Health Connect de Google Play;
   desde Android 14 viene integrada en *Ajustes → Seguridad y privacidad → Privacidad → Health Connect*.

Si algo falla, desde la app se puede abrir Health Connect para revisar los permisos.

## Cómo funciona la lectura

Ordena las líneas del OCR por posición (filas y columnas) y las recorre: cada fecha abre un
día nuevo y le asigna la duración, las kcal y la distancia que vengan detrás. Acepta día/mes o
mes/día (se configura en Ajustes), coma o punto decimal, kcal/Cal, km/mi y errores típicos
del OCR (`O` por `0`, `l` por `1`, espacios sueltos). Si falta un campo, la revisión lo deja
vacío para rellenarlo. Cada día se guarda con el id `ringfit-AAAA-MM-DD-segundos`, así que volver
a importar la misma pantalla no duplica nada; si un día ya importado cambia (jugaste otra vez),
la revisión avisa y reemplaza el registro anterior.

En la revisión, «Ver el texto reconocido → Compartir texto» sirve para mandar el texto del OCR
y añadirlo como caso de prueba.
