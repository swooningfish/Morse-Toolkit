# Morse Toolkit

A Morse code trainer for Android and iOS, written in Kotlin with Compose Multiplatform.
All the app code is shared between both platforms.

## What it does

| Tab | Purpose |
| --- | --- |
| **Key** | Tap out dits and dahs and see them turn into letters. Or type text and send it as Morse with sound and light. |
| **Learn** | Levels that teach characters a few at a time, then prosigns, Q-codes, abbreviations, callsigns, words, phrases and sentences. Score 90% to unlock the next level. Pick the learning order: **Koch**, **A to Z** or **Easiest first**. Also has a **Custom** mode for choosing your own characters. |
| **Practice** | Free practice in any category: letters, numbers, punctuation, prosigns, Q-codes, abbreviations, callsigns, words, phrases, sentences. Choose **Listen** (hear it and answer), **Send** (tap it out) or **Both**. |
| **Codes** | Reference list of every code with its meaning and a play button. |
| **Settings** | Theme (system / light / dark), key type, speeds, tone pitch, sound, and light (screen flash or camera torch). |

**Key types**: straight key (short press = dit, long press = dah), separate Dit + Dah buttons, or iambic paddles (hold to repeat, squeeze both to alternate).

**Timing**: playback uses Farnsworth timing. Letters play at the *letter speed* with longer gaps between them so the *overall speed* is slower, which is the standard way to learn by ear.

## Project layout

```
composeApp/src/commonMain   shared code (all UI and logic)
  morse/                    code table, keyer, player, content, levels, drills
  data/AppSettings.kt       saved settings and progress
  ui/                       screens
composeApp/src/androidMain  Android tone (AudioTrack), torch, MainActivity
composeApp/src/iosMain      iOS tone (AVAudioEngine), torch, view controller
iosApp/                     iOS app shell (Xcode project is generated with XcodeGen)
.github/workflows/build.yml CI that builds the Android APK and the iOS simulator app
```

## Building

You need JDK 17 or newer. This repo has no Gradle wrapper jar yet. Create one once with a
locally installed Gradle (`gradle wrapper --gradle-version 8.11.1`), or open the project in
Android Studio, which sets it up for you.

### Android

Open the folder in Android Studio (with the Kotlin Multiplatform plugin) and run `composeApp`, or:

```bash
./gradlew :composeApp:assembleDebug
```

The APK is written to `composeApp/build/outputs/apk/debug/`.

### iOS (needs a Mac with Xcode)

```bash
brew install xcodegen
```

```bash
cd iosApp && xcodegen && open iosApp.xcodeproj
```

Set your signing team in Xcode, then run on a simulator or device. The Xcode build phase
compiles the Kotlin framework for you.

### CI

Pushing to GitHub runs `.github/workflows/build.yml`. It builds the Android debug APK
(downloadable from the run's artifacts) and compiles the iOS app for the simulator on a macOS runner.
