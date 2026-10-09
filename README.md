# Scam Guardian

Android app with on-device AI (no INTERNET permission).

## Stack

- Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00)
- Android Gradle Plugin 9.4.1, Gradle 9.8.1, Kotlin DSL, version catalog
- LiteRT-LM 0.18.0
- Detection pipeline: kotlinx-serialization-json 1.11.0, commons-text 1.15.0, ICU4J 78.3, url-detector 0.1.24, Guava 33.7.2-android
- Spotless 8.10.4 with ktlint 1.8.0
- detekt 1.23.8 with Compose rules 0.4.28
- Lefthook 2.2.1
- minSdk 26, targetSdk 36, compileSdk 37
- JDK 17

## Setup

1. Install Android Studio and the SDK it requests.
2. Install Node.js LTS and run `npm install` (installs the git hooks).
3. Download the model `embeddinggemma-2-740m.litertlm`: https://huggingface.co/litert-community/embeddinggemma-2-740m-litert-lm
4. Install the app, then copy the model to the device (the app has no INTERNET permission):

   ```bash
   adb shell mkdir -p /sdcard/Android/data/ph.scamguardian/files/models
   adb push embeddinggemma-2-740m.litertlm /sdcard/Android/data/ph.scamguardian/files/models/embeddinggemma-2-740m.litertlm
   ```

Gradle needs a JDK to start. If `java` is not on your PATH, use the one bundled with Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Commands

| Task | Command |
| --- | --- |
| Build | `./gradlew assembleDebug` |
| Install | `./gradlew installDebug` |
| Test | `./gradlew testDebugUnitTest` |
| Lint | `./gradlew lintDebug detekt` |
| Format | `./gradlew spotlessApply` |
| Full check | `./gradlew spotlessCheck detekt lintDebug testDebugUnitTest` |

## Project structure

```
app/                                  Android app module (:app)
  src/main/java/ph/scamguardian/      Activity, navigation, theme, UI
  src/main/java/ph/scamguardian/core/ Business logic, no Android imports
  src/test/java/ph/scamguardian/core/ Unit tests and fakes
data/                                 Brand catalog, packaged as an app asset
config/detekt/                        detekt configuration
gradle/libs.versions.toml             Pinned versions
.agents/skills/                       Android agent skills (universal)
.claude/skills/                       Android agent skills (Claude)
.github/workflows/                    CI
```

## Agent rules

See AGENTS.md.
