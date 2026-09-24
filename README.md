# MIME Android

Offline Android client for **MIME** — Show → Mime opening drills. Parity with web v0 (`mime/app`): custom Compose board, no WebView, hard reset on mistakes (no hints).

## Features (v0)

- Library of Lines from bundled JSON assets
- Show → Mime loop matching web `src/mime/loop.ts`
- User plays only `side_to_learn`; opponent auto-plays on Show
- If standard start FEN and `side_to_learn=black`: Show auto-plays White’s first UCI before freeing the board
- Board orientation follows `side_to_learn`
- Mime accepts only the expected UCI
- Fail: red flash + hard reset to ply 0 — never shows the correct move as a hint
- 100% offline

## Stack

- Kotlin + Jetpack Compose
- Custom Canvas chessboard (dumb renderer + square clicks)
- [chesslib](https://github.com/bhlangonijr/chesslib) for in-memory rules
- Moshi for JSON

## Seed maps

- `maps/canon/italian-game.json` (White)
- `maps/canon/accelerated-dragon.json` (Black)

## Build

Requires JDK 17 and Android SDK (compileSdk 34).

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# local.properties already points sdk.dir for this workspace; otherwise:
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew :app:assembleDebug
```

Debug APK:

```
app/build/outputs/apk/debug/app-debug.apk
```

A copy is also produced under `dist/mime-debug.apk` after a release packaging step in CI/workspace.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# or
adb install -r dist/mime-debug.apk
```

Package id: `com.lughlammas.mime`

## Out of scope (v0)

Hub, Day Feed, engine, PGN import, WebView, extra maps, music, PvP.

## Test (v0.2)

```bash
./gradlew test
```

Goldens live under `app/src/test/resources/fixtures/golden/` (copied from mime web fixtures).
