<p align="center"><img src="assets/icon.png" width="128" alt="FitLog icon"></p>

# FitLog

*[Español más abajo](#español)*

Android app that reads the **Exercise Log** screen of Ring Fit Adventure (Nintendo Switch) from a
photo or a Switch screenshot, extracts every day shown (date, time exercising, calories, distance)
and saves it to **Health Connect** and to a local history.

It replaces Ring Fit Sync, which relied on the Google Fit API that Google is shutting down at the
end of 2026. Text recognition runs on the phone, the app has no ads, no analytics and never goes online.

> **Made with AI.** This app was written with [Claude Code](https://claude.com/claude-code), an AI
> coding assistant, under the direction and testing of its author. Keep that in mind if you prefer
> to avoid AI-generated software.

## Features

- Take a photo of the TV or pick a Switch screenshot (shared to the phone with the album QR code).
- On-device OCR with ML Kit; reads all days on the screen at once.
- Review screen to fix values, untick days and change the date and start time before saving.
- Writes an exercise session, active calories and distance to Health Connect.
- No duplicates: each day gets a stable id (`ringfit-YYYY-MM-DD-seconds`). Re-importing the same
  screen does not add it twice; if you played again that day, the old record is replaced.
- History with weekly and monthly totals and averages, deletion (also from Health Connect) and CSV export.
- Understands Spanish and English screens: day/month or month/day dates, comma or point decimals,
  kcal/Cal, km/mi, and common OCR mistakes (`O` for `0`, `l` for `1`, stray spaces).

## Stack

- Expo SDK 58 + TypeScript, development build (it does not run in Expo Go).
- [`react-native-health-connect`](https://github.com/matinzd/react-native-health-connect) (ships its own Expo config plugin).
- [`@infinitered/react-native-mlkit-text-recognition`](https://github.com/infinitered/react-native-mlkit) for OCR with line positions.
- `expo-sqlite` for history and settings; `expo-file-system` + `expo-sharing` for CSV export.
- The parser lives in `src/lib/parser.ts`, has no native dependencies and is covered by Jest tests in `src/lib/__tests__`.

## Development

```bash
npm install
npm test
npm run typecheck
```

Builds run on [EAS](https://docs.expo.dev/build/introduction/); no local Android SDK is needed.
The `extra.eas.projectId` and `owner` in `app.json` point to the author's Expo project. They are
public identifiers, not secrets: if you fork this, run `npx eas-cli init` to link your own project.

1. Development build, once (or whenever native libraries or `app.json` change):
   ```bash
   npm run build:dev
   ```
   Install the APK from the link or QR code EAS gives you.
2. Start the dev server and open the installed app; code changes show up instantly:
   ```bash
   npx expo start --dev-client --tunnel
   ```

### Release APK

```bash
npm run build:apk
```

Builds a release APK (`preview` profile) signed with the key EAS keeps in your Expo account.
Install it by opening the downloaded file on the phone (Android will ask to allow installs from that
source) or over USB with `adb install file.apk`.

## Health Connect permissions

1. Open the app and tap **Dar permisos** (grant permissions), or go to Settings.
2. Allow exercise, active calories and distance.
3. Android 13 and older need the Health Connect app from Google Play; on Android 14+ it is built in
   under *Settings → Security & privacy → Privacy → Health Connect*.

## Adding test cases

On the review screen, «Ver el texto reconocido → Compartir texto» shares the raw OCR text. Add it to
`src/lib/__tests__/real.test.ts` with the expected result.

## Disclaimer

Non-commercial hobby project. Not affiliated with, sponsored or endorsed by Nintendo. Nintendo Switch
and Ring Fit Adventure are trademarks of Nintendo. Health Connect is a trademark of Google LLC.
The screenshot in `src/lib/__tests__/fixtures` is used only as a test case.

## Credits and license

- Parsing approach inspired by [RingFitConnect](https://github.com/watanabetoshinori/RingFitConnect)
  by Toshinori Watanabe (MIT), the equivalent app for iOS and HealthKit.
- Code released under the [MIT license](LICENSE).

---

## Español

App Android que lee la pantalla **Registro de ejercicio** de Ring Fit Adventure (Nintendo Switch)
desde una foto o una captura de la Switch, extrae cada día que aparece (fecha, tiempo de ejercicio,
calorías y distancia) y lo guarda en **Health Connect** y en un historial local.

Sustituye a Ring Fit Sync, que usaba la API de Google Fit que Google cierra a finales de 2026. El
texto se reconoce en el propio móvil, la app no tiene anuncios ni analítica y nunca se conecta a internet.

> **Hecha con IA.** Esta app se escribió con [Claude Code](https://claude.com/claude-code), un
> asistente de programación con IA, bajo la dirección y las pruebas de su autor. Tenlo en cuenta si
> prefieres evitar software generado con IA.

### Funciones

- Haz una foto a la tele o elige una captura de la Switch (pasada al móvil con el código QR del álbum).
- Reconocimiento de texto en el móvil con ML Kit; lee todos los días de la pantalla a la vez.
- Pantalla de revisión para corregir valores, desmarcar días y cambiar la fecha y la hora de inicio antes de guardar.
- Guarda en Health Connect una sesión de ejercicio, las calorías activas y la distancia.
- Sin duplicados: cada día tiene un id estable (`ringfit-AAAA-MM-DD-segundos`). Si vuelves a importar
  la misma pantalla no se guarda dos veces; si jugaste otra vez ese día, se reemplaza el registro anterior.
- Historial con totales y medias por semana y por mes, borrado (también de Health Connect) y exportación a CSV.
- Entiende pantallas en español y en inglés: fechas día/mes o mes/día, coma o punto decimal,
  kcal/Cal, km/mi y errores típicos del OCR (`O` por `0`, `l` por `1`, espacios sueltos).

### Tecnología

- Expo SDK 58 + TypeScript, con development build (no funciona en Expo Go).
- [`react-native-health-connect`](https://github.com/matinzd/react-native-health-connect) (incluye su propio plugin de Expo).
- [`@infinitered/react-native-mlkit-text-recognition`](https://github.com/infinitered/react-native-mlkit) para el OCR con la posición de cada línea.
- `expo-sqlite` para el historial y los ajustes; `expo-file-system` + `expo-sharing` para exportar el CSV.
- La lógica de lectura está en `src/lib/parser.ts`, sin dependencias nativas y con tests de Jest en `src/lib/__tests__`.

### Desarrollo

```bash
npm install
npm test
npm run typecheck
```

Los builds se hacen en la nube con [EAS](https://docs.expo.dev/build/introduction/); no hace falta
el SDK de Android en el ordenador. `extra.eas.projectId` y `owner` en `app.json` apuntan al
proyecto de Expo del autor. Son identificadores públicos, no secretos: si haces un fork, ejecuta
`npx eas-cli init` para vincular tu propio proyecto.

1. Build de desarrollo, una vez (o cuando cambien las librerías nativas o `app.json`):
   ```bash
   npm run build:dev
   ```
   Instala el APK desde el enlace o el QR que te da EAS.
2. Arranca el servidor de desarrollo y abre la app instalada; los cambios de código se ven al instante:
   ```bash
   npx expo start --dev-client --tunnel
   ```

#### APK final

```bash
npm run build:apk
```

Genera un APK de release (perfil `preview`) firmado con la clave que EAS guarda en tu cuenta de Expo.
Para instalarlo, abre el archivo descargado en el móvil (Android pedirá permitir instalar apps de esa
fuente) o, por USB, `adb install archivo.apk`.

### Permisos de Health Connect

1. Abre la app y pulsa **Dar permisos**, o hazlo desde Ajustes.
2. Permite ejercicio, calorías activas y distancia.
3. En Android 13 o anterior hace falta la app Health Connect de Google Play; desde Android 14 viene
   integrada en *Ajustes → Seguridad y privacidad → Privacidad → Health Connect*.

### Añadir casos de prueba

En la pantalla de revisión, «Ver el texto reconocido → Compartir texto» comparte el texto del OCR.
Añádelo a `src/lib/__tests__/real.test.ts` con el resultado esperado.

### Aviso legal

Proyecto personal sin ánimo de lucro. No está afiliado, patrocinado ni aprobado por Nintendo.
Nintendo Switch y Ring Fit Adventure son marcas registradas de Nintendo. Health Connect es una marca
de Google LLC. La captura de `src/lib/__tests__/fixtures` se usa solo como caso de prueba.

### Créditos y licencia

- La lógica de lectura se inspira en [RingFitConnect](https://github.com/watanabetoshinori/RingFitConnect)
  de Toshinori Watanabe (MIT), la app equivalente para iOS y HealthKit.
- Código bajo [licencia MIT](LICENSE).
