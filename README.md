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

## Download

Get the latest APK from [Releases](../../releases) and open it on your phone (Android will ask to
allow installs from that source). Requires Android 8.0 or newer and Health Connect.

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

- Kotlin + Jetpack Compose (Material 3), minSdk 26.
- [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) (`androidx.health.connect:connect-client`).
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), bundled model so it works offline.
- Plain SQLite for the history and SharedPreferences for settings.
- The parser lives in `app/src/main/java/com/solof/fitlog/parser/Parser.kt`, has no Android
  dependencies and is covered by JUnit tests in `app/src/test`.

## Building

Needs JDK 17+ and the Android SDK (Android Studio, or just the command-line tools).

```bash
./gradlew :app:testDebugUnitTest   # tests
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # signed release APK (see below)
```

APKs end up in `app/build/outputs/apk/`. Install one over USB with `adb install <file>.apk`.

### Signing

The release key is never stored in the repo. Locally, Gradle reads it from `~/.gradle/gradle.properties`:

```properties
fitlog.storeFile=/path/to/fitlog-release.jks
fitlog.storePassword=...
fitlog.keyAlias=fitlog
fitlog.keyPassword=...
```

Without those, `assembleRelease` produces an unsigned APK. Android only installs an update signed
with the same key as the installed version, so keep a backup of the key and its password.

### Publishing with GitHub Actions

- `Tests` runs the unit tests and a debug build on every push and pull request.
- `Publicar APK` runs when a release is published: it builds the signed APK and attaches it to the release.

One-time setup, in *Settings → Secrets and variables → Actions*:

| Secret | Value |
| --- | --- |
| `FITLOG_KEYSTORE_BASE64` | the keystore in base64 (`base64 -i fitlog-release.jks \| pbcopy` on macOS) |
| `FITLOG_STORE_PASSWORD` | keystore password |
| `FITLOG_KEY_ALIAS` | `fitlog` |
| `FITLOG_KEY_PASSWORD` | key password |

Then create a release from the Releases tab with a tag such as `v1.0.0`. The version name comes from the tag.

## Health Connect permissions

1. Open the app and tap **Dar permisos** (grant permissions), or go to Settings.
2. Allow exercise, active calories and distance.
3. Android 13 and older need the Health Connect app from Google Play; on Android 14+ it is built in
   under *Settings → Security & privacy → Privacy → Health Connect*.

## Adding test cases

On the review screen, «Ver el texto reconocido → Compartir texto» shares the raw OCR text. Add it to
`RealScreenshotTest` in `app/src/test/java/com/solof/fitlog/parser/ParserTest.kt` with the expected result.

## Disclaimer

Non-commercial hobby project. Not affiliated with, sponsored or endorsed by Nintendo. Nintendo Switch
and Ring Fit Adventure are trademarks of Nintendo. Health Connect is a trademark of Google LLC.
The screenshot in `app/src/test/resources` is used only as a test case.

## Credits and license

- Parsing approach inspired by [RingFitConnect](https://github.com/watanabetoshinori/RingFitConnect)
  by Toshinori Watanabe (MIT), the equivalent app for iOS and HealthKit.
- An earlier version of this app was built with Expo; it is kept under the `expo-v1` tag.
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

### Descarga

Descarga el último APK desde [Releases](../../releases) y ábrelo en el móvil (Android pedirá permitir
instalar apps de esa fuente). Necesita Android 8.0 o superior y Health Connect.

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

- Kotlin + Jetpack Compose (Material 3), minSdk 26.
- [Health Connect](https://developer.android.com/health-and-fitness/guides/health-connect) (`androidx.health.connect:connect-client`).
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), con el modelo incluido para funcionar sin internet.
- SQLite para el historial y SharedPreferences para los ajustes.
- La lógica de lectura está en `app/src/main/java/com/solof/fitlog/parser/Parser.kt`, sin
  dependencias de Android y con tests de JUnit en `app/src/test`.

### Compilar

Necesita JDK 17 o superior y el SDK de Android (Android Studio, o solo las herramientas de línea de comandos).

```bash
./gradlew :app:testDebugUnitTest   # tests
./gradlew :app:assembleDebug       # APK de depuración
./gradlew :app:assembleRelease     # APK de release firmado (ver abajo)
```

Los APK quedan en `app/build/outputs/apk/`. Para instalar uno por USB: `adb install <archivo>.apk`.

#### Firma

La clave de firma nunca se guarda en el repo. En local, Gradle la lee de `~/.gradle/gradle.properties`:

```properties
fitlog.storeFile=/ruta/a/fitlog-release.jks
fitlog.storePassword=...
fitlog.keyAlias=fitlog
fitlog.keyPassword=...
```

Sin eso, `assembleRelease` genera un APK sin firmar. Android solo instala una actualización firmada
con la misma clave que la versión instalada, así que guarda una copia de la clave y su contraseña.

#### Publicar con GitHub Actions

- `Tests` ejecuta los tests y un build de depuración en cada push y pull request.
- `Publicar APK` se ejecuta al publicar una versión: compila el APK firmado y lo adjunta a esa versión.

Configuración, una sola vez, en *Settings → Secrets and variables → Actions*:

| Secret | Valor |
| --- | --- |
| `FITLOG_KEYSTORE_BASE64` | la clave en base64 (`base64 -i fitlog-release.jks \| pbcopy` en macOS) |
| `FITLOG_STORE_PASSWORD` | contraseña del almacén de claves |
| `FITLOG_KEY_ALIAS` | `fitlog` |
| `FITLOG_KEY_PASSWORD` | contraseña de la clave |

Después crea una versión desde la pestaña Releases con una etiqueta como `v1.0.0`. El nombre de la
versión sale de la etiqueta.

### Permisos de Health Connect

1. Abre la app y pulsa **Dar permisos**, o hazlo desde Ajustes.
2. Permite ejercicio, calorías activas y distancia.
3. En Android 13 o anterior hace falta la app Health Connect de Google Play; desde Android 14 viene
   integrada en *Ajustes → Seguridad y privacidad → Privacidad → Health Connect*.

### Añadir casos de prueba

En la pantalla de revisión, «Ver el texto reconocido → Compartir texto» comparte el texto del OCR.
Añádelo a `RealScreenshotTest` en `app/src/test/java/com/solof/fitlog/parser/ParserTest.kt` con el resultado esperado.

### Aviso legal

Proyecto personal sin ánimo de lucro. No está afiliado, patrocinado ni aprobado por Nintendo.
Nintendo Switch y Ring Fit Adventure son marcas registradas de Nintendo. Health Connect es una marca
de Google LLC. La captura de `app/src/test/resources` se usa solo como caso de prueba.

### Créditos y licencia

- La lógica de lectura se inspira en [RingFitConnect](https://github.com/watanabetoshinori/RingFitConnect)
  de Toshinori Watanabe (MIT), la app equivalente para iOS y HealthKit.
- Una versión anterior de esta app se hizo con Expo; se conserva en la etiqueta `expo-v1`.
- Código bajo [licencia MIT](LICENSE).
