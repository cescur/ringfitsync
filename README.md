# FitLog

*[Español más abajo](#español)*

Android app that reads the **Exercise Log** screen of Ring Fit Adventure (Nintendo Switch) from a
photo or a Switch screenshot, extracts every day shown (date, time exercising, calories, distance)
and saves it to **Health Connect** and to a local history.

It replaces Ring Fit Sync, which relied on the Google Fit API that Google is shutting down at the
end of 2026. Text recognition runs on the phone, the app has no ads, no analytics and never goes online.

## Features

- Take a photo of the TV or pick a Switch screenshot (shared to the phone with the album QR code).
- On-device OCR with ML Kit; reads all days on the screen at once.
- Review screen to fix values, untick days and change the start time before saving.
- Writes an exercise session, active calories and distance to Health Connect.
- No duplicates: each day gets a stable id (`ringfit-YYYY-MM-DD-seconds`). Re-importing the same
  screen updates the day instead of adding it twice; if you played again that day, the old record is replaced.
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
If you fork this, run `npx eas-cli init` to link your own Expo project (it replaces `extra.eas.projectId` and `owner` in `app.json`).

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

1. Open the app and tap **Dar permisos** (grant permissions).
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
desde una foto o una captura de la Switch, extrae cada día (fecha, tiempo, kcal y distancia) y lo
guarda en **Health Connect** y en un historial local. Sustituye a Ring Fit Sync, que usaba la API de
Google Fit que Google cierra a finales de 2026. El texto se reconoce en el propio móvil, sin anuncios,
sin analítica y sin conexión a internet.

### Uso

1. Pulsa **Hacer foto** o **Elegir de la galería**.
2. Revisa los días detectados: puedes corregir valores, desmarcar días y cambiar la hora de inicio
   (la pantalla del juego no la muestra; por defecto 19:00, configurable en Ajustes).
3. Pulsa **Guardar seleccionados**. Si vuelves a importar la misma pantalla no se duplica nada.
4. En **Historial** tienes los totales y medias por semana y mes, puedes borrar días y exportar a CSV.

### Compilar

Los builds se hacen en la nube con EAS: `npm run build:dev` para el build de desarrollo y
`npm run build:apk` para el APK final. Los detalles están en la sección en inglés de arriba.

### Aviso legal

Proyecto personal sin ánimo de lucro. No está afiliado, patrocinado ni aprobado por Nintendo.
Nintendo Switch y Ring Fit Adventure son marcas registradas de Nintendo. Health Connect es una
marca de Google LLC. Licencia [MIT](LICENSE).
