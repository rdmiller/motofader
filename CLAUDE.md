# CLAUDE.md — MotoFader

## What is this project?

MotoFader is an Android audio mixer app with real console-style fader controls, VU meters, and a 1/3-octave spectral analyzer. It controls Android audio stream volumes (Music, Ring, Notification, Alarm, System, Voice, Accessibility, DTMF) through a professional mixing console UI.

Uses Android's `Visualizer` API (session 0 = global output mix) for real-time VU metering and FFT-based spectrum analysis. Does **not** require root.

## Build & Run

### Toolchain (pinned — same as MotoMix, do not upgrade)

| Component | Version |
|-----------|---------|
| Gradle | **8.5** |
| AGP | **8.2.2** |
| Kotlin | **1.9.22** |
| Compose Compiler | **1.5.10** |
| Compose BOM | **2024.02.00** |
| JDK | **JBR 21** (Android Studio embedded) |

### Building

```bash
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="C:/Users/Rob_M/AppData/Local/Android/Sdk"
cd C:/Users/Rob_M/repos/motofader
bash gradlew assembleDebug
```

### Installing

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant RECORD_AUDIO permission when prompted (required for Visualizer / VU meters / spectrum).

### Device

- **Motorola One Zoom** (parker), serial `ZY227FWVG4`
- ADB: `adb -s ZY227FWVG4`

## Architecture

MVVM with StateFlow, single-Activity Compose app (same pattern as MotoMix).

### Key classes

| Class | Role |
|-------|------|
| `AudioCaptureManager` | Wraps `Visualizer` API. Captures waveform (for VU RMS/peak) and FFT (for 31-band 1/3-octave spectrum). Peak hold with decay. |
| `StreamVolumeManager` | Wraps `AudioManager`. Reads/writes stream volumes, observes changes via `ContentObserver`. Mute/unmute with volume restore. |
| `MixerViewModel` | `AndroidViewModel`. Combines audio capture and volume managers. Exposes all state as `StateFlow`. |
| `AudioChannel` | Data class: `streamType`, `name`, `shortName`, `volume`, `maxVolume`, `isMuted`. |

### Permissions

- `RECORD_AUDIO` — required for `Visualizer` (runtime permission, requested on launch)
- `MODIFY_AUDIO_SETTINGS` — required for `Visualizer` (normal, auto-granted)

## UI

### Mix tab
- Horizontally scrollable channel strips (one per Android audio stream)
- Each strip: channel label, vertical fader with dB tick marks, volume readout, mute button
- Fixed master section on right: L/R VU meters with peak dB readout

### Spectrum tab
- L/R VU meters with peak and RMS readouts
- Full 31-band 1/3-octave real-time analyzer (ISO center frequencies 20Hz–20kHz)
- Peak hold markers with 1.5s hold + decay
- dB grid lines with labels

### Fader design
- Canvas-drawn console-style fader with track groove, 3D gradient cap, center notch line, dB tick marks
- Touch + drag interaction with discrete step snapping (matches AudioManager volume steps)

### Color theme
Same as MotoMix: dark background (#121212), amber (#FFB300) primary, cyan (#00BCD4) secondary.

## Testing changes

```bash
bash gradlew assembleDebug && \
adb -s ZY227FWVG4 shell "am force-stop com.motofader" && \
adb -s ZY227FWVG4 install -r app/build/outputs/apk/debug/app-debug.apk && \
adb -s ZY227FWVG4 shell "am start -n com.motofader/.MainActivity"
```
