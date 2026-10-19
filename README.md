# Morse Toolkit

[![Build](https://github.com/swooningfish/Morse-Toolkit/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/swooningfish/Morse-Toolkit/actions/workflows/build.yml)

Learn Morse code on your phone. Tap it out, hear it, and work through levels at your own pace.
Works on Android and iPhone.

## What you can do

**Tap out Morse and see what you sent.** Use the key on the first screen and your dits and dahs
turn into letters as you go. You can also type a message and have the phone send it back to you
as sound, a flashing screen, or the camera torch.

**Learn step by step.** The Learn tab starts with two letters and adds one more each level.
Get 90% and the next level unlocks. Once you know the characters, it moves on to prosigns,
Q-codes, abbreviations, callsigns, words, phrases and full sentences.

You can choose how the letters are introduced:

- **Koch**: the method most radio operators learn with
- **A to Z**: plain alphabetical order
- **Easiest first**: short patterns like E, T and I before the longer ones

Struggling with a few characters? Use **Custom practice** to pick just those.

**Practise anything.** Pick a category and choose whether to listen (the app plays it and you
answer), send (you tap it out), or a mix of both.

**Look things up.** The Codes tab lists every letter, number, prosign, Q-code and abbreviation
with its meaning. Tap one to hear it.

## Pick the key that suits you

- **Straight key**: one big button. A quick tap is a dit, holding it is a dah.
- **Dit + Dah**: two buttons, one for each. The easiest way to start.
- **Paddles**: hold one side to repeat it, or squeeze both to alternate, just like an iambic paddle.

You can switch between them at any time from the Key screen or in Settings.

## Tips

- If your dahs keep showing up as dits, or letters run together, turn down
  **Your keying speed** in Settings.
- Learning by ear is easier when letters play at full speed with bigger gaps between them.
  Leave **Letter speed** around 20 WPM and lower **Overall speed** instead.
- Turn on **Light → Torch** and you can signal across a room or a field.

## Getting the app

There's no app store release yet.

**Android**: open the latest successful run on the
[Actions page](https://github.com/swooningfish/Morse-Toolkit/actions/workflows/build.yml),
download `morse-toolkit-debug-apk` from the Artifacts section, unzip it and install the APK.
Your phone may ask you to allow installs from unknown sources.

**iPhone**: for now you need a Mac with Xcode to build it yourself (see below).

## Building it yourself

The app is written in Kotlin with Compose Multiplatform, so Android and iOS share the same code.
You'll need JDK 17 or newer.

Android: open the project in Android Studio and run `composeApp`, or from the command line:

```bash
./gradlew :composeApp:assembleDebug
```

There's no Gradle wrapper checked in yet. If `./gradlew` is missing, run
`gradle wrapper --gradle-version 8.11.1` once, or just open the project in Android Studio.

iPhone (on a Mac):

```bash
brew install xcodegen
```

```bash
cd iosApp && xcodegen && open iosApp.xcodeproj
```

Choose your signing team in Xcode and press Run.

Every push to GitHub builds both versions automatically.
