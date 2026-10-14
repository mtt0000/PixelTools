# PixelXpert (Minimal Modded Fork for Android 17 / Pixel Launcher)

This repository is a minimal, clean, and modded fork of the original [PixelXpert/AOSPMods](https://github.com/siavash79/PixelXpert) project. It has been specifically redesigned for newer versions of Android (Android 17, API 36) and cleaned up from old injections and obsolete checks.

## Features & Changes

This minimal mod focuses on performance, stability, and safety for Android 17 (API 36). The following changes have been implemented compared to the original project:

- **Zero Aggressive Root (Safety):** Removed invasive SQLite settings that automatically assigned root privileges to the application or inserted it into the LSPosed database without the user's knowledge. Module activation and permission granting remain under the user's manual control.
- **Super Minimal Module:** Removed all non-essential Settings, Fragments, and Mods code to reduce weight and memory footprint.
- **Target Android 17 (API 36):** Recompiled and modified module properties and `build.gradle.kts` to target the new API level 36.
- **Robust and Safe Hooks:** Critical Xposed hooks in Launcher and SystemUI (like inserting the "Clear All" button in the Recents View of the *Pixel Launcher*) have been wrapped with comprehensive try/catch blocks. This prevents SystemUI and Launcher bootloops if system bytecodes no longer perfectly match in future Android 17 QPR updates.
- **Integrated Gesture Script:** Back Gesture scales are safely zeroed out `(0.0)` at startup from the boot service (without complicated hacks).

## How to Build the Magisk/KernelSU Package

A ready-to-use script is provided to safely compile and package the module:

```bash
./build_minimal.sh
```

The script will generate two files in the project's root folder:
- `PixelXpert-Minimal.zip` (The Magisk/KernelSU module)
- `PixelXpert-Minimal.apk` (The app to install manually)

## Installation Tutorial

1. Install **KernelSU** or **Magisk** on your device.
2. Ensure you have **LSPosed** (Zygisk version preferred) installed.
3. Flash the generated `PixelXpert-Minimal.zip` file using Magisk or KernelSU.
4. Install the generated `PixelXpert-Minimal.apk` on your device like a normal app.
5. Reboot your device.
6. Open LSPosed Manager and **manually enable the module**.
7. Make sure that **Pixel Launcher** and **System UI** are checked in the module's Scope list.
8. Reboot the device once more to apply the hooks.

## Usage

Because this is a minimal mod, there is no standalone app with complex settings UI to manage.
The modifications are automatically applied once the module is enabled in LSPosed and the target scopes (Pixel Launcher, System UI) are checked. The changes, such as the "Clear All" button in Recents and adjusted gesture scales, will be active right after the reboot.

## Original Credits & Acknowledgments

Thanks to the original AOSPMods / PixelXpert team (@siavash79 and @ElTifo) for their massive foundational work on this project. This is a fork built upon their incredible efforts.
