# MotoFader

Android audio mixer app with real console-style fader controls, VU meters, and a 31-band 1/3-octave spectral analyzer.

Controls Android audio stream volumes (Music, Ring, Notification, Alarm, System, Voice, Accessibility, DTMF) through a professional mixing console UI. Uses Android's `Visualizer` API for real-time VU metering and FFT-based spectrum analysis. Does not require root.

## Features

### Mix View
- Horizontally scrollable channel strips — one per Android audio stream
- Each strip: channel label, vertical fader with dB tick marks, volume readout, mute button
- Fixed master section with L/R VU meters and peak dB readout

### Spectrum Analyzer
- L/R VU meters with peak and RMS readouts
- Full 31-band 1/3-octave real-time analyzer (ISO center frequencies 20 Hz – 20 kHz)
- Peak hold markers with hold + decay
- dB grid lines with labels

### Fader Design
- Canvas-drawn console-style fader with track groove, 3D gradient cap, center notch line, dB tick marks
- Touch + drag interaction with discrete step snapping matching Android volume steps

## Architecture

MVVM with StateFlow, single-Activity Jetpack Compose app.

| Class | Role |
|-------|------|
| `AudioCaptureManager` | Wraps `Visualizer` API — waveform capture for VU RMS/peak and FFT for 31-band spectrum |
| `StreamVolumeManager` | Wraps `AudioManager` — reads/writes stream volumes, observes changes via `ContentObserver` |
| `MixerViewModel` | Combines capture and volume managers, exposes all state as `StateFlow` |
| `AudioChannel` | Data class: stream type, name, volume, max volume, mute state |

## Build

Requires Android SDK and JDK 17+.

```bash
./gradlew assembleDebug
```

Install to a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

- **RECORD_AUDIO** — required for `Visualizer` (runtime permission, requested on launch)
- **MODIFY_AUDIO_SETTINGS** — required for `Visualizer` (normal, auto-granted)

## Toolchain

| Component | Version |
|-----------|---------|
| Gradle | 8.5 |
| AGP | 8.2.2 |
| Kotlin | 1.9.22 |
| Compose Compiler | 1.5.10 |
| Compose BOM | 2024.02.00 |
| Min SDK | 26 |
| Target SDK | 34 |

## License

All rights reserved.
